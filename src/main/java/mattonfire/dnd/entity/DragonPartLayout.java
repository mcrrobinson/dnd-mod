package mattonfire.dnd.entity;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4d;
import org.joml.Vector3d;

/**
 * Splits a dragon's GeckoLib model into hittable parts, each covering a group of bones' cubes.
 * <p>
 * On the client, DragonRenderer fits each part to its cubes as animated. Everywhere else (the
 * server, and client dragons that aren't being rendered) the parts are posed from the .geo.json and,
 * for layouts with {@link #animations}, from the .animation.json at the same animation time the
 * client plays it (see DragonAnimationController), so server hits follow the flapping wings. Layouts
 * without animations use the rest pose, shifted by how far the animations move the root bone.
 */
public final class DragonPartLayout {
    private record Group(String name, String[] bones) {
    }

    /** A cube in bone space: its box (blocks, x mirrored like GeckoLib bakes it) and its own rotation. */
    private record CubeDef(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
                           double pivotX, double pivotY, double pivotZ, double rotX, double rotY, double rotZ) {
    }

    /** A bone's rest values, in the units GeckoLib uses (pivot in pixels with x flipped, rotation in radians). */
    private record BoneDef(String name, int parent, double pivotX, double pivotY, double pivotZ,
                           double rotX, double rotY, double rotZ, List<CubeDef> cubes) {
    }

    /**
     * Each group's cubes in GeckoLib model space, after every bone and cube transform, as
     * {corner, x edge, y edge, z edge}. Part shapes are rebuilt only when the pose object changes.
     */
    public record Pose(List<List<double[]>> groups) {
    }

    /** The skeleton (parents before children), the rest pose and how far it reaches from the origin. */
    private record Model(List<BoneDef> bones, Pose rest, double reach) {
    }

    /** The pose last computed for an animation, shared by every dragon with this layout at that time. */
    private record CachedPose(double phase, Pose pose) {
    }

    private final String modelName;
    private final double groundDrop;
    private final double flyingDrop;
    private final List<Group> groups = new ArrayList<>();
    private String[] animationNames = new String[0];

    // Loaded on first use by the server and the client threads alike; both load the same data.
    private volatile Model model;
    private volatile DragonAnimations animations;
    private final Map<String, CachedPose> poseCache = new ConcurrentHashMap<>();

    /**
     * @param modelName  geo (and animation) file under assets/dndclasses/, without .geo.json
     * @param groundDrop root ("dragon") bone y offset in blocks in the ground animations, for unanimated parts
     * @param flyingDrop the same in the flight animations
     */
    public DragonPartLayout(String modelName, double groundDrop, double flyingDrop) {
        this.modelName = modelName;
        this.groundDrop = groundDrop;
        this.flyingDrop = flyingDrop;
    }

    public DragonPartLayout part(String name, String... bones) {
        this.groups.add(new Group(name, bones));
        return this;
    }

    /** The same part on both sides; "_left" in bone names becomes "_right" for the mirrored part. */
    public DragonPartLayout pair(String name, String... leftBones) {
        String[] rightBones = new String[leftBones.length];
        for (int i = 0; i < leftBones.length; i++) {
            rightBones[i] = leftBones[i].replace("_left", "_right");
        }
        this.groups.add(new Group(name + "_left", leftBones));
        this.groups.add(new Group(name + "_right", rightBones));
        return this;
    }

    /** Animations (names in the .animation.json) the parts can follow outside the renderer. */
    public DragonPartLayout animations(String... names) {
        this.animationNames = names;
        return this;
    }

    public int size() {
        return this.groups.size();
    }

    public String[] bones(int part) {
        return this.groups.get(part).bones();
    }

    /** The part created for the group called {@code name} */
    public DragonPart part(DragonPart[] parts, String name) {
        for (int i = 0; i < this.groups.size(); i++) {
            if (this.groups.get(i).name().equals(name)) {
                return parts[i];
            }
        }
        throw new IllegalArgumentException("No dragon part " + name + " in " + this.modelName);
    }

    public DragonPart[] createParts(MobEntity owner) {
        DragonPart[] parts = new DragonPart[this.groups.size()];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = new DragonPart(owner, this.groups.get(i).name());
        }
        return parts;
    }

    /** Places parts at the rest pose (see {@link #update(MobEntity, DragonPart[], String, boolean)}). */
    public void update(MobEntity owner, DragonPart[] parts, boolean flying) {
        this.update(owner, parts, null, flying);
    }

    /**
     * Places parts in {@code animation} at the current world time, or at the rest pose if the layout
     * doesn't have that animation; client parts that the renderer placed recently are left alone. A
     * part keeps its shapes while the pose and the dragon's facing stay the same, and is shifted
     * rather than rebuilt when the dragon only moved.
     */
    public void update(MobEntity owner, DragonPart[] parts, @Nullable String animation, boolean flying) {
        long time = owner.world.getTime();
        Pose pose = animation == null ? null : this.animatedPose(animation, time);
        double drop = 0.0;
        if (pose == null) {
            pose = this.getModel().rest();
            drop = flying ? this.flyingDrop : this.groundDrop;
        }
        double x = owner.getX(), y = owner.getY() + drop, z = owner.getZ();
        float yaw = owner.bodyYaw;
        // GeckoLib renders entities rotated by (180 - body yaw) around Y
        float angle = (180.0F - yaw) * MathHelper.RADIANS_PER_DEGREE;
        double cos = MathHelper.cos(angle);
        double sin = MathHelper.sin(angle);
        for (int i = 0; i < parts.length; i++) {
            DragonPart part = parts[i];
            if (owner.world.isClient && part.hasRenderedShapes(time)) {
                continue;
            }
            if (part.hasPoseShapes(pose, yaw)) {
                if (!part.isPosedAt(x, y, z)) {
                    part.movePoseShapes(x, y, z);
                }
                continue;
            }
            List<double[]> cubes = pose.groups().get(i);
            List<Box> shapes = new ArrayList<>(cubes.size() * 8);
            for (double[] c : cubes) {
                // c = corner, then the three edge vectors, in model space (blocks)
                DragonPart.addCube(shapes,
                        x + c[0] * cos + c[2] * sin, y + c[1], z - c[0] * sin + c[2] * cos,
                        c[3] * cos + c[5] * sin, c[4], -c[3] * sin + c[5] * cos,
                        c[6] * cos + c[8] * sin, c[7], -c[6] * sin + c[8] * cos,
                        c[9] * cos + c[11] * sin, c[10], -c[9] * sin + c[11] * cos);
            }
            part.setPoseShapes(shapes, pose, x, y, z, yaw);
        }
    }

    /** The animation time (ticks) {@code animation} is at for world time {@code time}, or -1 if the layout lacks it. */
    public double phase(String animation, double time) {
        DragonAnimations.Animation anim = this.getAnimations().get(animation);
        return anim == null ? -1 : anim.phase(time);
    }

    @Nullable
    private Pose animatedPose(String animation, long time) {
        DragonAnimations.Animation anim = this.getAnimations().get(animation);
        if (anim == null) {
            return null;
        }
        double phase = anim.phase(time);
        CachedPose cached = this.poseCache.get(animation);
        if (cached != null && cached.phase() == phase) {
            return cached.pose();
        }
        Pose pose = computePose(this.getModel().bones(), this.groups, anim, phase);
        this.poseCache.put(animation, new CachedPose(phase, pose));
        return pose;
    }

    /**
     * How far (in blocks) any part can reach from the dragon's position: the rest pose's furthest
     * corner, with room for animations that swing wings and tail further out.
     */
    public double getReach() {
        return this.getModel().reach() * 1.5 + 2.0;
    }

    private DragonAnimations getAnimations() {
        DragonAnimations anims = this.animations;
        if (anims == null) {
            anims = DragonAnimations.load(this.modelName, this.animationNames);
            this.animations = anims;
        }
        return anims;
    }

    private Model getModel() {
        Model loaded = this.model;
        if (loaded == null) {
            loaded = this.loadModel();
            this.model = loaded;
        }
        return loaded;
    }

    // ------------------------------------------------------------------ posing

    /**
     * Poses every group's cubes: each bone's transform is its parent's, then GeckoLib's
     * RenderUtils.prepMatrixForBone (animated offset, pivot, rotation Z-Y-X, scale, back off the
     * pivot), then the cube's own rotation about its pivot.
     */
    private static Pose computePose(List<BoneDef> bones, List<Group> groupList, @Nullable DragonAnimations.Animation animation, double phase) {
        Map<String, Matrix4d> matrices = new HashMap<>();
        double[] rot = new double[3];
        double[] pos = new double[3];
        double[] scale = new double[3];
        for (BoneDef bone : bones) {
            Matrix4d matrix = bone.parent() < 0 ? new Matrix4d() : new Matrix4d(matrices.get(bones.get(bone.parent()).name()));
            double rx = bone.rotX(), ry = bone.rotY(), rz = bone.rotZ();
            double px = 0, py = 0, pz = 0, sx = 1, sy = 1, sz = 1;
            DragonAnimations.BoneAnimation anim = animation == null ? null : animation.bones().get(bone.name());
            if (anim != null) {
                if (anim.sample(anim.rotation(), phase, rot)) {
                    rx += rot[0];
                    ry += rot[1];
                    rz += rot[2];
                }
                if (anim.sample(anim.position(), phase, pos)) {
                    px = pos[0];
                    py = pos[1];
                    pz = pos[2];
                }
                if (anim.sample(anim.scale(), phase, scale)) {
                    sx = scale[0];
                    sy = scale[1];
                    sz = scale[2];
                }
            }
            double pivotX = bone.pivotX() / 16, pivotY = bone.pivotY() / 16, pivotZ = bone.pivotZ() / 16;
            matrix.translate(-px / 16, py / 16, pz / 16)
                    .translate(pivotX, pivotY, pivotZ)
                    .rotateZ(rz).rotateY(ry).rotateX(rx)
                    .scale(sx, sy, sz)
                    .translate(-pivotX, -pivotY, -pivotZ);
            matrices.put(bone.name(), matrix);
        }

        Map<String, BoneDef> byName = new HashMap<>();
        for (BoneDef bone : bones) {
            byName.put(bone.name(), bone);
        }
        List<List<double[]>> groups = new ArrayList<>();
        for (Group group : groupList) {
            List<double[]> cubes = new ArrayList<>();
            for (String boneName : group.bones()) {
                BoneDef bone = byName.get(boneName);
                if (bone == null) {
                    continue;
                }
                Matrix4d boneMatrix = matrices.get(boneName);
                for (CubeDef cube : bone.cubes()) {
                    cubes.add(poseCube(cube, boneMatrix));
                }
            }
            groups.add(cubes);
        }
        return new Pose(groups);
    }

    private static double[] poseCube(CubeDef cube, Matrix4d boneMatrix) {
        Matrix4d matrix = new Matrix4d(boneMatrix);
        if (cube.rotX() != 0 || cube.rotY() != 0 || cube.rotZ() != 0) {
            matrix.translate(cube.pivotX(), cube.pivotY(), cube.pivotZ())
                    .rotateZ(cube.rotZ()).rotateY(cube.rotY()).rotateX(cube.rotX())
                    .translate(-cube.pivotX(), -cube.pivotY(), -cube.pivotZ());
        }
        Vector3d o = matrix.transformPosition(cube.minX(), cube.minY(), cube.minZ(), new Vector3d());
        Vector3d ex = matrix.transformPosition(cube.maxX(), cube.minY(), cube.minZ(), new Vector3d()).sub(o);
        Vector3d ey = matrix.transformPosition(cube.minX(), cube.maxY(), cube.minZ(), new Vector3d()).sub(o);
        Vector3d ez = matrix.transformPosition(cube.minX(), cube.minY(), cube.maxZ(), new Vector3d()).sub(o);
        return new double[]{o.x, o.y, o.z, ex.x, ex.y, ex.z, ey.x, ey.y, ey.z, ez.x, ez.y, ez.z};
    }

    // ------------------------------------------------------------------ loading

    private Model loadModel() {
        List<BoneDef> bones = new ArrayList<>();
        String path = "assets/" + DnDClasses.MOD_ID + "/geo/" + this.modelName + ".geo.json";
        Optional<Path> file = FabricLoader.getInstance().getModContainer(DnDClasses.MOD_ID).flatMap(mod -> mod.findPath(path));
        if (file.isPresent()) {
            try (Reader reader = Files.newBufferedReader(file.get())) {
                JsonObject geometry = JsonParser.parseReader(reader).getAsJsonObject()
                        .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
                Map<String, JsonObject> json = new HashMap<>();
                List<String> order = new ArrayList<>();
                for (JsonElement boneElement : geometry.getAsJsonArray("bones")) {
                    JsonObject bone = boneElement.getAsJsonObject();
                    json.put(bone.get("name").getAsString(), bone);
                    order.add(bone.get("name").getAsString());
                }
                Map<String, Integer> index = new HashMap<>();
                for (String name : order) {
                    addBone(name, json, index, bones);
                }
            } catch (Exception e) {
                DnDClasses.LOGGER.error("Couldn't read dragon model {}", path, e);
            }
        } else {
            DnDClasses.LOGGER.error("Missing dragon model {}", path);
        }

        Map<String, BoneDef> byName = new HashMap<>();
        for (BoneDef bone : bones) {
            byName.put(bone.name(), bone);
        }
        for (Group group : this.groups) {
            boolean any = false;
            for (String boneName : group.bones()) {
                BoneDef bone = byName.get(boneName);
                any |= bone != null && !bone.cubes().isEmpty();
            }
            if (!any) {
                DnDClasses.LOGGER.warn("Dragon part {} in {} matches no bones with cubes", group.name(), this.modelName);
            }
        }

        Pose rest = computePose(bones, this.groups, null, 0.0);
        double reach = 0.0;
        for (List<double[]> group : rest.groups()) {
            for (double[] c : group) {
                for (int corner = 0; corner < 8; corner++) {
                    double px = c[0], py = c[1], pz = c[2];
                    for (int edge = 0; edge < 3; edge++) {
                        if ((corner & (1 << edge)) != 0) {
                            px += c[3 + edge * 3];
                            py += c[4 + edge * 3];
                            pz += c[5 + edge * 3];
                        }
                    }
                    reach = Math.max(reach, Math.sqrt(px * px + py * py + pz * pz));
                }
            }
        }
        return new Model(bones, rest, reach);
    }

    /** Adds a bone after its parents, with GeckoLib's BakedModelFactory conversions. */
    private static int addBone(String name, Map<String, JsonObject> json, Map<String, Integer> index, List<BoneDef> bones) {
        Integer existing = index.get(name);
        if (existing != null) {
            return existing;
        }
        JsonObject bone = json.get(name);
        int parent = -1;
        if (bone.has("parent") && json.containsKey(bone.get("parent").getAsString())) {
            parent = addBone(bone.get("parent").getAsString(), json, index, bones);
        }
        double[] pivot = triple(bone, "pivot");
        double[] rotation = triple(bone, "rotation");
        List<CubeDef> cubes = new ArrayList<>();
        if (bone.has("cubes")) {
            for (JsonElement cubeElement : bone.getAsJsonArray("cubes")) {
                cubes.add(readCube(cubeElement.getAsJsonObject()));
            }
        }
        bones.add(new BoneDef(name, parent, -pivot[0], pivot[1], pivot[2],
                Math.toRadians(-rotation[0]), Math.toRadians(-rotation[1]), Math.toRadians(rotation[2]), cubes));
        index.put(name, bones.size() - 1);
        return bones.size() - 1;
    }

    private static CubeDef readCube(JsonObject cube) {
        double[] origin = triple(cube, "origin");
        double[] size = triple(cube, "size");
        double[] pivot = triple(cube, "pivot");
        double[] rotation = triple(cube, "rotation");
        double inflate = cube.has("inflate") ? cube.get("inflate").getAsDouble() : 0.0;
        // Same conversion as GeckoLib's BakedModelFactory: pixels to blocks, x mirrored
        return new CubeDef(
                (-(origin[0] + size[0]) - inflate) / 16, (origin[1] - inflate) / 16, (origin[2] - inflate) / 16,
                (-origin[0] + inflate) / 16, (origin[1] + size[1] + inflate) / 16, (origin[2] + size[2] + inflate) / 16,
                -pivot[0] / 16, pivot[1] / 16, pivot[2] / 16,
                Math.toRadians(-rotation[0]), Math.toRadians(-rotation[1]), Math.toRadians(rotation[2]));
    }

    private static double[] triple(JsonObject object, String key) {
        if (!object.has(key)) {
            return new double[3];
        }
        JsonArray array = object.getAsJsonArray(key);
        return new double[]{array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble()};
    }

    /** Reserves consecutive entity ids so part ids are (owner id + i + 1) on server and client alike. */
    public static int reserveIds(DragonPart[] parts) {
        return Entity.CURRENT_ID.getAndAdd(parts.length + 1) + 1;
    }

    public static void assignIds(DragonPart[] parts, int ownerId) {
        if (parts == null) {
            return;
        }
        for (int i = 0; i < parts.length; i++) {
            parts[i].setId(ownerId + i + 1);
        }
    }
}

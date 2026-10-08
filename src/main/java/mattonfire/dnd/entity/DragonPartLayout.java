package mattonfire.dnd.entity;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import org.joml.Matrix4d;
import org.joml.Vector3d;

/**
 * Splits a dragon's GeckoLib model into hittable parts, each covering a group of bones' cubes.
 * <p>
 * On the client, DragonRenderer fits each part to its cubes as animated every frame. The server
 * has no animations, so it (and the client, for dragons not being rendered) falls back to the
 * cubes' rest pose from the .geo.json, shifted by how far the animations move the root bone.
 */
public final class DragonPartLayout {
    private record Group(String name, String[] bones) {
    }

    private final String modelName;
    private final double groundDrop;
    private final double flyingDrop;
    private final List<Group> groups = new ArrayList<>();
    /**
     * Each group's rest-pose cubes in GeckoLib model space (blocks, x mirrored like GeckoLib bakes it),
     * rotated by their bones and their own rotations, as {corner, x edge, y edge, z edge}.
     */
    private record RestPose(List<List<double[]>> groups, double reach) {
    }

    // Loaded on first use by the server and the client render thread alike; both load the same data.
    private volatile RestPose restPose;

    /**
     * @param modelName  geo file under assets/dndclasses/geo/, without .geo.json
     * @param groundDrop root ("dragon") bone y offset in blocks in the ground animations
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

    /**
     * Places parts at the rest pose, except client parts that the renderer has placed recently. A part
     * whose dragon hasn't moved or turned since its last update keeps its shapes, and one that only
     * moved is shifted rather than rebuilt.
     */
    public void update(MobEntity owner, DragonPart[] parts, boolean flying) {
        RestPose rest = this.getRestPose();
        long time = owner.world.getTime();
        double x = owner.getX(), y = owner.getY(), z = owner.getZ();
        float yaw = owner.bodyYaw;
        // GeckoLib renders entities rotated by (180 - body yaw) around Y
        float angle = (180.0F - yaw) * MathHelper.RADIANS_PER_DEGREE;
        double cos = MathHelper.cos(angle);
        double sin = MathHelper.sin(angle);
        double drop = flying ? this.flyingDrop : this.groundDrop;
        for (int i = 0; i < parts.length; i++) {
            DragonPart part = parts[i];
            if (owner.world.isClient && part.hasRenderedShapes(time)) {
                continue;
            }
            if (part.hasRestShapes(yaw, flying)) {
                if (!part.isRestAt(x, y, z)) {
                    part.moveRestShapes(x, y, z);
                }
                continue;
            }
            List<double[]> cubes = rest.groups.get(i);
            List<Box> shapes = new ArrayList<>(cubes.size() * 8);
            for (double[] c : cubes) {
                // c = corner, then the three edge vectors, in model space (blocks)
                DragonPart.addCube(shapes,
                        x + c[0] * cos + c[2] * sin, y + c[1] + drop, z - c[0] * sin + c[2] * cos,
                        c[3] * cos + c[5] * sin, c[4], -c[3] * sin + c[5] * cos,
                        c[6] * cos + c[8] * sin, c[7], -c[6] * sin + c[8] * cos,
                        c[9] * cos + c[11] * sin, c[10], -c[9] * sin + c[11] * cos);
            }
            part.setRestShapes(shapes, x, y, z, yaw, flying);
        }
    }

    /**
     * How far (in blocks) any part can reach from the dragon's position: the rest pose's furthest
     * corner, with room for animations that swing wings and tail further out.
     */
    public double getReach() {
        return this.getRestPose().reach * 1.5 + 2.0;
    }

    private RestPose getRestPose() {
        RestPose pose = this.restPose;
        if (pose == null) {
            pose = this.loadRestPose();
            this.restPose = pose;
        }
        return pose;
    }

    private RestPose loadRestPose() {
        Map<String, List<double[]>> boneCubes = new HashMap<>();
        String path = "assets/" + DnDClasses.MOD_ID + "/geo/" + this.modelName + ".geo.json";
        Optional<Path> file = FabricLoader.getInstance().getModContainer(DnDClasses.MOD_ID).flatMap(mod -> mod.findPath(path));
        if (file.isPresent()) {
            try (Reader reader = Files.newBufferedReader(file.get())) {
                JsonObject geometry = JsonParser.parseReader(reader).getAsJsonObject()
                        .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
                Map<String, JsonObject> bones = new HashMap<>();
                for (JsonElement boneElement : geometry.getAsJsonArray("bones")) {
                    JsonObject bone = boneElement.getAsJsonObject();
                    bones.put(bone.get("name").getAsString(), bone);
                }
                Map<String, Matrix4d> boneMatrices = new HashMap<>();
                for (JsonObject bone : bones.values()) {
                    if (!bone.has("cubes")) {
                        continue;
                    }
                    Matrix4d boneMatrix = boneMatrix(bone, bones, boneMatrices);
                    List<double[]> cubes = new ArrayList<>();
                    for (JsonElement cubeElement : bone.getAsJsonArray("cubes")) {
                        cubes.add(restCube(cubeElement.getAsJsonObject(), boneMatrix));
                    }
                    boneCubes.put(bone.get("name").getAsString(), cubes);
                }
            } catch (Exception e) {
                DnDClasses.LOGGER.error("Couldn't read dragon model {}", path, e);
            }
        } else {
            DnDClasses.LOGGER.error("Missing dragon model {}", path);
        }

        List<List<double[]>> groups = new ArrayList<>();
        double reach = 0.0;
        for (Group group : this.groups) {
            List<double[]> groupCubes = new ArrayList<>();
            for (String boneName : group.bones()) {
                groupCubes.addAll(boneCubes.getOrDefault(boneName, List.of()));
            }
            if (groupCubes.isEmpty()) {
                DnDClasses.LOGGER.warn("Dragon part {} in {} matches no bones with cubes", group.name(), this.modelName);
            }
            for (double[] c : groupCubes) {
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
            groups.add(groupCubes);
        }
        return new RestPose(groups, reach);
    }

    /**
     * A bone's rest-pose transform in model space, parents first, built the way GeckoLib renders it
     * (BakedModelFactory for the axis flips, RenderUtils.prepMatrixForBone for the order).
     */
    private static Matrix4d boneMatrix(JsonObject bone, Map<String, JsonObject> bones, Map<String, Matrix4d> done) {
        String name = bone.get("name").getAsString();
        Matrix4d cached = done.get(name);
        if (cached != null) {
            return cached;
        }
        Matrix4d matrix = new Matrix4d();
        if (bone.has("parent") && bones.containsKey(bone.get("parent").getAsString())) {
            matrix.set(boneMatrix(bones.get(bone.get("parent").getAsString()), bones, done));
        }
        rotateAbout(matrix, bone);
        done.put(name, matrix);
        return matrix;
    }

    /** Applies a bone's or cube's rotation about its pivot, if it has one. */
    private static void rotateAbout(Matrix4d matrix, JsonObject element) {
        if (!element.has("rotation")) {
            return;
        }
        JsonArray rotation = element.getAsJsonArray("rotation");
        double rx = Math.toRadians(-rotation.get(0).getAsDouble());
        double ry = Math.toRadians(-rotation.get(1).getAsDouble());
        double rz = Math.toRadians(rotation.get(2).getAsDouble());
        if (rx == 0 && ry == 0 && rz == 0) {
            return;
        }
        double px = 0, py = 0, pz = 0;
        if (element.has("pivot")) {
            JsonArray pivot = element.getAsJsonArray("pivot");
            px = -pivot.get(0).getAsDouble() / 16;
            py = pivot.get(1).getAsDouble() / 16;
            pz = pivot.get(2).getAsDouble() / 16;
        }
        matrix.translate(px, py, pz).rotateZ(rz).rotateY(ry).rotateX(rx).translate(-px, -py, -pz);
    }

    /** A cube as its corner and three edge vectors in model space, after its own and its bones' rotations. */
    private static double[] restCube(JsonObject cube, Matrix4d boneMatrix) {
        JsonArray origin = cube.getAsJsonArray("origin");
        JsonArray size = cube.getAsJsonArray("size");
        double inflate = cube.has("inflate") ? cube.get("inflate").getAsDouble() : 0.0;
        double ox = origin.get(0).getAsDouble(), oy = origin.get(1).getAsDouble(), oz = origin.get(2).getAsDouble();
        double sx = size.get(0).getAsDouble(), sy = size.get(1).getAsDouble(), sz = size.get(2).getAsDouble();
        // Same conversion as GeckoLib's BakedModelFactory: pixels to blocks, x mirrored
        double minX = (-(ox + sx) - inflate) / 16, minY = (oy - inflate) / 16, minZ = (oz - inflate) / 16;
        double maxX = (-ox + inflate) / 16, maxY = (oy + sy + inflate) / 16, maxZ = (oz + sz + inflate) / 16;
        Matrix4d matrix = new Matrix4d(boneMatrix);
        rotateAbout(matrix, cube);
        Vector3d o = matrix.transformPosition(minX, minY, minZ, new Vector3d());
        Vector3d ex = matrix.transformPosition(maxX, minY, minZ, new Vector3d()).sub(o);
        Vector3d ey = matrix.transformPosition(minX, maxY, minZ, new Vector3d()).sub(o);
        Vector3d ez = matrix.transformPosition(minX, minY, maxZ, new Vector3d()).sub(o);
        return new double[]{o.x, o.y, o.z, ex.x, ex.y, ex.z, ey.x, ey.y, ey.z, ez.x, ez.y, ez.z};
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

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
import net.minecraft.util.math.Vec3d;

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
    // Rest-pose cubes of each group in GeckoLib model space (blocks, x mirrored like GeckoLib bakes it)
    private List<List<Box>> restCubes;

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

    /** Places parts at the rest pose, except client parts that the renderer has placed recently. */
    public void update(MobEntity owner, DragonPart[] parts, boolean flying) {
        List<List<Box>> rest = this.getRestCubes();
        long time = owner.world.getTime();
        // GeckoLib renders entities rotated by (180 - body yaw) around Y
        float angle = (180.0F - owner.bodyYaw) * MathHelper.RADIANS_PER_DEGREE;
        double cos = MathHelper.cos(angle);
        double sin = MathHelper.sin(angle);
        double drop = flying ? this.flyingDrop : this.groundDrop;
        for (int i = 0; i < parts.length; i++) {
            if (owner.world.isClient && parts[i].hasRenderedShapes(time)) {
                continue;
            }
            List<Box> shapes = new ArrayList<>();
            for (Box cube : rest.get(i)) {
                Vec3d o = new Vec3d(cube.minX * cos + cube.minZ * sin, cube.minY + drop, -cube.minX * sin + cube.minZ * cos)
                        .add(owner.getX(), owner.getY(), owner.getZ());
                Vec3d ex = new Vec3d(cube.getXLength() * cos, 0, -cube.getXLength() * sin);
                Vec3d ey = new Vec3d(0, cube.getYLength(), 0);
                Vec3d ez = new Vec3d(cube.getZLength() * sin, 0, cube.getZLength() * cos);
                DragonPart.addCube(shapes, o, ex, ey, ez);
            }
            parts[i].setShapes(shapes);
        }
    }

    private List<List<Box>> getRestCubes() {
        if (this.restCubes == null) {
            this.restCubes = this.loadRestCubes();
        }
        return this.restCubes;
    }

    private List<List<Box>> loadRestCubes() {
        Map<String, List<Box>> boneCubes = new HashMap<>();
        String path = "assets/" + DnDClasses.MOD_ID + "/geo/" + this.modelName + ".geo.json";
        Optional<Path> file = FabricLoader.getInstance().getModContainer(DnDClasses.MOD_ID).flatMap(mod -> mod.findPath(path));
        if (file.isPresent()) {
            try (Reader reader = Files.newBufferedReader(file.get())) {
                JsonObject geometry = JsonParser.parseReader(reader).getAsJsonObject()
                        .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
                for (JsonElement boneElement : geometry.getAsJsonArray("bones")) {
                    JsonObject bone = boneElement.getAsJsonObject();
                    if (!bone.has("cubes")) {
                        continue;
                    }
                    List<Box> cubes = new ArrayList<>();
                    for (JsonElement cubeElement : bone.getAsJsonArray("cubes")) {
                        JsonArray origin = cubeElement.getAsJsonObject().getAsJsonArray("origin");
                        JsonArray size = cubeElement.getAsJsonObject().getAsJsonArray("size");
                        double ox = origin.get(0).getAsDouble(), oy = origin.get(1).getAsDouble(), oz = origin.get(2).getAsDouble();
                        // Same conversion as GeckoLib's BakedModelFactory: pixels to blocks, x mirrored
                        cubes.add(new Box(-(ox + size.get(0).getAsDouble()) / 16, oy / 16, oz / 16,
                                -ox / 16, (oy + size.get(1).getAsDouble()) / 16, (oz + size.get(2).getAsDouble()) / 16));
                    }
                    boneCubes.put(bone.get("name").getAsString(), cubes);
                }
            } catch (Exception e) {
                DnDClasses.LOGGER.error("Couldn't read dragon model {}", path, e);
            }
        } else {
            DnDClasses.LOGGER.error("Missing dragon model {}", path);
        }

        List<List<Box>> cubes = new ArrayList<>();
        for (Group group : this.groups) {
            List<Box> groupCubes = new ArrayList<>();
            for (String boneName : group.bones()) {
                groupCubes.addAll(boneCubes.getOrDefault(boneName, List.of()));
            }
            if (groupCubes.isEmpty()) {
                DnDClasses.LOGGER.warn("Dragon part {} in {} matches no bones with cubes", group.name(), this.modelName);
            }
            cubes.add(groupCubes);
        }
        return cubes;
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

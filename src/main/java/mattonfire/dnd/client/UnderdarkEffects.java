package mattonfire.dnd.client;

import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import mattonfire.dnd.world.underdark.Underdark;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * How the Underdark looks from inside (its dimension type's {@code effects}, {@code dndclasses:underdark}): no sky,
 * no clouds, no sun dimming the fog, and the darkened lightmap of a sunless world. The fog is each biome's own colour.
 * (Thick mist over the Darklake comes with the flora ticket, T3.)
 */
@Environment(EnvType.CLIENT)
public class UnderdarkEffects extends DimensionEffects {
    public static final Identifier ID = new Identifier(DnDClasses.MOD_ID, "underdark");
    /** How far above the Darklake's water the mist reaches. */
    public static final int MIST_HEIGHT = 6;
    /** How far across from the camera's column the mist looks for water. */
    public static final int MIST_REACH = 3;
    private static boolean mist = false;
    private static BlockPos lastCamera = null;
    private static World lastWorld = null;
    private static boolean lastMisty = false;

    public UnderdarkEffects() {
        super(Float.NaN, false, SkyType.NONE, false, true);
    }

    public static void register() {
        DimensionRenderingRegistry.registerDimensionEffects(ID, new UnderdarkEffects());
    }

    @Override
    public Vec3d adjustFogColor(Vec3d color, float sunHeight) {
        return color;
    }

    @Override
    public boolean useThickFog(int camX, int camY) {
        // The arguments are only the camera's x and y; read its whole position.
        MinecraftClient client = MinecraftClient.getInstance();
        boolean misty = client.world != null && client.gameRenderer != null
                && isMisty(client.world, BlockPos.ofFloored(client.gameRenderer.getCamera().getPos()));
        if (misty != mist) {
            mist = misty;
            DnDClasses.LOGGER.info("[Underdark] Darklake mist {}", misty ? "on" : "off");
        }
        return misty;
    }

    /**
     * Whether {@code camera} is in the Darklake and water lies at most {@value #MIST_HEIGHT} open blocks below it, in
     * its own column or one up to {@value #MIST_REACH} blocks across (so standing on the shore counts).
     */
    public static boolean isMisty(World world, BlockPos camera) {
        if (camera.equals(lastCamera) && world == lastWorld) {
            return lastMisty;
        }
        lastCamera = camera.toImmutable();
        lastWorld = world;
        lastMisty = world.getBiome(camera).matchesKey(Underdark.DARKLAKE) && waterBelow(world, camera);
        return lastMisty;
    }

    private static boolean waterBelow(World world, BlockPos camera) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int dx = -MIST_REACH; dx <= MIST_REACH; dx++) {
            for (int dz = -MIST_REACH; dz <= MIST_REACH; dz++) {
                for (int dy = 0; dy <= MIST_HEIGHT; dy++) {
                    pos.set(camera.getX() + dx, camera.getY() - dy, camera.getZ() + dz);
                    BlockState state = world.getBlockState(pos);
                    if (state.getFluidState().isIn(FluidTags.WATER)) {
                        return true;
                    }
                    if (state.isOpaqueFullCube(world, pos)) {
                        break;
                    }
                }
            }
        }
        return false;
    }
}

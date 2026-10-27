package mattonfire.dnd.classes.mixin;

import mattonfire.dnd.world.underdark.Underdark;
import mattonfire.dnd.world.underdark.UnderdarkMapColors;
import net.minecraft.entity.Entity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Underdark maps draw the cave floor ({@link UnderdarkMapColors}). Only maps of the Underdark change; the Overworld,
 * the Nether and other dimensions keep vanilla's drawing.
 */
@Mixin(FilledMapItem.class)
public abstract class FilledMapMixin {
    /**
     * The second ceiling check picks stone-and-dirt noise over reading blocks; the Underdark reads blocks. (The first
     * one, which halves the explore radius under a ceiling, stays.)
     */
    @Redirect(method = "updateColors", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/dimension/DimensionType;hasCeiling()Z", ordinal = 1))
    private boolean dnd$underdarkReadsBlocks(DimensionType type, World world, Entity entity, MapState state) {
        return !Underdark.isUnderdark(world) && type.hasCeiling();
    }

    /** The top of a column is the floor at the holder's level, not the roof. */
    @Redirect(method = "updateColors", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/WorldChunk;sampleHeightmap(Lnet/minecraft/world/Heightmap$Type;II)I"))
    private int dnd$underdarkFloor(WorldChunk chunk, Heightmap.Type type, int x, int z, World world, Entity entity,
            MapState state) {
        if (Underdark.isUnderdark(world)) {
            return UnderdarkMapColors.surfaceY(world, chunk, entity, x, z);
        }
        return chunk.sampleHeightmap(type, x, z);
    }

    /** An explorer map's biomes are sampled at a cavern floor's depth, not at y 0 (above the Underdark's roof). */
    @Redirect(method = "fillExplorationMap", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/world/ServerWorld;getBiome(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/registry/entry/RegistryEntry;"))
    private static RegistryEntry<Biome> dnd$underdarkBiomeDepth(ServerWorld world, BlockPos pos, ServerWorld enclosing,
            ItemStack map) {
        return world.getBiome(Underdark.isUnderdark(world) ? pos.withY(UnderdarkMapColors.EXPLORER_BIOME_Y) : pos);
    }

    /** The Darklake's shores get the outline vanilla gives oceans and rivers. */
    @Redirect(method = "fillExplorationMap", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/registry/entry/RegistryEntry;isIn(Lnet/minecraft/registry/tag/TagKey;)Z"))
    private static boolean dnd$darklakeOutline(RegistryEntry<Biome> biome, TagKey<Biome> tag, ServerWorld world,
            ItemStack map) {
        if (tag == BiomeTags.WATER_ON_MAP_OUTLINES && Underdark.isUnderdark(world) && biome.matchesKey(Underdark.DARKLAKE)) {
            return true;
        }
        return biome.isIn(tag);
    }
}

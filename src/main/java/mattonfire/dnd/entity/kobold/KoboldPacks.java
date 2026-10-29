package mattonfire.dnd.entity.kobold;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Natural kobold packs (design 10, 3.6): a kobold spawned by the world brings {@value #MIN_EXTRA}-{@value #MAX_EXTRA}
 * more round it (50% Kobold, 35% Slinger, 15% Trapmaker), and a {@value #LEADER_CHANCE} chance of a Dragonshield
 * leading them. The pillager-patrol trick: the biome spawn entry is one kobold.
 */
public final class KoboldPacks {
    public static final int MIN_EXTRA = 2;
    public static final int MAX_EXTRA = 4;
    public static final float LEADER_CHANCE = 0.10F;

    private KoboldPacks() {
    }

    static void spawnPack(ServerWorld world, KoboldEntity first, Random random) {
        int extra = MIN_EXTRA + random.nextInt(MAX_EXTRA - MIN_EXTRA + 1);
        int spawned = 0;
        for (int i = 0; i < extra; i++) {
            float roll = random.nextFloat();
            EntityType<? extends KoboldEntity> type = roll < 0.50F ? Kobolds.KOBOLD
                    : roll < 0.85F ? Kobolds.KOBOLD_SLINGER : Kobolds.KOBOLD_TRAPMAKER;
            spawned += spawnNear(world, first, type, random) ? 1 : 0;
        }
        if (random.nextFloat() < LEADER_CHANCE && spawnNear(world, first, Kobolds.KOBOLD_DRAGONSHIELD, random)) {
            spawned++;
        }
        DnDClasses.LOGGER.debug("[Kobold] a pack of {} at {}", spawned + 1, first.getBlockPos().toShortString());
    }

    private static boolean spawnNear(ServerWorld world, KoboldEntity first, EntityType<? extends KoboldEntity> type,
            Random random) {
        for (int attempt = 0; attempt < 4; attempt++) {
            int x = first.getBlockX() + random.nextInt(7) - 3;
            int z = first.getBlockZ() + random.nextInt(7) - 3;
            BlockPos pos = new BlockPos(x, first.getBlockY(), z);
            // Same floor, or the nearest one within a couple of blocks.
            for (int dy = 0; dy <= 2; dy++) {
                for (BlockPos at : new BlockPos[] {pos.up(dy), pos.down(dy)}) {
                    if (world.isSpaceEmpty(type.createSimpleBoundingBox(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D))
                            && world.getBlockState(at.down()).isSolidBlock(world, at.down())) {
                        KoboldEntity kobold = type.create(world);
                        if (kobold == null) {
                            return false;
                        }
                        kobold.refreshPositionAndAngles(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D,
                                random.nextFloat() * 360.0F, 0.0F);
                        // SpawnReason.EVENT, so it doesn't bring a pack of its own.
                        kobold.initialize(world, world.getLocalDifficulty(at), SpawnReason.EVENT, null, null);
                        world.spawnEntityAndPassengers(kobold);
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

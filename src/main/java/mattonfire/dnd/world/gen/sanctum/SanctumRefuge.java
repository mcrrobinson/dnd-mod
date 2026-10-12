package mattonfire.dnd.world.gen.sanctum;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.world.gen.RacialHomes;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * The Tiefling home bonuses of a Brimstone Sanctum that aren't the shared ones ({@link mattonfire.dnd.world.gen.HomeBonuses}):
 * <ul>
 * <li><b>The Refuge's anchors</b>: a Tiefling who uses one of the Refuge's respawn anchors binds their spawn to
 * it for free: an empty anchor is filled to {@link #CHARGES} charges first. After that the anchor they're bound
 * to refills to {@link #CHARGES} once every in-game day for them: when they use it, when they die (before the
 * respawn spends a charge), and every {@link #CHECK_INTERVAL} ticks while its chunk is loaded. The day of the
 * last refill is {@code DndSanctumAnchorDay} in the player's persistent data.</li>
 * <li><b>Hellish Resistance becomes immunity</b>: inside their own sanctum a Tiefling takes no fire, lava or
 * burning damage, and is put out.</li>
 * </ul>
 */
public final class SanctumRefuge {
    public static final int CHARGES = RespawnAnchorBlock.MAX_CHARGES;
    public static final int CHECK_INTERVAL = 200;
    private static final String DAY_KEY = "DndSanctumAnchorDay";

    private SanctumRefuge() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer && isTiefling(serverPlayer)) {
                BlockPos pos = hit.getBlockPos();
                if (isRefugeAnchor(world, pos)) {
                    refill(serverPlayer, (ServerWorld) world, pos, true);
                }
            }
            return ActionResult.PASS;
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player && isTiefling(player)) {
                refillBound(player, true);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % CHECK_INTERVAL == 0) {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    if (isTiefling(player)) {
                        refillBound(player, false);
                    }
                }
            }
        });
        // Inside their own sanctum, Hellish Resistance is full fire immunity.
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player && source.isIn(DamageTypeTags.IS_FIRE) && fireImmuneHere(player)) {
                player.extinguish();
                return false;
            }
            return true;
        });
    }

    private static boolean isTiefling(ServerPlayerEntity player) {
        return RaceLifecycle.activeRaceOf(player) == DndRace.TIEFLING;
    }

    /** Whether {@code player} is a Tiefling standing in their own Brimstone Sanctum. */
    public static boolean fireImmuneHere(ServerPlayerEntity player) {
        RacialHomes.Visit visit = RacialHomes.ownHomeAt(player);
        return visit != null && visit.home() == RacialHomes.BRIMSTONE_SANCTUM;
    }

    /** Whether {@code pos} holds a respawn anchor in a sanctum's Refuge. */
    public static boolean isRefugeAnchor(World world, BlockPos pos) {
        if (!(world instanceof ServerWorld serverWorld) || !world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR)) {
            return false;
        }
        RacialHomes.Visit visit = RacialHomes.homeAt(serverWorld, pos);
        return visit != null && visit.home() == RacialHomes.BRIMSTONE_SANCTUM
                && visit.pieceAt(pos, BrimstoneSanctumStructures.REFUGE) != null;
    }

    /** The Refuge anchor {@code player}'s spawn is bound to, or null. */
    @Nullable
    public static BlockPos boundAnchor(ServerPlayerEntity player) {
        BlockPos spawn = player.getSpawnPointPosition();
        if (spawn == null || player.getSpawnPointDimension() != World.NETHER) {
            return null;
        }
        return spawn;
    }

    /**
     * Refills the Refuge anchor {@code player} is bound to, if today's refill is still owed. With {@code load}
     * false the anchor's chunk must already be loaded.
     */
    static void refillBound(ServerPlayerEntity player, boolean load) {
        BlockPos anchor = boundAnchor(player);
        ServerWorld nether = player.getServer().getWorld(World.NETHER);
        if (anchor == null || nether == null || !owed(player)) {
            return;
        }
        if (!load && !nether.isChunkLoaded(ChunkPos.toLong(anchor.getX() >> 4, anchor.getZ() >> 4))) {
            return;
        }
        if (isRefugeAnchor(nether, anchor)) {
            refill(player, nether, anchor, false);
        }
    }

    /** Whether today's refill is still owed to {@code player}. */
    private static boolean owed(ServerPlayerEntity player) {
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        return !data.contains(DAY_KEY) || data.getLong(DAY_KEY) != today(player);
    }

    private static long today(ServerPlayerEntity player) {
        return player.getServer().getOverworld().getTimeOfDay() / 24000L;
    }

    /**
     * Fills the anchor at {@code pos} to {@link #CHARGES} for {@code player} if today's refill is owed (a full
     * anchor doesn't use it up). A use
     * ({@code used}) that finds it already spent today and the anchor empty says so; vanilla then does nothing.
     */
    static void refill(ServerPlayerEntity player, ServerWorld world, BlockPos pos, boolean used) {
        BlockState state = world.getBlockState(pos);
        if (!owed(player)) {
            if (used && state.get(RespawnAnchorBlock.CHARGES) == 0) {
                player.sendMessage(Text.translatable("message.dndclasses.sanctum.anchor_spent").formatted(Formatting.GRAY), true);
            }
            return;
        }
        if (state.get(RespawnAnchorBlock.CHARGES) >= CHARGES) {
            return;
        }
        ((IEntityDataSaver) player).getPersistentData().putLong(DAY_KEY, today(player));
        world.setBlockState(pos, state.with(RespawnAnchorBlock.CHARGES, CHARGES));
        world.playSound(null, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 1.0F, 1.0F);
        if (used) {
            player.sendMessage(Text.translatable("message.dndclasses.sanctum.anchor_kindled", CHARGES).formatted(Formatting.GOLD), true);
        }
        DnDClasses.LOGGER.info("[BrimstoneSanctum] Refilled Refuge anchor {} to {} for {} (day {})", pos.toShortString(),
                CHARGES, player.getEntityName(), today(player));
    }
}

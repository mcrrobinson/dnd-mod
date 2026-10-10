package mattonfire.dnd.dungeon;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.Blocks.DungeonWardBlockEntity;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.LichEntity;
import mattonfire.dnd.entity.PhylacteryEntity;
import mattonfire.dnd.entity.boss.BossFight;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Server-side glue for dungeon fights: the death listener that tells a room's ward one of its mobs
 * died (and spots a boss's true death), and what happens when a dungeon is cleared (title, sting,
 * the {@code dndclasses:delver} advancement).
 */
public final class DungeonCombat {
    /** The boss of a dungeon's boss room (and its phylactery, and a reformed Lich). */
    public static final String BOSS_TAG = "dndclasses.dungeon_boss";
    public static final Identifier DELVER = new Identifier(DnDClasses.MOD_ID, "delver");

    private DungeonCombat() {
    }

    public static void register() {
        DungeonEncounterPools.register();
        DungeonChampion.register();
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.world instanceof ServerWorld world && entity.getCommandTags().contains(EncounterBuilder.TAG)) {
                onDeath(world, entity);
            }
        });
        DungeonEvents.CLEARED.register(DungeonCombat::onCleared);
    }

    private static void onDeath(ServerWorld world, LivingEntity entity) {
        DungeonChampion.removeBar(entity.getUuid());
        long[] tag = EncounterBuilder.roomOf(entity);
        if (tag == null) {
            return;
        }
        DungeonState dungeon = DungeonRegistry.get(world).get(tag[0]);
        if (dungeon == null) {
            return;
        }
        DungeonState.Room room = dungeon.room((int) tag[1]);
        if (room == null) {
            return;
        }
        DungeonWardBlockEntity ward = ward(world, dungeon, room);
        if (ward != null) {
            ward.onMobDied(entity.getUuid());
        }
        if (entity.getCommandTags().contains(BOSS_TAG) && isTrueDeath(entity) && !dungeon.bossDefeated()) {
            bossDefeated(world, dungeon, room, entity);
        }
    }

    /**
     * A Lich whose soul fled to its phylactery isn't dead yet; nor is a phylactery smashed while its
     * Lich still stands. A phylactery smashed while the Lich reforms, or a Lich with no phylactery
     * left, is the end.
     */
    private static boolean isTrueDeath(LivingEntity entity) {
        if (entity instanceof LichEntity lich) {
            return !lich.hasFledToPhylactery();
        }
        if (entity instanceof PhylacteryEntity phylactery) {
            return phylactery.isReforming();
        }
        return true;
    }

    private static void bossDefeated(ServerWorld world, DungeonState dungeon, DungeonState.Room room, LivingEntity boss) {
        List<ServerPlayerEntity> players = playersIn(world, room);
        DnDClasses.LOGGER.info("[Dungeon] boss {} of {} defeated with {} player(s) in the boss room",
                boss.getType().getUntranslatedName(), dungeon.startKey(), players.size());
        DungeonEvents.BOSS_DEFEATED.invoker().onBossDefeated(world, dungeon, boss, players);
        DungeonRegistry.clear(world, dungeon);
    }

    private static void onCleared(ServerWorld world, DungeonState dungeon, List<ServerPlayerEntity> participants) {
        DnDClasses.LOGGER.info("[Dungeon] {} cleared ({} time(s)), {} participant(s)", dungeon.startKey(), dungeon.clears(),
                participants.size());
        for (ServerPlayerEntity player : participants) {
            player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 70, 20));
            player.networkHandler.sendPacket(new SubtitleS2CPacket(dungeon.name()));
            player.networkHandler.sendPacket(new TitleS2CPacket(
                    Text.translatable("dungeon.dndclasses.cleared").formatted(Formatting.GOLD)));
            player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.0F, 1.0F);
            BossFight.grantAdvancement(player, DELVER);
        }
    }

    /** Living survival/adventure players inside a room's box. */
    public static List<ServerPlayerEntity> playersIn(ServerWorld world, DungeonState.Room room) {
        List<ServerPlayerEntity> players = new ArrayList<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isAlive() && !player.isSpectator() && !player.isCreative() && room.box().contains(player.getBlockPos())) {
                players.add(player);
            }
        }
        return players;
    }

    /** Where a room's ward sits: under the middle of its floor. */
    public static BlockPos wardPos(DungeonState dungeon, DungeonState.Room room) {
        BlockPos centre = room.box().getCenter();
        return new BlockPos(centre.getX(), dungeon.floorY() - 1, centre.getZ());
    }

    @Nullable
    public static DungeonWardBlockEntity ward(ServerWorld world, DungeonState dungeon, DungeonState.Room room) {
        BlockPos pos = wardPos(dungeon, room);
        if (!world.isChunkLoaded(pos)) {
            return null;
        }
        return world.getBlockEntity(pos) instanceof DungeonWardBlockEntity ward ? ward : null;
    }

    /** True for an entity a room's ward follows. */
    public static boolean isDungeonMob(Entity entity) {
        return entity.getCommandTags().contains(EncounterBuilder.TAG);
    }
}

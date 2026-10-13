package mattonfire.dnd.entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.classes.Registry.ModItems;
import mattonfire.dnd.entity.boss.BossFight;
import mattonfire.dnd.faction.Faction;
import mattonfire.dnd.faction.Factions;
import mattonfire.dnd.faction.Reputation;
import mattonfire.dnd.faction.ReputationTier;
import mattonfire.dnd.world.gen.RacialHomes;
import mattonfire.dnd.world.gen.stronghold.BloodSworn;
import mattonfire.dnd.world.gen.stronghold.FightingPitPiece;
import mattonfire.dnd.world.gen.stronghold.OrcStrongholdStructures;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.structure.StructurePiece;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * The Trial of Strength at an Orc Stronghold's Fighting Pit. A Half-Orc (or anyone Honored with the
 * Bloodtusk Horde) steps into the pit and right-clicks the war drum on its rim; an Orc Champion jumps in and
 * a one-on-one duel starts under a duel bar. Other orcs watch and don't join in.
 * <p>
 * The duel is non-lethal: whoever drops to {@link #FLOOR} health or below loses, and damage that would take
 * either fighter below it is clamped there (the player's in {@code PlayerEntityMixin}, the Champion's in
 * {@link OrcChampionEntity#setHealth}). Leaving the pit's surroundings, logging out or running out the clock
 * ({@link #TIME_LIMIT}) loses too.
 * <ul>
 * <li><b>Win</b>: Blood-Sworn at this hold ({@link BloodSworn}): its orcs tolerate you and your party within
 * 8 blocks, and its Quartermaster trades with you. An Orcish Greataxe, {@link #CLASS_XP} class XP,
 * {@link #STANDING} standing with the horde and the advancement "Blood and Thunder".</li>
 * <li><b>Lose</b>: try again the next in-game day.</li>
 * </ul>
 */
public final class OrcTrial {
    /** The duel's floor: at or below this health a fighter yields. */
    public static final float FLOOR = 3.0F;
    public static final int TIME_LIMIT = 20 * 180;
    public static final int CLASS_XP = 100;
    public static final int STANDING = 100;
    /** The duellist must stay this close to the middle of the pit. */
    public static final double LEASH = 12.0D;
    public static final double BAR_RANGE = 32.0D;
    public static final Identifier ADVANCEMENT = new Identifier(DnDClasses.MOD_ID, "blood_and_thunder");
    public static final Identifier FACTION = new Identifier(DnDClasses.MOD_ID, "bloodtusk_horde");

    /** Quest hook: called when a player wins the Trial at a hold (the hold key). */
    public static BiConsumer<ServerPlayerEntity, Long> onTrialWon = (player, hold) -> {
    };

    private record Duel(UUID player, UUID champion, long hold, BlockPos middle, ServerWorld world, ServerBossBar bar,
                        long started) {
    }

    private static final Map<UUID, Duel> DUELS = new HashMap<>();

    private OrcTrial() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)
                    || !world.getBlockState(hit.getBlockPos()).isOf(Blocks.NOTE_BLOCK)) {
                return ActionResult.PASS;
            }
            FightingPitPiece pit = pitWithDrum((ServerWorld) world, hit.getBlockPos());
            if (pit == null) {
                return ActionResult.PASS;
            }
            beat(serverPlayer, (ServerWorld) world, pit);
            return ActionResult.SUCCESS;
        });
        ServerTickEvents.END_SERVER_TICK.register(OrcTrial::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            Duel duel = DUELS.get(handler.getPlayer().getUuid());
            if (duel != null) {
                end(duel, handler.getPlayer(), false, "fled");
            }
        });
    }

    /** The Fighting Pit whose war drum is at {@code pos}, or null. */
    private static FightingPitPiece pitWithDrum(ServerWorld world, BlockPos pos) {
        RacialHomes.Visit visit = RacialHomes.homeAt(world, pos);
        if (visit == null || visit.home() != RacialHomes.ORC_STRONGHOLD) {
            return null;
        }
        StructurePiece piece = visit.pieceAt(pos, OrcStrongholdStructures.PIT);
        return piece instanceof FightingPitPiece pit && pit.drum().equals(pos) ? pit : null;
    }

    public static boolean isDueling(PlayerEntity player) {
        return DUELS.containsKey(player.getUuid());
    }

    /** Whether {@code champion} is fighting a duel right now. */
    public static boolean isChampion(OrcChampionEntity champion) {
        for (Duel duel : DUELS.values()) {
            if (duel.champion().equals(champion.getUuid())) {
                return true;
            }
        }
        return false;
    }

    /** A blow between duellists: no rally, no anger beyond the duel. */
    static boolean isDuelBlow(OrcEntity orc, PlayerEntity player) {
        return orc instanceof OrcChampionEntity champion && champion.isDuelist(player);
    }

    /** May {@code player} take the Trial at all: a Half-Orc, or Honored with the horde. */
    public static boolean eligible(ServerPlayerEntity player) {
        if (RaceLifecycle.activeRaceOf(player) == DndRace.HALFORC) {
            return true;
        }
        Faction horde = Factions.get(FACTION);
        return horde != null && Reputation.tier(player, horde).atLeast(ReputationTier.HONORED);
    }

    /** The non-lethal clamp for a duelling player's new health (called as damage is applied). */
    public static float clampPlayerHealth(PlayerEntity player, float newHealth) {
        return DUELS.containsKey(player.getUuid()) && newHealth < FLOOR ? FLOOR : newHealth;
    }

    private static void beat(ServerPlayerEntity player, ServerWorld world, FightingPitPiece pit) {
        world.playSound(null, pit.drum(), SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value(), SoundCategory.BLOCKS, 2.0F, 0.6F);
        long hold = pit.hold();
        long today = world.getTimeOfDay() / 24000L;
        BloodSworn records = BloodSworn.get(world);
        String refusal = null;
        if (player.isCreative() || player.isSpectator()) {
            refusal = "message.dndclasses.trial.creative";
        } else if (!eligible(player)) {
            refusal = "message.dndclasses.trial.not_kin";
        } else if (records.isSworn(hold, player.getUuid())) {
            refusal = "message.dndclasses.trial.already";
        } else if (records.lostOn(hold, player.getUuid(), today)) {
            refusal = "message.dndclasses.trial.tomorrow";
        } else if (DUELS.containsKey(player.getUuid()) || DUELS.values().stream().anyMatch(d -> d.hold() == hold)) {
            refusal = "message.dndclasses.trial.busy";
        } else if (!pit.inPit(player.getBlockPos())) {
            refusal = "message.dndclasses.trial.step_in";
        } else if (player.getHealth() <= FLOOR + 1.0F) {
            refusal = "message.dndclasses.trial.wounded";
        }
        if (refusal != null) {
            player.sendMessage(Text.translatable(refusal).formatted(Formatting.GOLD), true);
            DnDClasses.LOGGER.info("[OrcTrial] {} beat the drum at hold {}: refused ({})", player.getEntityName(),
                    hold, refusal.substring(refusal.lastIndexOf('.') + 1));
            return;
        }
        start(player, world, pit);
    }

    private static void start(ServerPlayerEntity player, ServerWorld world, FightingPitPiece pit) {
        BlockPos middle = pit.floor(0);
        // The Champion jumps in on the far side of the pit from the player.
        int side = player.getBlockPos().getSquaredDistance(pit.floor(4)) < player.getBlockPos().getSquaredDistance(pit.floor(-4)) ? -4 : 4;
        BlockPos spot = pit.floor(side);
        OrcChampionEntity champion = ModEntityTypes.ORC_CHAMPION.create(world);
        if (champion == null) {
            return;
        }
        champion.refreshPositionAndAngles(spot.getX() + 0.5D, spot.getY() + 1.5D, spot.getZ() + 0.5D, 0.0F, 0.0F);
        champion.initialize(world, world.getLocalDifficulty(spot), SpawnReason.EVENT, null, null);
        champion.setHome(middle, FightingPitPiece.RADIUS);
        champion.setHold(pit.hold());
        champion.setDuelist(player.getUuid());
        world.spawnEntityAndPassengers(champion);
        champion.setTarget(player);
        world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, spot.getX() + 0.5D, spot.getY() + 0.2D, spot.getZ() + 0.5D,
                10, 0.5D, 0.1D, 0.5D, 0.02D);
        world.playSound(null, spot, SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 1.5F, 1.1F);

        ServerBossBar bar = new ServerBossBar(Text.translatable("bossbar.dndclasses.trial", champion.getDisplayName()),
                BossBar.Color.YELLOW, BossBar.Style.NOTCHED_10);
        Duel duel = new Duel(player.getUuid(), champion.getUuid(), pit.hold(), middle, world, bar, world.getTime());
        DUELS.put(player.getUuid(), duel);
        // The watching orcs stand down while the duel lasts.
        for (OrcEntity orc : world.getEntitiesByClass(OrcEntity.class, new net.minecraft.util.math.Box(middle).expand(48.0D),
                o -> o != champion && o.getTarget() == player)) {
            orc.setTarget(null);
        }
        player.sendMessage(Text.translatable("message.dndclasses.trial.start", champion.getDisplayName()).formatted(Formatting.RED), false);
        DnDClasses.LOGGER.info("[OrcTrial] {} started the Trial of Strength at hold {} against {}", player.getEntityName(),
                pit.hold(), champion.getEntityName());
    }

    private static void tick(MinecraftServer server) {
        if (DUELS.isEmpty()) {
            return;
        }
        Iterator<Duel> it = new java.util.ArrayList<>(DUELS.values()).iterator();
        while (it.hasNext()) {
            Duel duel = it.next();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(duel.player());
            Entity entity = duel.world().getEntity(duel.champion());
            OrcChampionEntity champion = entity instanceof OrcChampionEntity c ? c : null;
            if (player == null) {
                DUELS.remove(duel.player());
                duel.bar().clearPlayers();
                if (champion != null) {
                    champion.discard();
                }
                continue;
            }
            if (!player.isAlive() || player.getHealth() <= FLOOR) {
                end(duel, player, false, !player.isAlive() ? "died" : "yielded");
            } else if (champion == null || !champion.isAlive() || champion.getHealth() <= FLOOR) {
                end(duel, player, true, "champion yielded");
            } else if (player.getWorld() != duel.world() || !player.getBlockPos().isWithinDistance(duel.middle(), LEASH)) {
                end(duel, player, false, "fled");
            } else if (duel.world().getTime() - duel.started() > TIME_LIMIT) {
                end(duel, player, false, "time");
            } else {
                float max = champion.getMaxHealth() - FLOOR;
                duel.bar().setPercent(Math.max(0.0F, Math.min(1.0F, (champion.getHealth() - FLOOR) / max)));
                if (champion.getTarget() != player) {
                    champion.setTarget(player);
                }
                if (server.getTicks() % 10 == 0) {
                    for (ServerPlayerEntity viewer : duel.world().getPlayers()) {
                        boolean near = viewer.getBlockPos().isWithinDistance(duel.middle(), BAR_RANGE);
                        if (near && !duel.bar().getPlayers().contains(viewer)) {
                            duel.bar().addPlayer(viewer);
                        } else if (!near && duel.bar().getPlayers().contains(viewer)) {
                            duel.bar().removePlayer(viewer);
                        }
                    }
                }
            }
        }
    }

    private static void end(Duel duel, ServerPlayerEntity player, boolean won, String why) {
        DUELS.remove(duel.player());
        duel.bar().clearPlayers();
        ServerWorld world = duel.world();
        Entity entity = world.getEntity(duel.champion());
        if (entity instanceof OrcChampionEntity champion) {
            champion.setDuelist(null);
            world.spawnParticles(ParticleTypes.POOF, champion.getX(), champion.getBodyY(0.5D), champion.getZ(), 12, 0.4D, 0.6D, 0.4D, 0.02D);
            champion.discard();
        }
        BloodSworn records = BloodSworn.get(world);
        DnDClasses.LOGGER.info("[OrcTrial] {} {} the Trial at hold {} ({}), health {}", player.getEntityName(),
                won ? "won" : "lost", duel.hold(), why, player.getHealth());
        if (won) {
            records.setSworn(duel.hold(), player.getUuid(), true);
            player.getInventory().offerOrDrop(new ItemStack(ModItems.ORCISH_GREATAXE));
            Progression.addXp(player, CLASS_XP);
            Faction horde = Factions.get(FACTION);
            if (horde != null) {
                Reputation.add(player, horde, STANDING, Reputation.Source.QUEST);
            }
            BossFight.grantAdvancement(player, ADVANCEMENT);
            title(player, Text.translatable("message.dndclasses.trial.won").formatted(Formatting.DARK_RED),
                    Text.translatable("message.dndclasses.trial.won.subtitle").formatted(Formatting.GOLD));
            world.playSound(null, player.getBlockPos(), SoundEvents.EVENT_RAID_HORN.value(), SoundCategory.PLAYERS, 2.0F, 1.0F);
            onTrialWon.accept(player, duel.hold());
        } else {
            records.setLost(duel.hold(), player.getUuid(), world.getTimeOfDay() / 24000L);
            if (player.isAlive()) {
                title(player, Text.translatable("message.dndclasses.trial.lost").formatted(Formatting.GRAY),
                        Text.translatable("message.dndclasses.trial.lost.subtitle").formatted(Formatting.GRAY));
            }
        }
    }

    private static void title(ServerPlayerEntity player, Text title, Text subtitle) {
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 60, 20));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
        player.networkHandler.sendPacket(new TitleS2CPacket(title));
    }
}

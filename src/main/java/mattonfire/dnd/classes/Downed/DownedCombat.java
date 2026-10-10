package mattonfire.dnd.classes.Downed;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Abilities.Advantage;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.classes.Rest.RestEvents;
import mattonfire.dnd.entity.boss.BossFight;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Combat rules around Downed players (mobs, bosses, PvP and rests).
 * <ul>
 * <li>Mobs can't target a Downed player ({@code DownedTargetMixin} on {@code LivingEntity.canTarget}, which
 * target goals, revenge and brain sensors all check), so they go for the next player in range or idle.
 * Targets set some other way (boss scripts, minions told who to attack) are dropped by a sweep every
 * {@value #SWEEP_TICKS} ticks, and at once when a player goes Downed.</li>
 * <li>Area damage still lands and costs fails ({@link DownedEvents} turns every hit into fails).</li>
 * <li>Party wipe: if everyone in a boss fight's range is Downed, their death saves are at disadvantage.</li>
 * <li>PvP finishers: a player whose hit costs the last fail is credited with the kill, unless another
 * player downed you (then the one who downed you keeps the credit).</li>
 * <li>Nobody rests while a party member within {@value #REST_RANGE} blocks is Downed, or in a boss fight.</li>
 * </ul>
 */
public final class DownedCombat {
    /** How often mobs still targeting a Downed player are made to let go. */
    public static final int SWEEP_TICKS = 10;
    /** Mobs further than this from a Downed player aren't checked by the sweep. */
    public static final double SWEEP_RANGE = 64.0D;
    /** A Downed party member this close stops you resting. */
    public static final double REST_RANGE = 32.0D;

    /** Players already told their boss fight is a party wipe (so the message comes once). */
    private static final Set<UUID> TOLD_WIPE = new HashSet<>();

    private DownedCombat() {
    }

    static void register() {
        DownedEvents.AFTER_DOWNED.register((player, source) -> dropTargets(player));
        ServerTickEvents.END_SERVER_TICK.register(DownedCombat::tick);

        DeathSaveModifier.EVENT.register(new DeathSaveModifier() {
            @Override
            public int bonus(ServerPlayerEntity player) {
                return 0;
            }

            @Override
            public Advantage mode(ServerPlayerEntity player) {
                return partyWipe(player) ? Advantage.DISADVANTAGE : Advantage.NORMAL;
            }
        });

        RestEvents.ALLOW_REST.register((player, kind) -> {
            for (ServerPlayerEntity member : PartyManager.nearbyMembers(player, REST_RANGE)) {
                if (Downed.is(member)) {
                    return Text.literal("You can't rest while " + member.getEntityName() + " is Downed.");
                }
            }
            if (BossFight.inAnyFight(player)) {
                return Text.literal("You can't rest during a boss fight.");
            }
            return null;
        });
    }

    /** True if the player is Downed in a boss fight where every player in range is Downed. */
    static boolean partyWipe(ServerPlayerEntity player) {
        BossFight fight = BossFight.fightNear(player);
        boolean wiped = Downed.is(player) && fight != null && fight.isPartyWiped();
        if (!wiped) {
            TOLD_WIPE.remove(player.getUuid());
        } else if (TOLD_WIPE.add(player.getUuid())) {
            player.sendMessage(Text.literal("Your whole party is down: death saves at disadvantage!")
                    .formatted(net.minecraft.util.Formatting.DARK_RED), false);
            DnDClasses.LOGGER.info("[Downed] party wipe: {} rolls death saves at disadvantage",
                    player.getEntityName());
        }
        return wiped;
    }

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % SWEEP_TICKS != 0) {
            return;
        }
        if (Downed.STATES.isEmpty()) {
            TOLD_WIPE.clear();
            return;
        }
        for (UUID id : Set.copyOf(Downed.STATES.keySet())) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
            if (player != null) {
                dropTargets(player);
            }
        }
    }

    /** Mobs targeting (or remembering as their attack target) a Downed player let go of them. */
    static void dropTargets(ServerPlayerEntity player) {
        for (MobEntity mob : player.getWorld().getEntitiesByClass(MobEntity.class,
                player.getBoundingBox().expand(SWEEP_RANGE), mob -> mob.getTarget() == player
                        || mob.getBrain().hasMemoryModule(MemoryModuleType.ATTACK_TARGET)
                                && mob.getBrain().getOptionalMemory(MemoryModuleType.ATTACK_TARGET)
                                        .filter(target -> target == player).isPresent())) {
            if (mob.getTarget() == player) {
                mob.setTarget(null);
            }
            if (mob.getBrain().hasMemoryModule(MemoryModuleType.ATTACK_TARGET) && mob.getBrain()
                    .getOptionalMemory(MemoryModuleType.ATTACK_TARGET).filter(t -> t == player).isPresent()) {
                mob.getBrain().forget(MemoryModuleType.ATTACK_TARGET);
            }
            DnDClasses.LOGGER.info("[Downed] {} stops targeting Downed {}", mob.getName().getString(),
                    player.getEntityName());
        }
    }

    /**
     * Called by {@link DownedEvents} before a hit's fails are added: if a player's hit is the finishing
     * blow, they get the kill credit, unless another player downed you.
     */
    static void beforeFails(ServerPlayerEntity player, Downed.State state, DamageSource source, int fails) {
        Entity attacker = source.getAttacker();
        if (!(attacker instanceof PlayerEntity finisher) || finisher == player
                || state.fails + fails < Downed.FAILS_TO_DIE) {
            return;
        }
        if (state.attacker != null && !state.attacker.equals(finisher.getUuid())
                && player.getServer().getPlayerManager().getPlayer(state.attacker) != null) {
            return; // another player downed you: they keep the kill
        }
        state.attacker = finisher.getUuid();
        state.damageType = source.getTypeRegistryEntry().getKey().map(key -> key.getValue().toString())
                .orElse(state.damageType);
        DnDClasses.LOGGER.info("[Downed] {} finished off by {}", player.getEntityName(), finisher.getEntityName());
    }

    /** Whether a mob may target this entity: never a Downed player. */
    public static boolean untargetable(LivingEntity attacker, LivingEntity target) {
        return !(attacker instanceof PlayerEntity) && target instanceof PlayerEntity player && Downed.is(player);
    }
}

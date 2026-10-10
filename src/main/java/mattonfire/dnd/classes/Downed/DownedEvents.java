package mattonfire.dnd.classes.Downed;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.classes.Rest.DndRules;
import mattonfire.dnd.classes.Rest.RestEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;

/**
 * Wires the Downed state into the game.
 * <ul>
 * <li>{@code ALLOW_DEATH}, in order: a held totem wins; bypass damage ({@link #BYPASSES}: the void,
 * {@code /kill}) and massive damage kill; {@code dndDeathSaves} 0 kills; a PvP blow with {@code dndPvpDowned
 * false} kills; then Downed if an ally is near, else a Last Stand.</li>
 * <li>{@code ALLOW_DAMAGE}: hits on a Downed player are cancelled and become death save fails.</li>
 * <li>Downed players can't attack, use or break anything, rest or fire specials.</li>
 * <li>Logging out unstable kills; stable stands you up. A server stop keeps the state for the next join.</li>
 * </ul>
 */
public final class DownedEvents {
    /** Damage that kills outright, even with death saves on. */
    public static final TagKey<DamageType> BYPASSES = TagKey.of(RegistryKeys.DAMAGE_TYPE,
            new Identifier(DnDClasses.MOD_ID, "bypasses_death_saves"));
    /** Allies (party members, or anyone in mode 2) this close keep you Downed rather than dying. */
    public static final double ALLY_RANGE = 64.0D;
    /** C2S: the client is holding (true) or let go of (false) the power-up key while Downed. */
    public static final Identifier C2S_GIVE_UP = new Identifier(DnDClasses.MOD_ID, "downed_give_up");

    /** Fired after a player goes Downed (quests, obstacles, the party HUD). */
    public static final Event<AfterDowned> AFTER_DOWNED = EventFactory.createArrayBacked(AfterDowned.class,
            listeners -> (player, source) -> {
                for (AfterDowned listener : listeners) {
                    listener.afterDowned(player, source);
                }
            });

    @FunctionalInterface
    public interface AfterDowned {
        void afterDowned(ServerPlayerEntity player, DamageSource source);
    }

    private static final Identifier PHASE = new Identifier(DnDClasses.MOD_ID, "downed");

    private DownedEvents() {
    }

    public static void register() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> !(entity instanceof ServerPlayerEntity player)
                || allowDeath(player, source, amount));
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(entity instanceof ServerPlayerEntity player)
                || allowDamage(player, source, amount));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity player) {
                Downed.forget(player);
            }
        });

        // Restrictions, before other listeners (attack rolls, lockpicking) see the action
        AttackEntityCallback.EVENT.addPhaseOrdering(PHASE, Event.DEFAULT_PHASE);
        AttackBlockCallback.EVENT.addPhaseOrdering(PHASE, Event.DEFAULT_PHASE);
        UseBlockCallback.EVENT.addPhaseOrdering(PHASE, Event.DEFAULT_PHASE);
        UseEntityCallback.EVENT.addPhaseOrdering(PHASE, Event.DEFAULT_PHASE);
        UseItemCallback.EVENT.addPhaseOrdering(PHASE, Event.DEFAULT_PHASE);
        PlayerBlockBreakEvents.BEFORE.addPhaseOrdering(PHASE, Event.DEFAULT_PHASE);
        AttackEntityCallback.EVENT.register(PHASE, (player, world, hand, entity, hit) -> blocked(player));
        AttackBlockCallback.EVENT.register(PHASE, (player, world, hand, pos, direction) -> blocked(player));
        UseBlockCallback.EVENT.register(PHASE, (player, world, hand, hit) -> blocked(player));
        UseEntityCallback.EVENT.register(PHASE, (player, world, hand, entity, hit) -> blocked(player));
        UseItemCallback.EVENT.register(PHASE, (player, world, hand) -> blocked(player) == ActionResult.FAIL
                ? TypedActionResult.fail(player.getStackInHand(hand))
                : TypedActionResult.pass(player.getStackInHand(hand)));
        PlayerBlockBreakEvents.BEFORE.register(PHASE, (world, player, pos, state, blockEntity) -> !Downed.is(player));
        RestEvents.ALLOW_REST.register((player, kind) -> Downed.is(player)
                ? Text.literal("You can't rest while Downed.")
                : null);

        ServerTickEvents.END_SERVER_TICK.register(Downed::tick);
        DownedCombat.register();
        ServerPlayNetworking.registerGlobalReceiver(C2S_GIVE_UP, (server, player, handler, buf, sender) -> {
            boolean holding = buf.readBoolean();
            server.execute(() -> Downed.setGivingUp(player, holding));
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> Downed.resume(handler.getPlayer()));
        // ClassLifecycle copies dndDowned with the rest of the persistent data: a dead player starts fresh.
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (alive) {
                Downed.reapply(newPlayer);
            } else {
                Downed.forget(newPlayer);
            }
        });
    }

    /**
     * A player is leaving ({@code PlayerManager.remove}, before their data is saved): unstable dies, stable
     * stands up, and a server stop keeps the state for the next join.
     */
    public static void onLeave(ServerPlayerEntity player, net.minecraft.server.MinecraftServer server) {
        Downed.State state = Downed.state(player);
        if (state == null) {
            return;
        }
        if (!server.isRunning() || server.isHost(player.getGameProfile())) {
            // Server stop (or the singleplayer / LAN host leaving, which stops it): resume on the next join.
            Downed.STATES.remove(player.getUuid());
        } else if (state.stable) {
            Downed.revive(player, 1.0F);
        } else {
            // No combat logging out of a death save.
            DnDClasses.LOGGER.info("[Downed] {} logged out while dying", player.getEntityName());
            Downed.bleedOut(player);
        }
    }

    private static ActionResult blocked(PlayerEntity player) {
        return !player.world.isClient && Downed.is(player) ? ActionResult.FAIL : ActionResult.PASS;
    }

    /** True if the hit kills outright, ignoring death saves. */
    public static boolean bypasses(DamageSource source, float amount) {
        return source.isIn(BYPASSES) || amount >= Float.MAX_VALUE / 2;
    }

    private static boolean allowDeath(ServerPlayerEntity player, DamageSource source, float amount) {
        float overflow = ((mattonfire.dnd.classes.PlayerEntityExt) player).getLastDamageOverflow();
        if (player.isCreative() || player.isSpectator() || Downed.is(player)) {
            return true;
        }
        // The totem wins: let vanilla pop it.
        if (player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING) || player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            return true;
        }
        if (bypasses(source, amount)) {
            return true;
        }
        // Massive damage (5e): what's left of the hit after 0 HP is at least your max health.
        if (overflow >= player.getMaxHealth()) {
            DnDClasses.LOGGER.info("[Downed] {} killed outright by massive damage ({} past 0 HP)",
                    player.getEntityName(), overflow);
            return true;
        }
        int mode = player.world.getGameRules().getInt(DndRules.DEATH_SAVES);
        if (mode <= 0) {
            return true;
        }
        Entity attacker = responsible(source.getAttacker());
        boolean pvp = attacker instanceof PlayerEntity && attacker != player;
        if (pvp && !player.world.getGameRules().getBoolean(DndRules.PVP_DOWNED)) {
            return true;
        }
        if (hasAlly(player, mode, attacker)) {
            Downed.down(player, source);
            return false;
        }
        return !DeathSaves.lastStand(player);
    }

    /** Someone who could come and help: online, alive, not Downed, same dimension, within 64 blocks. */
    public static boolean hasAlly(ServerPlayerEntity player, int mode, Entity attacker) {
        if (mode >= 2) {
            for (ServerPlayerEntity other : ((net.minecraft.server.world.ServerWorld) player.world).getPlayers()) {
                if (other != player && other != attacker && other.isAlive() && !other.isSpectator()
                        && !Downed.is(other) && other.squaredDistanceTo(player) <= ALLY_RANGE * ALLY_RANGE) {
                    return true;
                }
            }
            return false;
        }
        for (ServerPlayerEntity member : PartyManager.nearbyMembers(player, ALLY_RANGE)) {
            if (!Downed.is(member)) {
                return true;
            }
        }
        return false;
    }

    private static Entity responsible(Entity attacker) {
        if (attacker instanceof TameableEntity tameable && tameable.getOwner() != null) {
            return tameable.getOwner();
        }
        return attacker;
    }

    private static boolean allowDamage(ServerPlayerEntity player, DamageSource source, float amount) {
        boolean bypass = bypasses(source, amount);
        if (Downed.inGrace(player) && !bypass) {
            return false;
        }
        Downed.State state = Downed.state(player);
        if (state == null) {
            return true;
        }
        if (bypass) {
            return true; // dies: ALLOW_DEATH lets a Downed player go
        }
        Entity attacker = source.getAttacker();
        boolean melee = attacker != null && (source.isOf(DamageTypes.MOB_ATTACK) || source.isOf(DamageTypes.PLAYER_ATTACK)
                || source.isOf(DamageTypes.MOB_ATTACK_NO_AGGRO));
        int fails;
        if (melee) {
            // A melee hit on a helpless target is an automatic critical: two fails.
            if (state.meleeCooldown.containsKey(attacker.getUuid())) {
                return false;
            }
            state.meleeCooldown.put(attacker.getUuid(), Downed.MELEE_COOLDOWN);
            fails = 2;
        } else {
            // Rate-limited per damage type, so lava doesn't kill in a tick but an explosion still counts.
            // Fire, burning and lava share one timer (lava sets you on fire too).
            String type = source.isIn(net.minecraft.registry.tag.DamageTypeTags.IS_FIRE) ? "fire"
                    : source.getTypeRegistryEntry().getKey().map(key -> key.getValue().toString()).orElse("");
            if (state.hazardCooldown.containsKey(type)) {
                return false;
            }
            state.hazardCooldown.put(type, Downed.HAZARD_COOLDOWN);
            fails = 1;
        }
        player.world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_PLAYER_HURT,
                SoundCategory.PLAYERS, 1.0F, 0.8F);
        player.sendMessage(Text.literal("Hit while Downed: " + fails + " failed death save" + (fails == 1 ? "" : "s"))
                .formatted(Formatting.RED), true);
        DnDClasses.LOGGER.info("[Downed] {} hit while Downed by {}: +{} fails", player.getEntityName(),
                source.getName(), fails);
        DownedCombat.beforeFails(player, state, source, fails);
        Downed.addFails(player, state, fails);
        return false;
    }
}

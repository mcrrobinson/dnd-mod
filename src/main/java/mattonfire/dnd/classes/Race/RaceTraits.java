package mattonfire.dnd.classes.Race;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Damages.ModDamageTypes;
import mattonfire.dnd.classes.Downed.Downed;
import mattonfire.dnd.classes.Downed.DownedEvents;
import mattonfire.dnd.classes.Items.racial.RacialGear;
import mattonfire.dnd.classes.Rest.RestEvents;
import mattonfire.dnd.classes.Rest.RestKind;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * The d20 and combat racial traits:
 * <ul>
 * <li><b>Lucky</b> (Halfling): a natural 1 on a d20 is rerolled once and the new roll stands. It's the
 * character sheet's {@code rerollNaturalOnes} flag, added here as the {@link #CONTRIBUTOR_ID} contributor;
 * {@code D20.Builder} does the reroll, flags the Roll ({@code rerolled}) and the HUD shows a clover.
 * Checks, saves, death saves and attack rolls all take it from the sheet.</li>
 * <li><b>Savage Attacks</b> (Half-Orc): a critical hit deals x{@value #SAVAGE_CRIT_MULTIPLIER} instead of
 * x{@value #CRIT_MULTIPLIER} ({@link #critMultiplier}, used by {@code AttackRolls}).</li>
 * <li><b>Relentless Endurance</b> (Half-Orc): damage that would kill you leaves you at 1 HP with
 * {@value #RELENTLESS_RESISTANCE_TICKS} ticks of Resistance II, once every {@value #RELENTLESS_COOLDOWN_TICKS}
 * ticks; a long rest recharges it.</li>
 * <li><b>Hellish Rebuke</b> (Tiefling): a living thing that hits you in melee is set on fire for
 * {@value #REBUKE_FIRE_SECONDS} s and takes {@value #REBUKE_DAMAGE} fire damage. 20 s cooldown, halved by the
 * full Infernal Regalia ({@link RacialGear#rebukeCooldown}).</li>
 * </ul>
 *
 * <p>Cooldowns are overworld times in the persistent compound under {@code dndRace} (like the Breath
 * Weapon), so they survive death and rejoining. Using a trait and its recharge are announced on the action
 * bar.
 *
 * <p><b>Death order</b> ({@code ALLOW_DEATH} phases): Relentless Endurance ({@link #RELENTLESS_PHASE}) runs
 * first, then the Phylactery Locket ({@code dndclasses:phylactery_locket}), then the default phase, where
 * the Downed rules let a held totem pop, or down you, or kill you. So a free racial trait is spent before a
 * held totem or the Locket's once-per-long-rest charge. Relentless Endurance doesn't fire for bypass damage
 * (the void, {@code /kill}), while you're already Downed, or in creative and spectator; it does fire for
 * "massive damage" hits, which the Downed rules would otherwise turn into an outright kill.
 */
public final class RaceTraits {
    public static final Identifier CONTRIBUTOR_ID = new Identifier(DnDClasses.MOD_ID, "race_traits");
    public static final Identifier RELENTLESS_PHASE = new Identifier(DnDClasses.MOD_ID, "relentless_endurance");
    /** The Phylactery Locket's ALLOW_DEATH phase ({@code BossItems.registerHooks}). */
    public static final Identifier LOCKET_PHASE = new Identifier(DnDClasses.MOD_ID, "phylactery_locket");

    public static final float CRIT_MULTIPLIER = 2.0F;
    public static final float SAVAGE_CRIT_MULTIPLIER = 2.5F;

    public static final int RELENTLESS_COOLDOWN_TICKS = 10 * 60 * 20;
    public static final int RELENTLESS_RESISTANCE_TICKS = 2 * 20;
    /** Resistance II. */
    public static final int RELENTLESS_RESISTANCE_AMPLIFIER = 1;

    public static final int REBUKE_COOLDOWN_TICKS = 20 * 20;
    public static final int REBUKE_FIRE_SECONDS = 4;
    public static final float REBUKE_DAMAGE = 2.0F;
    public static final RegistryKey<DamageType> REBUKE_DAMAGE_TYPE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE,
            new Identifier(DnDClasses.MOD_ID, "hellish_rebuke"));

    public static final String RELENTLESS_READY_AT_KEY = "relentlessReadyAt";
    public static final String REBUKE_READY_AT_KEY = "rebukeReadyAt";
    /** Set while a recharge message is owed: the "ready again" line goes out once, when the cooldown ends. */
    private static final String RELENTLESS_NOTIFY_KEY = "relentlessNotify";
    private static final String REBUKE_NOTIFY_KEY = "rebukeNotify";

    private RaceTraits() {
    }

    /** Called from {@link RaceLifecycle#register()}. */
    public static void register() {
        // Lucky: the sheet flag every d20 builder reads. RaceAbilityBonuses.onChange rebuilds it on a race change.
        AbilityScores.register(CONTRIBUTOR_ID, (player, c) -> {
            if (RaceLifecycle.activeRaceOf(player) == DndRace.HALFLING) {
                c.rerollNaturalOnes("Halfling (Lucky)");
            }
        });

        ServerLivingEntityEvents.ALLOW_DEATH.addPhaseOrdering(RELENTLESS_PHASE, LOCKET_PHASE);
        ServerLivingEntityEvents.ALLOW_DEATH.addPhaseOrdering(RELENTLESS_PHASE, Event.DEFAULT_PHASE);
        ServerLivingEntityEvents.ALLOW_DEATH.register(RELENTLESS_PHASE, (entity, source, amount) -> !(entity
                instanceof ServerPlayerEntity player) || !relentlessEndurance(player, source, amount));

        RestEvents.AFTER_REST.register((player, kind, source) -> {
            if (kind == RestKind.LONG && relentlessRemaining(player) > 0) {
                data(player).putLong(RELENTLESS_READY_AT_KEY, 0);
                data(player).remove(RELENTLESS_NOTIFY_KEY);
                player.sendMessage(Text.translatable("message.dndclasses.relentless.recharged")
                        .formatted(Formatting.GOLD), true);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(RaceTraits::tick);
    }

    // --- Savage Attacks ---

    /** How much a critical hit multiplies the swing's damage: x2, or x2.5 for a Half-Orc. */
    public static float critMultiplier(PlayerEntity player) {
        return RaceLifecycle.activeRaceOf(player) == DndRace.HALFORC ? SAVAGE_CRIT_MULTIPLIER : CRIT_MULTIPLIER;
    }

    // --- Relentless Endurance ---

    /** Whether Relentless Endurance saves the player from this killing blow (and if so, applies it). */
    static boolean relentlessEndurance(ServerPlayerEntity player, DamageSource source, float amount) {
        if (RaceLifecycle.activeRaceOf(player) != DndRace.HALFORC || player.isCreative() || player.isSpectator()
                || Downed.is(player) || DownedEvents.bypasses(source, amount) || relentlessRemaining(player) > 0) {
            return false;
        }
        player.setHealth(1.0F);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, RELENTLESS_RESISTANCE_TICKS,
                RELENTLESS_RESISTANCE_AMPLIFIER, false, false, true));
        data(player).putLong(RELENTLESS_READY_AT_KEY, now(player) + RELENTLESS_COOLDOWN_TICKS);
        data(player).putBoolean(RELENTLESS_NOTIFY_KEY, true);
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.PLAYERS, 0.8F, 1.2F);
        player.sendMessage(Text.translatable("message.dndclasses.relentless.used",
                RELENTLESS_COOLDOWN_TICKS / 20 / 60).formatted(Formatting.DARK_RED), true);
        DnDClasses.LOGGER.info("[Race] {} Relentless Endurance: survived {} damage at 1 HP",
                player.getEntityName(), amount);
        return true;
    }

    /** Ticks until Relentless Endurance works again, 0 when ready. */
    public static long relentlessRemaining(ServerPlayerEntity player) {
        return remaining(player, RELENTLESS_READY_AT_KEY, RELENTLESS_COOLDOWN_TICKS);
    }

    // --- Hellish Rebuke ---

    /**
     * Called after a living entity actually took damage ({@code LivingEntityMixin}, at the end of
     * {@code damage} when it returned true). A Tiefling hit in melee by a living thing rebukes it.
     */
    public static void afterDamaged(LivingEntity victim, DamageSource source, float amount) {
        if (!(victim instanceof ServerPlayerEntity player) || !player.isAlive() || player.isSpectator()
                || RaceLifecycle.activeRaceOf(player) != DndRace.TIEFLING || Downed.is(player)) {
            return;
        }
        Entity attacker = source.getAttacker();
        if (!(attacker instanceof LivingEntity living) || attacker == player || !living.isAlive()
                || source.getSource() != attacker || !isMelee(source)) {
            return;
        }
        if (remaining(player, REBUKE_READY_AT_KEY, REBUKE_COOLDOWN_TICKS) > 0) {
            return;
        }
        int cooldown = RacialGear.rebukeCooldown(player, REBUKE_COOLDOWN_TICKS);
        data(player).putLong(REBUKE_READY_AT_KEY, now(player) + cooldown);
        data(player).putBoolean(REBUKE_NOTIFY_KEY, true);

        living.setOnFireFor(REBUKE_FIRE_SECONDS);
        living.damage(ModDamageTypes.of(player.getWorld(), REBUKE_DAMAGE_TYPE, player), REBUKE_DAMAGE);
        ServerWorld world = (ServerWorld) player.getWorld();
        world.spawnParticles(ParticleTypes.FLAME, living.getX(), living.getBodyY(0.5), living.getZ(), 16,
                living.getWidth() * 0.4, living.getHeight() * 0.3, living.getWidth() * 0.4, 0.02);
        world.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.ITEM_FIRECHARGE_USE,
                SoundCategory.PLAYERS, 0.8F, 0.9F);
        player.sendMessage(Text.translatable("message.dndclasses.rebuke.used", (cooldown + 19) / 20)
                .formatted(Formatting.GOLD), true);
        DnDClasses.LOGGER.info("[Race] {} Hellish Rebuke on {} ({} s cooldown)", player.getEntityName(),
                attacker.getName().getString(), cooldown / 20);
    }

    /** A direct hit with a weapon or a mob's body (not arrows, spells or thorns). */
    private static boolean isMelee(DamageSource source) {
        return source.isOf(DamageTypes.MOB_ATTACK) || source.isOf(DamageTypes.MOB_ATTACK_NO_AGGRO)
                || source.isOf(DamageTypes.PLAYER_ATTACK);
    }

    /** Ticks until Hellish Rebuke works again, 0 when ready. */
    public static long rebukeRemaining(ServerPlayerEntity player) {
        return remaining(player, REBUKE_READY_AT_KEY, REBUKE_COOLDOWN_TICKS);
    }

    /** Readies both cooldowns now (admin). */
    public static void reset(ServerPlayerEntity player) {
        NbtCompound data = data(player);
        data.putLong(RELENTLESS_READY_AT_KEY, 0);
        data.putLong(REBUKE_READY_AT_KEY, 0);
        data.remove(RELENTLESS_NOTIFY_KEY);
        data.remove(REBUKE_NOTIFY_KEY);
    }

    // --- recharge messages ---

    private static void tick(MinecraftServer server) {
        if (server.getTicks() % 20 != 0) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            NbtCompound persistent = ((IEntityDataSaver) player).getPersistentData();
            if (!persistent.contains(BreathWeapon.DATA_KEY)) {
                continue;
            }
            NbtCompound data = persistent.getCompound(BreathWeapon.DATA_KEY);
            if (data.getBoolean(RELENTLESS_NOTIFY_KEY) && relentlessRemaining(player) <= 0) {
                data.remove(RELENTLESS_NOTIFY_KEY);
                player.sendMessage(Text.translatable("message.dndclasses.relentless.recharged")
                        .formatted(Formatting.GOLD), true);
            }
            if (data.getBoolean(REBUKE_NOTIFY_KEY) && rebukeRemaining(player) <= 0) {
                data.remove(REBUKE_NOTIFY_KEY);
                player.sendMessage(Text.translatable("message.dndclasses.rebuke.recharged")
                        .formatted(Formatting.GOLD), true);
            }
        }
    }

    // --- cooldown storage, shared with the Breath Weapon's compound ---

    private static long now(ServerPlayerEntity player) {
        return player.getServer().getOverworld().getTime();
    }

    private static NbtCompound data(ServerPlayerEntity player) {
        NbtCompound persistent = ((IEntityDataSaver) player).getPersistentData();
        if (!persistent.contains(BreathWeapon.DATA_KEY)) {
            persistent.put(BreathWeapon.DATA_KEY, new NbtCompound());
        }
        return persistent.getCompound(BreathWeapon.DATA_KEY);
    }

    private static long remaining(ServerPlayerEntity player, String key, int maxTicks) {
        long left = data(player).getLong(key) - now(player);
        // Longer than a cooldown can be means the world clock went back (another save); don't lock it.
        return left <= 0 || left > maxTicks * 2L ? 0 : left;
    }
}

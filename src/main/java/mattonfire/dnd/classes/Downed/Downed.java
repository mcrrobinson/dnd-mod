package mattonfire.dnd.classes.Downed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.classes.mixin.LivingEntityInvoker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * The Downed state: a player at 0 HP who rolls death saves instead of dying (see {@link DownedEvents} for
 * when it happens and {@link DeathSaves} for the rolls).
 * <ul>
 * <li>{@link #is} and {@link #isStable} read the bits synced on the player's DataTracker, so they work on
 * every client too (the crawl pose, no jump or sprint).</li>
 * <li>The tally (passes, fails, stable timer, who downed you) lives on the server, mirrored in the persistent
 * compound under {@value #KEY} so a server stop resumes it. {@code ClassLifecycle} copies that compound on
 * death, so it's cleared on death and respawn.</li>
 * </ul>
 * API for other systems: {@link #down}, {@link #stabilise}, {@link #revive}, {@link #bleedOut}.
 */
public final class Downed {
    public static final String KEY = "dndDowned";
    public static final byte BIT_DOWNED = 1;
    public static final byte BIT_STABLE = 2;

    /** Ticks between death saves (one 5e round). */
    public static final int SAVE_INTERVAL = 120;
    public static final int PASSES_TO_STABLE = 3;
    public static final int FAILS_TO_DIE = 3;
    /** A stable player stands up on their own after this many ticks. */
    public static final int STABLE_TICKS = 600;
    /** Holding the power-up key this long gives up. */
    public static final int GIVE_UP_TICKS = 60;
    /** Damage immunity after standing up, so the same blow doesn't down you again. */
    public static final int GRACE_TICKS = 20;
    /** Each type of non-melee damage (lava, fire, explosions...) costs at most one fail per this many ticks. */
    public static final int HAZARD_COOLDOWN = 30;
    /** One attacker's melee hits cost fails at most this often. */
    public static final int MELEE_COOLDOWN = 10;
    /** Movement speed while Downed, on top of vanilla's crawl slowdown. */
    public static final double SPEED_PENALTY = -0.5D;

    private static final UUID SPEED_MODIFIER = UUID.fromString("0b7e3c52-6d1f-4c8a-a1f4-5e2d9b7a3c61");

    static final Map<UUID, State> STATES = new HashMap<>();
    /** Server tick each player's post-revive damage immunity ends. */
    private static final Map<UUID, Integer> GRACE = new HashMap<>();

    private Downed() {
    }

    /** One Downed player's tally (server side). */
    static final class State {
        int passes;
        int fails;
        boolean stable;
        /** Ticks to the next death save. */
        int nextSave = SAVE_INTERVAL;
        /** Ticks until a stable player stands up. */
        int stableLeft;
        @Nullable
        UUID attacker;
        String damageType = "minecraft:generic";
        /** Damage type -> ticks until it can cost another fail. */
        final Map<String, Integer> hazardCooldown = new HashMap<>();
        final Map<UUID, Integer> meleeCooldown = new HashMap<>();
        boolean givingUp;
        int giveUpHeld;
        int age;

        NbtCompound write() {
            NbtCompound nbt = new NbtCompound();
            nbt.putInt("passes", passes);
            nbt.putInt("fails", fails);
            nbt.putBoolean("stable", stable);
            nbt.putInt("stableLeft", stableLeft);
            if (attacker != null) {
                nbt.putUuid("attacker", attacker);
            }
            nbt.putString("damageType", damageType);
            return nbt;
        }

        static State read(NbtCompound nbt) {
            State state = new State();
            state.passes = nbt.getInt("passes");
            state.fails = nbt.getInt("fails");
            state.stable = nbt.getBoolean("stable");
            state.stableLeft = nbt.getInt("stableLeft");
            state.attacker = nbt.containsUuid("attacker") ? nbt.getUuid("attacker") : null;
            state.damageType = nbt.contains("damageType") ? nbt.getString("damageType") : "minecraft:generic";
            return state;
        }
    }

    // ---- Queries (both sides) ----

    public static boolean is(@Nullable PlayerEntity player) {
        return player instanceof PlayerEntityExt ext && (ext.getDownedBits() & BIT_DOWNED) != 0;
    }

    public static boolean isStable(@Nullable PlayerEntity player) {
        return player instanceof PlayerEntityExt ext && (ext.getDownedBits() & BIT_STABLE) != 0;
    }

    /** Passes, fails and seconds to the next save (or to standing up), or null if not Downed. Server only. */
    @Nullable
    public static int[] tally(ServerPlayerEntity player) {
        State state = STATES.get(player.getUuid());
        if (state == null) {
            return null;
        }
        return new int[] { state.passes, state.fails, ((state.stable ? state.stableLeft : state.nextSave) + 19) / 20 };
    }

    @Nullable
    static State state(ServerPlayerEntity player) {
        return STATES.get(player.getUuid());
    }

    /** True for a moment after standing up: hits are ignored so the same blow can't down you again. */
    public static boolean inGrace(ServerPlayerEntity player) {
        Integer until = GRACE.get(player.getUuid());
        return until != null && player.getServer().getTicks() < until;
    }

    // ---- State changes (server) ----

    /**
     * Downs a player: 1 HP, crawling, death saves every 6 s. Does nothing if they're already Downed or dead.
     * Called from {@code ALLOW_DEATH} with health already at 0 or below, so it only checks the death flag.
     */
    public static void down(ServerPlayerEntity player, DamageSource source) {
        if (STATES.containsKey(player.getUuid()) || player.isRemoved() || player.isDead() && player.deathTime > 0) {
            return;
        }
        State state = new State();
        Entity attacker = source.getAttacker();
        state.attacker = attacker == null || attacker == player ? null : attacker.getUuid();
        state.damageType = source.getTypeRegistryEntry().getKey().map(key -> key.getValue().toString())
                .orElse("minecraft:generic");
        STATES.put(player.getUuid(), state);

        player.setHealth(1.0F);
        player.removeStatusEffect(StatusEffects.REGENERATION);
        player.removeStatusEffect(StatusEffects.ABSORPTION);
        player.setAbsorptionAmount(0.0F);
        if (!player.isInLava() && !player.world.getStatesInBoxIfLoaded(player.getBoundingBox())
                .anyMatch(s -> s.isIn(net.minecraft.registry.tag.BlockTags.FIRE))) {
            player.extinguish();
        }
        player.stopFallFlying();
        player.stopRiding();
        player.stopUsingItem();
        player.setSprinting(false);
        player.closeHandledScreen();
        apply(player, state);
        save(player, state);

        player.world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_PLAYER_BIG_FALL,
                SoundCategory.PLAYERS, 1.0F, 0.6F);
        player.sendMessage(Text.literal("You're Downed! ").formatted(Formatting.RED, Formatting.BOLD)
                .append(Text.literal("Death save every 6 s: 3 passes and you're stable, 3 fails and you die. "
                        + "Hold the power-up key to give up.").formatted(Formatting.GRAY)), false);
        tellParty(player, Text.literal(player.getEntityName() + " is Downed!").formatted(Formatting.RED));
        DnDClasses.LOGGER.info("[Downed] {} downed by {}{}", player.getEntityName(), state.damageType,
                attacker == null ? "" : " (" + attacker.getEntityName() + ")");
        DownedEvents.AFTER_DOWNED.invoker().afterDowned(player, source);
    }

    /** Makes a Downed player stable: no more saves; they stand up on their own after 30 s. */
    public static void stabilise(ServerPlayerEntity player) {
        stabilise(player, STABLE_TICKS);
    }

    /** Stabilises with a custom stand-up timer (e.g. a Medicine success). */
    public static void stabilise(ServerPlayerEntity player, int standUpTicks) {
        State state = STATES.get(player.getUuid());
        if (state == null) {
            return;
        }
        state.stable = true;
        state.passes = 0;
        state.fails = 0;
        state.stableLeft = standUpTicks;
        apply(player, state);
        save(player, state);
        player.sendMessage(Text.literal("You're stable. ").formatted(Formatting.YELLOW, Formatting.BOLD)
                .append(Text.literal("You'll stand up in " + (standUpTicks + 19) / 20 + " s, unless you're hit.")
                        .formatted(Formatting.GRAY)), false);
        tellParty(player, Text.literal(player.getEntityName() + " is stable.").formatted(Formatting.YELLOW));
        DnDClasses.LOGGER.info("[Downed] {} is stable", player.getEntityName());
    }

    /** Stands a Downed player up with {@code health} HP. Does nothing if they aren't Downed. */
    public static void revive(ServerPlayerEntity player, float health) {
        if (STATES.remove(player.getUuid()) == null) {
            return;
        }
        release(player);
        player.setHealth(Math.max(1.0F, Math.min(health, player.getMaxHealth())));
        standUpEffects(player);
        player.sendMessage(Text.literal("You're back on your feet!").formatted(Formatting.GREEN, Formatting.BOLD),
                false);
        tellParty(player, Text.literal(player.getEntityName() + " is back up.").formatted(Formatting.GREEN));
        DnDClasses.LOGGER.info("[Downed] {} stood up with {} HP", player.getEntityName(), player.getHealth());
    }

    /** Resistance I for 3 s, a moment of damage immunity, and a little fanfare. Also used by Last Stand. */
    static void standUpEffects(ServerPlayerEntity player) {
        GRACE.put(player.getUuid(), player.getServer().getTicks() + GRACE_TICKS);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 0));
        player.world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_PLAYER_LEVELUP,
                SoundCategory.PLAYERS, 0.6F, 1.4F);
        ((ServerWorld) player.world).spawnParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0D,
                player.getZ(), 6, 0.4D, 0.4D, 0.4D, 0.0D);
    }

    /**
     * The player dies from their wounds, with the source that downed them (death message, kill credit,
     * advancements). A Totem of Undying in hand still saves them.
     */
    public static void bleedOut(ServerPlayerEntity player) {
        State state = STATES.remove(player.getUuid());
        if (state == null) {
            return;
        }
        release(player);
        DamageSource source = source(player, state);
        if (((LivingEntityInvoker) player).dnd$tryUseTotem(source)) {
            GRACE.put(player.getUuid(), player.getServer().getTicks() + GRACE_TICKS);
            DnDClasses.LOGGER.info("[Downed] {} was saved by a totem", player.getEntityName());
            return;
        }
        DnDClasses.LOGGER.info("[Downed] {} died ({})", player.getEntityName(), state.damageType);
        // The damage tracker forgets hits after a few seconds; put the downing blow back for the message.
        player.getDamageTracker().onDamage(source, player.getHealth(), player.getHealth());
        player.setHealth(0.0F);
        player.onDeath(source);
    }

    /** Drops the state without any effect (the player died some other way, or respawned). */
    static void forget(ServerPlayerEntity player) {
        STATES.remove(player.getUuid());
        GRACE.remove(player.getUuid());
        release(player);
    }

    /** Clears the bits, the speed modifier and the saved state. */
    private static void release(ServerPlayerEntity player) {
        ((PlayerEntityExt) player).setDownedBits((byte) 0);
        EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SPEED_MODIFIER);
        }
        ((IEntityDataSaver) player).getPersistentData().remove(KEY);
    }

    /** Sets the synced bits and the speed modifier from the state. */
    private static void apply(ServerPlayerEntity player, State state) {
        ((PlayerEntityExt) player).setDownedBits((byte) (BIT_DOWNED | (state.stable ? BIT_STABLE : 0)));
        EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(SPEED_MODIFIER) == null) {
            speed.addTemporaryModifier(new EntityAttributeModifier(SPEED_MODIFIER, "Downed", SPEED_PENALTY,
                    EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void save(ServerPlayerEntity player, State state) {
        ((IEntityDataSaver) player).getPersistentData().put(KEY, state.write());
    }

    /** On join: resumes a state saved by a server stop, with a fresh 6 s timer. */
    static void resume(ServerPlayerEntity player) {
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        if (!data.contains(KEY)) {
            return;
        }
        if (!player.isAlive()) {
            data.remove(KEY);
            return;
        }
        State state = State.read(data.getCompound(KEY));
        STATES.put(player.getUuid(), state);
        player.setHealth(Math.min(player.getHealth(), 1.0F));
        apply(player, state);
        player.sendMessage(Text.literal("You're still Downed.").formatted(Formatting.RED), false);
        DnDClasses.LOGGER.info("[Downed] {} resumed Downed ({} passes, {} fails)", player.getEntityName(),
                state.passes, state.fails);
    }

    /** A new player entity for the same player (leaving the End): keep the state on it. */
    static void reapply(ServerPlayerEntity player) {
        State state = STATES.get(player.getUuid());
        if (state != null) {
            apply(player, state);
            save(player, state);
        }
    }

    /** The source that downed the player, rebuilt from what was stored. */
    static DamageSource source(ServerPlayerEntity player, State state) {
        RegistryEntry<DamageType> type = player.getServer().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE)
                .getEntry(RegistryKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier(state.damageType)))
                .map(entry -> (RegistryEntry<DamageType>) entry).orElse(null);
        if (type == null) {
            return player.getDamageSources().generic();
        }
        if (state.attacker == null) {
            return new DamageSource(type);
        }
        Entity attacker = null;
        for (ServerWorld world : player.getServer().getWorlds()) {
            attacker = world.getEntity(state.attacker);
            if (attacker != null) {
                break;
            }
        }
        // An attack type with its attacker gone would print a broken message: fall back to generic
        return attacker != null ? new DamageSource(type, attacker) : player.getDamageSources().generic();
    }

    // ---- Death save bookkeeping ----

    /** Adds fails (from a roll or a hit); three and the player bleeds out. */
    static void addFails(ServerPlayerEntity player, State state, int count) {
        state.fails = Math.min(FAILS_TO_DIE, state.fails + count);
        if (state.stable) {
            // A hit knocks a stable player back to rolling saves.
            state.stable = false;
            state.nextSave = SAVE_INTERVAL;
            apply(player, state);
        }
        save(player, state);
        if (state.fails >= FAILS_TO_DIE) {
            bleedOut(player);
        }
    }

    static void addPass(ServerPlayerEntity player, State state) {
        state.passes++;
        save(player, state);
        if (state.passes >= PASSES_TO_STABLE) {
            stabilise(player);
        }
    }

    /** Every server tick: saves, the stable timer, giving up, and closing screens. */
    static void tick(MinecraftServer server) {
        if (STATES.isEmpty()) {
            GRACE.values().removeIf(until -> until < server.getTicks());
            return;
        }
        for (Map.Entry<UUID, State> entry : new ArrayList<>(STATES.entrySet())) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            State state = entry.getValue();
            if (player == null || STATES.get(entry.getKey()) != state) {
                continue;
            }
            if (!player.isAlive()) {
                forget(player);
                continue;
            }
            state.age++;
            state.hazardCooldown.replaceAll((k, v) -> v - 1);
            state.hazardCooldown.values().removeIf(v -> v <= 0);
            state.meleeCooldown.replaceAll((k, v) -> v - 1);
            state.meleeCooldown.values().removeIf(v -> v <= 0);
            if (player.currentScreenHandler != player.playerScreenHandler) {
                player.closeHandledScreen();
            }
            if (player.getHealth() > 1.0F) {
                player.setHealth(1.0F);
            }

            if (state.givingUp) {
                if (++state.giveUpHeld >= GIVE_UP_TICKS) {
                    player.sendMessage(Text.literal("You give in to your wounds.").formatted(Formatting.DARK_RED),
                            false);
                    bleedOut(player);
                    continue;
                }
            } else {
                state.giveUpHeld = 0;
            }

            if (state.stable) {
                if (--state.stableLeft <= 0) {
                    revive(player, 1.0F);
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 200, 0));
                    continue;
                }
            } else if (--state.nextSave <= 0) {
                state.nextSave = SAVE_INTERVAL;
                DeathSaves.rollSave(player, state);
                if (STATES.get(player.getUuid()) != state) {
                    continue;
                }
            }
            if (state.age % 20 == 0) {
                player.sendMessage(statusLine(state), true);
            }
        }
    }

    /** The action bar line: "DOWNED  ●●○  ✕○○  next save 4 s  (hold Z to give up)". */
    static Text statusLine(State state) {
        if (state.stable) {
            return Text.literal("STABLE").formatted(Formatting.YELLOW, Formatting.BOLD)
                    .append(Text.literal("  standing up in " + (state.stableLeft + 19) / 20 + " s")
                            .formatted(Formatting.GRAY));
        }
        MutableText line = Text.literal("DOWNED  ").formatted(Formatting.RED, Formatting.BOLD);
        line.append(Text.literal("●".repeat(state.passes) + "○".repeat(PASSES_TO_STABLE - state.passes) + "  ")
                .formatted(Formatting.GREEN).styled(s -> s.withBold(false)));
        line.append(Text.literal("✕".repeat(state.fails) + "○".repeat(FAILS_TO_DIE - state.fails) + "  ")
                .formatted(Formatting.RED).styled(s -> s.withBold(false)));
        line.append(Text.literal("next save in " + (state.nextSave + 19) / 20 + " s")
                .formatted(Formatting.GRAY).styled(s -> s.withBold(false)));
        if (state.giveUpHeld > 0) {
            line.append(Text.literal("  giving up...").formatted(Formatting.DARK_RED)
                    .styled(s -> s.withBold(false)));
        }
        return line;
    }

    /** Whether the player is holding the give-up key (C2S hold packet). */
    static void setGivingUp(ServerPlayerEntity player, boolean holding) {
        State state = STATES.get(player.getUuid());
        if (state != null) {
            state.givingUp = holding;
            if (!holding) {
                state.giveUpHeld = 0;
            }
        }
    }

    private static void tellParty(ServerPlayerEntity player, Text message) {
        for (ServerPlayerEntity member : PartyManager.nearbyMembers(player, 256)) {
            member.sendMessage(message, false);
        }
    }
}

package mattonfire.dnd.classes.Abilities;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.ClassInfo;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.Progression.Progression;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Every player's {@link CharacterSheet}: the entry point for ability scores, proficiency, check and save
 * bonuses. Other features add to sheets by registering an {@link AbilityContributor}:
 *
 * <pre>{@code
 * AbilityScores.register(new Identifier("dndclasses", "race"), (player, c) -> {
 *     if (Races.of(player) == Race.ELF) {
 *         c.add(Ability.DEX, 2, "Elf").proficiency(Skill.PERCEPTION, Proficiency.PROFICIENT, "Elf");
 *     }
 * });
 * // ...and when the player's race changes:
 * AbilityScores.invalidate(player);
 * }</pre>
 *
 * Sheets are built on the server, cached per player, and sent to the player's client
 * ({@link #S2C_SHEET_SYNC}, kept in {@link CharacterSheet#client}).
 */
public final class AbilityScores {
    public static final Identifier S2C_SHEET_SYNC = new Identifier(DnDClasses.MOD_ID, "sheet_sync");
    public static final Identifier CLASS = new Identifier(DnDClasses.MOD_ID, "class");
    public static final Identifier ADMIN = new Identifier(DnDClasses.MOD_ID, "admin");
    /** Persistent data key for {@code /dndclass score} overrides (ability short key to score). */
    public static final String OVERRIDES_KEY = "dndAbilityOverrides";
    /** How often sheets with a dynamic contributor are rebuilt. */
    private static final int DYNAMIC_REFRESH_TICKS = 20;

    private record Registered(AbilityContributor contributor, boolean dynamic) {
    }

    private record Cached(CharacterSheet sheet, int builtAt) {
    }

    private static final Map<Identifier, Registered> CONTRIBUTORS = new LinkedHashMap<>();
    private static final Map<UUID, Cached> CACHE = new HashMap<>();
    private static boolean anyDynamic;

    private AbilityScores() {
    }

    /** Adds a contributor; registration order is breakdown order. Re-registering an id replaces it. */
    public static void register(Identifier id, AbilityContributor contributor) {
        register(id, contributor, false);
    }

    /**
     * @param dynamic the contributor reads state that changes without anyone calling {@link #invalidate}
     *                (status effects, nearby allies), so sheets are rebuilt at most once a second
     */
    public static synchronized void register(Identifier id, AbilityContributor contributor, boolean dynamic) {
        CONTRIBUTORS.put(id, new Registered(contributor, dynamic));
        anyDynamic |= dynamic;
        CACHE.clear();
    }

    /** Proficiency bonus by class level: +2 at 0-4, +3 at 5-8, +4 at 9-10. */
    public static int proficiencyBonus(int level) {
        return level >= 9 ? 4 : level >= 5 ? 3 : 2;
    }

    /** The player's sheet. On the client this is the synced copy for the client's own player. */
    public static CharacterSheet sheet(PlayerEntity player) {
        if (player.getWorld().isClient) {
            return CharacterSheet.client;
        }
        Cached cached = CACHE.get(player.getUuid());
        if (cached == null || anyDynamic && player.age - cached.builtAt() >= DYNAMIC_REFRESH_TICKS
                || player.age < cached.builtAt()) {
            cached = new Cached(build(player), player.age);
            CACHE.put(player.getUuid(), cached);
        }
        return cached.sheet();
    }

    public static int score(PlayerEntity player, Ability ability) {
        return sheet(player).score(ability);
    }

    public static int modifier(PlayerEntity player, Ability ability) {
        return sheet(player).modifier(ability);
    }

    public static int checkBonus(PlayerEntity player, Skill skill) {
        return sheet(player).check(skill);
    }

    public static int saveBonus(PlayerEntity player, Ability ability) {
        return sheet(player).save(ability);
    }

    public static int passive(PlayerEntity player, Skill skill) {
        return sheet(player).passive(skill);
    }

    /** Rebuilds the player's sheet and sends it to their client. Call when a contributor's input changes. */
    public static void invalidate(PlayerEntity player) {
        CACHE.remove(player.getUuid());
        if (player instanceof ServerPlayerEntity serverPlayer && serverPlayer.networkHandler != null) {
            PacketByteBuf buf = PacketByteBufs.create();
            sheet(player).write(buf);
            ServerPlayNetworking.send(serverPlayer, S2C_SHEET_SYNC, buf);
        }
    }

    /** Drops the cached sheet; called on disconnect. */
    public static void forget(UUID player) {
        CACHE.remove(player);
    }

    private static CharacterSheet build(PlayerEntity player) {
        DndCharacter dndClass = Progression.classOf(player);
        int level = dndClass == DndCharacter.NONE ? 0 : Progression.current(player).level();
        Contribution c = new Contribution();
        Map<Identifier, Registered> contributors;
        synchronized (AbilityScores.class) {
            contributors = new LinkedHashMap<>(CONTRIBUTORS);
        }
        for (Map.Entry<Identifier, Registered> e : contributors.entrySet()) {
            try {
                e.getValue().contributor().contribute(player, c);
            } catch (RuntimeException ex) {
                DnDClasses.LOGGER.error("Ability contributor " + e.getKey() + " failed for "
                        + player.getEntityName(), ex);
            }
        }
        return c.build(dndClass, level, proficiencyBonus(level));
    }

    /** The {@code /dndclass score} override for an ability, or null. */
    @Nullable
    public static Integer override(PlayerEntity player, Ability ability) {
        NbtCompound overrides = ((IEntityDataSaver) player).getPersistentData().getCompound(OVERRIDES_KEY);
        return overrides.contains(ability.shortKey()) ? overrides.getInt(ability.shortKey()) : null;
    }

    /** Sets (or with null, clears) an admin override and resyncs the sheet. */
    public static void setOverride(PlayerEntity player, Ability ability, @Nullable Integer score) {
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        NbtCompound overrides = data.getCompound(OVERRIDES_KEY);
        if (score == null) {
            overrides.remove(ability.shortKey());
        } else {
            overrides.putInt(ability.shortKey(), score);
        }
        if (overrides.isEmpty()) {
            data.remove(OVERRIDES_KEY);
        } else {
            data.put(OVERRIDES_KEY, overrides);
        }
        invalidate(player);
    }

    /** Validates class_info.json and registers the built-in contributors. Called once at startup. */
    public static void bootstrap() {
        ClassAbilities.validateAll();
        register(CLASS, AbilityScores::classContribution);
        register(ADMIN, (player, c) -> {
            for (Ability a : Ability.values()) {
                Integer score = override(player, a);
                if (score != null) {
                    c.override(a, score, "Admin override");
                }
            }
        });
    }

    private static void classContribution(PlayerEntity player, Contribution c) {
        DndCharacter dndClass = Progression.classOf(player);
        ClassInfo info = ClassInfo.get(dndClass);
        ClassAbilities abilities = ClassAbilities.of(dndClass);
        if (info == null) {
            return;
        }
        String source = info.name();
        for (Ability a : Ability.values()) {
            c.base(a, abilities.score(a), source);
        }
        for (Ability a : abilities.saves()) {
            c.saveProficiency(a, Proficiency.PROFICIENT, source);
        }
        for (Skill s : abilities.skills()) {
            c.proficiency(s, abilities.proficiency(s), source);
        }
        if (dndClass == DndCharacter.FIGHTER) {
            c.critRange(19, "Improved Critical");
        }
    }
}

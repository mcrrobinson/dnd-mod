package mattonfire.dnd.classes.SkillChecks;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PotionImmunity;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.Classes.PaladinSkills;
import mattonfire.dnd.classes.Registry.ModEffects;
import mattonfire.dnd.magic.items.BossItems;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Data-driven saving throws against status effects and damage types from any mob, vanilla included, read from
 * {@code data/<namespace>/saving_throws/*.json}. A datapack can add, change or remove (by overriding a file with
 * {@code "enabled": false}, or replacing it) a rule, and {@code /reload} picks it up.
 *
 * <pre>{@code
 * {"effect": "minecraft:poison", "ability": "CON", "default_dc": 12,
 *  "dc_by_source": {"minecraft:cave_spider": 11}, "on_success": "half_duration",
 *  "hostile_source_only": true, "label": "save.dndclasses.poison", "tags": ["poison"]}
 * {"damage_type": "minecraft:sonic_boom", "ability": "CON", "dc": 15, "on_success": "half"}
 * }</pre>
 *
 * Fields:
 * <ul>
 * <li>{@code effect} or {@code damage_type}: what the rule saves against (exactly one).</li>
 * <li>{@code ability}: the save's ability ("CON", "dex", "wisdom").</li>
 * <li>{@code dc} / {@code default_dc}: the DC for any source not in {@code dc_by_source}. Leave it out to roll only
 * against the listed sources.</li>
 * <li>{@code dc_by_source}: entity type id to DC. The source is the attacker (a projectile's or cloud's owner).</li>
 * <li>{@code on_success}: {@code half_duration} (effects), {@code half} (damage) or {@code negate} (either).</li>
 * <li>{@code hostile_source_only}: only roll when the source is a hostile mob ({@link Monster}).</li>
 * <li>{@code label}: translation key shown on the save lane (defaults to the effect's name or "Damage").</li>
 * <li>{@code tags}: keywords for advantage filters ("poison").</li>
 * <li>{@code window}: ticks one roll covers per source (one exposure); default {@link #DEFAULT_WINDOW}. Damage
 * from a cloud or projectile is keyed by that entity, so one breath cloud is one exposure.</li>
 * </ul>
 *
 * Only players save. No roll happens when the effect or damage comes from nowhere, from the player themselves (a
 * potion they drink) or from another player; when the player is immune (Rogue poison, Wizard freeze, Purity of
 * Spirit, Iron Stomach, magic items); when the effect wouldn't change anything; or when the player already made a
 * save this tick (the trap or attack that caused it rolled its own, see {@link SavingThrow#savedThisTick}).
 */
public final class SaveRules {
    public static final String FOLDER = "saving_throws";
    /** One roll per source and rule for this long unless the rule sets {@code window}. */
    public static final int DEFAULT_WINDOW = 40;

    public enum OnSuccess {
        HALF_DURATION("save.dndclasses.effect.half_duration"),
        HALF("save.dndclasses.effect.half_damage"),
        NEGATE("save.dndclasses.effect.negated");

        final String effectKey;

        OnSuccess(String effectKey) {
            this.effectKey = effectKey;
        }
    }

    /** One loaded rule. Exactly one of {@code effect} and {@code damageType} is set. */
    public record Rule(Identifier id, @Nullable Identifier effect, @Nullable Identifier damageType, Ability ability,
            @Nullable Integer defaultDc, Map<Identifier, Integer> dcBySource, OnSuccess onSuccess,
            boolean hostileSourceOnly, @Nullable String label, String[] tags, int window) {

        /** The DC against this source, or null when the rule doesn't cover it. */
        @Nullable
        public Integer dcFor(@Nullable Entity source) {
            if (source == null) {
                return null;
            }
            if (hostileSourceOnly && !(source instanceof Monster)) {
                return null;
            }
            Integer dc = dcBySource.get(Registries.ENTITY_TYPE.getId(source.getType()));
            return dc != null ? dc : defaultDc;
        }

        Text labelText() {
            if (label != null) {
                return Text.translatable(label);
            }
            if (effect != null) {
                StatusEffect type = Registries.STATUS_EFFECT.get(effect);
                if (type != null) {
                    return type.getName();
                }
            }
            return Text.translatable("save.dndclasses.damage");
        }

        String labelKey() {
            if (label != null) {
                return label;
            }
            if (effect != null) {
                StatusEffect type = Registries.STATUS_EFFECT.get(effect);
                if (type != null) {
                    return type.getTranslationKey();
                }
            }
            return "save.dndclasses.damage";
        }
    }

    private static volatile List<Rule> rules = List.of();
    /** Set while re-adding an effect with the halved duration, so the mixin doesn't save against it again. */
    private static final ThreadLocal<Boolean> REAPPLYING = ThreadLocal.withInitial(() -> false);

    private SaveRules() {
    }

    public static List<Rule> rules() {
        return rules;
    }

    public static void register() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return new Identifier(DnDClasses.MOD_ID, FOLDER);
            }

            @Override
            public void reload(ResourceManager manager) {
                load(manager);
            }
        });
        MobSaveInfo.registerProvider(SaveRules::studyEntries);
    }

    static void load(ResourceManager manager) {
        List<Rule> loaded = new ArrayList<>();
        for (Map.Entry<Identifier, Resource> entry : manager.findResources(FOLDER,
                path -> path.getPath().endsWith(".json")).entrySet()) {
            Identifier file = entry.getKey();
            String path = file.getPath().substring(FOLDER.length() + 1, file.getPath().length() - ".json".length());
            Identifier id = new Identifier(file.getNamespace(), path);
            try (Reader reader = entry.getValue().getReader()) {
                JsonElement json = JsonParser.parseReader(reader);
                if (!json.isJsonObject()) {
                    throw new IllegalArgumentException("not a JSON object");
                }
                Rule rule = parse(id, json.getAsJsonObject());
                if (rule != null) {
                    loaded.add(rule);
                }
            } catch (Exception e) {
                DnDClasses.LOGGER.error("[SaveRules] Skipping saving throw {} ({}): {}", id, file, e.getMessage());
            }
        }
        rules = List.copyOf(loaded);
        DnDClasses.LOGGER.info("[SaveRules] Loaded {} data-driven saving throws", loaded.size());
    }

    /** Parses one rule; null when it's switched off ({@code "enabled": false}). */
    @Nullable
    static Rule parse(Identifier id, JsonObject o) {
        if (o.has("enabled") && !o.get("enabled").getAsBoolean()) {
            return null;
        }
        Identifier effect = o.has("effect") ? new Identifier(o.get("effect").getAsString()) : null;
        Identifier damageType = o.has("damage_type") ? new Identifier(o.get("damage_type").getAsString()) : null;
        if ((effect == null) == (damageType == null)) {
            throw new IllegalArgumentException("needs exactly one of \"effect\" and \"damage_type\"");
        }
        if (effect != null && !Registries.STATUS_EFFECT.containsId(effect)) {
            throw new IllegalArgumentException("unknown effect " + effect);
        }
        if (!o.has("ability")) {
            throw new IllegalArgumentException("missing \"ability\"");
        }
        Ability ability = Ability.byId(o.get("ability").getAsString());
        if (ability == null) {
            throw new IllegalArgumentException("unknown ability " + o.get("ability").getAsString());
        }
        Integer defaultDc = o.has("default_dc") ? Integer.valueOf(o.get("default_dc").getAsInt())
                : o.has("dc") ? Integer.valueOf(o.get("dc").getAsInt()) : null;
        Map<Identifier, Integer> bySource = new LinkedHashMap<>();
        if (o.has("dc_by_source")) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("dc_by_source").entrySet()) {
                bySource.put(new Identifier(e.getKey()), e.getValue().getAsInt());
            }
        }
        if (defaultDc == null && bySource.isEmpty()) {
            throw new IllegalArgumentException("needs \"dc\"/\"default_dc\" or \"dc_by_source\"");
        }
        OnSuccess onSuccess;
        String success = o.has("on_success") ? o.get("on_success").getAsString()
                : effect != null ? "half_duration" : "half";
        try {
            onSuccess = OnSuccess.valueOf(success.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown on_success " + success);
        }
        if (onSuccess == OnSuccess.HALF_DURATION && effect == null) {
            throw new IllegalArgumentException("half_duration needs an \"effect\"");
        }
        if (onSuccess == OnSuccess.HALF && damageType == null) {
            throw new IllegalArgumentException("half needs a \"damage_type\"");
        }
        String[] tags = new String[0];
        if (o.has("tags")) {
            JsonArray array = o.getAsJsonArray("tags");
            tags = new String[array.size()];
            for (int i = 0; i < tags.length; i++) {
                tags[i] = array.get(i).getAsString();
            }
        }
        boolean hostileOnly = o.has("hostile_source_only") && o.get("hostile_source_only").getAsBoolean();
        String label = o.has("label") ? o.get("label").getAsString() : null;
        int window = o.has("window") ? Math.max(1, o.get("window").getAsInt()) : DEFAULT_WINDOW;
        return new Rule(id, effect, damageType, ability, defaultDc, Map.copyOf(bySource), onSuccess, hostileOnly,
                label, tags, window);
    }

    // ---------------------------------------------------------------- effects

    /**
     * Saves against an effect a player is about to get. Returns {@code effect} to apply it unchanged, null to drop
     * it (a negating save), or a shorter copy (a halving save). Called from {@code SavingThrowMixin} at the head of
     * {@code LivingEntity.addStatusEffect(StatusEffectInstance, Entity)}.
     */
    @Nullable
    public static StatusEffectInstance onEffect(LivingEntity target, StatusEffectInstance effect,
            @Nullable Entity source) {
        if (REAPPLYING.get() || !(target instanceof ServerPlayerEntity player) || source == null || source == target
                || source instanceof PlayerEntity || player.isCreative() || player.isSpectator()
                || effect.isInfinite()) {
            return effect;
        }
        Identifier effectId = Registries.STATUS_EFFECT.getId(effect.getEffectType());
        Rule rule = null;
        Integer dc = null;
        for (Rule r : rules) {
            if (effectId != null && effectId.equals(r.effect())) {
                dc = r.dcFor(source);
                if (dc != null) {
                    rule = r;
                    break;
                }
            }
        }
        if (rule == null || immune(player, effect) || unchanged(player, effect)
                || SavingThrow.savedThisTick(player)) {
            return effect;
        }
        SaveResult result = SavingThrow.of(player, rule.ability(), dc).label(rule.labelText()).source(source)
                .exposure(source, "rule:" + rule.id(), rule.window()).tags(rule.tags())
                .onSuccess(Text.translatable(rule.onSuccess().effectKey))
                .onFailure(Text.translatable("save.dndclasses.effect.full_effect")).roll();
        if (result.failed()) {
            return effect;
        }
        if (rule.onSuccess() == OnSuccess.NEGATE) {
            return null;
        }
        int ticks = result.duration(effect.getDuration());
        if (ticks <= 0) {
            return null;
        }
        return new StatusEffectInstance(effect.getEffectType(), ticks, effect.getAmplifier(), effect.isAmbient(),
                effect.shouldShowParticles(), effect.shouldShowIcon());
    }

    /** Adds the saved-against effect without saving again. */
    public static boolean reapply(LivingEntity target, StatusEffectInstance effect, @Nullable Entity source) {
        REAPPLYING.set(true);
        try {
            return target.addStatusEffect(effect, source);
        } finally {
            REAPPLYING.set(false);
        }
    }

    /** The existing immunities: no save when the effect wouldn't land (or is stripped straight away) anyway. */
    static boolean immune(PlayerEntity player, StatusEffectInstance effect) {
        StatusEffect type = effect.getEffectType();
        if (!player.canHaveStatusEffect(effect) || PaladinSkills.blocksEffect(player, type)
                || BossItems.blocksEffect(player, type) || PotionImmunity.shouldBlock(player, type)) {
            return true;
        }
        DndCharacter dndClass = Progression.classOf(player);
        if (dndClass == DndCharacter.ROGUE && type == StatusEffects.POISON
                || dndClass == DndCharacter.WIZARD && type == ModEffects.FREEZE) {
            return true;
        }
        return dndClass == DndCharacter.ALCHEMIST
                && (type == StatusEffects.POISON || type == StatusEffects.WITHER || type == StatusEffects.NAUSEA)
                && Progression.get(player, dndClass).hasPassive("alchemist.iron_stomach");
    }

    /** True when the player already has at least this effect, so adding it changes nothing and isn't worth a roll. */
    private static boolean unchanged(PlayerEntity player, StatusEffectInstance effect) {
        StatusEffectInstance current = player.getStatusEffect(effect.getEffectType());
        return current != null && current.getAmplifier() >= effect.getAmplifier()
                && (current.isInfinite() || current.getDuration() >= effect.getDuration());
    }

    // ---------------------------------------------------------------- damage

    /**
     * Saves against damage a player is about to take and returns the amount after it. Called from
     * {@code SavingThrowMixin} at the head of {@code LivingEntity.damage}, before armour and the Paladin aura.
     */
    public static float onDamage(LivingEntity target, DamageSource source, float amount) {
        if (amount <= 0.0F || !(target instanceof ServerPlayerEntity player) || player.isCreative()
                || player.isSpectator() || player.isInvulnerableTo(source) || player.isDead()) {
            return amount;
        }
        Entity attacker = source.getAttacker();
        if (attacker == player || attacker instanceof PlayerEntity) {
            return amount;
        }
        Rule rule = null;
        Integer dc = null;
        for (Rule r : rules) {
            if (r.damageType() != null && source.getTypeRegistryEntry().matchesId(r.damageType())) {
                dc = r.dcFor(attacker != null ? attacker : source.getSource());
                if (dc != null) {
                    rule = r;
                    break;
                }
            }
        }
        if (rule == null || SavingThrow.savedThisTick(player)) {
            return amount;
        }
        // A cloud or projectile is its own exposure (one breath cloud = one save)
        Entity exposure = source.getSource() != null ? source.getSource() : attacker;
        SavingThrow.Builder save = SavingThrow.of(player, rule.ability(), dc).label(rule.labelText())
                .source(attacker).exposure(exposure, "rule:" + rule.id(), rule.window()).tags(rule.tags())
                .onSuccess(Text.translatable(rule.onSuccess().effectKey));
        if (rule.onSuccess() == OnSuccess.HALF) {
            save.halvesDamage();
        } else {
            save.onFailure(Text.translatable("save.dndclasses.effect.full_damage"));
        }
        SaveResult result = save.roll();
        if (rule.onSuccess() == OnSuccess.NEGATE) {
            return result.succeeded() ? 0.0F : amount;
        }
        return result.damage(amount);
    }

    // ---------------------------------------------------------------- study

    /** The rules that name this creature in {@code dc_by_source}, for the Study card. */
    private static List<MobSaveInfo.Entry> studyEntries(EntityType<?> type) {
        Identifier id = Registries.ENTITY_TYPE.getId(type);
        List<MobSaveInfo.Entry> list = new ArrayList<>();
        for (Rule rule : rules) {
            Integer dc = rule.dcBySource().get(id);
            if (dc != null) {
                list.add(new MobSaveInfo.Entry(type, rule.labelKey(), rule.ability(), dc,
                        rule.onSuccess() == OnSuccess.HALF, rule.onSuccess().effectKey));
            }
        }
        return list;
    }
}

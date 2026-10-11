package mattonfire.dnd.classes.Items.racial;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Items.lib.FAArmorEffectHandler;
import mattonfire.dnd.classes.Race.DragonAncestry;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.classes.Registry.ModEffects;
import mattonfire.dnd.classes.Registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

/**
 * The racial gear's effects that aren't status effects: the hooks the mixins and damage events call.
 * <ul>
 * <li>Elven Chain: an Elf in the full set makes no sculk vibrations while sneaking.</li>
 * <li>Draconic Scale: a Dragonborn in the full set takes no damage from their ancestry's element (Ember: fire,
 * Frost: freezing and the Freeze effect, Storm: lightning).</li>
 * <li>Infernal Regalia: a Tiefling in the full set has Hellish Rebuke's cooldown halved.</li>
 * <li>Dwarven Waraxe: +50% damage to goblins ({@code #dndclasses:goblins}).</li>
 * </ul>
 * Rarity and attunement hooks for magic items are item tags: {@code #dndclasses:rarity/<tier>} and
 * {@code #dndclasses:racial_gear/<race>}.
 */
public final class RacialGear {
    public static final TagKey<EntityType<?>> GOBLINS = TagKey.of(RegistryKeys.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "goblins"));
    /** The Dwarven Waraxe's damage against goblins. */
    public static final float WARAXE_GOBLIN_MULTIPLIER = 1.5F;
    /** Hellish Rebuke's cooldown for a Tiefling in the full Infernal Regalia. */
    public static final float REBUKE_COOLDOWN_MULTIPLIER = 0.5F;

    private RacialGear() {
    }

    /** Racial gear tagged for a race: {@code #dndclasses:racial_gear/<race id>} (attunement hook for area 5). */
    public static TagKey<Item> racialGearTag(mattonfire.dnd.classes.Race.DndRace race) {
        return TagKey.of(RegistryKeys.ITEM, new Identifier(DnDClasses.MOD_ID, "racial_gear/" + race.id()));
    }

    /** Elven Chain: a sneaking Elf in the full set doesn't set off sculk sensors, shriekers or wardens. */
    public static boolean silencesVibrations(Entity entity) {
        return entity instanceof PlayerEntity player && player.isSneaking()
                && FAArmorEffectHandler.wearsKinSet(player, ElvenChainArmorItem.class);
    }

    /** How long (ticks) the drops of a block a silenced Elf broke stay silent: long enough to land. */
    public static final int SILENT_DROP_TICKS = 100;
    /** How many silenced Elves' block breaks are running on the server thread right now. */
    private static int silentBreaks;

    /**
     * A block break by {@code player} starts ({@code ServerPlayerInteractionManager.tryBreakBlock}). The
     * break event itself has the player as its source, but the items and experience it drops are entities of
     * their own and would set off sensors when they land, so while a silenced Elf's break runs, every entity
     * spawned is silenced for {@link #SILENT_DROP_TICKS} ticks ({@link #onEntitySpawned}).
     */
    public static void blockBreakStarted(PlayerEntity player) {
        if (silencesVibrations(player)) {
            silentBreaks++;
        }
    }

    /** The break that {@link #blockBreakStarted} saw for {@code player} is over. */
    public static void blockBreakEnded(PlayerEntity player) {
        if (silentBreaks > 0 && silencesVibrations(player)) {
            silentBreaks--;
        }
    }

    /** An entity was spawned on the server: a silenced Elf's break drop makes no vibrations for a while. */
    public static void onEntitySpawned(Entity entity) {
        if (silentBreaks > 0 && entity instanceof SilencedVibrations silenced) {
            silenced.dnd$silenceVibrationsUntil(entity.getWorld().getTime() + SILENT_DROP_TICKS);
        }
    }

    /** Draconic Scale: whether {@code source} is the element of a Dragonborn wearer's ancestry. */
    public static boolean immuneTo(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof PlayerEntity player)
                || !FAArmorEffectHandler.wearsKinSet(player, DraconicScaleArmorItem.class)) {
            return false;
        }
        return switch (RaceLifecycle.ancestryOf(player)) {
            case EMBER -> source.isIn(DamageTypeTags.IS_FIRE);
            case FROST -> source.isIn(DamageTypeTags.IS_FREEZING);
            case STORM -> source.isIn(DamageTypeTags.IS_LIGHTNING);
            default -> false;
        };
    }

    /** Draconic Scale: a Frost Dragonborn in the full set can't be given the Freeze effect. */
    public static boolean blocksEffect(LivingEntity entity, StatusEffectInstance effect) {
        return effect.getEffectType() == ModEffects.FREEZE && entity instanceof PlayerEntity player
                && RaceLifecycle.ancestryOf(player) == DragonAncestry.FROST
                && FAArmorEffectHandler.wearsKinSet(player, DraconicScaleArmorItem.class);
    }

    /**
     * Infernal Regalia: Hellish Rebuke's cooldown for this Tiefling, from its normal cooldown. The racial
     * traits code calls this when it starts the cooldown.
     */
    public static int rebukeCooldown(PlayerEntity player, int baseTicks) {
        return FAArmorEffectHandler.wearsKinSet(player, InfernalRegaliaArmorItem.class)
                ? Math.round(baseTicks * REBUKE_COOLDOWN_MULTIPLIER)
                : baseTicks;
    }

    /** Dwarven Waraxe: +50% to goblins, for a melee hit with the axe in the main hand. */
    public static float modifyDealtDamage(PlayerEntity player, LivingEntity target, DamageSource source,
            float amount) {
        if (source.getSource() == player && player.getMainHandStack().isOf(ModItems.DWARVEN_WARAXE)
                && target.getType().isIn(GOBLINS)) {
            return amount * WARAXE_GOBLIN_MULTIPLIER;
        }
        return amount;
    }
}

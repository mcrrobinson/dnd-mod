package mattonfire.dnd.classes.Feats;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.Contribution;
import mattonfire.dnd.classes.Abilities.Proficiency;
import mattonfire.dnd.classes.Abilities.RollFilter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.Progression;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Feats: what a player has taken (stored per class in {@link ClassProgress#feats}, chosen through
 * {@link Progression#chooseFeat}), the attribute bonuses they give (Tough, Mobile), the sheet contributor
 * ({@code dndclasses:feats}: Resilient, War Caster, Alert) and the per-player state kept in persistent data
 * ({@value #DATA_KEY}: the N toggle and Lucky's points). The effects themselves are in {@link FeatEffects}.
 */
public final class Feats {
    public static final Identifier CONTRIBUTOR = new Identifier(DnDClasses.MOD_ID, "feats");
    /** Great Weapon Master's heavy weapons, beside any melee weapon with attack speed {@value #HEAVY_SPEED} or less. */
    public static final TagKey<Item> HEAVY_WEAPONS = TagKey.of(RegistryKeys.ITEM,
            new Identifier(DnDClasses.MOD_ID, "heavy_weapons"));
    public static final double HEAVY_SPEED = 1.0;

    /** Player persistent data: {@code {Toggle:1b, LuckUsed:n, LuckDay:d}}. Copied on respawn with the rest. */
    public static final String DATA_KEY = "dndFeat";
    private static final String TOGGLE = "Toggle";
    private static final String LUCK_USED = "LuckUsed";
    private static final String LUCK_DAY = "LuckDay";
    public static final int LUCK_POINTS = 3;

    public static final UUID TOUGH_UUID = UUID.fromString("5f1c0b7e-7a43-4d0e-9b8e-2e5f3a7d0f61");
    public static final UUID MOBILE_UUID = UUID.fromString("0c3f8f2a-1d55-4c6b-a2f4-8b7e6d9a4c12");
    public static final double MOBILE_SPEED = 0.10;

    private Feats() {
    }

    public static void register() {
        AbilityScores.register(CONTRIBUTOR, Feats::contribute);
        FeatPackets.register();
        FeatEffects.register();
    }

    /**
     * Whether the player has the feat in their current class. Works on both sides; on the client only for the local
     * player (other players' progress isn't synced).
     */
    public static boolean has(@Nullable PlayerEntity player, Feat feat) {
        if (player == null || player.getWorld().isClient && !player.isMainPlayer())
            return false;
        if (Progression.classOf(player) == DndCharacter.NONE)
            return false;
        return Progression.current(player).hasFeat(feat);
    }

    /** Great Weapon Master's heavy weapons: the item tag, or a melee weapon with attack speed 1.0 or less. */
    public static boolean isHeavy(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        if (stack.isIn(HEAVY_WEAPONS))
            return true;
        var modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        if (modifiers.get(EntityAttributes.GENERIC_ATTACK_DAMAGE).isEmpty())
            return false;
        var speedModifiers = modifiers.get(EntityAttributes.GENERIC_ATTACK_SPEED);
        if (speedModifiers.isEmpty())
            return false;
        double speed = 4.0; // the player's base attack speed
        for (EntityAttributeModifier m : speedModifiers) {
            if (m.getOperation() == EntityAttributeModifier.Operation.ADDITION)
                speed += m.getValue();
        }
        return speed <= HEAVY_SPEED + 1.0E-6;
    }

    /** War Caster's somatic hands: a weapon (anything with attack damage) or a shield. */
    public static boolean isWeaponOrShield(ItemStack stack) {
        return stack.getItem() instanceof ShieldItem
                || !stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                        .isEmpty();
    }

    /** Whether a Warlock can throw a fireball with what's in the main hand: empty, or a weapon/shield with War Caster. */
    public static boolean castingHandFree(PlayerEntity player) {
        ItemStack main = player.getMainHandStack();
        return main.isEmpty() || has(player, Feat.WAR_CASTER) && isWeaponOrShield(main);
    }

    // ---- Attribute bonuses ----

    /** Puts Tough's health and Mobile's speed on the player to match their current class's feats and level. */
    public static void refresh(ServerPlayerEntity player) {
        ClassProgress progress = Progression.classOf(player) == DndCharacter.NONE ? null : Progression.current(player);
        double tough = progress != null && progress.hasFeat(Feat.TOUGH) ? progress.level() : 0;
        double mobile = progress != null && progress.hasFeat(Feat.MOBILE) ? MOBILE_SPEED : 0;
        setModifier(player, EntityAttributes.GENERIC_MAX_HEALTH, TOUGH_UUID, "Tough", tough,
                EntityAttributeModifier.Operation.ADDITION);
        setModifier(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, MOBILE_UUID, "Mobile", mobile,
                EntityAttributeModifier.Operation.MULTIPLY_BASE);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static void setModifier(PlayerEntity player, EntityAttribute attribute, UUID uuid, String name,
            double amount, EntityAttributeModifier.Operation operation) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance == null)
            return;
        EntityAttributeModifier current = instance.getModifier(uuid);
        if (current != null && current.getValue() == amount)
            return;
        if (current != null)
            instance.removeModifier(uuid);
        if (amount != 0) {
            // Temporary so it isn't saved; refresh puts it back after a relog
            instance.addTemporaryModifier(new EntityAttributeModifier(uuid, name, amount, operation));
        }
    }

    // ---- Sheet ----

    /** The {@link #CONTRIBUTOR}: Resilient's +1 and save proficiency, War Caster's and Alert's advantage. */
    static void contribute(PlayerEntity player, Contribution c) {
        if (Progression.classOf(player) == DndCharacter.NONE)
            return;
        ClassProgress progress = Progression.current(player);
        for (FeatChoice choice : progress.feats) {
            String source = choice.label();
            switch (choice.feat()) {
                case RESILIENT -> {
                    Ability a = choice.ability();
                    c.add(a, 1, source);
                    c.saveProficiency(a, Proficiency.PROFICIENT, source);
                }
                case WAR_CASTER -> c.advantage(RollFilter.save(Ability.CON), "CON saves", source);
                case ALERT -> c.advantage(RollFilter.check(Skill.PERCEPTION).and(RollFilter.tag("watch")),
                        "camp watch", source);
                default -> c.feature(choice.feat().summary1() + " " + choice.feat().summary2(), "Feat: " + source);
            }
        }
    }

    // ---- Persistent per-player state ----

    private static NbtCompound data(PlayerEntity player) {
        return ((IEntityDataSaver) player).getPersistentData().getCompound(DATA_KEY);
    }

    private static void putData(PlayerEntity player, NbtCompound data) {
        ((IEntityDataSaver) player).getPersistentData().put(DATA_KEY, data);
    }

    /** Power Attack / Steady Aim (N). Off until turned on. */
    public static boolean toggleOn(PlayerEntity player) {
        return data(player).getBoolean(TOGGLE);
    }

    public static void setToggle(PlayerEntity player, boolean on) {
        NbtCompound data = data(player);
        data.putBoolean(TOGGLE, on);
        putData(player, data);
    }

    /** Lucky's points left. */
    public static int luckPoints(PlayerEntity player) {
        return Math.max(0, LUCK_POINTS - data(player).getInt(LUCK_USED));
    }

    /** Spends a luck point; false if none are left. */
    static boolean spendLuck(PlayerEntity player) {
        NbtCompound data = data(player);
        int used = data.getInt(LUCK_USED);
        if (used >= LUCK_POINTS)
            return false;
        data.putInt(LUCK_USED, used + 1);
        if (!data.contains(LUCK_DAY))
            data.putLong(LUCK_DAY, day(player));
        putData(player, data);
        return true;
    }

    /** Lucky's points back (long rest, or dawn with rests off). Returns whether any came back. */
    static boolean refillLuck(PlayerEntity player) {
        NbtCompound data = data(player);
        boolean any = data.getInt(LUCK_USED) > 0;
        data.putInt(LUCK_USED, 0);
        data.putLong(LUCK_DAY, day(player));
        putData(player, data);
        return any;
    }

    /** The day Lucky last refilled (or was first used), for the dawn refill when rests are off. */
    static long luckDay(PlayerEntity player) {
        NbtCompound data = data(player);
        return data.contains(LUCK_DAY) ? data.getLong(LUCK_DAY) : day(player);
    }

    static long day(PlayerEntity player) {
        return player.getWorld().getTimeOfDay() / 24000L;
    }
}

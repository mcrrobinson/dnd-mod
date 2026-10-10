package mattonfire.dnd.magic;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.RollFilter;
import mattonfire.dnd.classes.SkillChecks.D20;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * Curses on magic items (design section 3.6). A cursed item binds to whoever equips it (armor) or holds it for
 * {@value #HOLD_SECONDS} s (anything else): it takes an attunement slot, can't be unattuned or released, and its
 * drawback applies while the bond lasts, wherever the item is. Only Remove Curse (a Cleric, or a Scroll of Remove
 * Curse) breaks the bond; the item stays cursed.
 *
 * The drawbacks are read from the player's bonds ({@link Attunement}); {@link #active} caches them per player so
 * per-tick hooks (hunger, mob follow range, d20 fumbles) stay cheap.
 */
public enum Curse {
    /** Every 60 s without a hostile kill, take 1 magic damage. */
    BLOODTHIRST("bloodthirst"),
    /** -4 max health. */
    FRAILTY("frailty"),
    /** Natural 1s and 2s fumble; disadvantage on saving throws. */
    ILL_OMEN("ill_omen"),
    /** Hostile mobs' follow range against you doubles, and they come for you from twice as far. */
    BEACON("beacon"),
    /** Weakness I in daylight while outdoors. */
    SUN_SICK("sun_sick"),
    /** Hunger drains 50% faster. */
    GLUTTONY("gluttony");

    public final String id;

    Curse(String id) {
        this.id = id;
    }

    public static final int HOLD_SECONDS = 3;
    public static final int BLOODTHIRST_SECONDS = 60;
    public static final float BLOODTHIRST_DAMAGE = 1.0F;
    public static final double FRAILTY_HEALTH = -4.0;
    public static final float GLUTTONY_EXHAUSTION = 1.5F;
    public static final double BEACON_RANGE_MULTIPLIER = 2.0;
    /** Ill Omen: naturals up to this fumble. */
    public static final int ILL_OMEN_FUMBLE = 2;

    static final UUID FRAILTY_UUID = UUID.fromString("5d1b8f0e-2c7a-4a9e-9f43-1c0b7e2a6f01");

    /** Curse chance for random loot, by tier (design section 3.1). */
    public static float defaultChance(MagicTier tier) {
        return switch (tier) {
            case UNCOMMON, RARE -> 0.10F;
            case VERY_RARE -> 0.08F;
            default -> 0.0F;
        };
    }

    public static @Nullable Curse byId(@Nullable String id) {
        if (id == null || id.isEmpty())
            return null;
        String key = id.toLowerCase(Locale.ROOT).replace('-', '_');
        for (Curse curse : values())
            if (curse.id.equals(key) || curse.id.replace("_", "").equals(key))
                return curse;
        return null;
    }

    public static Curse random(Random random) {
        return values()[random.nextInt(values().length)];
    }

    public MutableText displayName() {
        return Text.translatable("magic.dndclasses.curse." + id);
    }

    public MutableText description() {
        return Text.translatable("magic.dndclasses.curse." + id + ".desc");
    }

    /** Whether this item can carry a curse: worn or held gear and wondrous items, not potions or scrolls. */
    public static boolean canCurse(ItemStack stack) {
        MagicItems.Info info = MagicItems.info(stack);
        if (info == null)
            return false;
        return switch (info.kind()) {
            case POTION, SCROLL, CONSUMABLE -> false;
            default -> true;
        };
    }

    // ---- Per-player state ----

    /** The curses of each online player's bonds, refreshed whenever bonds change and once a second. */
    private static final Map<UUID, Set<Curse>> ACTIVE = new HashMap<>();
    /** Seconds since the last hostile kill (Bloodthirst). */
    private static final Map<UUID, Integer> THIRST = new HashMap<>();
    /** What the player has held in the main hand and offhand, for how many seconds (bind-on-hold). */
    private record Held(UUID main, int mainSeconds, UUID off, int offSeconds) {
    }

    private static final Map<UUID, Held> HELD = new HashMap<>();

    /** The curses binding this player (server: from their bonds; empty on the client). */
    public static Set<Curse> active(PlayerEntity player) {
        Set<Curse> set = ACTIVE.get(player.getUuid());
        return set == null ? Set.of() : set;
    }

    public static boolean has(@Nullable LivingEntity entity, Curse curse) {
        return entity instanceof PlayerEntity player && !player.getWorld().isClient && active(player).contains(curse);
    }

    /** Reads the curses straight from the bonds in persistent data (no text parsing). */
    private static Set<Curse> readCurses(PlayerEntity player) {
        EnumSet<Curse> set = EnumSet.noneOf(Curse.class);
        NbtList list = ((IEntityDataSaver) player).getPersistentData().getList(Attunement.DATA_KEY,
                NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound nbt = list.getCompound(i);
            if (!nbt.getBoolean("cursed"))
                continue;
            Curse curse = byId(nbt.getString("curse"));
            if (curse != null)
                set.add(curse);
        }
        return set;
    }

    /** Called whenever the player's bonds change: refreshes the cache, Frailty and the sheet (Ill Omen). */
    static void onBondsChanged(PlayerEntity player) {
        if (player.getWorld().isClient)
            return;
        Set<Curse> before = active(player);
        Set<Curse> now = readCurses(player);
        ACTIVE.put(player.getUuid(), now);
        updateFrailty(player, now.contains(FRAILTY));
        if (before.contains(ILL_OMEN) != now.contains(ILL_OMEN))
            AbilityScores.invalidate(player);
    }

    private static void updateFrailty(PlayerEntity player, boolean wanted) {
        EntityAttributeInstance health = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (health == null)
            return;
        boolean has = health.getModifier(FRAILTY_UUID) != null;
        if (wanted && !has) {
            health.addTemporaryModifier(new EntityAttributeModifier(FRAILTY_UUID, "Curse: Frailty", FRAILTY_HEALTH,
                    EntityAttributeModifier.Operation.ADDITION));
            if (player.getHealth() > player.getMaxHealth())
                player.setHealth(player.getMaxHealth());
        } else if (!wanted && has) {
            health.removeModifier(FRAILTY_UUID);
        }
    }

    static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onBondsChanged(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.player.getUuid();
            ACTIVE.remove(id);
            THIRST.remove(id);
            HELD.remove(id);
        });
        // Ill Omen: disadvantage on every saving throw...
        AbilityScores.register(new Identifier(DnDClasses.MOD_ID, "curse/ill_omen"), (player, c) -> {
            if (readCurses(player).contains(ILL_OMEN))
                c.disadvantage(RollFilter.allSaves(), "Ill Omen: disadvantage on saving throws", "Curse: Ill Omen");
        });
        // ...and natural 2s fumble too
        D20.registerFumbleRange(entity -> has(entity, ILL_OMEN) ? ILL_OMEN_FUMBLE : 1);
    }

    // ---- Hooks ----

    /** Once a second (from {@link MagicEffects#secondTick}): bind-on-equip, then the drawbacks. */
    static void secondTick(ServerPlayerEntity player) {
        bindOnEquip(player);
        Set<Curse> curses = readCurses(player);
        ACTIVE.put(player.getUuid(), curses);
        updateFrailty(player, curses.contains(FRAILTY));
        if (curses.isEmpty() || !player.isAlive() || player.isCreative() || player.isSpectator()) {
            THIRST.remove(player.getUuid());
            return;
        }
        if (curses.contains(BLOODTHIRST)) {
            int seconds = THIRST.merge(player.getUuid(), 1, Integer::sum);
            if (seconds >= BLOODTHIRST_SECONDS) {
                THIRST.put(player.getUuid(), 0);
                player.sendMessage(Text.translatable("magic.dndclasses.curse.bloodthirst.hunger")
                        .formatted(Formatting.DARK_RED), true);
                player.damage(player.getDamageSources().magic(), BLOODTHIRST_DAMAGE);
            }
        } else {
            THIRST.remove(player.getUuid());
        }
        if (curses.contains(SUN_SICK)) {
            BlockPos eyes = BlockPos.ofFloored(player.getEyePos());
            if (player.getWorld().isDay() && player.getWorld().isSkyVisible(eyes)) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 0, true, true, true));
            }
        }
        if (curses.contains(BEACON))
            beacon(player);
    }

    /** Beacon: hostiles that can see you from up to twice their follow range come for you. */
    private static void beacon(ServerPlayerEntity player) {
        double reach = 48.0; // twice the usual follow range of 16-35 blocks, capped
        for (HostileEntity mob : player.getWorld().getEntitiesByClass(HostileEntity.class,
                player.getBoundingBox().expand(reach), mob -> mob.isAlive() && mob.getTarget() == null)) {
            double range = mob.getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE) * BEACON_RANGE_MULTIPLIER;
            if (mob.squaredDistanceTo(player) <= range * range && mob.canSee(player)
                    && mob.canTarget(player)) {
                mob.setTarget(player);
            }
        }
    }

    /** A hostile kill quenches Bloodthirst. */
    static void onKill(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
        if (killed instanceof Monster)
            THIRST.remove(player.getUuid());
    }

    /** Gluttony: exhaustion x1.5. Called from the {@code PlayerEntity#addExhaustion} mixin. */
    public static float modifyExhaustion(PlayerEntity player, float exhaustion) {
        return has(player, GLUTTONY) ? exhaustion * GLUTTONY_EXHAUSTION : exhaustion;
    }

    /** Beacon: a mob chasing a Beacon-cursed player keeps chasing from twice as far. */
    public static double modifyFollowRange(@Nullable LivingEntity target, double range) {
        return has(target, BEACON) ? range * BEACON_RANGE_MULTIPLIER : range;
    }

    // ---- Binding ----

    /**
     * Cursed armor binds as soon as it's worn; anything else once it's been in a hand for {@value #HOLD_SECONDS}
     * seconds. Items bonded to someone else are left alone.
     */
    private static void bindOnEquip(ServerPlayerEntity player) {
        if (player.isSpectator() || !player.isAlive())
            return;
        for (ItemStack armor : player.getInventory().armor) {
            if (bindable(player, armor))
                bind(player, armor);
        }
        Held held = HELD.get(player.getUuid());
        ItemStack main = player.getMainHandStack();
        ItemStack off = player.getOffHandStack();
        UUID mainId = bindable(player, main) && !(main.getItem() instanceof ArmorItem) ? MagicData.ensureUuid(main) : null;
        UUID offId = bindable(player, off) && !(off.getItem() instanceof ArmorItem) ? MagicData.ensureUuid(off) : null;
        int mainSeconds = mainId == null ? 0 : held != null && mainId.equals(held.main()) ? held.mainSeconds() + 1 : 1;
        int offSeconds = offId == null ? 0 : held != null && offId.equals(held.off()) ? held.offSeconds() + 1 : 1;
        if (mainSeconds >= HOLD_SECONDS) {
            bind(player, main);
            mainId = null;
            mainSeconds = 0;
        }
        if (offSeconds >= HOLD_SECONDS) {
            bind(player, off);
            offId = null;
            offSeconds = 0;
        }
        if (mainId == null && offId == null)
            HELD.remove(player.getUuid());
        else
            HELD.put(player.getUuid(), new Held(mainId, mainSeconds, offId, offSeconds));
    }

    private static boolean bindable(ServerPlayerEntity player, ItemStack stack) {
        if (stack.isEmpty() || MagicData.curse(stack).isEmpty() || byId(MagicData.curse(stack)) == null)
            return false;
        UUID owner = MagicData.attunedTo(stack);
        if (owner != null && !owner.equals(player.getUuid()))
            return false; // someone else's bond (if it's stale, Attunement clears it)
        UUID uuid = MagicData.uuid(stack);
        return uuid == null || Attunement.bond(player, uuid) == null;
    }

    /** The curse takes hold: the item binds to the player (even past their slots), identifies and reveals itself. */
    public static void bind(ServerPlayerEntity player, ItemStack stack) {
        Curse curse = byId(MagicData.curse(stack));
        if (curse == null)
            return;
        Attunement.bindCursed(player, stack);
        player.sendMessage(Text.translatable("magic.dndclasses.curse.binds", stack.toHoverableText(),
                curse.displayName().formatted(Formatting.RED)).formatted(Formatting.DARK_RED), false);
        player.sendMessage(curse.description().formatted(Formatting.GRAY, Formatting.ITALIC), false);
        player.getWorld().playSound(null, player.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE,
                net.minecraft.sound.SoundCategory.PLAYERS, 0.6F, 1.2F);
        DnDClasses.LOGGER.info("[Curse] {} bound to {} ({})", player.getEntityName(), stack.getName().getString(),
                curse.id);
    }

    /** Puts vanilla Curse of Binding on cursed armor once it binds, remembering it was ours. */
    static void addBinding(ItemStack stack) {
        if (!(stack.getItem() instanceof ArmorItem)
                || EnchantmentHelper.getLevel(Enchantments.BINDING_CURSE, stack) > 0)
            return;
        stack.addEnchantment(Enchantments.BINDING_CURSE, 1);
        MagicData.getOrCreate(stack).putBoolean("addedBinding", true);
    }

    /** Takes off the Curse of Binding {@link #addBinding} put on. */
    static void removeBinding(ItemStack stack) {
        NbtCompound magic = MagicData.get(stack);
        if (magic == null || !magic.getBoolean("addedBinding"))
            return;
        magic.remove("addedBinding");
        Map<net.minecraft.enchantment.Enchantment, Integer> enchants = EnchantmentHelper.get(stack);
        enchants.remove(Enchantments.BINDING_CURSE);
        EnchantmentHelper.set(enchants, stack);
    }
}

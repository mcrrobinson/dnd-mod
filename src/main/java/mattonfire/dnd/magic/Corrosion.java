package mattonfire.dnd.magic;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.item.v1.ModifyItemAttributeModifiersCallback;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.SwordItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Rust: what a Rust Monster does to metal gear. A stack in {@code #dndclasses:corrodible} carries a
 * corrosion level in NBT ({@value #KEY}, 0-{@value #MAX_LEVEL}). Each level costs armour -1 armor in
 * its slot, and weapons and tools -1 attack damage in the main hand. Every step also eats
 * {@link #DURABILITY_SHARE} of the item's max durability, and a step past {@value #MAX_LEVEL}
 * crumbles it away.
 * <p>
 * +N gear loses its +N first, one per step; corrosion starts once it's +0. Named magic items are
 * never in the tag, and attuned items are immune. Repairing or combining the item on an
 * anvil clears its corrosion ({@code AnvilScreenHandlerMixin}); a grindstone doesn't.
 */
public final class Corrosion {
    public static final String KEY = "dndCorrosion";
    public static final int MAX_LEVEL = 3;
    public static final float DURABILITY_SHARE = 0.15F;

    /** Gear that rusts: iron, chain and gold, and the mod's metal sets. Diamond and netherite don't. */
    public static final TagKey<Item> CORRODIBLE = TagKey.of(RegistryKeys.ITEM,
            new Identifier(DnDClasses.MOD_ID, "corrodible"));
    /** What a Rust Monster eats off the floor: corrodible gear plus ingots, nuggets and raw ore. */
    public static final TagKey<Item> RUST_MONSTER_FOOD = TagKey.of(RegistryKeys.ITEM,
            new Identifier(DnDClasses.MOD_ID, "rust_monster_food"));

    private static final UUID DAMAGE_ID = UUID.fromString("9e2b6c1d-4f3a-4b7e-8c21-5d0e7a3f1b01");
    // One id per armor slot (feet, legs, chest, head), so a corroded set stacks
    private static final UUID[] ARMOR_IDS = {
            UUID.fromString("9e2b6c1d-4f3a-4b7e-8c21-5d0e7a3f1b10"),
            UUID.fromString("9e2b6c1d-4f3a-4b7e-8c21-5d0e7a3f1b11"),
            UUID.fromString("9e2b6c1d-4f3a-4b7e-8c21-5d0e7a3f1b12"),
            UUID.fromString("9e2b6c1d-4f3a-4b7e-8c21-5d0e7a3f1b13"),
    };

    /** What one corrosion step did. */
    public enum Result {
        /** Not corrodible, a named magic item or attuned: nothing happened. */
        IMMUNE,
        /** A +N item lost one point of its +N. */
        PLUS_LOST,
        /** The corrosion level went up by one. */
        CORRODED,
        /** It was already at the last level and crumbled away. */
        CRUMBLED
    }

    private Corrosion() {
    }

    public static int level(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt == null ? 0 : Math.max(0, Math.min(MAX_LEVEL, nbt.getInt(KEY)));
    }

    public static void setLevel(ItemStack stack, int level) {
        if (level <= 0) {
            clear(stack);
        } else {
            stack.getOrCreateNbt().putInt(KEY, Math.min(MAX_LEVEL, level));
        }
    }

    /** Removes the rust (an anvil repair). */
    public static void clear(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(KEY)) {
            nbt.remove(KEY);
            if (nbt.isEmpty()) {
                stack.setNbt(null);
            }
        }
    }

    /** Whether the stack is metal a Rust Monster can corrode (and isn't protected by its own magic). */
    public static boolean canCorrode(ItemStack stack) {
        if (stack.isEmpty() || !stack.isIn(CORRODIBLE)) {
            return false;
        }
        // The tag is the whole list: the mod's class and racial sets have a MagicItems def (they're Uncommon
        // magic items) but are plain metal, so a def alone doesn't protect. Named magic items (boss drops,
        // wondrous items) are items of their own and never in the tag. An attuned item's bond protects it.
        return MagicData.attunedTo(stack) == null;
    }

    /**
     * One step of rust on {@code stack}, worn or held by {@code holder} in {@code slot}: +N first,
     * then a corrosion level, then it crumbles. Tells a player what happened and logs it
     * ({@code [Rust] ...}).
     */
    public static Result corrode(LivingEntity holder, ItemStack stack, EquipmentSlot slot) {
        if (!canCorrode(stack)) {
            return Result.IMMUNE;
        }
        String name = stack.getName().getString();
        int plus = MagicData.plus(stack);
        if (plus > 0) {
            MagicData.setPlus(stack, plus - 1);
            DnDClasses.LOGGER.info("[Rust] eats the +{} on {} ({})", plus, name, holder.getName().getString());
            tell(holder, Text.translatable("rust.dndclasses.plus_lost", name));
            holder.world.playSound(null, holder.getX(), holder.getY(), holder.getZ(),
                    SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.HOSTILE, 0.6F, 1.4F);
            return Result.PLUS_LOST;
        }
        int level = level(stack);
        if (level >= MAX_LEVEL) {
            DnDClasses.LOGGER.info("[Rust] {} crumbles ({})", name, holder.getName().getString());
            tell(holder, Text.translatable("rust.dndclasses.crumbles", name));
            stack.decrement(1);
            holder.sendEquipmentBreakStatus(slot);
            if (stack.isEmpty()) {
                holder.equipStack(slot, ItemStack.EMPTY);
            }
            return Result.CRUMBLED;
        }
        setLevel(stack, level + 1);
        if (stack.isDamageable()) {
            int cost = Math.max(1, Math.round(stack.getMaxDamage() * DURABILITY_SHARE));
            int damage = stack.getDamage() + cost;
            if (damage >= stack.getMaxDamage()) {
                // Worn through on top of the rust.
                DnDClasses.LOGGER.info("[Rust] {} crumbles ({})", name, holder.getName().getString());
                tell(holder, Text.translatable("rust.dndclasses.crumbles", name));
                stack.decrement(1);
                holder.sendEquipmentBreakStatus(slot);
                if (stack.isEmpty()) {
                    holder.equipStack(slot, ItemStack.EMPTY);
                }
                return Result.CRUMBLED;
            }
            stack.setDamage(damage);
        }
        DnDClasses.LOGGER.info("[Rust] corrodes {} ({}/{})", name, level + 1, MAX_LEVEL);
        tell(holder, Text.translatable("rust.dndclasses.corrodes", name, level + 1, MAX_LEVEL));
        holder.world.playSound(null, holder.getX(), holder.getY(), holder.getZ(),
                SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.HOSTILE, 0.6F, 0.7F);
        return Result.CORRODED;
    }

    private static void tell(LivingEntity holder, Text text) {
        if (holder instanceof PlayerEntity player) {
            player.sendMessage(text.copy().formatted(Formatting.GOLD), true);
        }
    }

    /** The orange "Corroded (2/3): -2 armor" tooltip line, or null for clean gear. */
    public static @Nullable Text tooltipLine(ItemStack stack) {
        int level = level(stack);
        if (level <= 0) {
            return null;
        }
        Item item = stack.getItem();
        String key = item instanceof ArmorItem ? "tooltip.dndclasses.corroded.armor"
                : hasAttackPenalty(item) ? "tooltip.dndclasses.corroded.damage" : "tooltip.dndclasses.corroded";
        return Text.translatable(key, level, MAX_LEVEL).formatted(Formatting.GOLD);
    }

    private static boolean hasAttackPenalty(Item item) {
        return item instanceof SwordItem || item instanceof MiningToolItem;
    }

    static void register() {
        ModifyItemAttributeModifiersCallback.EVENT.register((stack, slot, modifiers) -> {
            int level = level(stack);
            if (level <= 0) {
                return;
            }
            Item item = stack.getItem();
            if (item instanceof ArmorItem armor) {
                if (armor.getSlotType() == slot) {
                    modifiers.put(EntityAttributes.GENERIC_ARMOR, new EntityAttributeModifier(
                            ARMOR_IDS[slot.getEntitySlotId()], "Corrosion", -level,
                            EntityAttributeModifier.Operation.ADDITION));
                }
            } else if (slot == EquipmentSlot.MAINHAND && hasAttackPenalty(item)) {
                modifiers.put(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(DAMAGE_ID,
                        "Corrosion", -level, EntityAttributeModifier.Operation.ADDITION));
            }
        });
    }
}

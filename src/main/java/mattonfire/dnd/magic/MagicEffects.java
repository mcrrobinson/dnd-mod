package mattonfire.dnd.magic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Dispatches {@link MagicEffect} hooks to the player's active magic items. Called from the same places as the
 * {@code ClassSkills} hooks ({@code ProgressionEvents}), right after them, so damage is modified in a fixed
 * order: class, then items.
 */
public final class MagicEffects {
    private static final Map<Item, MagicEffect> EFFECTS = new HashMap<>();

    private MagicEffects() {
    }

    public static void register(Item item, MagicEffect effect) {
        EFFECTS.put(item, effect);
    }

    public static @Nullable MagicEffect effect(Item item) {
        return EFFECTS.get(item);
    }

    /** One of the player's items with an effect, active and in place. */
    public record Active(ItemStack stack, MagicEffect effect) {
    }

    /** Whether inventory slot {@code slot} is where an item of this kind works (design section 3.3). */
    public static boolean inPlace(PlayerEntity player, int slot, MagicKind kind) {
        return switch (kind) {
            case WEAPON, STAFF -> slot == player.getInventory().selectedSlot;
            case ARMOR -> slot >= PlayerInventory.MAIN_SIZE && slot < PlayerInventory.MAIN_SIZE + 4;
            default -> true; // wondrous items, rings, wands...: carried is worn
        };
    }

    /** The player's items whose effects apply right now. */
    public static List<Active> active(PlayerEntity player) {
        List<Active> list = new ArrayList<>();
        if (EFFECTS.isEmpty())
            return list;
        PlayerInventory inventory = player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty())
                continue;
            MagicEffect effect = EFFECTS.get(stack.getItem());
            if (effect == null)
                continue;
            MagicItems.Info info = MagicItems.info(stack);
            if (info == null || !inPlace(player, i, info.kind()) || !Attunement.isActive(player, stack))
                continue;
            list.add(new Active(stack, effect));
        }
        return list;
    }

    public static float modifyDealtDamage(PlayerEntity player, LivingEntity target, DamageSource source,
            float amount) {
        for (Active a : active(player))
            amount = a.effect().modifyDealtDamage(player, a.stack(), target, source, amount);
        return amount;
    }

    public static float modifyTakenDamage(PlayerEntity player, DamageSource source, float amount) {
        for (Active a : active(player))
            amount = a.effect().modifyTakenDamage(player, a.stack(), source, amount);
        return amount;
    }

    public static void onKill(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
        for (Active a : active(player))
            a.effect().onKill(player, a.stack(), killed, source);
    }

    /** Once a second: item ticks, and the item attribute bonuses put on or taken off. */
    public static void secondTick(ServerPlayerEntity player) {
        Attunement.secondTick(player);
        List<Active> active = active(player);
        for (Active a : active)
            a.effect().secondTick(player, a.stack());
        updateAttributes(player, active);
    }

    /** Puts on the bonuses of the active items and takes off every other item's (anti-magic, unattuned...). */
    public static void updateAttributes(PlayerEntity player) {
        updateAttributes(player, active(player));
    }

    private static void updateAttributes(PlayerEntity player, List<Active> active) {
        Set<UUID> wanted = new HashSet<>();
        for (Active a : active) {
            for (MagicEffect.Bonus bonus : a.effect().attributeBonuses()) {
                if (!wanted.add(bonus.uuid()))
                    continue; // two of the same item don't stack
                EntityAttributeInstance instance = player.getAttributeInstance(bonus.attribute());
                if (instance != null && instance.getModifier(bonus.uuid()) == null) {
                    instance.addTemporaryModifier(new EntityAttributeModifier(bonus.uuid(), "Magic item",
                            bonus.amount(), bonus.operation()));
                }
            }
        }
        for (MagicEffect effect : EFFECTS.values()) {
            for (MagicEffect.Bonus bonus : effect.attributeBonuses()) {
                if (wanted.contains(bonus.uuid()))
                    continue;
                EntityAttributeInstance instance = player.getAttributeInstance(bonus.attribute());
                if (instance != null && instance.getModifier(bonus.uuid()) != null)
                    instance.removeModifier(bonus.uuid());
            }
        }
    }
}

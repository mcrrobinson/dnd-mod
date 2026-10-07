package mattonfire.dnd.classes.Items.lib;

import mattonfire.dnd.classes.Config.FAConfig;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.world.World;

public class FAArmorEffectHandler {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    public static void register() {
        ServerTickEvents.START_WORLD_TICK.register(FAArmorEffectHandler::onWorldTick);
    }

    private static void onWorldTick(World world) {
        if (!FAConfig.getValues().applyArmorEffects()) {
            return;
        }
        for (PlayerEntity player : world.getPlayers()) {
            SetBonusArmor set = getFullSet(player);
            if (set != null) {
                applyFullSetEffects(player, set);
            }
        }
    }

    /** The set the player wears all four pieces of, or null. */
    public static SetBonusArmor getFullSet(PlayerEntity player) {
        Item helmet = player.getEquippedStack(EquipmentSlot.HEAD).getItem();
        if (!(helmet instanceof SetBonusArmor set)) {
            return null;
        }
        Class<?> armorClass = helmet.getClass();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!armorClass.isInstance(player.getEquippedStack(slot).getItem())) {
                return null;
            }
        }
        return set;
    }

    /** Whether the player's class is one the set is made for. */
    public static boolean classMatches(PlayerEntity player, SetBonusArmor set) {
        DndCharacter dndClass = player instanceof PlayerEntityExt ext ? ext.getDndClass() : null;
        return dndClass != null && set.getMatchingClasses().contains(dndClass);
    }

    private static void applyFullSetEffects(PlayerEntity player, SetBonusArmor set) {
        boolean matches = classMatches(player, set);
        int boost = matches ? 1 : 0;

        for (StatusEffectInstance effectInstance : set.getFullSetEffects()) {
            apply(player, effectInstance, effectInstance.getAmplifier() + boost);
        }
        if (matches) {
            for (StatusEffectInstance effectInstance : set.getMatchingClassEffects()) {
                apply(player, effectInstance, effectInstance.getAmplifier());
            }
        }
    }

    private static void apply(PlayerEntity player, StatusEffectInstance effectInstance, int amplifier) {
        StatusEffect effect = effectInstance.getEffectType();
        StatusEffectInstance existingEffect = player.getStatusEffect(effect);

        // Refresh before it runs low. A boosted effect also replaces a weaker
        // one, e.g. right after the wearer switches to the matching class.
        if (existingEffect == null || existingEffect.getDuration() < 221
                || existingEffect.getAmplifier() < amplifier) {
            player.addStatusEffect(new StatusEffectInstance(effect, effectInstance.getDuration(), amplifier,
                    effectInstance.isAmbient(), effectInstance.shouldShowParticles(), effectInstance.shouldShowIcon()));
        }
    }
}

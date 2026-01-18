package mattonfire.dnd.classes.Items.lib;
import mattonfire.dnd.classes.Config.FAConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class FAArmorEffectHandler {

    public static void register() {
        ServerTickEvents.START_WORLD_TICK.register(FAArmorEffectHandler::onWorldTick);
    }

    private static void onWorldTick(World world) {
        for (PlayerEntity player : world.getPlayers()) {
            if (FAConfig.getValues().applyArmorEffects() && hasFullSet(player)) {
                applyFullSetEffects(player);
            }
        }
    }

    private static boolean hasFullSet(PlayerEntity player) {
        ItemStack helmetItemStack = player.getEquippedStack(EquipmentSlot.HEAD);
        ItemStack chestplateItemStack = player.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack leggingsItemStack = player.getEquippedStack(EquipmentSlot.LEGS);
        ItemStack bootsItemStack = player.getEquippedStack(EquipmentSlot.FEET);

        if (helmetItemStack.getItem() instanceof FAArmorItem &&
                chestplateItemStack.getItem() instanceof FAArmorItem &&
                leggingsItemStack.getItem() instanceof FAArmorItem &&
                bootsItemStack.getItem() instanceof FAArmorItem) {

            Class<?> armorClass = helmetItemStack.getItem().getClass();
            return armorClass.isInstance(chestplateItemStack.getItem()) &&
                    armorClass.isInstance(leggingsItemStack.getItem()) &&
                    armorClass.isInstance(bootsItemStack.getItem());
        }

        return false;
    }

    private static void applyFullSetEffects(PlayerEntity player) {
        ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
        FAArmorItem armorItem = (FAArmorItem) helmet.getItem();

        for (StatusEffectInstance effectInstance : armorItem.getFullSetEffects()) {
            StatusEffect effect = effectInstance.getEffectType();
            StatusEffectInstance existingEffect = player.getStatusEffect(effect);

            if (existingEffect == null || existingEffect.getDuration() < 221) {
                player.addStatusEffect(new StatusEffectInstance(effect, effectInstance.getDuration(), effectInstance.getAmplifier()));
            }
        }
    }
}
package mattonfire.dnd.classes;

import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class SetClassAttributes {

    public void resetToDefault(PlayerEntity player) {
        player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(1.D);
        player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.10000000149011612D);
        player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(20.D);
        player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED).setBaseValue(4.0D);
        player.getAttributeInstance(EntityAttributes.GENERIC_LUCK).setBaseValue(0.0D);
        player.clearStatusEffects();
        Druid.onClassReset(player);
    }

    /** Tells the player what their new class does, from the same data as the guidebook and README. */
    public void sendPlayerMessage(PlayerEntity player, ClassInfo info) {
        player.sendMessage(
                Text.literal("The " + info.name() + "\n").formatted(Formatting.UNDERLINE, Formatting.GOLD),
                false);
        player.sendMessage(
                Text.literal("Pros: " + ClassInfo.joinForGame(info.pros())).formatted(Formatting.GREEN),
                false);
        player.sendMessage(
                Text.literal("Cons: " + ClassInfo.joinForGame(info.cons())).formatted(Formatting.RED),
                false);
        player.sendMessage(
                Text.literal("Special: " + ClassInfo.joinForGame(info.special())).formatted(Formatting.DARK_PURPLE),
                false);
        player.sendMessage(
                Text.literal("Read your Class Guidebook for the details.").formatted(Formatting.GRAY,
                        Formatting.ITALIC),
                false);
    }

    public void typeBarbarian(PlayerEntity player) {
        System.out.println("Barbarian...");
        player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(6);
        player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.08);
        player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(40);
    }

    public void typeBard(PlayerEntity player) {
        System.out.println("Bard...");
        // player.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE).setBaseValue(0);
        player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.12);
        player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(15);
    }

    public void typeCleric(PlayerEntity player) {
        System.out.println("Cleric...");
        player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(0.67); // -33%, Default 1.0
        // Haste and Night Vision are kept up every tick - in ClericHandler

    }

    public void typeDruid(PlayerEntity player) {
        System.out.println("Druid...");
        // Extra hearts from tamed animals are kept up to date by Druid.serverTick.
        if (player instanceof ServerPlayerEntity serverPlayer) {
            Druid.updateAnimalHearts(serverPlayer);
        }
    }

    public void typeFighter(PlayerEntity player) {
        System.out.println("Fighter...");
        player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(6);
        player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(26);
        // player.getAttributeInstance(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS).setBaseValue(1);
        // player.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE).setBaseValue(64);
    }

    public void typeMonk(PlayerEntity player) {
        // Staff/fist-only attacks and the armor-scaled damage penalty live in MonkHandler.
        player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.12); // Default 0.1
        player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED).setBaseValue(6.0); // Default 4.0
    }

    public void typePaladin(PlayerEntity player) {
        System.out.println("Paladin...");
        // Natural smite...
        player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(26); // Default 20

        // Disallow potion effects - in living entity mixin
        // Weak in nether - in Misc/PaladinNetherWeakness
        // Cannot craft or brew - in SlotMixin and BrewingStandBlockMixin
    }

    public void typeRanger(PlayerEntity player) {
        System.out.println("Ranger...");
        // 2x Zoom with a bow - found in BowItemMixin
        player.getAttributeInstance(EntityAttributes.GENERIC_LUCK).setBaseValue(5); // Default 0.0
        // Can't use swords - in global callback listener
    }

    public void typeRogue(PlayerEntity player) {
        System.out.println("Rogue...");

        player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(14);

        // No poison - in living entity mixin
        // No hunger -
        // Nether mobs are alies.
        // ALL Overworld mobs will always attack.
    }

    public void typeNecromancer(PlayerEntity player) {
        System.out.println("Necromancer...");
        // Attack attacked take wither debuff.
        // Not attacked by undead.
        // Less health
        player.setHealth(5);
        // Less attack damage.
        player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(0.5); // Default 1.0
    }

    public void typeWarlock(PlayerEntity player) {
        System.out.println("Warlock...");
        player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(0.5); // Default 1
        // Slow fireball, fire/lava immunity and water/rain damage live in Warlock.
    }

    public void typeWizard(PlayerEntity player) {
        player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(10); // Default 20
        // Max iron armor.
    }

    public void typeArtificer(PlayerEntity player) {
        player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.12); // Default 0.1
        // Reduced damage: -25% modifier applied every tick in ArtificerDamage.
        // No potion buffs.
        // Automatic random enchantment chance...
    }

    public void typeBloodHunter(PlayerEntity player) {
        // Fire aspect on all swords.
        // x2 Damage during the night.
        // Swords cannot be droppped.
        // 1/2 damage during the day.
        System.out.println("Blood Hunter...");
    }

    public void typeAlchemist(PlayerEntity player) {
        // random potion backfires.
        System.out.println("Alchemist");
    }
}

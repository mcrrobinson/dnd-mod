package mattonfire.dnd.classes;

import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;

public class SetClassAttributes {

    public void resetToDefault(PlayerEntity player) {
        player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(1.D);
        player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.10000000149011612D);
        player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(20.D);
        player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED).setBaseValue(4.0D);
        player.getAttributeInstance(EntityAttributes.GENERIC_LUCK).setBaseValue(0.0D);
        player.clearStatusEffects();
    }

    public void sendPlayerMessage(PlayerEntity player, String className, String pros, String cons, String special) {
        player.sendMessage(
                Text.literal("The " + className + "\n").formatted(Formatting.UNDERLINE, Formatting.GOLD),
                false);
        player.sendMessage(
                Text.literal("Pros: " + pros).formatted(Formatting.GREEN),
                false);
        player.sendMessage(
                Text.literal("Cons: " + cons).formatted(Formatting.RED),
                false);
        player.sendMessage(
                Text.literal("Special: " + special).formatted(Formatting.DARK_PURPLE),
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
        player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(4); // -33%
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 20000, 2, false, false, true));
        player.addStatusEffect(
                new StatusEffectInstance(StatusEffects.NIGHT_VISION, 20000, 2, false, false, true));

    }

    public void typeDruid(PlayerEntity player) {

        final int range = 10; // 10 blocks

        Box searchBox = new Box(
                player.getX() - range, player.getY() - range, player.getZ() - range,
                player.getX() + range, player.getY() + range, player.getZ() + range);

        System.out.println("Search box" + searchBox.toString());

        // Get the number of tamed animals this player has
        int tamedAnimals = (int) player.getWorld()
                .getEntitiesByClass(Entity.class, searchBox, entity -> true).size();

        // TODO: THIS IS GETTING ALL ENTITIES, NOT THE ONES YOU TAMED

        // AND THE HEALTH ISNT WORKING PROPERLY IT WONT BE ABOVE THE MAX HEALTH.. MAYBE
        // CUZ ITS NOT STATIC?
        float health = player.getMaxHealth() + (tamedAnimals * 2);
        player.setHealth(health);
        System.out.println(tamedAnimals);
        System.out.println("Druid...");
    }

    public void typeFighter(PlayerEntity player) {
        System.out.println("Fighter...");
        player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(6);
        player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(26);
        // player.getAttributeInstance(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS).setBaseValue(1);
        // player.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE).setBaseValue(64);
    }

    public void typeMonk(PlayerEntity player) {
        // Weakness on anything but a staff.
        player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.12); // Default 0.1
        player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED).setBaseValue(6.0); // Default 4.0
        player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE)
                .setBaseValue(10 * (1 / (player.getArmor() + 1)));
    }

    public void typePaladin(PlayerEntity player) {
        System.out.println("Paladin...");
        // Natural smite...
        player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(26); // Default 20

        // Disallow potion effects - in living entity mixin
        // Weak in nether.
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
        // Slow fireball.
        // Resistant to fire and lava.
        // Fire tick in water and rain.
    }

    public void typeWizard(PlayerEntity player) {
        player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(10); // Default 20
        // Max iron armor.
    }

    public void typeArtificer(PlayerEntity player) {
        player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.12); // Default 0.1
        player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(1.5);
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

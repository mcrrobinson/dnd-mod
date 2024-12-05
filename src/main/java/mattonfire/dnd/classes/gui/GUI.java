package mattonfire.dnd.classes.gui;

import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.WSprite;
import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class GUI extends LightweightGuiDescription {
    public void packetConstructor(int classID) {
        PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
        passedData.writeInt(classID);
        ClientPlayNetworking.send(DnDClasses.C2S_CLASS_PICK_PACKET_ID, passedData);
    }

    public GUI() {
        WGridPanel root = new WGridPanel();
        setRootPanel(root);
        root.setSize(300, 300);

        WLabel label = new WLabel(Text.literal("Classes"), 0xFFFFFF);
        root.add(label, 0, 0, 2, 1);

        // Wizard Class.
        WButton wizard_button = new WButton(Text.translatable("Wizard"));
        root.add(wizard_button, 0, 2, 8, 1);
        WSprite wizard_icon = new WSprite(new Identifier("minecraft:textures/item/bamboo.png"));
        root.add(wizard_icon, 1, 2, 1, 1);

        // Barbarian
        WButton barbarian_button = new WButton(Text.translatable("Barbarian"));
        root.add(barbarian_button, 9, 2, 8, 1);
        WSprite barbarian_icon = new WSprite(new Identifier("minecraft:textures/block/cobweb.png"));
        root.add(barbarian_icon, 10, 2, 1, 1);

        // Rogue Class.
        WButton rogue_button = new WButton(Text.translatable("Rogue"));
        root.add(rogue_button, 0, 4, 8, 1);
        WSprite rogue_icon = new WSprite(new Identifier("minecraft:textures/item/poisonous_potato.png"));
        root.add(rogue_icon, 1, 4, 1, 1);

        // Bard
        WButton bard_button = new WButton(Text.translatable("Bard"));
        root.add(bard_button, 9, 4, 8, 1);
        WSprite bard_icon = new WSprite(new Identifier("minecraft:textures/item/nether_brick.png"));
        root.add(bard_icon, 10, 4, 1, 1);

        // Cleric
        WButton cleric_button = new WButton(Text.translatable("Cleric"));
        root.add(cleric_button, 0, 6, 8, 1);
        WSprite cleric_icon = new WSprite(new Identifier("minecraft:textures/item/chainmail_chestplate.png"));
        root.add(cleric_icon, 1, 6, 1, 1);

        // Druid
        WButton druid_button = new WButton(Text.translatable("Druid"));
        root.add(druid_button, 9, 6, 8, 1);
        WSprite druid_icon = new WSprite(new Identifier("minecraft:textures/block/glowstone.png"));
        root.add(druid_icon, 10, 6, 1, 1);

        // Fighter
        WButton fighter_button = new WButton(Text.translatable("Fighter"));
        root.add(fighter_button, 0, 8, 8, 1);
        WSprite fighter_icon = new WSprite(new Identifier("minecraft:textures/block/gold_block.png"));
        root.add(fighter_icon, 1, 8, 1, 1);

        // Monk
        WButton monk_button = new WButton(Text.translatable("Monk"));
        root.add(monk_button, 9, 8, 8, 1);
        WSprite monk_icon = new WSprite(new Identifier("minecraft:textures/item/book.png"));
        root.add(monk_icon, 10, 8, 1, 1);

        // Paladin
        WButton paladin_button = new WButton(Text.translatable("Paladin"));
        root.add(paladin_button, 0, 10, 8, 1);
        WSprite paladin_icon = new WSprite(new Identifier("minecraft:textures/item/kelp.png"));
        root.add(paladin_icon, 1, 10, 1, 1);

        // Ranger
        WButton ranger_button = new WButton(Text.translatable("Ranger"));
        root.add(ranger_button, 9, 10, 8, 1);
        WSprite ranger_icon = new WSprite(new Identifier("minecraft:textures/item/bow.png"));
        root.add(ranger_icon, 10, 10, 1, 1);

        // Necromancer
        WButton necromancer_button = new WButton(Text.translatable("Necromancer"));
        root.add(necromancer_button, 0, 12, 8, 1);
        WSprite necromancer_icon = new WSprite(new Identifier("minecraft:textures/block/wither_rose.png"));
        root.add(necromancer_icon, 1, 12, 1, 1);

        // Warlock
        WButton warlock_button = new WButton(Text.translatable("Warlock"));
        root.add(warlock_button, 9, 12, 8, 1);
        WSprite warlock_icon = new WSprite(new Identifier("minecraft:textures/item/fire_charge.png"));
        root.add(warlock_icon, 10, 12, 1, 1);

        // Artificer
        WButton artificer_button = new WButton(Text.translatable("Artificer"));
        root.add(artificer_button, 0, 14, 8, 1);
        WSprite artificer_icon = new WSprite(new Identifier("minecraft:textures/block/crafting_table_front.png"));
        root.add(artificer_icon, 1, 14, 1, 1);

        // Ranger
        WButton blood_hunter_button = new WButton(Text.translatable("Blood Hunter"));
        root.add(blood_hunter_button, 9, 14, 8, 1);
        WSprite blood_hunter_icon = new WSprite(new Identifier("minecraft:textures/item/redstone.png"));
        root.add(blood_hunter_icon, 10, 14, 1, 1);

        // Alchemist
        WButton alchemist_button = new WButton(Text.translatable("Alchemist"));
        root.add(alchemist_button, 9, 16, 8, 1);
        WSprite alchemist_icon = new WSprite(new Identifier("minecraft:textures/item/potion.png"));
        root.add(alchemist_icon, 10, 16, 1, 1);

        barbarian_button.setOnClick(() -> {
            // Buff: Strength increase. More health.
            // Nerf: Short sighted. Very slow.
            // Special: One punch man.
            packetConstructor(1);
        });
        bard_button.setOnClick(() -> {
            // Buff: Unnoticed by mobs. Double Jump. Fast.
            // Nerf: Less Health. Can't use Netherrite.
            // Special: Attracts passive animals as meat shields.
            packetConstructor(2);
        });
        cleric_button.setOnClick(() -> {
            // Buff: Mine a lot faster. Night Vision.
            // Nerf: Viewing distance shorter. Less attack damage.
            // Special: Burst of healing spell.
            packetConstructor(3);
        });
        druid_button.setOnClick(() -> {
            // Buff: Every taiimed animal adds a heart (capped at 5). Regen in the light
            // even when not full of food.
            // Nerf: Can't swim. Hunger in dark enviroments.
            // Special: Can turn into an animal they've killed.
            packetConstructor(4);
        });
        fighter_button.setOnClick(() -> {
            // Buff: High health. High Strength.
            // Nerf: Attracts mobs. Can't use bows. No potions.
            // Special: Super regen.
            packetConstructor(5);
        });
        monk_button.setOnClick(() -> {
            // Buff: The less armor means more damage. Staff is +10 attack. Speed 1.
            // Nerf: Weakness on anything but a staff.
            // Special: Haste 5 for a period.
            packetConstructor(6);

        });
        paladin_button.setOnClick(() -> {
            // Buff: Natural smite enchartment. High health.
            // Nerf: No potions. Extremely weak in nether.
            // Special: Self heal.
            packetConstructor(7);
        });
        ranger_button.setOnClick(() -> {
            // Buff: 2x Zoom using a bow and natural power. Natural Looting.
            // Nerf: Can't use swords. Every fire is blue fire.
            // Special: Machine gun bow.
            packetConstructor(8);
        });
        rogue_button.setOnClick(() -> {
            // Buff: No poison damage. No hunger.
            // Nerf: Nether mobs are alies but all overworld mobs will attempt to kill.
            // Special: Invisibility for a period.
            packetConstructor(9);
        });
        necromancer_button.setOnClick(() -> {
            // Buff: Wither debuff to all attacked. Not attacked by undead.
            // Nerf: Less Health. Less attack damage.
            // Special: Spawn allied undead that attack foe.
            packetConstructor(10);
        });
        warlock_button.setOnClick(() -> {
            // Buff: Slow fireball. Resistent to all fire and lava.
            // Nerf: Fire tick damage in water & rain.
            // Special: Breathe fire.
            packetConstructor(11);
        });
        wizard_button.setOnClick(() -> {
            // Buff: Elemental staffs scattered; Fireball, Lightning, Ice (freeze enemies)
            // Nerf: Half health. Max Iron armor.
            // Special: Nuclear bomb + blast resistance.
            packetConstructor(12);
        });
        artificer_button.setOnClick(() -> {
            // Buff: Speed increase. Automatic enchanting chance or Armor upgrade.
            // Nerf: Less damage. Uneffected by bonus potions (unless an ability)
            // Special: Armor buff for a period of time.
            packetConstructor(13);
        });
        blood_hunter_button.setOnClick(() -> {
            // Buff: Fire aspect on all swords. x2 Damage during Night.
            // Nerf: Swords cannot be dropped. 1/2 Damage during the Day.
            // Special: Takes control of ANY mob within 30m (including players).
            packetConstructor(14);
        });
        alchemist_button.setOnClick(() -> {
            // Buff: Craft special potions. e.g. Mob fight mob potion. Anxiety Potion.
            // Immune to poison and wither.
            // Nerf: Random potion backfire (1/5).
            // Special: Spam potions of strength and healing.
            packetConstructor(15);
        });

        root.validate(this);
    }
}

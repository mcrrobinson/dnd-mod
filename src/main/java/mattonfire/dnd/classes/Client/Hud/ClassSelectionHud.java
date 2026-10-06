package mattonfire.dnd.classes.Client.Hud;

import io.github.cottonmc.cotton.gui.client.LightweightGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WButton;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.data.Insets;
import io.github.cottonmc.cotton.gui.widget.icon.TextureIcon;
import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ClassSelectionHud extends LightweightGuiDescription {
    public void packetConstructor(int classID) {
        PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
        passedData.writeInt(classID);
        ClientPlayNetworking.send(DnDClasses.C2S_CLASS_PICK_PACKET_ID, passedData);
    }

    private record ClassOption(String name, int classID, String icon) {
    }

    // Shown in this order, left to right then top to bottom.
    private static final ClassOption[] CLASSES = {
            // Buff: Elemental staffs scattered; Fireball, Lightning, Ice (freeze enemies)
            // Nerf: Half health. Max Iron armor.
            // Special: Nuclear bomb + blast resistance.
            new ClassOption("Wizard", 12, "minecraft:textures/item/bamboo.png"),
            // Buff: Strength increase. More health.
            // Nerf: Short sighted. Very slow.
            // Special: One punch man.
            new ClassOption("Barbarian", 1, "minecraft:textures/block/cobweb.png"),
            // Buff: No poison damage. No hunger.
            // Nerf: Nether mobs are alies but all overworld mobs will attempt to kill.
            // Special: Invisibility for a period.
            new ClassOption("Rogue", 9, "minecraft:textures/item/poisonous_potato.png"),
            // Buff: Unnoticed by mobs. Double Jump. Fast.
            // Nerf: Less Health. Can't use Netherrite.
            // Special: Attracts passive animals as meat shields.
            new ClassOption("Bard", 2, "minecraft:textures/item/nether_brick.png"),
            // Buff: Mine a lot faster. Night Vision.
            // Nerf: Viewing distance shorter. Less attack damage.
            // Special: Burst of healing spell.
            new ClassOption("Cleric", 3, "minecraft:textures/item/chainmail_chestplate.png"),
            // Buff: Every taiimed animal adds a heart (capped at 5). Regen in the light
            // even when not full of food.
            // Nerf: Can't swim. Hunger in dark enviroments.
            // Special: Can turn into an animal they've killed.
            new ClassOption("Druid", 4, "minecraft:textures/block/glowstone.png"),
            // Buff: High health. High Strength.
            // Nerf: Attracts mobs. Can't use bows. No potions.
            // Special: Super regen.
            new ClassOption("Fighter", 5, "minecraft:textures/block/gold_block.png"),
            // Buff: The less armor means more damage. Staff is +10 attack. Speed 1.
            // Nerf: Weakness on anything but a staff.
            // Special: Haste 5 for a period.
            new ClassOption("Monk", 6, "minecraft:textures/item/book.png"),
            // Buff: Natural smite enchartment. High health.
            // Nerf: No potions. Extremely weak in nether.
            // Special: Self heal.
            new ClassOption("Paladin", 7, "minecraft:textures/item/kelp.png"),
            // Buff: 2x Zoom using a bow and natural power. Natural Looting.
            // Nerf: Can't use swords. Every fire is blue fire.
            // Special: Machine gun bow.
            new ClassOption("Ranger", 8, "minecraft:textures/item/bow.png"),
            // Buff: Wither debuff to all attacked. Not attacked by undead.
            // Nerf: Less Health. Less attack damage.
            // Special: Spawn allied undead that attack foe.
            new ClassOption("Necromancer", 10, "minecraft:textures/block/wither_rose.png"),
            // Buff: Slow fireball. Resistent to all fire and lava.
            // Nerf: Fire tick damage in water & rain.
            // Special: Breathe fire.
            new ClassOption("Warlock", 11, "minecraft:textures/item/fire_charge.png"),
            // Buff: Speed increase. Automatic enchanting chance or Armor upgrade.
            // Nerf: Less damage. Uneffected by bonus potions (unless an ability)
            // Special: Armor buff for a period of time.
            new ClassOption("Artificer", 13, "minecraft:textures/block/crafting_table_front.png"),
            // Buff: Fire aspect on all swords. x2 Damage during Night.
            // Nerf: Swords cannot be dropped. 1/2 Damage during the Day.
            // Special: Takes control of ANY mob within 30m (including players).
            new ClassOption("Blood Hunter", 14, "minecraft:textures/item/redstone.png"),
            // Buff: Craft special potions. e.g. Mob fight mob potion. Anxiety Potion.
            // Immune to poison and wither.
            // Nerf: Random potion backfire (1/5).
            // Special: Spam potions of strength and healing.
            new ClassOption("Alchemist", 15, "minecraft:textures/item/potion.png"),
    };

    // 3 columns x 5 rows of 5-cell buttons comes to about 312x142 scaled pixels,
    // which fits the smallest scaled screen Minecraft allows (320x240).
    private static final int COLUMNS = 3;
    private static final int BUTTON_CELLS = 5;

    public ClassSelectionHud() {
        WGridPanel root = new WGridPanel();
        root.setInsets(Insets.ROOT_PANEL);
        root.setGaps(2, 4);
        setRootPanel(root);

        root.add(new WLabel(Text.literal("Classes"), 0xFFFFFF), 0, 0, COLUMNS * BUTTON_CELLS, 1);

        for (int i = 0; i < CLASSES.length; i++) {
            ClassOption option = CLASSES[i];
            WButton button = new WButton(new TextureIcon(new Identifier(option.icon())), Text.translatable(option.name()));
            button.setOnClick(() -> packetConstructor(option.classID()));
            root.add(button, (i % COLUMNS) * BUTTON_CELLS, 1 + i / COLUMNS, BUTTON_CELLS, 1);
        }

        root.validate(this);
    }
}

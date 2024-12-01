package mattonfire.dnd.classes;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;
import mattonfire.dnd.classes.gui.GUI;
import mattonfire.dnd.classes.gui.Screen;

public class SetPlayerClass {
    public static void setPlayerClass(@Nullable MinecraftClient client, PlayerEntity player, int classID) {
        SetClassAttributes playerClasses = new SetClassAttributes();
        playerClasses.resetToDefault(player);
        switch (classID) {
            case 0:
                client.openScreen(new Screen(new GUI()));
            case 1:
                playerClasses.typeBarbarian(player);
                break;
            case 2:
                playerClasses.typeBard(player);
                break;
            case 3:
                playerClasses.typeCleric(player);
                break;
            case 4:
                playerClasses.typeDruid(player);
                break;
            case 5:
                playerClasses.typeFighter(player);
                break;
            case 6:
                playerClasses.typeMonk(player);
                break;
            case 7:
                playerClasses.typePaladin(player);
                break;
            case 8:
                playerClasses.typeRanger(player);
                break;
            case 9:
                playerClasses.typeRogue(player);
                break;
            case 10:
                playerClasses.typeNecromancer(player);
                break;
            case 11:
                playerClasses.typeWarlock(player);
                break;
            case 12:
                playerClasses.typeWizard(player);
                break;
            case 13:
                playerClasses.typeArtificer(player);
                break;
            case 14:
                playerClasses.typeBloodHunter(player);
                break;
            case 15:
                playerClasses.typeAlchemist(player);
                break;
            default:
                break;
        }
    }
}

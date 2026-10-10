package mattonfire.dnd.magic;

import mattonfire.dnd.classes.Commands.MagicCommand;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/** Entry point for the magic item system; called from {@code DnDClasses.onInitialize} after items exist. */
public final class Magic {
    private Magic() {
    }

    public static void register() {
        MagicItems.registerDefaults();
        MagicGear.register();
        MagicItemLootFunction.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> MagicCommand
                .register(dispatcher));
    }
}

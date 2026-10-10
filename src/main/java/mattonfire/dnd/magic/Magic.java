package mattonfire.dnd.magic;

import java.util.List;
import java.util.UUID;

import mattonfire.dnd.classes.Commands.MagicCommand;
import mattonfire.dnd.classes.Registry.ModItems;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/** Entry point for the magic item system; called from {@code DnDClasses.onInitialize} after items exist. */
public final class Magic {
    private Magic() {
    }

    /** Cloak of Protection: +1 armor, +1 toughness (and +1 to saves once saving throws exist). */
    private static final UUID CLOAK_ARMOR = UUID.fromString("0f3c2a1e-7d4b-4e55-9b0a-6c1d2e3f4a01");
    private static final UUID CLOAK_TOUGHNESS = UUID.fromString("0f3c2a1e-7d4b-4e55-9b0a-6c1d2e3f4a02");

    private static void registerEffects() {
        MagicEffects.register(ModItems.CLOAK_OF_PROTECTION, new MagicEffect() {
            private final List<Bonus> bonuses = List.of(
                    new Bonus(EntityAttributes.GENERIC_ARMOR, CLOAK_ARMOR, 1.0, EntityAttributeModifier.Operation.ADDITION),
                    new Bonus(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, CLOAK_TOUGHNESS, 1.0,
                            EntityAttributeModifier.Operation.ADDITION));

            @Override
            public List<Bonus> attributeBonuses() {
                return bonuses;
            }
        });
    }

    public static void register() {
        MagicItems.registerDefaults();
        MagicGear.register();
        MagicItemLootFunction.register();
        Attunement.register();
        ForgeBlessing.register();
        registerEffects();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> MagicCommand
                .register(dispatcher));
    }
}

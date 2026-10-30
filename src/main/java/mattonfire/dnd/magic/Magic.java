package mattonfire.dnd.magic;

import java.util.List;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Commands.MagicCommand;
import mattonfire.dnd.classes.Registry.ModItems;
import mattonfire.dnd.magic.items.WondrousItems;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

/** Entry point for the magic item system; called from {@code DnDClasses.onInitialize} after items exist. */
public final class Magic {
    private Magic() {
    }

    /** Cloak of Protection: +1 armor, +1 toughness and +1 to every saving throw. */
    public static final int CLOAK_SAVE_BONUS = 1;
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

        // The save bonus counts only while the cloak is in place and active (identified, attuned, the
        // right class): WondrousItems.has goes through MagicEffects.active, which checks Attunement.isActive.
        // Dynamic, so the sheet follows the cloak entering and leaving the inventory or being unattuned.
        AbilityScores.register(new Identifier(DnDClasses.MOD_ID, "magic/cloak_of_protection"), (player, c) -> {
            if (WondrousItems.has(player, ModItems.CLOAK_OF_PROTECTION))
                c.saveBonus(null, CLOAK_SAVE_BONUS, "Cloak of Protection");
        }, true);
    }

    public static void register() {
        MagicItems.registerDefaults();
        MagicGear.register();
        Corrosion.register();
        MagicItemLootFunction.register();
        MagicLoot.register();
        Attunement.register();
        ForgeBlessing.register();
        Curse.register();
        Identify.register();
        RemoveCurse.register();
        registerEffects();
        WondrousItems.register();
        mattonfire.dnd.magic.items.BossItems.register();
        mattonfire.dnd.magic.items.PlanarItems.register();
        mattonfire.dnd.magic.items.VorpalSwordItem.register();
        mattonfire.dnd.magic.items.SendingStones.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> MagicCommand
                .register(dispatcher));
    }
}

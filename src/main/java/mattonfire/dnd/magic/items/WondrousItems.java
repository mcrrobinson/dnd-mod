package mattonfire.dnd.magic.items;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Downed.Downed;
import mattonfire.dnd.classes.Downed.DownedEvents;
import mattonfire.dnd.classes.Registry.ModItems;
import mattonfire.dnd.classes.Rest.RestEvents;
import mattonfire.dnd.classes.Rest.RestKind;
import mattonfire.dnd.magic.MagicEffect;
import mattonfire.dnd.magic.MagicEffects;
import mattonfire.dnd.magic.MagicItemDef;
import mattonfire.dnd.magic.MagicItems;
import mattonfire.dnd.magic.MagicKind;
import mattonfire.dnd.magic.MagicTier;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;

/**
 * The wondrous and utility magic items (design 5, section 3.7, items 1-9 and 19): their tiers, the passive
 * effects of the Ring, Amulet and Periapt, and the rest/death-save hooks. The active items are their own
 * classes in this package. The Cloak of Protection's def and effect stay in {@code MagicItems}/{@code Magic}.
 */
public final class WondrousItems {
    public static final double RING_ARMOR = 2.0;
    public static final int RING_SAVE_BONUS = 1;
    public static final double AMULET_HEALTH = 6.0;
    public static final int AMULET_CON = 19;
    /** Periapt: an extra natural-regeneration heal every this many seconds (vanilla's slow heal is 4 s). */
    public static final int PERIAPT_REGEN_SECONDS = 4;

    private static final UUID RING_ARMOR_ID = UUID.fromString("0f3c2a1e-7d4b-4e55-9b0a-6c1d2e3f4b01");
    private static final UUID AMULET_HEALTH_ID = UUID.fromString("0f3c2a1e-7d4b-4e55-9b0a-6c1d2e3f4b02");

    /** Seconds since the Periapt's last extra heal, per player. */
    private static final Map<UUID, Integer> PERIAPT_CLOCK = new HashMap<>();

    private WondrousItems() {
    }

    public static void register() {
        registerDefs();
        registerEffects();

        // Long rest: the Wand of Magic Missiles regains 1d6+1 (dawn is the fallback, in the wand itself)
        RestEvents.AFTER_REST.register((player, kind, source) -> {
            if (kind != RestKind.LONG)
                return;
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.getItem() instanceof WandOfMagicMissilesItem)
                    WandOfMagicMissilesItem.recharge(stack, player.getRandom());
            }
        });

        // Periapt of Wound Closure: you stabilise at once when Downed
        DownedEvents.AFTER_DOWNED.register((player, source) -> {
            if (has(player, ModItems.PERIAPT_OF_WOUND_CLOSURE)) {
                player.sendMessage(Text.translatable("item.dndclasses.periapt_of_wound_closure.stabilise")
                        .formatted(Formatting.LIGHT_PURPLE), false);
                Downed.stabilise(player);
            }
        });

        // Ring of Protection: +1 to every saving throw. Amulet of Health: CON 19.
        AbilityScores.register(new Identifier(DnDClasses.MOD_ID, "magic/wondrous_items"), (player, c) -> {
            if (has(player, ModItems.RING_OF_PROTECTION))
                c.saveBonus(null, RING_SAVE_BONUS, "Ring of Protection");
            if (has(player, ModItems.AMULET_OF_HEALTH))
                c.setAtLeast(Ability.CON, AMULET_CON, "Amulet of Health");
        }, true);

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> PERIAPT_CLOCK
                .remove(handler.player.getUuid()));
    }

    private static void registerDefs() {
        MagicItems.register(MagicItemDef.of(ModItems.WAND_OF_MAGIC_MISSILES, MagicTier.UNCOMMON, MagicKind.WAND,
                "wand"));
        MagicItems.register(MagicItemDef.of(ModItems.BAG_OF_HOLDING, MagicTier.UNCOMMON, MagicKind.WONDROUS,
                "bag"));
        MagicItems.register(MagicItemDef.of(ModItems.DECANTER_OF_ENDLESS_WATER, MagicTier.UNCOMMON,
                MagicKind.WONDROUS, "wondrous_item"));
        MagicItems.register(MagicItemDef.of(ModItems.IMMOVABLE_ROD, MagicTier.UNCOMMON, MagicKind.WONDROUS, "rod"));
        MagicItems.register(MagicItemDef.of(ModItems.PERIAPT_OF_WOUND_CLOSURE, MagicTier.UNCOMMON,
                MagicKind.WONDROUS, "amulet").withAttunement());
        MagicItems.register(MagicItemDef.of(ModItems.RING_OF_PROTECTION, MagicTier.RARE, MagicKind.RING, "ring")
                .withAttunement());
        MagicItems.register(MagicItemDef.of(ModItems.AMULET_OF_HEALTH, MagicTier.RARE, MagicKind.WONDROUS,
                "amulet").withAttunement());
        MagicItems.register(MagicItemDef.of(ModItems.DOSS_LUTE, MagicTier.RARE, MagicKind.WONDROUS, "instrument")
                .withAttunement().onlyFor(DndCharacter.BARD));
        MagicItems.register(MagicItemDef.of(ModItems.TOME_OF_CLEAR_THOUGHT, MagicTier.VERY_RARE,
                MagicKind.CONSUMABLE, "book"));
    }

    private static void registerEffects() {
        MagicEffects.register(ModItems.RING_OF_PROTECTION, bonuses(new MagicEffect.Bonus(
                EntityAttributes.GENERIC_ARMOR, RING_ARMOR_ID, RING_ARMOR, EntityAttributeModifier.Operation.ADDITION)));
        MagicEffects.register(ModItems.AMULET_OF_HEALTH, bonuses(new MagicEffect.Bonus(
                EntityAttributes.GENERIC_MAX_HEALTH, AMULET_HEALTH_ID, AMULET_HEALTH,
                EntityAttributeModifier.Operation.ADDITION)));
        MagicEffects.register(ModItems.PERIAPT_OF_WOUND_CLOSURE, new MagicEffect() {
            @Override
            public void secondTick(ServerPlayerEntity player, ItemStack stack) {
                periaptRegen(player);
            }
        });
    }

    private static MagicEffect bonuses(MagicEffect.Bonus... bonuses) {
        List<MagicEffect.Bonus> list = List.of(bonuses);
        return new MagicEffect() {
            @Override
            public List<Bonus> attributeBonuses() {
                return list;
            }
        };
    }

    /** Whether one of the player's items of this kind is active and in place (server side). */
    public static boolean has(PlayerEntity player, Item item) {
        for (MagicEffects.Active active : MagicEffects.active(player)) {
            if (active.stack().isOf(item))
                return true;
        }
        return false;
    }

    /**
     * Natural regeneration twice as fast: the vanilla slow heal (1 HP per 4 s while food is 18+, for 6
     * exhaustion) happens a second time. The saturation fast heal isn't doubled.
     */
    private static void periaptRegen(ServerPlayerEntity player) {
        int seconds = PERIAPT_CLOCK.merge(player.getUuid(), 1, Integer::sum);
        if (seconds < PERIAPT_REGEN_SECONDS)
            return;
        PERIAPT_CLOCK.put(player.getUuid(), 0);
        if (!player.getWorld().getGameRules().getBoolean(GameRules.NATURAL_REGENERATION) || !player.isAlive()
                || Downed.is(player) || player.getHealth() >= player.getMaxHealth()
                || player.getHungerManager().getFoodLevel() < 18)
            return;
        player.heal(1.0F);
        player.getHungerManager().addExhaustion(6.0F);
    }
}

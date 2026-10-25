package mattonfire.dnd.magic.items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.Conditions;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModItems;
import mattonfire.dnd.classes.SkillChecks.SavingThrow;
import mattonfire.dnd.classes.SkillChecks.Stealth;
import mattonfire.dnd.entity.drow.DrowPoison;
import mattonfire.dnd.entity.drow.DrowSaves;
import mattonfire.dnd.magic.MagicEffect;
import mattonfire.dnd.magic.MagicEffects;
import mattonfire.dnd.magic.MagicItemDef;
import mattonfire.dnd.magic.MagicItems;
import mattonfire.dnd.magic.MagicKind;
import mattonfire.dnd.magic.MagicTier;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The drow magic items (design 9, 9.4; docs/items/magic-items.md):
 * <ul>
 * <li><b>Matron's Scourge</b> (Very Rare weapon, attunement), the Matron Mother's drop: its melee hits carry Drow
 * Poison (CON {@value DrowSaves#MATRON_POISON_DC}), and at most once every {@value #SCOURGE_FEAR_COOLDOWN} ticks a hit
 * frightens the target (WIS {@value #SCOURGE_FEAR_DC}, Frightened for {@value #SCOURGE_FEAR_TICKS} ticks). Reach is in
 * {@link MatronsScourgeItem}.</li>
 * <li><b>Piwafwi</b>, the Cloak of the Drow (Rare armour, attunement): worn in the chest slot (2 armour), +{@value
 * #PIWAFWI_STEALTH} Stealth on the sheet, and while you sneak where the light is {@value #PIWAFWI_LIGHT} or less, mobs
 * further than {@value #PIWAFWI_RANGE} blocks can't see you: they can't pick you as a target and lose you if they had
 * you ({@link Stealth.Concealment}).</li>
 * </ul>
 */
public final class DrowItems {
    public static final int SCOURGE_FEAR_COOLDOWN = 600;
    public static final int SCOURGE_FEAR_DC = 14;
    public static final int SCOURGE_FEAR_TICKS = 80;
    public static final int PIWAFWI_STEALTH = 2;
    public static final int PIWAFWI_LIGHT = 4;
    public static final double PIWAFWI_RANGE = 6.0;

    /** The Piwafwi's material: 2 armour as a chestplate, spidersilk to mend it. */
    public static final ArmorMaterial PIWAFWI_MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurability(ArmorItem.Type type) {
            return type == ArmorItem.Type.CHESTPLATE ? 240 : 160;
        }

        @Override
        public int getProtection(ArmorItem.Type type) {
            return type == ArmorItem.Type.CHESTPLATE ? 2 : 1;
        }

        @Override
        public int getEnchantability() {
            return 15;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ITEM_ARMOR_EQUIP_LEATHER;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.ofItems(mattonfire.dnd.entity.drow.Drow.SPIDERSILK);
        }

        @Override
        public String getName() {
            return "piwafwi";
        }

        @Override
        public float getToughness() {
            return 0.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    };

    /** World time each wielder's Scourge can frighten again. */
    private static final Map<UUID, Long> FEAR_READY = new HashMap<>();

    private DrowItems() {
    }

    static void register() {
        MagicItems.register(MagicItemDef.of(ModItems.MATRONS_SCOURGE, MagicTier.VERY_RARE, MagicKind.WEAPON, "whip")
                .withAttunement());
        MagicItems.register(MagicItemDef.of(ModItems.PIWAFWI, MagicTier.RARE, MagicKind.ARMOR, "cloak")
                .withAttunement());

        MagicEffects.register(ModItems.MATRONS_SCOURGE, new MagicEffect() {
            @Override
            public float modifyDealtDamage(PlayerEntity player, ItemStack stack, LivingEntity target,
                    DamageSource source, float amount) {
                if (source.getSource() == player && source.isOf(DamageTypes.PLAYER_ATTACK)) {
                    scourge(player, target);
                }
                return amount;
            }
        });
        // No passive effect beyond the hooks below, but registered so has() finds it while active
        MagicEffects.register(ModItems.PIWAFWI, new MagicEffect() {
        });

        AbilityScores.register(new Identifier(DnDClasses.MOD_ID, "magic/piwafwi"), (player, c) -> {
            if (WondrousItems.has(player, ModItems.PIWAFWI))
                c.skillBonus(Skill.STEALTH, PIWAFWI_STEALTH, "Piwafwi");
        }, true);
        Stealth.registerConcealment(DrowItems::piwafwiHides);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> FEAR_READY.remove(handler.player.getUuid()));
    }

    // ---- Matron's Scourge ----

    private static void scourge(PlayerEntity player, LivingEntity target) {
        if (target.getWorld().isClient || !target.isAlive()) {
            return;
        }
        DrowPoison.afflict(target, player, DrowSaves.MATRON_POISON);
        long now = target.getWorld().getTime();
        Long ready = FEAR_READY.get(player.getUuid());
        if (ready != null && now < ready || Conditions.isImmune(target, Condition.FRIGHTENED, player)) {
            return;
        }
        FEAR_READY.put(player.getUuid(), now + SCOURGE_FEAR_COOLDOWN);
        boolean failed = SavingThrow.of(target, Ability.WIS, SCOURGE_FEAR_DC).source(player).tags("fear")
                .label(Text.translatable("item.dndclasses.matrons_scourge.save"))
                .roll().failed();
        DnDClasses.LOGGER.info("[Magic] {}'s Matron's Scourge lashes {}: {}", player.getEntityName(),
                target.getEntityName(), failed ? "frightened" : "resists");
        if (failed) {
            Conditions.of(target, Condition.FRIGHTENED).source(player).key("matrons_scourge").ticks(SCOURGE_FEAR_TICKS)
                    .apply();
        }
    }

    // ---- Piwafwi ----

    /**
     * Whether the Piwafwi hides {@code player} from {@code observer}: sneaking, in light {@value #PIWAFWI_LIGHT} or
     * less, more than {@value #PIWAFWI_RANGE} blocks off, wearing it attuned. The cheap checks go first: mobs ask this
     * every tick they hold a target.
     */
    public static boolean piwafwiHides(PlayerEntity player, LivingEntity observer) {
        if (!player.isSneaking() || !player.getEquippedStack(EquipmentSlot.CHEST).isOf(ModItems.PIWAFWI)
                || observer.squaredDistanceTo(player) <= PIWAFWI_RANGE * PIWAFWI_RANGE) {
            return false;
        }
        return lightAround(player) <= PIWAFWI_LIGHT && WondrousItems.has(player, ModItems.PIWAFWI);
    }

    /** The light where the player stands (block light, or sky light less the night's darkness). */
    public static int lightAround(PlayerEntity player) {
        return player.getWorld().getLightLevel(player.getBlockPos());
    }
}

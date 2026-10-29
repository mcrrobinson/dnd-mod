package mattonfire.dnd.magic.items;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.RollFilter;
import mattonfire.dnd.classes.Registry.ModItems;
import mattonfire.dnd.magic.MagicEffect;
import mattonfire.dnd.magic.MagicEffects;
import mattonfire.dnd.magic.MagicItemDef;
import mattonfire.dnd.magic.MagicItems;
import mattonfire.dnd.magic.MagicKind;
import mattonfire.dnd.magic.MagicTier;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.structure.Structure;

/**
 * Red Dragon Scale Mail (design 10, 5.11): the Ancient Red's unique drop. A Very Rare chestplate (attunement, any
 * class) with {@value #ARMOR} armour, {@value #TOUGHNESS} toughness and +{@value #KNOCKBACK_RESISTANCE} knockback
 * resistance.
 * <ul>
 * <li>Fire damage (anything tagged {@code minecraft:is_fire}) is halved.</li>
 * <li>Advantage on saves tagged "dragon": every dragon's breath and storm, and the Ancient's other attacks.</li>
 * <li><b>Dragon sense</b>: crouch still for {@value #SENSE_SECONDS} s and, once a day, the action bar points to
 * the nearest Ancient dragon lair ({@code #dndclasses:ancient_dragon_lairs}) within {@value #SENSE_RANGE}
 * blocks.</li>
 * </ul>
 */
public class RedDragonScaleMailItem extends DescribedItem.Armor {
    public static final int ARMOR = 8;
    public static final float TOUGHNESS = 3.0F;
    public static final float KNOCKBACK_RESISTANCE = 0.1F;
    public static final float FIRE_MULTIPLIER = 0.5F;
    public static final int SENSE_SECONDS = 3;
    public static final int SENSE_RANGE = 3000;
    /** Structure tag the lair ticket fills with the Ancient lairs. */
    public static final TagKey<Structure> ANCIENT_LAIRS = TagKey.of(RegistryKeys.STRUCTURE,
            new Identifier(DnDClasses.MOD_ID, "ancient_dragon_lairs"));
    private static final String SENSE_DAY = "DragonSenseDay";

    /** Seconds each wearer has crouched without moving, and where. */
    private static final Map<UUID, Vec3d> STILL_AT = new HashMap<>();
    private static final Map<UUID, Integer> STILL_SECONDS = new HashMap<>();

    public static final ArmorMaterial MATERIAL = new ArmorMaterial() {
        @Override
        public int getDurability(ArmorItem.Type type) {
            return 37 * 16;
        }

        @Override
        public int getProtection(ArmorItem.Type type) {
            return ARMOR;
        }

        @Override
        public int getEnchantability() {
            return 15;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.ofItems(ModItems.ANCIENT_RED_SCALE);
        }

        @Override
        public String getName() {
            return "red_dragon_scale";
        }

        @Override
        public float getToughness() {
            return TOUGHNESS;
        }

        @Override
        public float getKnockbackResistance() {
            return KNOCKBACK_RESISTANCE;
        }
    };

    public RedDragonScaleMailItem(Settings settings) {
        super(MATERIAL, ArmorItem.Type.CHESTPLATE, settings);
    }

    /** Its tier, effects and the save advantage; called from {@link BossItems#register()}. */
    static void register() {
        MagicItems.register(MagicItemDef.of(ModItems.RED_DRAGON_SCALE_MAIL, MagicTier.VERY_RARE, MagicKind.ARMOR,
                "chestplate").withAttunement());
        MagicEffects.register(ModItems.RED_DRAGON_SCALE_MAIL, new MagicEffect() {
            @Override
            public float modifyTakenDamage(PlayerEntity player, ItemStack stack, DamageSource source, float amount) {
                return source.isIn(DamageTypeTags.IS_FIRE) ? amount * FIRE_MULTIPLIER : amount;
            }

            @Override
            public void secondTick(ServerPlayerEntity player, ItemStack stack) {
                dragonSense(player, stack);
            }
        });
        AbilityScores.register(new Identifier(DnDClasses.MOD_ID, "magic/dragon_scale_mail"), (player, c) -> {
            if (WondrousItems.has(player, ModItems.RED_DRAGON_SCALE_MAIL))
                c.advantage(RollFilter.tag("dragon"), "saves against dragons", "Red Dragon Scale Mail");
        }, true);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            STILL_AT.remove(handler.player.getUuid());
            STILL_SECONDS.remove(handler.player.getUuid());
        });
    }

    private static void dragonSense(ServerPlayerEntity player, ItemStack stack) {
        UUID id = player.getUuid();
        Vec3d pos = player.getPos();
        Vec3d last = STILL_AT.put(id, pos);
        if (!player.isSneaking() || last == null || last.squaredDistanceTo(pos) > 0.01) {
            STILL_SECONDS.remove(id);
            return;
        }
        int seconds = STILL_SECONDS.merge(id, 1, Integer::sum);
        if (seconds != SENSE_SECONDS) {
            return;
        }
        ServerWorld world = (ServerWorld) player.getWorld();
        long day = world.getTimeOfDay() / 24000L;
        if (stack.getOrCreateNbt().contains(SENSE_DAY) && stack.getNbt().getLong(SENSE_DAY) == day) {
            player.sendMessage(Text.translatable("item.dndclasses.red_dragon_scale_mail.sense_spent")
                    .formatted(Formatting.GRAY), true);
            return;
        }
        stack.getOrCreateNbt().putLong(SENSE_DAY, day);
        BlockPos lair = world.locateStructure(ANCIENT_LAIRS, player.getBlockPos(), SENSE_RANGE / 16, false);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.PLAYERS, 0.6F, 0.6F);
        if (lair == null || lair.getSquaredDistance(player.getBlockPos().withY(lair.getY())) > (double) SENSE_RANGE * SENSE_RANGE) {
            player.sendMessage(Text.translatable("item.dndclasses.red_dragon_scale_mail.sense_none")
                    .formatted(Formatting.GOLD), true);
            DnDClasses.LOGGER.info("[Magic] {}'s dragon sense finds no lair", player.getEntityName());
            return;
        }
        double dx = lair.getX() + 0.5 - player.getX();
        double dz = lair.getZ() + 0.5 - player.getZ();
        int distance = (int) (Math.round(Math.sqrt(dx * dx + dz * dz) / 100.0) * 100);
        player.sendMessage(Text.translatable("item.dndclasses.red_dragon_scale_mail.sense",
                Text.translatable("direction.dndclasses." + compass(dx, dz)), Math.max(100, distance))
                .formatted(Formatting.GOLD), true);
        DnDClasses.LOGGER.info("[Magic] {}'s dragon sense points to {}", player.getEntityName(), lair.toShortString());
    }

    /** One of the eight compass points for a horizontal offset (north is -z). */
    static String compass(double dx, double dz) {
        double angle = MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dx, -dz)));
        String[] names = { "north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west" };
        int index = (int) Math.floorMod(Math.round(angle / 45.0), 8);
        return names[index];
    }
}

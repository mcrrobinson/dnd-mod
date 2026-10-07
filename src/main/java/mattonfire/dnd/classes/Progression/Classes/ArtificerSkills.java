package mattonfire.dnd.classes.Progression.Classes;

import static mattonfire.dnd.classes.Progression.SkillHelpers.effects;
import static mattonfire.dnd.classes.Progression.SkillHelpers.spawnSummon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.AttributeBonus;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.SkillNode;
import mattonfire.dnd.classes.Registry.ModEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;

public class ArtificerSkills extends ClassSkills {
    private static final int CRAFT_XP = 1;
    private static final int CRAFT_XP_PER_MINUTE = 10; // Stops XP farming with wooden swords
    private static final int ENCHANT_XP = 3;
    private static final int SMITHING_XP = 3;

    private static final double REPAIR_FRACTION = 0.25;
    private static final float THORNS_DAMAGE = 2.0F;
    private static final float TINKERER_SKIP_CHANCE = 0.5F;
    private static final int GOLEM_LIFETIME_TICKS = 600;
    private static final int TITAN_TICKS = 400;

    // Smithing upgrades show up as "crafted" stats for netherite gear.
    private static final Item[] NETHERITE_GEAR = { Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE,
            Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE, Items.NETHERITE_HELMET,
            Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS };

    /** Last seen enchant/smithing stat counts, to turn stat increases into XP. */
    private record StatSnapshot(int enchants, int smiths, long time) {
    }

    private static final Map<UUID, StatSnapshot> SNAPSHOTS = new HashMap<>();
    /** Crafting XP given this minute: [minute, xp]. */
    private static final Map<UUID, long[]> CRAFT_XP_GIVEN = new HashMap<>();

    @Override
    public DndCharacter dndClass() {
        return DndCharacter.ARTIFICER;
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of(
                active("artificer.arcane_armor", "Arcane Armor", "+8 armor and +4 armor toughness for 30 seconds.",
                        "minecraft:iron_chestplate", 9, 0, 1, 3),
                // Armorer
                passive("artificer.reinforced_plating", "Reinforced Plating", "+3 armor toughness.",
                        "minecraft:iron_block", 1, 2, 3, "artificer.arcane_armor"),
                active("artificer.repair_field", "Repair Field",
                        "Repair a quarter of the durability of your worn armor and held items.", "minecraft:anvil",
                        4, 1, 2, 2, "artificer.reinforced_plating"),
                passive("artificer.thorned_plating", "Thorned Plating", "Mobs that hit you in melee take 2 damage.",
                        "minecraft:cactus", 1, 2, 1, "artificer.repair_field"),
                // Artillerist
                passive("artificer.tinkerer", "Tinkerer", "Tools and weapons lose durability half as often.",
                        "minecraft:smithing_table", 1, 0, 3, "artificer.arcane_armor"),
                active("artificer.iron_defender", "Iron Defender", "An iron golem fights for you for 30 seconds.",
                        "minecraft:carved_pumpkin", 6, 1, 0, 2, "artificer.tinkerer"),
                passive("artificer.overclock", "Overclock", "Haste I and 10% faster attacks.",
                        "minecraft:redstone", 1, 0, 1, "artificer.iron_defender"),
                active("artificer.mechanical_titan", "Mechanical Titan",
                        "Arcane Armor, Strength II and Resistance II for 20 seconds.",
                        "minecraft:netherite_chestplate", 9, 2, 1, 0, "artificer.thorned_plating",
                        "artificer.overclock"));
    }

    @Override
    public List<AttributeBonus> attributeBonuses() {
        return List.of(
                new AttributeBonus("artificer.reinforced_plating", EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 3,
                        EntityAttributeModifier.Operation.ADDITION),
                new AttributeBonus("artificer.overclock", EntityAttributes.GENERIC_ATTACK_SPEED, 0.1,
                        EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    @Override
    public boolean activate(ServerPlayerEntity player, SkillNode node) {
        ServerWorld world = (ServerWorld) player.getWorld();
        switch (node.id()) {
            case "artificer.repair_field" -> {
                boolean repaired = false;
                for (ItemStack stack : player.getArmorItems()) {
                    repaired |= repair(stack);
                }
                repaired |= repair(player.getStackInHand(Hand.MAIN_HAND));
                repaired |= repair(player.getStackInHand(Hand.OFF_HAND));
                if (!repaired)
                    return false; // Nothing damaged, keep the mana
                effects(player, SoundEvents.BLOCK_ANVIL_USE, ParticleTypes.WAX_OFF, 20);
            }
            case "artificer.iron_defender" -> {
                IronGolemEntity golem = EntityType.IRON_GOLEM.create(world);
                if (golem == null)
                    return false;
                golem.setPlayerCreated(true); // Never turns on players
                golem.refreshPositionAndAngles(player.getX() + 1.5, player.getY(), player.getZ(), player.getYaw(),
                        0);
                spawnSummon(world, golem, GOLEM_LIFETIME_TICKS);
                effects(player, SoundEvents.ENTITY_IRON_GOLEM_REPAIR, ParticleTypes.CLOUD, 20);
            }
            case "artificer.mechanical_titan" -> {
                player.addStatusEffect(new StatusEffectInstance(ModEffects.ARMOR_BUFF, TITAN_TICKS, 0));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, TITAN_TICKS, 1));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, TITAN_TICKS, 1));
                effects(player, SoundEvents.BLOCK_ANVIL_LAND, ParticleTypes.CRIT, 30);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /** Repairs part of a damaged item; false if there was nothing to repair. */
    private static boolean repair(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageable() || !stack.isDamaged())
            return false;
        int amount = (int) Math.ceil(stack.getMaxDamage() * REPAIR_FRACTION);
        stack.setDamage(Math.max(0, stack.getDamage() - amount));
        return true;
    }

    @Override
    public float modifyTakenDamage(PlayerEntity player, ClassProgress progress, DamageSource source, float amount) {
        // Direct melee hits only; thorns damage is skipped so two Artificers can't bounce it forever.
        if (progress.hasPassive("artificer.thorned_plating") && source.getAttacker() instanceof LivingEntity attacker
                && attacker != player && source.getSource() == attacker && !source.isIn(DamageTypeTags.IS_PROJECTILE)
                && !source.isOf(DamageTypes.THORNS)) {
            attacker.damage(player.getWorld().getDamageSources().thorns(player), THORNS_DAMAGE);
        }
        return amount;
    }

    @Override
    public void secondTick(ServerPlayerEntity player, ClassProgress progress) {
        if (progress.hasPassive("artificer.overclock")) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 60, 0, true, false, true));
        }
        giveStatXp(player);
    }

    /** XP for enchanting and smithing, read from the player's stats so no shared mixin is needed. */
    private static void giveStatXp(ServerPlayerEntity player) {
        int enchants = player.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.ENCHANT_ITEM));
        int smiths = 0;
        for (Item item : NETHERITE_GEAR) {
            smiths += player.getStatHandler().getStat(Stats.CRAFTED.getOrCreateStat(item));
        }
        long now = player.getWorld().getTime();
        StatSnapshot last = SNAPSHOTS.put(player.getUuid(), new StatSnapshot(enchants, smiths, now));
        // No recent snapshot (just joined, or was another class): only take a baseline.
        if (last == null || now - last.time() > 40 || now < last.time())
            return;
        int xp = Math.max(0, enchants - last.enchants()) * ENCHANT_XP
                + Math.max(0, smiths - last.smiths()) * SMITHING_XP;
        if (xp > 0) {
            Progression.addXp(player, xp);
        }
    }

    /** Called by {@code ArtificerCrafting} when an Artificer crafts a tool, weapon or armor piece. */
    public static void onCraftedEquipment(ServerPlayerEntity player) {
        long minute = player.getWorld().getTime() / 1200;
        long[] given = CRAFT_XP_GIVEN.computeIfAbsent(player.getUuid(), id -> new long[] { minute, 0 });
        if (given[0] != minute) {
            given[0] = minute;
            given[1] = 0;
        }
        if (given[1] >= CRAFT_XP_PER_MINUTE)
            return;
        given[1] += CRAFT_XP;
        Progression.addXp(player, CRAFT_XP);
    }

    /** Tinkerer: each point of tool or weapon wear has a chance to be skipped. */
    public static int tinkererWear(ServerPlayerEntity player, ItemStack stack, int amount) {
        if (player == null || amount <= 0 || stack.getItem() instanceof ArmorItem
                || !Progression.hasPassive(player, "artificer.tinkerer"))
            return amount;
        int kept = 0;
        for (int i = 0; i < amount; i++) {
            if (player.getRandom().nextFloat() >= TINKERER_SKIP_CHANCE)
                kept++;
        }
        return kept;
    }
}

package mattonfire.dnd.classes.Progression.Classes;

import static mattonfire.dnd.classes.Progression.SkillHelpers.alliesNear;
import static mattonfire.dnd.classes.Progression.SkillHelpers.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.SkillNode;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.recipe.BrewingRecipeRegistry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class AlchemistSkills extends ClassSkills {
    private static final int BREW_XP = 2;
    private static final int POTION_USE_XP = 1;
    private static final int POTION_KILL_BONUS_XP = 2;
    private static final float EFFICIENT_BREWER_CHANCE = 0.25F;
    private static final float PHILOSOPHER_CHANCE = 0.1F;
    private static final float POTENT_BREWS_MULTIPLIER = 1.5F;
    private static final float FLASK_POWER = 2.5F;
    private static final int FLASK_COLOR = 0xFF6A00;
    private static final int FLASK_MAX_AGE = 200;
    private static final int GRAND_ELIXIR_TICKS = 600;
    private static final int GRAND_ELIXIR_EFFECTS = 3;

    /** Buffs Grand Elixir picks from. */
    private static final List<StatusEffect> ELIXIR_EFFECTS = List.of(StatusEffects.SPEED, StatusEffects.STRENGTH,
            StatusEffects.REGENERATION, StatusEffects.RESISTANCE, StatusEffects.FIRE_RESISTANCE, StatusEffects.HASTE,
            StatusEffects.JUMP_BOOST, StatusEffects.ABSORPTION, StatusEffects.NIGHT_VISION);

    /** Volatile flasks in flight; each explodes when it shatters. */
    private static final Set<PotionEntity> FLASKS = new HashSet<>();

    @Override
    public DndCharacter dndClass() {
        return DndCharacter.ALCHEMIST;
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of(
                active("alchemist.distill", "Distill",
                        "Upgrade every potion in your inventory to its strongest version.", "minecraft:glowstone_dust",
                        9, 0, 1, 3),
                // Mutagen
                passive("alchemist.iron_stomach", "Iron Stomach", "You're immune to Poison, Wither and Nausea.",
                        "minecraft:milk_bucket", 1, 2, 3, "alchemist.distill"),
                active("alchemist.volatile_flask", "Volatile Flask",
                        "Throw a flask that explodes where it lands. It doesn't break blocks or hurt you.",
                        "minecraft:splash_potion", 3, 1, 2, 2, "alchemist.iron_stomach"),
                passive("alchemist.potent_brews", "Potent Brews", "Potions you drink last 50% longer.",
                        "minecraft:redstone", 1, 2, 1, "alchemist.volatile_flask"),
                // Transmuter
                passive("alchemist.efficient_brewer", "Efficient Brewer",
                        "25% chance a brew doesn't use up its ingredient.", "minecraft:brewing_stand", 1, 0, 3,
                        "alchemist.distill"),
                active("alchemist.elixir_of_healing", "Elixir of Healing",
                        "Heal yourself and allies within 6 blocks by 4 hearts.", "minecraft:glistering_melon_slice", 4,
                        1, 0, 2, "alchemist.efficient_brewer"),
                passive("alchemist.philosophers_touch", "Philosopher's Touch",
                        "10% chance of an extra drop from ores.", "minecraft:raw_gold", 1, 0, 1,
                        "alchemist.elixir_of_healing"),
                active("alchemist.grand_elixir", "Grand Elixir",
                        "You and allies within 8 blocks get 3 random buffs at level II for 30 seconds.",
                        "minecraft:dragon_breath", 9, 2, 1, 0, "alchemist.potent_brews",
                        "alchemist.philosophers_touch"));
    }

    @Override
    public void register() {
        // XP for throwing splash and lingering potions.
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer
                    && (stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION))) {
                onPotionUsed(serverPlayer, stack);
            }
            return TypedActionResult.pass(stack);
        });

        PlayerBlockBreakEvents.AFTER.register(AlchemistSkills::philosophersTouch);

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (FLASKS.isEmpty())
                return;
            FLASKS.removeIf(flask -> {
                // A potion is discarded when it shatters
                if (flask.getRemovalReason() == Entity.RemovalReason.DISCARDED) {
                    flask.getWorld().createExplosion(flask.getOwner(), flask.getX(), flask.getY(), flask.getZ(),
                            FLASK_POWER, World.ExplosionSourceType.NONE);
                    return true;
                }
                // Unloaded, or flew off without landing
                return flask.isRemoved() || flask.age > FLASK_MAX_AGE;
            });
        });
    }

    @Override
    public boolean activate(ServerPlayerEntity player, SkillNode node) {
        ServerWorld world = (ServerWorld) player.getWorld();
        switch (node.id()) {
            case "alchemist.volatile_flask" -> {
                ItemStack flaskItem = PotionUtil.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.AWKWARD);
                flaskItem.getOrCreateNbt().putInt("CustomPotionColor", FLASK_COLOR);
                PotionEntity flask = new PotionEntity(world, player);
                flask.setItem(flaskItem);
                flask.setVelocity(player, player.getPitch(), player.getYaw(), -20.0F, 1.0F, 1.0F);
                world.spawnEntity(flask);
                FLASKS.add(flask);
                world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_SPLASH_POTION_THROW,
                        SoundCategory.PLAYERS, 1.0F, 0.8F);
            }
            case "alchemist.elixir_of_healing" -> {
                for (LivingEntity ally : alliesNear(player, 6)) {
                    ally.heal(8.0F);
                }
                effects(player, SoundEvents.ENTITY_WITCH_DRINK, ParticleTypes.HEART, 15);
            }
            case "alchemist.grand_elixir" -> {
                List<StatusEffect> pool = new ArrayList<>(ELIXIR_EFFECTS);
                Collections.shuffle(pool, new Random(player.getRandom().nextLong()));
                List<StatusEffect> picked = pool.subList(0, GRAND_ELIXIR_EFFECTS);
                for (LivingEntity ally : alliesNear(player, 8)) {
                    for (StatusEffect effect : picked) {
                        ally.addStatusEffect(new StatusEffectInstance(effect, GRAND_ELIXIR_TICKS, 1), player);
                    }
                }
                effects(player, SoundEvents.BLOCK_BREWING_STAND_BREW, ParticleTypes.WITCH, 40);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public int killXp(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
        // Potion and flask kills
        boolean alchemical = source.isOf(DamageTypes.MAGIC) || source.isOf(DamageTypes.INDIRECT_MAGIC)
                || source.isIn(DamageTypeTags.IS_EXPLOSION);
        return alchemical ? POTION_KILL_BONUS_XP : 0;
    }

    @Override
    public void secondTick(ServerPlayerEntity player, ClassProgress progress) {
        if (progress.hasPassive("alchemist.iron_stomach")) {
            player.removeStatusEffect(StatusEffects.POISON);
            player.removeStatusEffect(StatusEffects.WITHER);
            player.removeStatusEffect(StatusEffects.NAUSEA);
        }
    }

    private static boolean isAlchemist(PlayerEntity player) {
        return Progression.classOf(player) == DndCharacter.ALCHEMIST;
    }

    /** XP for drinking or throwing a potion that has effects. */
    public static void onPotionUsed(ServerPlayerEntity player, ItemStack stack) {
        if (isAlchemist(player) && !PotionUtil.getPotionEffects(stack).isEmpty()) {
            Progression.addXp(player, POTION_USE_XP);
        }
    }

    /** Potent Brews: lengthens an effect from a potion the player drinks. */
    public static StatusEffectInstance potentBrew(LivingEntity user, StatusEffectInstance effect) {
        if (!(user instanceof ServerPlayerEntity player) || !Progression.hasPassive(player, "alchemist.potent_brews"))
            return effect;
        return new StatusEffectInstance(effect.getEffectType(),
                Math.round(effect.getDuration() * POTENT_BREWS_MULTIPLIER), effect.getAmplifier(),
                effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon());
    }

    /**
     * Called by a brewing stand just before a brew finishes: the brewer gets
     * XP per potion.
     *
     * @return true if Efficient Brewer saves the ingredient
     */
    public static boolean onBrew(World world, UUID brewer, List<ItemStack> slots) {
        if (brewer == null || world.getServer() == null)
            return false;
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(brewer);
        if (player == null || !isAlchemist(player))
            return false;

        ItemStack ingredient = slots.get(3);
        int brewed = 0;
        for (int i = 0; i < 3; i++) {
            ItemStack bottle = slots.get(i);
            if (!bottle.isEmpty() && BrewingRecipeRegistry.hasRecipe(bottle, ingredient))
                brewed++;
        }
        Progression.addXp(player, brewed * BREW_XP);
        return Progression.hasPassive(player, "alchemist.efficient_brewer")
                && world.getRandom().nextFloat() < EFFICIENT_BREWER_CHANCE;
    }

    private static void philosophersTouch(World world, PlayerEntity player, BlockPos pos, BlockState state,
            BlockEntity blockEntity) {
        if (world.isClient || player.isCreative() || !isOre(state) || !player.canHarvest(state)
                || !Progression.hasPassive(player, "alchemist.philosophers_touch")
                || EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, player.getMainHandStack()) > 0
                || world.getRandom().nextFloat() >= PHILOSOPHER_CHANCE)
            return;
        for (ItemStack drop : Block.getDroppedStacks(state, (ServerWorld) world, pos, blockEntity, player,
                player.getMainHandStack())) {
            Block.dropStack(world, pos, drop);
        }
        ((ServerWorld) world).spawnParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 0.5,
                pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.05);
    }

    private static boolean isOre(BlockState state) {
        return state.isIn(BlockTags.COAL_ORES) || state.isIn(BlockTags.IRON_ORES) || state.isIn(BlockTags.GOLD_ORES)
                || state.isIn(BlockTags.COPPER_ORES) || state.isIn(BlockTags.DIAMOND_ORES)
                || state.isIn(BlockTags.EMERALD_ORES) || state.isIn(BlockTags.LAPIS_ORES)
                || state.isIn(BlockTags.REDSTONE_ORES) || state.isOf(Blocks.NETHER_QUARTZ_ORE);
    }
}

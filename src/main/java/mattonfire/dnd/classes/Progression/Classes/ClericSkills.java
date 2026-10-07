package mattonfire.dnd.classes.Progression.Classes;

import static mattonfire.dnd.classes.Progression.SkillHelpers.alliesNear;
import static mattonfire.dnd.classes.Progression.SkillHelpers.effects;
import static mattonfire.dnd.classes.Progression.SkillHelpers.enemiesNear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.SkillNode;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBlockTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;

/** Cleric skill tree: Life (healing, smiting undead) and Forge (mining, radiance). */
public class ClericSkills extends ClassSkills {
    private static final int UNDEAD_KILL_BONUS_XP = 3;
    private static final int ORE_XP = 1;
    private static final int RARE_ORE_XP = 3;

    private static final float PRESERVE_LIFE_THRESHOLD = 0.3F;
    private static final int PRESERVE_LIFE_COOLDOWN = 60 * 20;
    private static final float CURE_WOUNDS_HEAL = 8.0F;
    private static final float SMITE_MULTIPLIER = 1.5F;
    private static final float PROSPECTOR_CHANCE = 0.2F;
    private static final float RADIANCE_DAMAGE = 4.0F;

    /** Server tick each player's Preserve Life is ready again at. */
    private static final Map<UUID, Integer> PRESERVE_LIFE_READY = new HashMap<>();

    @Override
    public DndCharacter dndClass() {
        return DndCharacter.CLERIC;
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of(
                active("cleric.sanctuary", "Sanctuary", "Mobs ignore you for 15 seconds.", "minecraft:bell", 9, 0, 1,
                        3),
                // Life
                passive("cleric.preserve_life", "Preserve Life",
                        "Below 30% health, gain Regeneration II for 5 seconds (once a minute).",
                        "minecraft:golden_apple", 1, 2, 3, "cleric.sanctuary"),
                active("cleric.cure_wounds", "Cure Wounds", "Heal you and allies within 8 blocks by 4 hearts.",
                        "minecraft:glistering_melon_slice", 4, 1, 2, 2, "cleric.preserve_life"),
                passive("cleric.smite_undead", "Smite Undead", "Deal 50% more damage to undead.",
                        "minecraft:golden_sword", 1, 2, 1, "cleric.cure_wounds"),
                // Forge
                passive("cleric.prospector", "Prospector", "20% chance of an extra drop from ores.",
                        "minecraft:raw_gold", 1, 0, 3, "cleric.sanctuary"),
                active("cleric.radiance", "Radiance",
                        "Undead within 8 blocks burn and take 2 hearts of damage; all mobs nearby glow for 10 seconds.",
                        "minecraft:glowstone_dust", 4, 1, 0, 2, "cleric.prospector"),
                passive("cleric.deep_delver", "Deep Delver", "Haste IV instead of Haste III.",
                        "minecraft:diamond_pickaxe", 1, 0, 1, "cleric.radiance"),
                active("cleric.divine_intervention", "Divine Intervention",
                        "Fully heal and cleanse you and allies within 10 blocks, with Resistance III for 10 seconds.",
                        "minecraft:totem_of_undying", 9, 2, 1, 0, "cleric.smite_undead", "cleric.deep_delver"));
    }

    @Override
    public void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayerEntity serverPlayer) || player.isCreative()
                    || Progression.classOf(player) != DndCharacter.CLERIC || !isOre(state)) {
                return;
            }
            Progression.addXp(serverPlayer, isRareOre(state) ? RARE_ORE_XP : ORE_XP);

            // Prospector: roll the block's loot a second time. Not with Silk Touch, or ores could be duplicated.
            ItemStack tool = player.getMainHandStack();
            if (Progression.hasPassive(player, "cleric.prospector") && player.canHarvest(state)
                    && EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, tool) == 0
                    && world.random.nextFloat() < PROSPECTOR_CHANCE) {
                // getDroppedStacks rather than dropStacks, so the ore's XP orbs aren't doubled too.
                Block.getDroppedStacks(state, (ServerWorld) world, pos, blockEntity, player, tool)
                        .forEach(stack -> Block.dropStack(world, pos, stack));
                ((ServerWorld) world).spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5,
                        pos.getY() + 0.5, pos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0);
            }
        });
    }

    private static boolean isOre(BlockState state) {
        return state.isIn(ConventionalBlockTags.ORES) || state.isOf(Blocks.ANCIENT_DEBRIS);
    }

    private static boolean isRareOre(BlockState state) {
        return state.isIn(BlockTags.DIAMOND_ORES) || state.isIn(BlockTags.EMERALD_ORES)
                || state.isOf(Blocks.ANCIENT_DEBRIS);
    }

    private static boolean isUndead(LivingEntity entity) {
        return entity.getGroup() == EntityGroup.UNDEAD;
    }

    @Override
    public boolean activate(ServerPlayerEntity player, SkillNode node) {
        ServerWorld world = (ServerWorld) player.getWorld();
        switch (node.id()) {
            case "cleric.cure_wounds" -> {
                for (LivingEntity ally : alliesNear(player, 8)) {
                    ally.heal(CURE_WOUNDS_HEAL);
                    world.spawnParticles(ParticleTypes.HEART, ally.getX(), ally.getY() + 1.5, ally.getZ(), 4, 0.4,
                            0.3, 0.4, 0);
                }
                effects(player, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, ParticleTypes.HAPPY_VILLAGER, 20);
            }
            case "cleric.radiance" -> {
                for (LivingEntity mob : enemiesNear(player, 8)) {
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 200, 0), player);
                    if (isUndead(mob)) {
                        mob.setOnFireFor(5);
                        mob.damage(world.getDamageSources().indirectMagic(player, player), RADIANCE_DAMAGE);
                    }
                }
                effects(player, SoundEvents.BLOCK_BEACON_ACTIVATE, ParticleTypes.END_ROD, 40);
            }
            case "cleric.divine_intervention" -> {
                for (LivingEntity ally : alliesNear(player, 10)) {
                    ally.setHealth(ally.getMaxHealth());
                    ally.extinguish();
                    List<StatusEffect> harmful = new ArrayList<>();
                    for (StatusEffectInstance effect : ally.getStatusEffects()) {
                        if (effect.getEffectType().getCategory() == StatusEffectCategory.HARMFUL) {
                            harmful.add(effect.getEffectType());
                        }
                    }
                    harmful.forEach(ally::removeStatusEffect);
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 2));
                    world.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, ally.getX(), ally.getY() + 1, ally.getZ(),
                            30, 0.5, 0.8, 0.5, 0.2);
                }
                effects(player, SoundEvents.ITEM_TOTEM_USE, ParticleTypes.END_ROD, 30);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public int killXp(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
        return isUndead(killed) ? UNDEAD_KILL_BONUS_XP : 0;
    }

    @Override
    public float modifyDealtDamage(PlayerEntity player, ClassProgress progress, LivingEntity target,
            DamageSource source, float amount) {
        return progress.hasPassive("cleric.smite_undead") && isUndead(target) ? amount * SMITE_MULTIPLIER : amount;
    }

    @Override
    public void secondTick(ServerPlayerEntity player, ClassProgress progress) {
        if (!progress.hasPassive("cleric.preserve_life") || !player.isAlive()
                || player.getHealth() >= player.getMaxHealth() * PRESERVE_LIFE_THRESHOLD) {
            return;
        }
        int now = player.getServer().getTicks();
        if (now < PRESERVE_LIFE_READY.getOrDefault(player.getUuid(), 0)) {
            return;
        }
        PRESERVE_LIFE_READY.put(player.getUuid(), now + PRESERVE_LIFE_COOLDOWN);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1));
        effects(player, SoundEvents.ENTITY_PLAYER_LEVELUP, ParticleTypes.HEART, 8);
    }
}

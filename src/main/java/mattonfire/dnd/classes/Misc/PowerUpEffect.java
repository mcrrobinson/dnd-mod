package mattonfire.dnd.classes.Misc;

import java.util.List;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Goals.FollowSummonerGoal;
import mattonfire.dnd.classes.Goals.TimedDespawnGoal;
import mattonfire.dnd.classes.Registry.ModEffects;
import mattonfire.dnd.classes.mixin.MobEntityAccessor;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World.ExplosionSourceType;

public class PowerUpEffect {

    /**
     * Adds an AI goal that makes the entity target hostile mobs.
     */
    private static void addHostileTargetGoal(MobEntity mob, PlayerEntity player) {
        if (mob instanceof ZombieEntity || mob instanceof SkeletonEntity) {
            MobEntityAccessor mobAccessor = (MobEntityAccessor) mob;
            mobAccessor.getTargetSelector().add(2, new ActiveTargetGoal<LivingEntity>(
                    mob, LivingEntity.class, 10, true, false, entity -> entity instanceof Monster));

            mobAccessor.getGoalSelector().add(3,
                    new FollowSummonerGoal((PathAwareEntity) mob, player, 2.0D, 5.0F, 10.0F));

            mobAccessor.getGoalSelector().add(1, new TimedDespawnGoal(mob, 100));
        }
    }

    public static void spawnUndead(PlayerEntity player) {
        if (!(player.world instanceof ServerWorld world))
            return; // Ensure it's server-side

        BlockPos pos = player.getBlockPos(); // Get necromancer's position

        Team allyTeam = world.getScoreboard().getTeam(player.getUuidAsString());
        if (allyTeam == null) {
            allyTeam = world.getScoreboard().addTeam(player.getUuidAsString());
            // Optionally configure the team to not show nametags, etc.
        }

        // Spawn a Zombie
        ZombieEntity zombie = EntityType.ZOMBIE.create(world);
        if (zombie != null) {
            zombie.refreshPositionAndAngles(pos.getX(), pos.getY(), pos.getZ(), world.random.nextFloat() * 360F, 0F);
            addHostileTargetGoal(zombie, player); // Add AI to target hostiles

            world.getScoreboard().addPlayerToTeam(zombie.getUuidAsString(), allyTeam);

            zombie.setPersistent();
            world.spawnEntity(zombie); // Add to the world
        }

        // Spawn a Skeleton
        SkeletonEntity skeleton = EntityType.SKELETON.create(world);
        if (skeleton != null) {
            skeleton.refreshPositionAndAngles(pos.getX() + 1, pos.getY(), pos.getZ(), world.random.nextFloat() * 360F,
                    0F);
            addHostileTargetGoal(skeleton, player); // Add AI to target hostiles
            world.getScoreboard().addPlayerToTeam(skeleton.getUuidAsString(), allyTeam);

            skeleton.setPersistent();
            world.spawnEntity(skeleton);
        }
    }

    public static void bardEffect(PlayerEntity player) {
        List<LivingEntity> nearbyEntities = player.getEntityWorld().getEntitiesByClass(
                LivingEntity.class,
                player.getBoundingBox().expand(10), // 10-block radius
                entity -> entity instanceof PassiveEntity && !(entity instanceof PlayerEntity));

        for (LivingEntity entity : nearbyEntities) {
            if (entity instanceof PassiveEntity passiveMob) {

                // If it's a tameable entity (e.g., wolf, cat, etc.), make it follow the player
                if (passiveMob instanceof TameableEntity tameable) {
                    if (!tameable.isTamed()) {
                        tameable.setOwner(player);
                    }
                }

                // Modify AI Goals using Mixin Accessor
                if (passiveMob instanceof MobEntity) {
                    MobEntityAccessor accessor = (MobEntityAccessor) passiveMob;
                    accessor.getTargetSelector().add(1, new ActiveTargetGoal<>(
                            passiveMob, HostileEntity.class, true));
                }
            }
        }

        // Make player unseen by hostile mobs
        List<HostileEntity> hostileEntities = player.getEntityWorld().getEntitiesByClass(
                HostileEntity.class,
                player.getBoundingBox().expand(10), // 10-block radius
                entity -> true);
    }

    public static void play(PlayerEntity player, DndCharacter character) {

        System.out.println("Starting powerup on: " + character.toString());
        switch (character) {
            case RANGER:
                // Make the bow shoot faster
                break;
            case WIZARD:
                player.getEntityWorld().createExplosion(null, player.getX(), player.getY(), player.getZ(), 10.F, true,
                        ExplosionSourceType.TNT);
                break;
            case BARBARIAN:
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 300, 2));
                break;
            case BARD:
                bardEffect(player);
                break;
            case CLERIC:
                player.addStatusEffect(new StatusEffectInstance(ModEffects.MOB_REPEL, 300));
                break;
            case PALADIN:
                // Heal everyone within a area of the user
                List<PlayerEntity> nearbyEntities = player.getEntityWorld().getEntitiesByClass(
                        PlayerEntity.class,
                        player.getBoundingBox().expand(10), // 10-block radius
                        entity -> true);

                for (PlayerEntity entity : nearbyEntities) {
                    entity.heal(entity.getMaxHealth());
                }
                break;
            case ROGUE:
                // Make the player immune to poison
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 300, 0));
                break;
            case NECROMANCER:
                spawnUndead(player);
                // Spawn undead enemies
                break;
            case WARLOCK:
                // Breathes fire
                break;
            case ARTIFICER:
                // temporary buff to armor
                break;
            case BLOODHUNTER:
                // temporarily take control of mobs
                break;
            case ALCHEMIST:
                // buffs all potions in inventory
                // Get all the things in the players inventory
                PlayerInventory inventory = player.getInventory();
                for (int i = 0; i < inventory.main.size(); i++) {
                    ItemStack stack = inventory.main.get(i);
                    // Check if the stack is a potion
                    Item item = stack.getItem();
                    if (item instanceof PotionItem) {

                        Potion potion = PotionUtil.getPotion(stack);
                        if (potion == null) {
                            return;
                        }
                        // Get potion effects
                        Potion maxPotion = potion;
                        int maxAmplifier = 0;

                        List<StatusEffectInstance> effects = PotionUtil.getPotionEffects(stack);
                        if (effects.size() == 1) {
                            StatusEffectInstance effect = effects.get(0);
                            StatusEffect effectType = effect.getEffectType();

                            for (Potion localPotion : Registries.POTION) {
                                if (localPotion == null) {
                                    continue;
                                }

                                List<StatusEffectInstance> localEffects = localPotion.getEffects();
                                if (localEffects.size() == 1) {
                                    StatusEffectInstance localEffect = localEffects.get(0);
                                    StatusEffect localEffectType = localEffect.getEffectType();

                                    if (localEffectType.equals(effectType)) {
                                        if (localEffect.getAmplifier() > maxAmplifier) {
                                            maxAmplifier = localEffect.getAmplifier();
                                            maxPotion = localPotion;
                                        }
                                    }
                                }
                            }

                            // If we found a better potion, replace the old stack
                            if (maxPotion != potion) {
                                // Create a new potion stack, same count as the old one
                                ItemStack newStack = new ItemStack(Items.POTION, stack.getCount());
                                PotionUtil.setPotion(newStack, maxPotion);

                                // Replace the slot in the inventory
                                inventory.setStack(i, newStack);

                                // Optionally print or log something
                                System.out.println("Upgraded potion at slot " + i
                                        + " from " + potion.getEffects()
                                        + " to " + maxPotion.getEffects());
                            }
                        }
                    }
                }
                ;
                break;
            default:
                break;
        }
    }
}

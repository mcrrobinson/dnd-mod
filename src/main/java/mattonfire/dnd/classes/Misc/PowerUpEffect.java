package mattonfire.dnd.classes.Misc;

import java.util.Iterator;
import java.util.List;

import draylar.identity.impl.PlayerDataProvider;
import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.BloodhunterIdentityData;
import mattonfire.dnd.classes.DnDClasses; // For DnDClasses.WARLOCK_FIREBREATH and FIREBREATH_DURATION_TICKS
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Damages.ModDamageTypes;
import mattonfire.dnd.classes.Goals.FollowSummonerGoal;
import mattonfire.dnd.classes.Goals.TimedDespawnGoal;
import mattonfire.dnd.classes.Registry.ModEffects;
import mattonfire.dnd.classes.mixin.MobEntityAccessor;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.damage.DamageSource;
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
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PotionItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.explosion.Explosion;

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

    public static boolean play(MinecraftServer server, PlayerEntity player, DndCharacter character) {

        System.out.println("Starting powerup on: " + character.toString());
        switch (character) {
            case RANGER:
                player.addStatusEffect(new StatusEffectInstance(ModEffects.ARROW_STORM, 300, 1));
                break;
            case WIZARD:

                // TODO: This code comes from ServerWorld for create explosion
                // thought that extracting this would make it easier to make a
                // custom explosion. But we need to know the ExplosionS2CPacket
                // handler
                ServerWorld world = (ServerWorld) player.getEntityWorld();
                double playerX = player.getX();
                double playerY = player.getY();
                double playerZ = player.getZ();
                float radius = 40.F;

                DamageSource damageSource = world.getDamageSources()
                        .create(ModDamageTypes.WIZARD_EXPLOSION_DAMAGE_SOURCE, player);

                Explosion explosion = new Explosion(world, player, damageSource, null,
                        playerX,
                        playerY, playerZ, radius, false,
                        Explosion.DestructionType.KEEP);

                explosion.collectBlocksAndDamageEntities();
                explosion.affectWorld(true);

                if (!explosion.shouldDestroy()) {
                    explosion.clearAffectedBlocks();
                }

                Iterator<? extends LivingEntity> var14 = world.getPlayers().iterator();
                while (var14.hasNext()) {
                    ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity) var14.next();
                    if (serverPlayerEntity.squaredDistanceTo(playerX, playerY, playerZ) < 4096.0) {
                        serverPlayerEntity.networkHandler
                                .sendPacket(new ExplosionS2CPacket(playerX, playerY, playerZ, radius,
                                        explosion.getAffectedBlocks(),
                                        (Vec3d) explosion.getAffectedPlayers().get(serverPlayerEntity)));

                        ServerPlayNetworking.send(serverPlayerEntity, DnDClasses.S2C_WIZARD_EFFECTS_PACKET_ID,
                                new PacketByteBuf(Unpooled.buffer()));
                    }
                }

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
                // For 20 seconds breathe fire.
                DnDClasses.WARLOCK_FIREBREATH.put(player.getUuid(),
                        player.getWorld().getTime() + DnDClasses.FIREBREATH_DURATION_TICKS);

                break;
            case ARTIFICER:
                // temporary buff to armor
                break;
            case BLOODHUNTER: {
                Vec3d vec3d = player.getCameraPosVec(1.0F);
                Vec3d vec3d3 = vec3d.add(player.getRotationVec(1.0F).multiply(20.0D));
                Box box = player.getBoundingBox()
                        .stretch(player.getRotationVec(1.0F).multiply(20.0D)).expand(1.0D, 1.0D, 1.0D);

                EntityHitResult entityHitResult = ProjectileUtil.raycast(player, vec3d, vec3d3, box, (entityx) -> {
                    return entityx instanceof LivingEntity && entityx != player;
                }, 20.0D);

                if (entityHitResult == null) {
                    return false; // No target found
                }

                // Update the player's identity to the target entity
                LivingEntity livingTarget = (LivingEntity) entityHitResult.getEntity();

                // Duplicate it
                EntityType<?> type = livingTarget.getType();
                LivingEntity duplicateEntity = (LivingEntity) type.create(player.getWorld());
                if (duplicateEntity != null) {
                    // Copy NBT data from the original entity
                    NbtCompound nbt = new NbtCompound();
                    livingTarget.writeNbt(nbt);

                    // Remove UUID to avoid conflicts
                    nbt.remove("UUID");

                    duplicateEntity.readNbt(nbt);
                }

                // Also teleport the player to the target entity's position
                // this doesn't work?
                player.refreshPositionAndAngles(livingTarget.getX(), livingTarget.getY(), livingTarget.getZ(),
                        player.getYaw(), player.getPitch());

                ((PlayerDataProvider) player).setIdentity(duplicateEntity);

                if (player.getWorld() instanceof ServerWorld serverWorld) {
                    DnDClasses.BLOODHUNTER_IDENTITY_EXPIRY.put(player.getUuid(),
                            new BloodhunterIdentityData(
                                    serverWorld.getTime() + 400,
                                    duplicateEntity));
                }

                // It's duplicate gets spawned in later.
                livingTarget.discard();

                break;
            }
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
                            return false;
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
        return true; // Indicate that the power-up was successfully applied

    }
}

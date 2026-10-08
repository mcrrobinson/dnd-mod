package mattonfire.dnd.classes.Misc;

import java.util.Iterator;
import java.util.List;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DnDClasses; // For DnDClasses.WARLOCK_FIREBREATH and FIREBREATH_DURATION_TICKS
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Druid;
import mattonfire.dnd.classes.Damages.ModDamageTypes;
import mattonfire.dnd.classes.Goals.FollowSummonerGoal;
import mattonfire.dnd.classes.Progression.SkillHelpers;
import mattonfire.dnd.classes.Progression.Classes.NecromancerSkills;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.classes.Registry.ModEffects;
import mattonfire.dnd.classes.mixin.MobEntityAccessor;
import mattonfire.dnd.entity.boss.Boss;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import mattonfire.dnd.classes.Progression.Classes.FighterSkills;
import mattonfire.dnd.classes.Progression.Classes.PaladinSkills;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.GoalSelector;
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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.explosion.Explosion;

public class PowerUpEffect {

    /** How long the Wizard special's invulnerability lasts. */
    public static final int WIZARD_INVULNERABLE_TICKS = 5 * 20;

    /** How long the Necromancer special's zombie and skeleton last. */
    public static final int UNDEAD_LIFETIME_TICKS = 10 * 20;

    /**
     * Adds AI goals that make the summon target hostile mobs and follow the player.
     */
    private static void addHostileTargetGoal(MobEntity mob, PlayerEntity player) {
        if (mob instanceof ZombieEntity || mob instanceof SkeletonEntity) {
            MobEntityAccessor mobAccessor = (MobEntityAccessor) mob;
            mobAccessor.getTargetSelector().add(2, new ActiveTargetGoal<LivingEntity>(
                    mob, LivingEntity.class, 10, true, false, entity -> entity instanceof Monster));

            mobAccessor.getGoalSelector().add(3,
                    new FollowSummonerGoal((PathAwareEntity) mob, player, 2.0D, 5.0F, 10.0F));
        }
    }

    public static void spawnUndead(PlayerEntity player) {
        if (!(player.world instanceof ServerWorld world))
            return; // Ensure it's server-side

        BlockPos pos = player.getBlockPos(); // Get necromancer's position
        Team allyTeam = NecromancerSkills.allyTeam(world, player);

        spawnAlly(world, player, EntityType.ZOMBIE.create(world), pos.getX(), pos.getY(), pos.getZ(), allyTeam);
        spawnAlly(world, player, EntityType.SKELETON.create(world), pos.getX() + 1, pos.getY(), pos.getZ(),
                allyTeam);
    }

    /**
     * Summons go through {@link SkillHelpers#spawnSummon}, which removes them when
     * their time is up and when they'd come back from a chunk reload or restart
     * without their AI goals.
     */
    private static void spawnAlly(ServerWorld world, PlayerEntity player, HostileEntity mob, double x, double y,
            double z, Team allyTeam) {
        if (mob == null)
            return;
        mob.refreshPositionAndAngles(x, y, z, world.random.nextFloat() * 360F, 0F);
        addHostileTargetGoal(mob, player); // Add AI to target hostiles
        world.getScoreboard().addPlayerToTeam(mob.getUuidAsString(), allyTeam);
        mob.addCommandTag(NecromancerSkills.SUMMON_TAG);
        mob.setPersistent();
        SkillHelpers.spawnSummon(world, mob, UNDEAD_LIFETIME_TICKS);
    }

    /** The Bard special's "attack hostile mobs" goal; a marker type so it's only added once per mob. */
    private static class BardRallyGoal extends ActiveTargetGoal<HostileEntity> {
        BardRallyGoal(MobEntity mob) {
            super(mob, HostileEntity.class, true);
        }
    }

    public static void bardEffect(PlayerEntity player) {
        List<PassiveEntity> nearbyEntities = player.getEntityWorld().getEntitiesByClass(
                PassiveEntity.class,
                player.getBoundingBox().expand(10), // 10-block radius
                // Bosses (Wyvern, Lightning Chaser) can't be charmed.
                entity -> entity.isAlive() && !(entity instanceof Boss));

        for (PassiveEntity passiveMob : nearbyEntities) {
            // Untamed tameable animals (wolves, cats, parrots...) become the Bard's
            if (passiveMob instanceof TameableEntity tameable && !tameable.isTamed()) {
                tameable.setOwner(player);
            }

            // Turn them on hostile mobs, once: the goal stays until the mob is unloaded.
            GoalSelector targets = ((MobEntityAccessor) passiveMob).getTargetSelector();
            boolean rallied = targets.getGoals().stream().anyMatch(goal -> goal.getGoal() instanceof BardRallyGoal);
            if (!rallied) {
                targets.add(1, new BardRallyGoal(passiveMob));
            }
        }
    }

    public static boolean play(MinecraftServer server, PlayerEntity player, DndCharacter character) {
        if (character == null || character == DndCharacter.NONE)
            return false; // No class picked yet, keep the mana

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

                // A few seconds of invulnerability: Resistance V blocks all normal damage
                // and wears off on its own (unlike setInvulnerable, which is saved).
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, WIZARD_INVULNERABLE_TICKS, 4));

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
            case MONK:
                mattonfire.dnd.classes.Progression.Classes.MonkSkills.kiSurge(player);
                break;
            case FIGHTER:
                // Super regeneration: Regeneration V (~3 hearts/sec), longer with each rank.
                // Not potion-sourced, so the Fighter's potion block doesn't stop it.
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION,
                        FighterSkills.SUPER_REGEN.ticks(player, "Duration"),
                        FighterSkills.SUPER_REGEN.amplifier(player, "Regeneration")));
                break;
            case BARD:
                bardEffect(player);
                break;
            case CLERIC:
                player.addStatusEffect(new StatusEffectInstance(ModEffects.MOB_REPEL, 300));
                // Party members inside the circle share it and get some regeneration.
                if (player instanceof ServerPlayerEntity cleric) {
                    for (ServerPlayerEntity member : PartyManager.nearbyMembers(cleric, ClericHandler.REPEL_RADIUS)) {
                        member.addStatusEffect(new StatusEffectInstance(ModEffects.MOB_REPEL, 300));
                        member.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 0));
                    }
                }
                break;
            case PALADIN:
                // Divine Judgment: a beam of holy light on the mob in sight. Nothing in sight keeps the mana.
                if (!(player instanceof ServerPlayerEntity paladin) || !PaladinSkills.divineJudgment(paladin))
                    return false;
                break;
            case ROGUE:
                // Make the player immune to poison
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 300, 0));
                break;
            case DRUID:
                if (!(player instanceof ServerPlayerEntity serverPlayer) || !Druid.transform(serverPlayer))
                    return false; // Nothing to transform into, keep the mana
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
                // Temporary buff to armor (+8 armor, +4 toughness for 30 seconds)
                player.addStatusEffect(new StatusEffectInstance(ModEffects.ARMOR_BUFF, 600, 0));
                break;
            case BLOODHUNTER:
                // Take control of the mob being looked at (within 30 blocks) for 20 seconds.
                if (!(player instanceof ServerPlayerEntity serverPlayer)
                        || !BloodHunterControl.takeControl(serverPlayer)) {
                    return false;
                }
                break;
            case ALCHEMIST:
                // Upgrades every potion in the inventory to its strongest version.
                // Nothing to upgrade keeps the mana.
                if (!alchemistUpgradePotions(player))
                    return false;
                break;
            default:
                break;

        }
        return true; // Indicate that the power-up was successfully applied

    }

    /**
     * Swaps each single-effect potion in the main inventory for the registered
     * potion with the same effect at the highest amplifier (e.g. Swiftness to
     * Swiftness II). Splash and lingering potions stay splash and lingering.
     *
     * @return whether any potion was upgraded
     */
    private static boolean alchemistUpgradePotions(PlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        boolean upgraded = false;
        for (int i = 0; i < inventory.main.size(); i++) {
            ItemStack stack = inventory.main.get(i);
            if (!(stack.getItem() instanceof PotionItem))
                continue;

            Potion potion = PotionUtil.getPotion(stack);
            List<StatusEffectInstance> effects = potion.getEffects();
            if (effects.size() != 1)
                continue; // Water, awkward, custom and mixed potions
            StatusEffect effectType = effects.get(0).getEffectType();

            Potion best = potion;
            int bestAmplifier = effects.get(0).getAmplifier();
            for (Potion candidate : Registries.POTION) {
                List<StatusEffectInstance> candidateEffects = candidate.getEffects();
                if (candidateEffects.size() == 1 && candidateEffects.get(0).getEffectType() == effectType
                        && candidateEffects.get(0).getAmplifier() > bestAmplifier) {
                    bestAmplifier = candidateEffects.get(0).getAmplifier();
                    best = candidate;
                }
            }

            if (best != potion) {
                // Copy keeps the item (potion, splash, lingering), count and any custom name.
                ItemStack newStack = stack.copy();
                PotionUtil.setPotion(newStack, best);
                inventory.setStack(i, newStack);
                upgraded = true;
            }
        }
        return upgraded;
    }
}

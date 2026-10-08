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
import mattonfire.dnd.entity.BoneWyvernEntity;
import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.AbstractSkeletonEntity;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.TypeFilter;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import mattonfire.dnd.classes.Progression.Classes.FighterSkills;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.explosion.Explosion;

public class PowerUpEffect {

    /** Paladins heal party members within this many blocks (everyone else: 10). */
    public static final double PALADIN_PARTY_HEAL_RADIUS = 24;
    /** How long the Wizard special's invulnerability lasts. */
    public static final int WIZARD_INVULNERABLE_TICKS = 5 * 20;

    /** Tag on Raise Dead's units, for {@link #MAX_RAISED}. */
    public static final String RAISED_TAG = "dndclasses.raised_dead";
    /** Most Raise Dead units (not counting the Bone Wyvern) a Necromancer can have at once. */
    public static final int MAX_RAISED = 10;

    /**
     * Turns a summon on hostile mobs and has it follow the player. Its own target goals go, so
     * spiders and the like don't hunt players and zombies leave villagers alone.
     */
    private static void addHostileTargetGoal(PathAwareEntity mob, PlayerEntity player) {
        MobEntityAccessor mobAccessor = (MobEntityAccessor) mob;
        mobAccessor.getTargetSelector().clear(goal -> true);
        mobAccessor.getTargetSelector().add(2, new ActiveTargetGoal<LivingEntity>(
                mob, LivingEntity.class, 10, true, false, entity -> entity instanceof Monster));
        mobAccessor.getGoalSelector().add(3, new FollowSummonerGoal(mob, player, 2.0D, 5.0F, 10.0F));
    }

    /**
     * Raise Dead: the undead for the special's rank ({@link NecromancerSkills#RAISE_DEAD}) in a ring
     * around the player, and a Bone Wyvern at the top rank if the player hasn't got one.
     */
    public static void spawnUndead(PlayerEntity player) {
        if (!(player.world instanceof ServerWorld world))
            return; // Ensure it's server-side

        int rank = NecromancerSkills.RAISE_DEAD.rank(player);
        int count = NecromancerSkills.RAISE_DEAD.getInt(player, "Undead");
        int lifetime = NecromancerSkills.RAISE_DEAD.ticks(player, "Duration");
        Team allyTeam = NecromancerSkills.allyTeam(world, player);
        List<EntityType<? extends HostileEntity>> roster = NecromancerSkills.raisedUndead(rank);

        // Recasting while the last lot is still up tops it up to the cap rather than stacking forever.
        String team = player.getUuidAsString();
        int alive = world.getEntitiesByType(TypeFilter.instanceOf(MobEntity.class),
                e -> e.isAlive() && e.getCommandTags().contains(RAISED_TAG) && e.getScoreboardTeam() != null
                        && team.equals(e.getScoreboardTeam().getName())).size();
        count = Math.min(count, MAX_RAISED - alive);

        for (int i = 0; i < count; i++) {
            double angle = i * Math.PI * 2 / count;
            HostileEntity mob = roster.get(i % roster.size()).create(world);
            if (mob == null)
                continue;
            mob.refreshPositionAndAngles(player.getX() + Math.cos(angle) * 1.5, player.getY(),
                    player.getZ() + Math.sin(angle) * 1.5, world.random.nextFloat() * 360F, 0F);
            equipRaised(mob, rank);
            addHostileTargetGoal(mob, player);
            mob.addCommandTag(RAISED_TAG);
            spawnAlly(world, mob, allyTeam, lifetime);
        }

        if (rank >= NecromancerSkills.BONE_WYVERN_RANK && !hasBoneWyvern(world, player)) {
            BoneWyvernEntity wyvern = ModEntityTypes.BONE_WYVERN.create(world);
            if (wyvern != null) {
                wyvern.refreshPositionAndAngles(player.getX(), player.getY() + 1.5, player.getZ(), player.getYaw(), 0F);
                wyvern.setOwner(player);
                spawnAlly(world, wyvern, allyTeam, lifetime);
            }
        }
        world.spawnParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 0.5, player.getZ(), 10 + 4 * count,
                1.5, 0.5, 1.5, 0.02);
    }

    /** Weapons and helmets by kind and rank; nothing drops. Fire resistance covers fire and lava. */
    private static void equipRaised(HostileEntity mob, int rank) {
        Item weapon = null;
        if (mob instanceof AbstractSkeletonEntity) {
            // Equipping re-picks a skeleton's bow or melee attack
            weapon = mob.getType() == EntityType.WITHER_SKELETON ? Items.STONE_SWORD : Items.BOW;
        } else if (mob instanceof ZombieEntity) {
            weapon = rank >= 3 ? Items.IRON_SWORD : rank == 2 ? Items.STONE_SWORD : null;
        }
        if (weapon != null) {
            mob.equipStack(EquipmentSlot.MAINHAND, new ItemStack(weapon));
        }
        // A helmet stops zombies and skeletons catching fire in daylight
        if (mob instanceof AbstractSkeletonEntity || mob instanceof ZombieEntity) {
            Item helmet = rank >= 4 ? Items.IRON_HELMET : rank == 3 ? Items.CHAINMAIL_HELMET : Items.LEATHER_HELMET;
            mob.equipStack(EquipmentSlot.HEAD, new ItemStack(helmet));
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setEquipmentDropChance(slot, 0);
        }
        mob.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, StatusEffectInstance.INFINITE, 0,
                false, false));
    }

    private static boolean hasBoneWyvern(ServerWorld world, PlayerEntity player) {
        return !world.getEntitiesByType(ModEntityTypes.BONE_WYVERN, e -> e.isAlive() && e.isOwner(player)).isEmpty();
    }

    /**
     * Summons go through {@link SkillHelpers#spawnSummon}, which removes them when
     * their time is up and when they'd come back from a chunk reload or restart
     * without their AI goals.
     */
    private static void spawnAlly(ServerWorld world, MobEntity mob, Team allyTeam, int lifetime) {
        world.getScoreboard().addPlayerToTeam(mob.getUuidAsString(), allyTeam);
        mob.addCommandTag(NecromancerSkills.SUMMON_TAG);
        mob.setPersistent();
        SkillHelpers.spawnSummon(world, mob, lifetime);
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
                // Heal everyone within a area of the user
                List<PlayerEntity> nearbyEntities = player.getEntityWorld().getEntitiesByClass(
                        PlayerEntity.class,
                        player.getBoundingBox().expand(10), // 10-block radius
                        entity -> true);

                for (PlayerEntity entity : nearbyEntities) {
                    entity.heal(entity.getMaxHealth());
                }

                // Party members get it from further away, plus a few absorption hearts.
                if (player instanceof ServerPlayerEntity paladin) {
                    List<ServerPlayerEntity> party = PartyManager.nearbyMembers(paladin, PALADIN_PARTY_HEAL_RADIUS);
                    party.add(paladin);
                    for (ServerPlayerEntity member : party) {
                        member.heal(member.getMaxHealth());
                        member.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 600, 0));
                    }
                }
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

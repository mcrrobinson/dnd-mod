package mattonfire.dnd.classes.Progression.Classes;

import static mattonfire.dnd.classes.Progression.SkillHelpers.effects;
import static mattonfire.dnd.classes.Progression.SkillHelpers.hostilesNear;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Damages.ModDamageTypes;
import mattonfire.dnd.classes.Items.ExtendedSwordItem;
import mattonfire.dnd.classes.Progression.AttributeBonus;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.SkillNode;
import mattonfire.dnd.classes.Registry.ModEffects;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

/**
 * Wizard skills. The root is the arcane explosion in {@code PowerUpEffect}.
 * "Staff damage" is a melee hit with an elemental staff or a staff blast (an
 * explosion the player set off directly).
 */
public class WizardSkills extends ClassSkills {
    private static final int SPELL_KILL_BONUS_XP = 3;
    private static final float ARCANE_FOCUS_MULTIPLIER = 1.25F;
    private static final float FROZEN_TARGET_MULTIPLIER = 1.5F;
    private static final int FROST_NOVA_RADIUS = 6;
    private static final int FROST_NOVA_TICKS = 3 * 20;
    private static final int ARCANE_SHIELD_TICKS = 15 * 20;

    private static final int METEOR_RANGE = 30;
    private static final int METEOR_SWARM_TICKS = 3 * 20;
    private static final int METEOR_INTERVAL_TICKS = 3;
    private static final double METEOR_SPREAD = 5;
    private static final double METEOR_HEIGHT = 20;
    private static final float METEOR_POWER = 1.5F;

    /** Meteor Swarms still falling. Server thread only. */
    private static final List<Swarm> SWARMS = new ArrayList<>();

    @Override
    public DndCharacter dndClass() {
        return DndCharacter.WIZARD;
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of(
                active("wizard.arcane_explosion", "Arcane Explosion",
                        "A huge arcane blast around you that throws everything back.", "minecraft:end_crystal", 9, 0,
                        1, 3),
                // Evocation
                passive("wizard.arcane_focus", "Arcane Focus", "Staff hits and staff blasts deal 25% more damage.",
                        "minecraft:amethyst_shard", 1, 2, 3, "wizard.arcane_explosion"),
                active("wizard.frost_nova", "Frost Nova", "Freeze mobs within 6 blocks for 3 seconds.",
                        "minecraft:blue_ice", 4, 1, 2, 2, "wizard.arcane_focus"),
                passive("wizard.spell_mastery", "Spell Mastery",
                        "Your own blasts and meteors can't hurt you, and staff damage to frozen mobs is 50% higher.",
                        "minecraft:enchanted_book", 1, 2, 1, "wizard.frost_nova"),
                // Abjuration
                passive("wizard.mage_armor", "Mage Armor", "+4 armor.", "minecraft:iron_chestplate", 1, 0, 3,
                        "wizard.arcane_explosion"),
                active("wizard.arcane_shield", "Arcane Shield", "Absorption II (4 hearts) for 15 seconds.",
                        "minecraft:shield", 4, 1, 0, 2, "wizard.mage_armor"),
                passive("wizard.fortitude", "Fortitude", "+2 hearts of max health.", "minecraft:golden_apple", 1, 0,
                        1, "wizard.arcane_shield"),
                active("wizard.meteor_swarm", "Meteor Swarm",
                        "Fireballs rain down where you're looking (up to 30 blocks) for 3 seconds.",
                        "minecraft:fire_charge", 9, 2, 1, 0, "wizard.spell_mastery", "wizard.fortitude"));
    }

    @Override
    public List<AttributeBonus> attributeBonuses() {
        return List.of(
                new AttributeBonus("wizard.mage_armor", EntityAttributes.GENERIC_ARMOR, 4,
                        EntityAttributeModifier.Operation.ADDITION),
                new AttributeBonus("wizard.fortitude", EntityAttributes.GENERIC_MAX_HEALTH, 4,
                        EntityAttributeModifier.Operation.ADDITION));
    }

    @Override
    public void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tickSwarms());
    }

    @Override
    public boolean activate(ServerPlayerEntity player, SkillNode node) {
        ServerWorld world = (ServerWorld) player.getWorld();
        switch (node.id()) {
            case "wizard.frost_nova" -> {
                for (LivingEntity mob : hostilesNear(player, FROST_NOVA_RADIUS)) {
                    mob.addStatusEffect(new StatusEffectInstance(ModEffects.FREEZE, FROST_NOVA_TICKS), player);
                    world.spawnParticles(ParticleTypes.SNOWFLAKE, mob.getX(), mob.getY() + 1, mob.getZ(), 10, 0.3,
                            0.5, 0.3, 0.02);
                }
                world.spawnParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 0.2, player.getZ(), 80,
                        FROST_NOVA_RADIUS / 2.0, 0.1, FROST_NOVA_RADIUS / 2.0, 0.05);
                effects(player, SoundEvents.BLOCK_GLASS_BREAK, ParticleTypes.ITEM_SNOWBALL, 30);
            }
            case "wizard.arcane_shield" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, ARCANE_SHIELD_TICKS, 1));
                effects(player, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, ParticleTypes.ENCHANT, 40);
            }
            case "wizard.meteor_swarm" -> {
                HitResult hit = player.raycast(METEOR_RANGE, 0, false);
                SWARMS.add(new Swarm(world, player.getUuid(), hit.getPos(), METEOR_SWARM_TICKS));
                effects(player, SoundEvents.ENTITY_BLAZE_SHOOT, ParticleTypes.FLAME, 30);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public int killXp(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
        return killed instanceof Monster && isSpellDamage(player, source) ? SPELL_KILL_BONUS_XP : 0;
    }

    @Override
    public float modifyDealtDamage(PlayerEntity player, ClassProgress progress, LivingEntity target,
            DamageSource source, float amount) {
        if (!isStaffDamage(player, source))
            return amount;
        if (progress.hasPassive("wizard.arcane_focus")) {
            amount *= ARCANE_FOCUS_MULTIPLIER;
        }
        if (progress.hasPassive("wizard.spell_mastery") && target.hasStatusEffect(ModEffects.FREEZE)) {
            amount *= FROZEN_TARGET_MULTIPLIER;
        }
        return amount;
    }

    @Override
    public float modifyTakenDamage(PlayerEntity player, ClassProgress progress, DamageSource source, float amount) {
        if (progress.hasPassive("wizard.spell_mastery") && source.getAttacker() == player
                && (source.isIn(DamageTypeTags.IS_EXPLOSION)
                        || source.isOf(ModDamageTypes.WIZARD_EXPLOSION_DAMAGE_SOURCE))) {
            return 0;
        }
        return amount;
    }

    /** A melee hit with an elemental staff, or a blast the player set off directly (the staffs). */
    private static boolean isStaffDamage(PlayerEntity player, DamageSource source) {
        if (source.getSource() != player)
            return false; // Meteors and other projectiles
        if (source.isIn(DamageTypeTags.IS_EXPLOSION))
            return true;
        return source.isOf(DamageTypes.PLAYER_ATTACK)
                && player.getMainHandStack().getItem() instanceof ExtendedSwordItem;
    }

    /** Staff damage, magic, the arcane explosion or a meteor. */
    private static boolean isSpellDamage(PlayerEntity player, DamageSource source) {
        return isStaffDamage(player, source) || source.isIn(DamageTypeTags.IS_EXPLOSION)
                || source.isOf(ModDamageTypes.WIZARD_EXPLOSION_DAMAGE_SOURCE) || source.isOf(DamageTypes.MAGIC)
                || source.isOf(DamageTypes.INDIRECT_MAGIC);
    }

    private static void tickSwarms() {
        Iterator<Swarm> it = SWARMS.iterator();
        while (it.hasNext()) {
            Swarm swarm = it.next();
            PlayerEntity owner = swarm.world.getPlayerByUuid(swarm.owner);
            if (owner == null || swarm.ticksLeft-- <= 0) {
                it.remove();
                continue;
            }
            if (swarm.ticksLeft % METEOR_INTERVAL_TICKS == 0) {
                dropMeteor(swarm.world, owner, swarm.center);
            }
        }
    }

    /** Drops one meteor somewhere around the target, from just under any ceiling. */
    private static void dropMeteor(ServerWorld world, PlayerEntity owner, Vec3d center) {
        double angle = world.random.nextDouble() * Math.PI * 2;
        double dist = Math.sqrt(world.random.nextDouble()) * METEOR_SPREAD;
        Vec3d ground = center.add(Math.cos(angle) * dist, 0.5, Math.sin(angle) * dist);
        HitResult ceiling = world.raycast(new RaycastContext(ground, ground.add(0, METEOR_HEIGHT, 0),
                RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, owner));
        Vec3d top = ceiling.getType() == HitResult.Type.MISS ? ceiling.getPos() : ceiling.getPos().add(0, -1, 0);

        Meteor meteor = new Meteor(world);
        meteor.setOwner(owner);
        meteor.refreshPositionAndAngles(top.x, top.y, top.z, 0, 90);
        meteor.powerX = 0;
        meteor.powerY = -0.1;
        meteor.powerZ = 0;
        meteor.setVelocity(0, -1, 0);
        world.spawnEntity(meteor);
        world.playSound(null, top.x, top.y, top.z, SoundEvents.ENTITY_GHAST_SHOOT, SoundCategory.PLAYERS, 0.6F,
                0.8F);
    }

    private static class Swarm {
        final ServerWorld world;
        final UUID owner;
        final Vec3d center;
        int ticksLeft;

        Swarm(ServerWorld world, UUID owner, Vec3d center, int ticksLeft) {
            this.world = world;
            this.owner = owner;
            this.center = center;
            this.ticksLeft = ticksLeft;
        }
    }

    /**
     * A ghast fireball that explodes without breaking blocks or lighting fires.
     * Shows as a normal fireball on the client, and isn't saved with the chunk.
     */
    private static class Meteor extends FireballEntity {
        Meteor(World world) {
            super(EntityType.FIREBALL, world);
        }

        @Override
        protected void onCollision(HitResult hitResult) {
            if (!this.world.isClient) {
                this.world.createExplosion(this, getX(), getY(), getZ(), METEOR_POWER, false,
                        World.ExplosionSourceType.NONE);
                discard();
            }
        }

        @Override
        public boolean shouldSave() {
            return false;
        }
    }
}

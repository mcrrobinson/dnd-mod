package mattonfire.dnd.classes.Progression.Classes;

import static mattonfire.dnd.classes.Progression.SkillHelpers.alliesNear;
import static mattonfire.dnd.classes.Progression.SkillHelpers.effects;
import static mattonfire.dnd.classes.Progression.SkillHelpers.enemiesNear;
import static mattonfire.dnd.classes.Progression.SkillHelpers.hostilesNear;

import java.util.List;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.AttributeBonus;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.SkillNode;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public class BardSkills extends ClassSkills {
    private static final int PET_KILL_BONUS_XP = 3;
    private static final int GROUP_KILL_BONUS_XP = 1;
    private static final double GROUP_RADIUS = 16;

    private static final double AURA_RADIUS = 8;
    /** Aura effects outlast the 1-second refresh so they don't flicker. */
    private static final int AURA_TICKS = 60;
    private static final int HERO_TICKS = 300;

    private static final double THUNDERWAVE_RADIUS = 6;
    private static final float THUNDERWAVE_DAMAGE = 4.0F;
    private static final double SONG_RADIUS = 8;
    private static final float SONG_HEAL = 6.0F;
    private static final double CRESCENDO_RADIUS = 12;
    private static final int CRESCENDO_TICKS = 400;
    private static final float JACK_DAMAGE_MULTIPLIER = 1.1F;

    @Override
    public DndCharacter dndClass() {
        return DndCharacter.BARD;
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of(
                active("bard.animal_friends", "Animal Friends",
                        "Animals within 10 blocks become yours and defend you.", "minecraft:lead", 9, 0, 1, 3),
                // Valor
                passive("bard.inspiring_presence", "Inspiring Presence",
                        "Other players within 8 blocks get Speed I.", "minecraft:sugar", 1, 2, 3,
                        "bard.animal_friends"),
                active("bard.thunderwave", "Thunderwave",
                        "Hit mobs within 6 blocks for 4 damage and blast them away.", "minecraft:goat_horn", 4, 1,
                        2, 2, "bard.inspiring_presence"),
                passive("bard.battle_hymn", "Battle Hymn", "Other players within 8 blocks get Strength I.",
                        "minecraft:blaze_powder", 1, 2, 1, "bard.thunderwave"),
                // Lore
                passive("bard.silver_tongue", "Silver Tongue", "Permanent Hero of the Village.",
                        "minecraft:emerald", 1, 0, 3, "bard.animal_friends"),
                active("bard.song_of_rest", "Song of Rest",
                        "Heal you and allies within 8 blocks by 3 hearts and clear bad effects.",
                        "minecraft:note_block", 3, 1, 0, 2, "bard.silver_tongue"),
                passive("bard.jack_of_all_trades", "Jack of All Trades", "+1 heart and 10% more damage.",
                        "minecraft:book", 1, 0, 1, "bard.song_of_rest"),
                active("bard.crescendo", "Crescendo",
                        "Allies within 12 blocks get Strength II, Speed II and Resistance for 20 seconds; "
                                + "mobs glow and are weakened.",
                        "minecraft:jukebox", 9, 2, 1, 0, "bard.battle_hymn", "bard.jack_of_all_trades"));
    }

    @Override
    public List<AttributeBonus> attributeBonuses() {
        return List.of(new AttributeBonus("bard.jack_of_all_trades", EntityAttributes.GENERIC_MAX_HEALTH, 2,
                EntityAttributeModifier.Operation.ADDITION));
    }

    @Override
    public void register() {
        // Kills by a bard's pets aren't the bard's own kills, so their XP is handed out here.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof Monster && source.getAttacker() instanceof TameableEntity pet
                    && pet.getOwner() instanceof ServerPlayerEntity owner
                    && Progression.classOf(owner) == DndCharacter.BARD) {
                Progression.addXp(owner, PET_KILL_BONUS_XP);
            }
        });
    }

    @Override
    public boolean activate(ServerPlayerEntity player, SkillNode node) {
        ServerWorld world = (ServerWorld) player.getWorld();
        switch (node.id()) {
            case "bard.thunderwave" -> {
                for (LivingEntity mob : enemiesNear(player, THUNDERWAVE_RADIUS)) {
                    mob.damage(world.getDamageSources().playerAttack(player), THUNDERWAVE_DAMAGE);
                    Vec3d push = mob.getPos().subtract(player.getPos()).multiply(1, 0, 1).normalize();
                    mob.takeKnockback(2.0, -push.x, -push.z);
                    mob.addVelocity(0, 0.5, 0);
                    mob.velocityModified = true;
                }
                world.spawnParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1, player.getZ(), 1,
                        0, 0, 0, 0);
                effects(player, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, ParticleTypes.NOTE, 30);
            }
            case "bard.song_of_rest" -> {
                for (LivingEntity ally : alliesNear(player, SONG_RADIUS)) {
                    ally.heal(SONG_HEAL);
                    List<StatusEffect> bad = ally.getStatusEffects().stream().map(StatusEffectInstance::getEffectType)
                            .filter(e -> e.getCategory() == StatusEffectCategory.HARMFUL).toList();
                    bad.forEach(ally::removeStatusEffect);
                }
                effects(player, SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), ParticleTypes.HEART, 20);
            }
            case "bard.crescendo" -> {
                for (LivingEntity ally : alliesNear(player, CRESCENDO_RADIUS)) {
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, CRESCENDO_TICKS, 1));
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, CRESCENDO_TICKS, 1));
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, CRESCENDO_TICKS, 0));
                }
                for (LivingEntity mob : hostilesNear(player, CRESCENDO_RADIUS)) {
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, CRESCENDO_TICKS, 0), player);
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, CRESCENDO_TICKS, 0),
                            player);
                }
                effects(player, SoundEvents.ENTITY_PLAYER_LEVELUP, ParticleTypes.NOTE, 40);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean usesBestiary() {
        return true;
    }

    /** Tiers are set by the Animal Friends card. */
    @Override
    public boolean learnsFrom(LivingEntity killed) {
        return killed instanceof AnimalEntity;
    }

    @Override
    public int killXp(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
        if (!(killed instanceof Monster))
            return 0;
        boolean withFriend = !player.getWorld().getEntitiesByClass(PlayerEntity.class,
                player.getBoundingBox().expand(GROUP_RADIUS), p -> p != player && p.isAlive()).isEmpty();
        return withFriend ? GROUP_KILL_BONUS_XP : 0;
    }

    @Override
    public float modifyDealtDamage(PlayerEntity player, ClassProgress progress, LivingEntity target,
            DamageSource source, float amount) {
        return progress.hasPassive("bard.jack_of_all_trades") ? amount * JACK_DAMAGE_MULTIPLIER : amount;
    }

    @Override
    public void secondTick(ServerPlayerEntity player, ClassProgress progress) {
        if (progress.hasPassive("bard.silver_tongue")) {
            player.addStatusEffect(
                    new StatusEffectInstance(StatusEffects.HERO_OF_THE_VILLAGE, HERO_TICKS, 0, true, false, true));
        }
        boolean speed = progress.hasPassive("bard.inspiring_presence");
        boolean strength = progress.hasPassive("bard.battle_hymn");
        if (!speed && !strength)
            return;
        for (PlayerEntity other : player.getWorld().getEntitiesByClass(PlayerEntity.class,
                player.getBoundingBox().expand(AURA_RADIUS), p -> p != player && p.isAlive() && !p.isSpectator())) {
            if (speed) {
                other.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, AURA_TICKS, 0, true, true, true));
            }
            if (strength) {
                other.addStatusEffect(
                        new StatusEffectInstance(StatusEffects.STRENGTH, AURA_TICKS, 0, true, true, true));
            }
        }
    }
}

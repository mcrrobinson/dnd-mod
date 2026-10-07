package mattonfire.dnd.classes.Progression.Classes;

import static mattonfire.dnd.classes.Progression.SkillHelpers.alliesNear;
import static mattonfire.dnd.classes.Progression.SkillHelpers.effects;
import static mattonfire.dnd.classes.Progression.SkillHelpers.hostilesNear;
import static mattonfire.dnd.classes.Progression.SkillHelpers.spawnSummon;

import java.util.List;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Druid;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.SkillNode;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;

/**
 * Druid skills. Beast Bond, Photosynthesis and Tidecaller are checked in
 * {@link Druid} and {@code PlayerEntityMixin}.
 */
public class DruidSkills extends ClassSkills {
    private static final int ANIMAL_KILL_XP = 3;
    private static final int FORM_KILL_BONUS_XP = 4;
    private static final int WOLF_LIFETIME_TICKS = 60 * 20;

    @Override
    public DndCharacter dndClass() {
        return DndCharacter.DRUID;
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of(
                active("druid.wild_shape", "Wild Shape", "Turn into an animal you've killed for 30 seconds.",
                        "minecraft:rabbit_foot", 9, 0, 1, 3),
                // Circle of the Moon
                passive("druid.hardy_form", "Hardy Form", "Resistance while in animal form.",
                        "minecraft:turtle_helmet", 1, 2, 3, "druid.wild_shape"),
                active("druid.thorn_burst", "Thorn Burst",
                        "Thorns hurt mobs within 6 blocks and root them for 4 seconds.", "minecraft:sweet_berries",
                        4, 1, 2, 2, "druid.hardy_form"),
                passive("druid.beast_bond", "Beast Bond", "Tamed animals give up to 10 extra hearts instead of 5.",
                        "minecraft:lead", 1, 2, 1, "druid.thorn_burst"),
                // Circle of the Land
                passive("druid.tidecaller", "Tidecaller", "You can swim again.", "minecraft:heart_of_the_sea", 1, 0,
                        3, "druid.wild_shape"),
                active("druid.regrowth", "Regrowth",
                        "Regeneration II for you, nearby players and your animals for 8 seconds.",
                        "minecraft:glow_berries", 4, 1, 0, 2, "druid.tidecaller"),
                passive("druid.photosynthesis", "Photosynthesis", "Bright light also feeds you.",
                        "minecraft:oak_sapling", 1, 0, 1, "druid.regrowth"),
                active("druid.call_of_the_wild", "Call of the Wild", "Three wolves fight for you for a minute.",
                        "minecraft:bone", 9, 2, 1, 0, "druid.beast_bond", "druid.photosynthesis"));
    }

    @Override
    public boolean activate(ServerPlayerEntity player, SkillNode node) {
        ServerWorld world = (ServerWorld) player.getWorld();
        switch (node.id()) {
            case "druid.thorn_burst" -> {
                for (LivingEntity mob : hostilesNear(player, 6)) {
                    mob.damage(world.getDamageSources().thorns(player), 3.0F);
                    mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 3), player);
                }
                effects(player, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PLACE, ParticleTypes.COMPOSTER, 40);
            }
            case "druid.regrowth" -> {
                for (LivingEntity ally : alliesNear(player, 8)) {
                    ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 160, 1), player);
                    world.spawnParticles(ParticleTypes.HEART, ally.getX(), ally.getY() + 1, ally.getZ(), 3, 0.4,
                            0.4, 0.4, 0);
                }
                effects(player, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, ParticleTypes.HAPPY_VILLAGER, 20);
            }
            case "druid.call_of_the_wild" -> {
                for (int i = 0; i < 3; i++) {
                    WolfEntity wolf = EntityType.WOLF.create(world);
                    if (wolf == null)
                        continue;
                    double angle = i * Math.PI * 2 / 3;
                    wolf.refreshPositionAndAngles(player.getX() + Math.cos(angle) * 1.5, player.getY(),
                            player.getZ() + Math.sin(angle) * 1.5, player.getYaw(), 0);
                    wolf.setOwner(player);
                    spawnSummon(world, wolf, WOLF_LIFETIME_TICKS);
                }
                effects(player, SoundEvents.ENTITY_WOLF_HOWL, ParticleTypes.POOF, 20);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public int killXp(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
        int xp = killed instanceof AnimalEntity ? ANIMAL_KILL_XP : 0;
        if (killed instanceof Monster && Druid.isTransformed(player)) {
            xp += FORM_KILL_BONUS_XP;
        }
        return xp;
    }

    @Override
    public void secondTick(ServerPlayerEntity player, ClassProgress progress) {
        if (progress.hasPassive("druid.hardy_form") && Druid.isTransformed(player)) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 0, true, false, true));
        }
    }
}

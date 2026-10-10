package mattonfire.dnd.classes.Obstacles;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Effects.AntiMagicEffect;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * A violet glyph wall filling a doorway. Dispelled with an Arcana check.
 *
 * <ul>
 * <li>Lesser: Wizards (primary) and Warlocks (secondary). Anyone can mine it instead (hardness 50,
 * like obsidian), but each block lashes out for 6 magic damage and Weakness I for 30 seconds.</li>
 * <li>Greater: gold-rimmed, Wizards only, and can't be broken by hand.</li>
 * </ul>
 * Nobody can dispel either while under the Beholder's Anti-Magic.
 */
public final class ArcaneSealType extends ObstacleType {
    public static final float BACKLASH_DAMAGE = 2.0f;
    public static final float FUMBLE_DAMAGE = 4.0f;
    public static final int FUMBLE_WEAKNESS_TICKS = 200;
    public static final float BREAK_DAMAGE = 6.0f;
    public static final int BREAK_WEAKNESS_TICKS = 600;

    private final boolean greater;

    public ArcaneSealType(Identifier id, boolean greater) {
        super(id);
        this.greater = greater;
    }

    public boolean greater() {
        return greater;
    }

    @Override
    public Skill skill() {
        return Skill.ARCANA;
    }

    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        if (dndClass == DndCharacter.WIZARD) {
            return Eligibility.PRIMARY;
        }
        if (dndClass == DndCharacter.WARLOCK && !greater) {
            return Eligibility.SECONDARY;
        }
        return Eligibility.NONE;
    }

    @Override
    public boolean breakable() {
        return !greater;
    }

    @Override
    public boolean blocked(ServerPlayerEntity player) {
        return AntiMagicEffect.blocks(player);
    }

    @Override
    public void onFailure(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        player.damage(world.getDamageSources().magic(), BACKLASH_DAMAGE);
        world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.BLOCKS, 1.0f, 0.6f);
    }

    @Override
    public void onFumble(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        player.damage(world.getDamageSources().magic(), FUMBLE_DAMAGE);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, FUMBLE_WEAKNESS_TICKS, 0));
        world.playSound(null, pos, SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.BLOCKS, 1.0f, 0.5f);
        world.spawnParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                20, 0.6, 0.6, 0.6, 0.2);
    }

    @Override
    public void onBrokenByHand(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        player.damage(world.getDamageSources().magic(), BREAK_DAMAGE);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, BREAK_WEAKNESS_TICKS, 0));
        world.playSound(null, pos, SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.BLOCKS, 1.0f, 0.7f);
        world.spawnParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                30, 0.5, 0.5, 0.5, 0.3);
    }

    @Override
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
        world.spawnParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                12, 0.35, 0.35, 0.35, 0.05);
        world.spawnParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                10, 0.4, 0.4, 0.4, 0.6);
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.BLOCKS, 1.2f, 0.8f);
            world.playSound(null, pos, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 0.8f, 1.2f);
        }
    }

    @Override
    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
        world.spawnParticles(ParticleTypes.PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                8, 0.3, 0.3, 0.3, 0.2);
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 0.8f, 1.4f);
        }
    }
}

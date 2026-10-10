package mattonfire.dnd.classes.Obstacles.types;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.classes.Obstacles.ObstacleGroups;
import mattonfire.dnd.classes.Obstacles.ObstacleIndex;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.PotionUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * A gold-glyph door in pale stone. Opened with a Religion check by Clerics (primary) or Paladins
 * (secondary). It can't be broken; anyone can douse it with {@value #POTIONS_TO_OPEN} thrown Potions of
 * Healing ("holy water") instead, each landing within {@value #DOUSE_RADIUS} blocks of the ward
 * counting once. The count is kept in the group's {@code Data.Doused} and cleared when it reseals.
 */
public final class HolyWardType extends ObstacleType {
    public static final float STING_DAMAGE = 2.0f;
    public static final float FUMBLE_DAMAGE = 4.0f;
    public static final int FUMBLE_BLINDNESS_TICKS = 60;
    public static final int POTIONS_TO_OPEN = 3;
    /** How close to a ward block a thrown potion must burst. */
    public static final double DOUSE_RADIUS = 2.5;
    public static final String DOUSED_KEY = "Doused";

    public HolyWardType(Identifier id) {
        super(id);
    }

    @Override
    public Skill skill() {
        return Skill.RELIGION;
    }

    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        return switch (dndClass) {
            case CLERIC -> Eligibility.PRIMARY;
            case PALADIN -> Eligibility.SECONDARY;
            default -> Eligibility.NONE;
        };
    }

    @Override
    public boolean breakable() {
        return false;
    }

    @Override
    public void onFailure(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        player.damage(world.getDamageSources().magic(), STING_DAMAGE);
        world.playSound(null, pos, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 0.8f, 1.6f);
        radiance(world, pos, 8);
    }

    @Override
    public void onFumble(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        player.damage(world.getDamageSources().magic(), FUMBLE_DAMAGE);
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, FUMBLE_BLINDNESS_TICKS, 0));
        world.playSound(null, pos, SoundEvents.BLOCK_BELL_USE, SoundCategory.BLOCKS, 1.5f, 0.5f);
        radiance(world, pos, 30);
    }

    private static void radiance(ServerWorld world, BlockPos pos, int count) {
        world.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                count, 0.4, 0.4, 0.4, 0.08);
    }

    /**
     * A thrown potion burst at {@code at}. If it's a Potion of Healing landing by a sealed ward, it
     * counts towards dousing the nearest one. Returns true if it counted.
     */
    public boolean onPotionBurst(ServerWorld world, Vec3d at, ItemStack potion, @Nullable Entity thrower) {
        boolean healing = PotionUtil.getPotionEffects(potion).stream()
                .anyMatch(effect -> effect.getEffectType() == StatusEffects.INSTANT_HEALTH);
        if (!healing) {
            return false;
        }
        BlockPos ward = nearestWard(world, at);
        if (ward == null) {
            return false;
        }
        int doused = ObstacleGroups.dataInt(world, ward, DOUSED_KEY) + 1;
        world.spawnParticles(ParticleTypes.END_ROD, ward.getX() + 0.5, ward.getY() + 0.5, ward.getZ() + 0.5,
                20, 0.5, 0.5, 0.5, 0.05);
        world.playSound(null, ward, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0f, 1.4f);
        DnDClasses.LOGGER.info("[Obstacle] holy ward at {} doused {}/{}", ward.toShortString(), doused,
                POTIONS_TO_OPEN);
        if (doused >= POTIONS_TO_OPEN) {
            // The fallback: no class XP, no SOLVED event
            ObstacleGroups.open(world, ward);
            tell(thrower, Text.translatable(translationKey() + ".doused_open"));
            return true;
        }
        ObstacleGroups.setDataInt(world, ward, DOUSED_KEY, doused);
        tell(thrower, Text.translatable(translationKey() + ".doused", doused, POTIONS_TO_OPEN));
        return true;
    }

    private static void tell(@Nullable Entity thrower, Text message) {
        if (thrower instanceof PlayerEntity player) {
            player.sendMessage(message, true);
        }
    }

    @Nullable
    private BlockPos nearestWard(ServerWorld world, Vec3d at) {
        BlockPos best = null;
        double bestDistance = DOUSE_RADIUS * DOUSE_RADIUS;
        for (BlockPos pos : ObstacleIndex.get(world).within(BlockPos.ofFloored(at), (int) Math.ceil(DOUSE_RADIUS) + 1)) {
            BlockState state = world.getBlockState(pos);
            if (!ObstacleBlock.isSealed(state) || ((ObstacleBlock) state.getBlock()).type() != this) {
                continue;
            }
            double distance = Vec3d.ofCenter(pos).squaredDistanceTo(at);
            if (distance <= bestDistance) {
                best = pos;
                bestDistance = distance;
            }
        }
        return best;
    }

    @Override
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
        radiance(world, pos, 10);
        world.spawnParticles(ParticleTypes.GLOW, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                6, 0.4, 0.4, 0.4, 0.02);
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 1.2f, 1.2f);
            world.playSound(null, pos, SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }
    }

    @Override
    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
        clearDoused(world, pos);
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 0.8f, 1.6f);
        }
    }

    private static void clearDoused(ServerWorld world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof mattonfire.dnd.classes.Obstacles.ObstacleBlockEntity be
                && be.data().contains(DOUSED_KEY)) {
            be.data().remove(DOUSED_KEY);
            be.markDirty();
        }
    }
}

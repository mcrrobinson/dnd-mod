package mattonfire.dnd.classes.Obstacles.types;

import java.util.Comparator;
import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.ObstacleBlockEntity;
import mattonfire.dnd.classes.Obstacles.ObstacleGroups;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.minecraft.block.Blocks;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * A dripping red sigil door that opens for a price in blood, not a roll. Right-click it with an empty hand:
 * <ul>
 * <li>A Blood Hunter pays {@value #HUNTER_PRICE} health (2 hearts) and it opens, for class XP.</li>
 * <li>Anyone else pays toward {@value #PRICE} health (8 hearts). Up to {@value #MAX_SHARERS} players within
 * {@value #SHARE_RADIUS} blocks of the seal split it: each right-click pays one share (the price over the
 * number of players there, rounded up, never more than what's left), and it opens once the whole price is
 * paid. No XP for paying it. Payments so far are kept in the group's {@code Data.Paid} until it reseals.</li>
 * <li>Never lethal: a payment that would leave you at 0 health is refused. The price is taken straight
 * from health, so armour, absorption and resistance don't change it.</li>
 * </ul>
 */
public final class BloodSealType extends ObstacleType {
    public static final float HUNTER_PRICE = 4.0f;
    public static final int PRICE = 16;
    public static final int MAX_SHARERS = 3;
    public static final double SHARE_RADIUS = 4.0;
    public static final String PAID_KEY = "Paid";

    public BloodSealType(Identifier id) {
        super(id);
    }

    @Override
    public Skill skill() {
        return null;
    }

    /** The Blood Hunter's price is the class route; everyone may pay the full one. */
    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        return dndClass == DndCharacter.BLOODHUNTER ? Eligibility.PRIMARY : Eligibility.UNTRAINED;
    }

    @Override
    public boolean breakable() {
        return false;
    }

    @Override
    public boolean ownWhoText() {
        return true;
    }

    @Override
    public boolean alarmsOnFumble() {
        return false;
    }

    @Override
    public boolean solve(ServerWorld world, BlockPos pos, ServerPlayerEntity solver) {
        if (solver.isCreative()) {
            return ObstacleGroups.open(world, pos) > 0;
        }
        if (Progression.classOf(solver) == DndCharacter.BLOODHUNTER) {
            if (!pay(world, pos, solver, HUNTER_PRICE)) {
                return false;
            }
            DnDClasses.LOGGER.info("[Obstacle] {} paid the blood seal at {} as a Blood Hunter",
                    solver.getEntityName(), pos.toShortString());
            return ObstacleGroups.open(world, pos) > 0;
        }
        int paid = ObstacleGroups.dataInt(world, pos, PAID_KEY);
        int sharers = sharers(world, pos);
        int share = Math.min(PRICE - paid, MathHelper.ceil((float) PRICE / sharers));
        if (!pay(world, pos, solver, share)) {
            return false;
        }
        paid += share;
        DnDClasses.LOGGER.info("[Obstacle] {} paid {} toward the blood seal at {}: {}/{} ({} sharing)",
                solver.getEntityName(), share, pos.toShortString(), paid, PRICE, sharers);
        if (paid >= PRICE) {
            ObstacleGroups.open(world, pos);
            solver.sendMessage(Text.translatable(translationKey() + ".paid_open"), true);
        } else {
            ObstacleGroups.setDataInt(world, pos, PAID_KEY, paid);
            solver.sendMessage(Text.translatable(translationKey() + ".paid", hearts(share), hearts(PRICE - paid)),
                    true);
        }
        // Paying the full price is the fallback: it earns nothing
        return false;
    }

    /** Players (not spectators or creative) within reach who could share the price, at most {@value #MAX_SHARERS}. */
    public static int sharers(ServerWorld world, BlockPos pos) {
        Vec3d centre = Vec3d.ofCenter(pos);
        List<ServerPlayerEntity> near = world.getPlayers(p -> p.isAlive() && !p.isSpectator() && !p.isCreative()
                && p.squaredDistanceTo(centre) <= SHARE_RADIUS * SHARE_RADIUS);
        near.sort(Comparator.comparingDouble(p -> p.squaredDistanceTo(centre)));
        return MathHelper.clamp(near.size(), 1, MAX_SHARERS);
    }

    /** Takes {@code amount} health, unless it would kill. Returns whether it was paid. */
    private boolean pay(ServerWorld world, BlockPos pos, ServerPlayerEntity player, float amount) {
        if (player.getHealth() <= amount) {
            player.sendMessage(Text.translatable(translationKey() + ".too_weak", hearts(amount)), true);
            world.playSound(null, pos, SoundEvents.BLOCK_SCULK_SENSOR_CLICKING_STOP, SoundCategory.BLOCKS, 1.0f, 0.6f);
            return false;
        }
        player.setHealth(player.getHealth() - amount);
        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0f, 0.8f);
        world.playSound(null, pos, SoundEvents.BLOCK_HONEY_BLOCK_SLIDE, SoundCategory.BLOCKS, 1.2f, 0.5f);
        blood(world, player.getX(), player.getBodyY(0.6), player.getZ(), 12);
        blood(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12);
        return true;
    }

    /** "4" or "2.5" hearts for {@code health} points. */
    public static String hearts(float health) {
        float hearts = health / 2.0f;
        return hearts == Math.floor(hearts) ? String.valueOf((int) hearts) : String.valueOf(hearts);
    }

    private static void blood(ServerWorld world, double x, double y, double z, int count) {
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.getDefaultState()),
                x, y, z, count, 0.3, 0.3, 0.3, 0.05);
    }

    @Override
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
        blood(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16);
        world.spawnParticles(ParticleTypes.DRIPPING_LAVA, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5,
                4, 0.4, 0.2, 0.4, 0.0);
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE.value(), SoundCategory.BLOCKS, 1.2f,
                    0.7f);
        }
    }

    @Override
    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
        if (world.getBlockEntity(pos) instanceof ObstacleBlockEntity be && be.data().contains(PAID_KEY)) {
            be.data().remove(PAID_KEY);
            be.markDirty();
        }
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 1.0f, 0.6f);
        }
    }

    @Override
    public List<String> progressKeys() {
        return List.of(PAID_KEY);
    }
}

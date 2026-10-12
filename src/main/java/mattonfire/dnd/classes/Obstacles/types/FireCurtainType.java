package mattonfire.dnd.classes.Obstacles.types;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.classes.Obstacles.ObstacleIndex;
import mattonfire.dnd.classes.Obstacles.ObstacleState;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

/**
 * A doorway of eternal soul fire, with a {@link FireBrazierType brazier} on the far side. There's no roll:
 * <ul>
 * <li>A Warlock (immune to fire) walks straight through and douses the brazier, which puts the curtain out
 * for everyone.</li>
 * <li>So does anyone with Fire Resistance, the fallback. A Fighter, Paladin or Artificer can't drink the
 * potion (their potion immunity), but an Alchemist's Transmute cloud of it still covers them, as that's an
 * ability. Creative and spectator players and fire-immune mobs pass too.</li>
 * <li>Anyone else finds it solid: it drives them back, and touching it burns ({@value #STING_DAMAGE} fire
 * damage and {@value #BURN_SECONDS} s alight, every {@value #STING_INTERVAL} ticks).</li>
 * </ul>
 * Water doesn't douse it, and it never burns out. It can't be broken; {@code /dndobstacle open} puts it out.
 * Who it lets through is decided from state both sides know (class, Fire Resistance, game mode), so the
 * client predicts the same collision the server checks.
 */
public final class FireCurtainType extends ObstacleType {
    public static final float STING_DAMAGE = 2.0f;
    public static final int BURN_SECONDS = 3;
    public static final int STING_INTERVAL = 10;
    /** How close (blocks) to a sealed curtain block counts as touching it. */
    private static final double TOUCH_MARGIN = 0.2;

    public FireCurtainType(Identifier id) {
        super(id);
    }

    @Override
    public Skill skill() {
        return null;
    }

    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        return dndClass == DndCharacter.WARLOCK ? Eligibility.PRIMARY : Eligibility.NONE;
    }

    @Override
    public boolean breakable() {
        return false;
    }

    /** Nothing to click: you walk through it, and douse its brazier. */
    @Override
    public boolean interactive() {
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

    /** Solid only to those the fire would stop. */
    @Override
    public boolean collides(ObstacleState state, ShapeContext context) {
        if (state != ObstacleState.SEALED) {
            return false;
        }
        return !(context instanceof EntityShapeContext entityContext && entityContext.getEntity() != null
                && canPass(entityContext.getEntity()));
    }

    /** Whether the fire lets this entity through: Warlocks, Fire Resistance, creative, spectators, fire-immune mobs. */
    public static boolean canPass(Entity entity) {
        if (entity instanceof PlayerEntity player && (player.isCreative() || player.isSpectator()
                || Progression.classOf(player) == DndCharacter.WARLOCK)) {
            return true;
        }
        return entity.isFireImmune()
                || entity instanceof LivingEntity living && living.hasStatusEffect(StatusEffects.FIRE_RESISTANCE);
    }

    @Override
    public void registerEvents() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % STING_INTERVAL != 0) {
                return;
            }
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (player.isAlive() && !canPass(player) && touching(world, player)) {
                    scorch(world, player);
                }
            }
        });
    }

    /** Whether the player is up against (or inside) a sealed curtain block. */
    private boolean touching(ServerWorld world, PlayerEntity player) {
        Box body = player.getBoundingBox().expand(TOUCH_MARGIN);
        for (BlockPos pos : ObstacleIndex.get(world).within(player.getBlockPos(), 3)) {
            if (!new Box(pos).intersects(body)) {
                continue;
            }
            BlockState state = world.getBlockState(pos);
            if (ObstacleBlock.isSealed(state) && ((ObstacleBlock) state.getBlock()).type() == this) {
                return true;
            }
        }
        return false;
    }

    private void scorch(ServerWorld world, ServerPlayerEntity player) {
        player.setOnFireFor(BURN_SECONDS);
        player.damage(world.getDamageSources().inFire(), STING_DAMAGE);
        player.sendMessage(Text.translatable(translationKey() + ".scorched"), true);
    }

    @Override
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
        world.spawnParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                6, 0.3, 0.4, 0.3, 0.02);
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.2f, 0.7f);
        }
    }

    @Override
    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                6, 0.3, 0.3, 0.3, 0.02);
        if (first) {
            world.playSound(null, pos, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.BLOCKS, 1.0f, 0.7f);
        }
    }
}

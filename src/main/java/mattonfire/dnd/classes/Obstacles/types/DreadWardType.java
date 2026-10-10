package mattonfire.dnd.classes.Obstacles.types;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.classes.Obstacles.ObstacleIndex;
import mattonfire.dnd.classes.Obstacles.ObstacleState;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Registry.ModEffects;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.effect.StatusEffectInstance;
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
 * Black-bannered stone that fills the passage round it with dread: an aura, no roll. Its zone is
 * every block within {@value #ZONE_RADIUS} of a sealed Dread Ward block (5x5x5 round a single block),
 * so a template paints the wards into an archway's frame and the passage through it is the zone.
 * <ul>
 * <li>Once a second, a player in the zone is Frightened (half melee damage) with Slowness IV and
 * Weakness II, each for {@value #EFFECT_TICKS} ticks, so it wears off soon after leaving.</li>
 * <li>A Paladin is immune, and so is every player within {@value #SHIELD_RADIUS} blocks of a Paladin.</li>
 * <li>Anyone else can still push through, slowly. It can't be broken; {@code /dndobstacle open} turns it off.</li>
 * </ul>
 */
public final class DreadWardType extends ObstacleType {
    public static final int ZONE_RADIUS = 2;
    public static final int SHIELD_RADIUS = 6;
    public static final int EFFECT_TICKS = 30;
    public static final int SLOWNESS_AMPLIFIER = 3;
    public static final int WEAKNESS_AMPLIFIER = 1;
    private static final int CHECK_INTERVAL = 20;

    public DreadWardType(Identifier id) {
        super(id);
    }

    @Override
    public Skill skill() {
        return null;
    }

    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        return dndClass == DndCharacter.PALADIN ? Eligibility.PRIMARY : Eligibility.NONE;
    }

    @Override
    public boolean breakable() {
        return false;
    }

    /** Nothing to click: you walk through it. */
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

    /** The stone frame stays solid whether or not the dread is on. */
    @Override
    public boolean solid(ObstacleState state) {
        return true;
    }

    @Override
    public void registerEvents() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % CHECK_INTERVAL != 0) {
                return;
            }
            for (ServerPlayerEntity player : world.getPlayers()) {
                if (!player.isCreative() && !player.isSpectator() && player.isAlive() && inZone(world, player)) {
                    frighten(world, player);
                }
            }
        });
    }

    /** Whether the player stands within the zone of a sealed Dread Ward. */
    public boolean inZone(ServerWorld world, PlayerEntity player) {
        Box body = player.getBoundingBox();
        for (BlockPos pos : ObstacleIndex.get(world).within(player.getBlockPos(), ZONE_RADIUS + 3)) {
            if (!new Box(pos).expand(ZONE_RADIUS).intersects(body)) {
                continue;
            }
            BlockState state = world.getBlockState(pos);
            if (ObstacleBlock.isSealed(state) && ((ObstacleBlock) state.getBlock()).type() == this) {
                return true;
            }
        }
        return false;
    }

    /** A Paladin, or anyone within {@value #SHIELD_RADIUS} blocks of one, ignores the dread. */
    public static boolean shielded(ServerWorld world, PlayerEntity player) {
        if (Progression.classOf(player) == DndCharacter.PALADIN) {
            return true;
        }
        double max = (double) SHIELD_RADIUS * SHIELD_RADIUS;
        for (ServerPlayerEntity other : world.getPlayers()) {
            if (other != player && other.isAlive() && !other.isSpectator()
                    && Progression.classOf(other) == DndCharacter.PALADIN && other.squaredDistanceTo(player) <= max) {
                return true;
            }
        }
        return false;
    }

    private void frighten(ServerWorld world, ServerPlayerEntity player) {
        if (shielded(world, player)) {
            if (Progression.classOf(player) != DndCharacter.PALADIN) {
                player.sendMessage(Text.translatable(translationKey() + ".shielded"), true);
            }
            world.spawnParticles(ParticleTypes.WAX_OFF, player.getX(), player.getBodyY(0.6), player.getZ(),
                    3, 0.3, 0.4, 0.3, 0.0);
            return;
        }
        boolean fresh = !player.hasStatusEffect(ModEffects.FRIGHTENED);
        player.addStatusEffect(new StatusEffectInstance(ModEffects.FRIGHTENED, EFFECT_TICKS, 0, true, true));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, EFFECT_TICKS, SLOWNESS_AMPLIFIER,
                true, false));
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, EFFECT_TICKS, WEAKNESS_AMPLIFIER,
                true, false));
        player.sendMessage(Text.translatable(translationKey() + ".afraid"), true);
        world.spawnParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getBodyY(0.8), player.getZ(),
                2, 0.3, 0.3, 0.3, 0.01);
        if (fresh) {
            world.playSound(null, player.getBlockPos(), SoundEvents.AMBIENT_CAVE.value(), SoundCategory.AMBIENT,
                    1.0f, 0.6f);
        }
    }

    @Override
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.BLOCKS, 1.0f, 0.6f);
        }
    }

    @Override
    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
        world.spawnParticles(ParticleTypes.SCULK_SOUL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                4, 0.4, 0.4, 0.4, 0.02);
    }
}

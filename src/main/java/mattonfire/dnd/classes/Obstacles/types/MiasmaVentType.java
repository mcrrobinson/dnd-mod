package mattonfire.dnd.classes.Obstacles.types;

import java.util.HashSet;
import java.util.Set;

import org.joml.Vector3f;

import mattonfire.dnd.classes.DnDClasses;
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
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * A grated vent that fills the air within {@value #ZONE_RADIUS} blocks with poison gas. Only an Alchemist can
 * neutralise it for good (Alchemist's Supplies); every attempt uses up a potion.
 * <ul>
 * <li>Once a second, a player in the gas gets Poison II for {@value #POISON_TICKS} ticks and Nausea for
 * {@value #NAUSEA_TICKS} ticks. Rogues are immune to poison and breathe it without harm (no nausea either).
 * Poison can't kill, so anyone can run it.</li>
 * <li>Failure: the vent belches gas at the roller (Poison II and Nausea for {@value #BURST_TICKS} ticks).
 * Natural 1: it bursts over everyone in its gas, and an alarm.</li>
 * <li>No fallback by hand: it can't be broken.</li>
 * </ul>
 */
public final class MiasmaVentType extends ObstacleType {
    public static final int ZONE_RADIUS = 5;
    public static final int POISON_AMPLIFIER = 1;
    /** Refreshed every second, so it wears off soon after leaving the gas. */
    public static final int POISON_TICKS = 60;
    /** Nausea needs a few seconds to warp the screen, so it lasts longer. */
    public static final int NAUSEA_TICKS = 140;
    public static final int BURST_TICKS = 200;
    private static final int CHECK_INTERVAL = 20;
    /** Immune players hear about it this often (ticks), not every second. */
    private static final int IMMUNE_MESSAGE_INTERVAL = 100;
    /** Gas is drawn at vents this close to a player. */
    private static final int PARTICLE_RANGE = 32;
    private static final DustParticleEffect GAS = new DustParticleEffect(new Vector3f(0.45f, 0.72f, 0.18f), 1.6f);

    public MiasmaVentType(Identifier id) {
        super(id);
    }

    @Override
    public Skill skill() {
        return Skill.ALCHEMISTS_SUPPLIES;
    }

    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        return dndClass == DndCharacter.ALCHEMIST ? Eligibility.PRIMARY : Eligibility.NONE;
    }

    @Override
    public boolean breakable() {
        return false;
    }

    /** The grate stays in the floor once the gas stops. */
    @Override
    public boolean solid(ObstacleState state) {
        return true;
    }

    @Override
    public boolean accepts(PlayerEntity player, ItemStack held) {
        return held.isEmpty() || Reagents.isPotion(held);
    }

    @Override
    public boolean payForAttempt(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        if (!Reagents.takePotion(player)) {
            player.sendMessage(Text.translatable("obstacle.dndclasses.needs_potion"), true);
            return false;
        }
        DnDClasses.LOGGER.info("[Obstacle] {} poured a potion into the {} at {}", player.getEntityName(), id(),
                pos.toShortString());
        return true;
    }

    @Override
    public void onFailure(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        burst(world, pos);
        gas(player, BURST_TICKS, BURST_TICKS);
    }

    @Override
    public void onFumble(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        burst(world, pos);
        Vec3d center = Vec3d.ofCenter(pos);
        for (ServerPlayerEntity other : world.getPlayers()) {
            if (!other.isCreative() && !other.isSpectator() && other.isAlive()
                    && other.getPos().squaredDistanceTo(center) <= ZONE_RADIUS * ZONE_RADIUS) {
                gas(other, BURST_TICKS, BURST_TICKS);
            }
        }
    }

    private static void burst(ServerWorld world, BlockPos pos) {
        world.spawnParticles(GAS, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 60, 1.5, 1.0, 1.5, 0.0);
        world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0f, 0.5f);
    }

    /** One dose of gas. Returns whether any of it took (a Rogue's poison immunity turns the whole dose away). */
    private static boolean gas(PlayerEntity player, int poisonTicks, int nauseaTicks) {
        if (immune(player)) {
            return false;
        }
        boolean poisoned = player.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, poisonTicks,
                POISON_AMPLIFIER, false, true, true));
        boolean nauseous = player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, nauseaTicks, 0,
                false, true, true));
        return poisoned || nauseous;
    }

    /** Rogues (immune to poison) breathe the miasma without harm. */
    public static boolean immune(PlayerEntity player) {
        return Progression.classOf(player) == DndCharacter.ROGUE;
    }

    @Override
    public void registerEvents() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % CHECK_INTERVAL != 0 || world.getPlayers().isEmpty()) {
                return;
            }
            Set<BlockPos> vents = new HashSet<>();
            ObstacleIndex index = ObstacleIndex.get(world);
            for (ServerPlayerEntity player : world.getPlayers()) {
                for (BlockPos pos : index.within(player.getBlockPos(), PARTICLE_RANGE)) {
                    if (isSealedVent(world, pos)) {
                        vents.add(pos);
                    }
                }
                if (player.isCreative() || player.isSpectator() || !player.isAlive() || !inGas(world, player)) {
                    continue;
                }
                if (gas(player, POISON_TICKS, NAUSEA_TICKS)) {
                    player.sendMessage(Text.translatable(translationKey() + ".choking"), true);
                } else if (world.getTime() % IMMUNE_MESSAGE_INTERVAL == 0) {
                    player.sendMessage(Text.translatable(translationKey() + ".immune"), true);
                }
            }
            for (BlockPos pos : vents) {
                world.spawnParticles(GAS, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 10, 1.6, 0.8, 1.6,
                        0.0);
                world.spawnParticles(ParticleTypes.SNEEZE, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 3,
                        0.2, 0.2, 0.2, 0.02);
            }
        });
    }

    private boolean isSealedVent(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return ObstacleBlock.isSealed(state) && ((ObstacleBlock) state.getBlock()).type() == this;
    }

    /** Whether the player's body is within {@value #ZONE_RADIUS} blocks of a sealed vent. */
    public boolean inGas(ServerWorld world, PlayerEntity player) {
        Vec3d body = player.getPos().add(0.0, player.getHeight() / 2.0, 0.0);
        for (BlockPos pos : ObstacleIndex.get(world).within(player.getBlockPos(), ZONE_RADIUS + 2)) {
            if (Vec3d.ofCenter(pos).squaredDistanceTo(body) <= ZONE_RADIUS * ZONE_RADIUS && isSealedVent(world, pos)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
        world.spawnParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                8, 0.3, 0.2, 0.3, 0.02);
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCKS, 1.0f, 1.2f);
            world.playSound(null, pos, SoundEvents.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 0.8f, 0.8f);
        }
    }
}

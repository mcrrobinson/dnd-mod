package mattonfire.dnd.classes.SkillChecks;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Blocks.TrapBlocks;
import mattonfire.dnd.classes.Blocks.TrapKind;
import mattonfire.dnd.classes.Blocks.TrapTriggerBlockEntity;
import mattonfire.dnd.classes.Damages.ModDamageTypes;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Disarming dungeon traps, the way {@link Lockpicking} picks locks: a Rogue sneak-uses a trap's tile (the
 * trigger, a flame vent or a crumbling floor tile) with an empty hand and rolls Thieves' Tools against the
 * trap's DC ({@link TrapKind#dc}: 12/13/15/17 by tier), shown on the HUD as "Disarm".
 *
 * <ul>
 * <li>Success: the trap is disarmed (safe until the dungeon resets).</li>
 * <li>Failure: it goes off on the Rogue.</li>
 * <li>Natural 1: it goes off, and the nearest spent or disarmed trap down the corridor arms itself again.</li>
 * </ul>
 * Anyone else who has spotted the trap ({@link TrapSense}) is told only a Rogue could disarm it; they can still
 * set it off on purpose by throwing an item onto it or shooting it with an arrow.
 *
 * Also the poison needles of trapped chests: a failed lockpick or a smashed lock sets the needle off.
 */
public final class TrapDisarm {
    private TrapDisarm() {
    }

    static void register() {
        // [Trap] damage lines for tests and balancing
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            Entity direct = source.getSource();
            String what = null;
            if (direct != null && direct.getCommandTags().contains(TrapBlocks.DART_TAG)) {
                what = "dart";
            } else if (source.isOf(ModDamageTypes.TRAP)) {
                what = "trap";
            } else if (source.isOf(DamageTypes.STALAGMITE)) {
                what = "stalagmite";
            }
            if (what != null) {
                DnDClasses.LOGGER.info("[Trap] {} takes {} ({}), health {}", entity.getName().getString(), amount, what,
                        entity.getHealth());
            }
            return true;
        });
    }

    /** Sneak-use with an empty hand on any of a trap's floor tiles. */
    public static ActionResult onUse(World world, BlockPos pos, PlayerEntity player, Hand hand) {
        if (world.isClient || hand != Hand.MAIN_HAND || player.isSpectator() || !player.isSneaking()
                || !player.getMainHandStack().isEmpty()) {
            return ActionResult.PASS;
        }
        TrapTriggerBlockEntity trap = TrapTriggerBlockEntity.ownerOf(world, pos);
        if (trap == null || trap.kind() == TrapKind.NEEDLE || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }
        boolean rogue = D20.classOf(player) == DndCharacter.ROGUE;
        if (!rogue) {
            if (trap.isArmed() && TrapSense.get().spots(serverPlayer, trap, trap.dc())) {
                player.sendMessage(Text.translatable("trap.dndclasses.rogue_only"), true);
                return ActionResult.CONSUME;
            }
            return ActionResult.PASS;
        }
        if (!trap.isArmed()) {
            player.sendMessage(Text.translatable(trap.isDisarmed() ? "trap.dndclasses.already_disarmed"
                    : "trap.dndclasses.already_spent"), true);
            return ActionResult.CONSUME;
        }
        attempt(serverPlayer, trap);
        return ActionResult.SUCCESS;
    }

    /** The Rogue's Thieves' Tools roll against the trap. */
    public static D20.Roll attempt(ServerPlayerEntity player, TrapTriggerBlockEntity trap) {
        ServerWorld world = (ServerWorld) player.getWorld();
        D20.Roll roll = SkillCheck.builder(player, Skill.THIEVES_TOOLS, trap.dc(), "trap").label(D20.DISARM).roll();
        Text trapName = Text.translatable(trap.kind().translationKey());
        if (roll.outcome().succeeded()) {
            D20.show(player, roll, Text.translatable(roll.outcome() == D20.Outcome.CRITICAL
                    ? "skill.dndclasses.disarm.critical" : "skill.dndclasses.disarm.success", trapName));
            trap.disarm();
            world.playSound(null, trap.getPos(), SoundEvents.BLOCK_TRIPWIRE_DETACH, SoundCategory.BLOCKS, 1.0F, 1.2F);
            return roll;
        }
        boolean fumble = roll.outcome() == D20.Outcome.FUMBLE;
        TrapTriggerBlockEntity next = null;
        if (fumble) {
            for (TrapTriggerBlockEntity other : trap.neighbours(16)) {
                if (!other.isArmed()) {
                    next = other;
                    break;
                }
            }
        }
        D20.show(player, roll, Text.translatable(fumble && next != null ? "skill.dndclasses.disarm.fumble"
                : "skill.dndclasses.disarm.failure", trapName));
        trap.trigger(world, player);
        if (next != null) {
            next.rearm("fumbled disarm at " + trap.getPos().toShortString());
            world.playSound(null, next.getPos(), SoundEvents.BLOCK_STONE_PRESSURE_PLATE_CLICK_OFF, SoundCategory.BLOCKS,
                    1.0F, 0.5F);
        }
        return roll;
    }

    /** An arrow, trident or snowball hitting a trap tile sets the trap off. */
    public static void onProjectileHit(World world, BlockPos pos, ProjectileEntity projectile) {
        if (world instanceof ServerWorld server && !projectile.getCommandTags().contains(TrapBlocks.DART_TAG)) {
            TrapTriggerBlockEntity trap = TrapTriggerBlockEntity.ownerOf(world, pos);
            if (trap != null && trap.isArmed() && trap.kind() != TrapKind.NEEDLE) {
                trap.trigger(server, projectile);
            }
        }
    }

    /** The armed poison needle under a chest half, or null. */
    @Nullable
    private static TrapTriggerBlockEntity needleUnder(World world, @Nullable BlockPos chest) {
        if (chest != null && world.getBlockEntity(chest.down()) instanceof TrapTriggerBlockEntity trap
                && trap.kind() == TrapKind.NEEDLE && trap.isArmed()) {
            return trap;
        }
        return null;
    }

    /** {@link Lockpicking}: the pick slipped. Sets off a needle under either half of the chest. */
    static void lockFailed(World world, BlockPos chest, @Nullable BlockPos otherHalf, PlayerEntity player) {
        spring(world, chest, otherHalf, player);
    }

    /** {@link Lockpicking}: the chest was broken open. */
    static void lockForced(World world, BlockPos chest, PlayerEntity player) {
        spring(world, chest, null, player);
    }

    private static void spring(World world, BlockPos chest, @Nullable BlockPos otherHalf, PlayerEntity player) {
        if (!(world instanceof ServerWorld server)) {
            return;
        }
        TrapTriggerBlockEntity needle = needleUnder(world, chest);
        if (needle == null) {
            needle = needleUnder(world, otherHalf);
        }
        if (needle != null) {
            needle.trigger(server, player);
        }
    }
}

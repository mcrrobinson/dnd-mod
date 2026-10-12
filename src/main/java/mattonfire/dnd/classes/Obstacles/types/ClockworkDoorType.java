package mattonfire.dnd.classes.Obstacles.types;

import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.ObstacleBlockEntity;
import mattonfire.dnd.classes.Obstacles.ObstacleGroups;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import mattonfire.dnd.entity.MountainDwarfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * A brass gear door of dwarven make. An Artificer (primary) or a Rogue (secondary) repairs it open with a
 * Tinker's Tools check; every attempt uses up an iron ingot and a redstone dust from the inventory.
 * <ul>
 * <li>Failure: sparks, {@value #SPARK_DAMAGE} damage. Natural 1: the gears jam and nobody can work it for
 * {@value #JAM_TICKS} ticks, and an alarm.</li>
 * <li>Fallback: smash the mechanism (hardness 50, like obsidian). Breaking any block opens the whole door (the
 * block stays, open, so a reset rebuilds it), but Mountain Dwarves who see it take it as an offence ({@link MountainDwarfEntity#witness}).</li>
 * </ul>
 */
public final class ClockworkDoorType extends ObstacleType {
    public static final float SPARK_DAMAGE = 1.0f;
    public static final int JAM_TICKS = 600;
    /** World time until which the group is jammed, on every block of it. */
    public static final String JAMMED_KEY = "JammedUntil";

    public ClockworkDoorType(Identifier id) {
        super(id);
    }

    @Override
    public Skill skill() {
        return Skill.TINKERS_TOOLS;
    }

    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        return switch (dndClass) {
            case ARTIFICER -> Eligibility.PRIMARY;
            case ROGUE -> Eligibility.SECONDARY;
            default -> Eligibility.NONE;
        };
    }

    @Override
    public boolean breakable() {
        return true;
    }

    /** An empty hand, or the parts it takes. */
    @Override
    public boolean accepts(PlayerEntity player, ItemStack held) {
        return held.isEmpty() || held.isOf(Items.IRON_INGOT) || held.isOf(Items.REDSTONE);
    }

    @Override
    public boolean payForAttempt(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        long jammedFor = jammedUntil(world, pos) - world.getTime();
        if (jammedFor > 0) {
            player.sendMessage(Text.translatable(translationKey() + ".jammed", (jammedFor + 19) / 20), true);
            world.playSound(null, pos, SoundEvents.BLOCK_CHAIN_HIT, SoundCategory.BLOCKS, 1.0f, 0.6f);
            return false;
        }
        if (!Reagents.takeIronAndRedstone(player)) {
            player.sendMessage(Text.translatable(translationKey() + ".parts"), true);
            return false;
        }
        DnDClasses.LOGGER.info("[Obstacle] {} spent an iron ingot and a redstone dust on the clockwork door at {}",
                player.getEntityName(), pos.toShortString());
        return true;
    }

    private static long jammedUntil(ServerWorld world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof ObstacleBlockEntity be ? be.data().getLong(JAMMED_KEY) : 0L;
    }

    @Override
    public List<String> progressKeys() {
        return List.of(JAMMED_KEY);
    }

    @Override
    public void onFailure(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        sparks(world, pos);
        player.damage(world.getDamageSources().generic(), SPARK_DAMAGE);
    }

    @Override
    public void onFumble(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        onFailure(world, pos, player);
        long until = world.getTime() + JAM_TICKS;
        for (BlockPos part : ObstacleGroups.group(world, pos)) {
            if (world.getBlockEntity(part) instanceof ObstacleBlockEntity be) {
                be.data().putLong(JAMMED_KEY, until);
                be.markDirty();
            }
        }
        world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS, 0.8f, 0.7f);
        DnDClasses.LOGGER.info("[Obstacle] {} jammed the clockwork door at {} until {}", player.getEntityName(),
                pos.toShortString(), until);
    }

    /** The smashed block stays in place, open, so a reset rebuilds the whole door. */
    @Override
    public boolean opensWhenBroken() {
        return true;
    }

    /** Smashing the mechanism opens the door, and any Mountain Dwarf who sees it takes offence. */
    @Override
    public void onBrokenByHand(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
        int opened = ObstacleGroups.open(world, pos);
        sparks(world, pos);
        world.playSound(null, pos, SoundEvents.ENTITY_ITEM_BREAK, SoundCategory.BLOCKS, 1.2f, 0.6f);
        player.sendMessage(Text.translatable(translationKey() + ".smashed"), true);
        DnDClasses.LOGGER.info("[Obstacle] {} smashed the clockwork door at {} ({} blocks opened)",
                player.getEntityName(), pos.toShortString(), opened);
        MountainDwarfEntity.witness(player, pos);
    }

    private static void sparks(ServerWorld world, BlockPos pos) {
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                16, 0.4, 0.4, 0.4, 0.2);
        world.playSound(null, pos, SoundEvents.BLOCK_REDSTONE_TORCH_BURNOUT, SoundCategory.BLOCKS, 1.0f, 1.2f);
    }

    @Override
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
        world.spawnParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                6, 0.3, 0.3, 0.3, 0.05);
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 1.0f, 0.7f);
            world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_OPEN, SoundCategory.BLOCKS, 1.0f, 0.8f);
        }
    }

    @Override
    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 1.0f, 0.7f);
            world.playSound(null, pos, SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 1.0f, 0.8f);
        }
    }
}

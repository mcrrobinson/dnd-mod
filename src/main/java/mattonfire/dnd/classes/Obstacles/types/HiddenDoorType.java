package mattonfire.dnd.classes.Obstacles.types;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.classes.Obstacles.ObstacleBlockEntity;
import mattonfire.dnd.classes.Obstacles.ObstacleEvents;
import mattonfire.dnd.classes.Obstacles.ObstacleGroups;
import mattonfire.dnd.classes.Obstacles.ObstacleType;
import mattonfire.dnd.classes.Obstacles.Tier;
import mattonfire.dnd.classes.Party.Party;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;

/**
 * A door that looks exactly like the wall round it (stone, deepslate or blackstone bricks). Nobody can use it
 * until it's found, and once someone has found it, it's found for their whole {@code /party}:
 * <ul>
 * <li><b>Ranger, passive:</b> a Ranger within {@value #PASSIVE_RANGE} blocks whose passive Perception (or
 * Investigation, whichever is better) meets the tier's DC finds it without trying
 * ({@code ObstacleAuras}).</li>
 * <li><b>Dwarf, sneaking:</b> Stonecunning, the same passive check with advantage (+5) within 8 blocks
 * ({@code RacePassives}).</li>
 * <li><b>Anyone, searching:</b> sneak + right-click a wall with an empty hand ({@code HiddenSearch}), or the
 * Search key (V).</li>
 * </ul>
 * Whoever finds it earns the tier's class XP (once per reset). After that, anyone who knows about it
 * right-clicks it to open it: no roll. Found doors are outlined for those who know them. It can't be broken.
 *
 * <p>Who knows about it is the {@value #REVEALED_KEY} list in the group's {@code Data} (player UUIDs), synced to
 * clients so the crosshair hint stays quiet until you know. A reset forgets it.
 */
public final class HiddenDoorType extends ObstacleType {
    /** How close a Ranger has to be to notice one passively. */
    public static final double PASSIVE_RANGE = 6.0;
    /** Data key: UUIDs (strings) of the players the door has been revealed to. */
    public static final String REVEALED_KEY = "Revealed";

    /** How a door was found, for the log and the message. */
    public enum How {
        PASSIVE, SEARCH, PARTY, STONECUNNING
    }

    private final Block lookalike;

    public HiddenDoorType(Identifier id, Block lookalike) {
        super(id);
        this.lookalike = lookalike;
    }

    /** The block it passes for. */
    public Block lookalike() {
        return lookalike;
    }

    /** No roll to open it: finding it is the check. */
    @Override
    public Skill skill() {
        return null;
    }

    /** Rangers find them on their own; once found, anyone can open one. */
    @Override
    public Eligibility eligibility(DndCharacter dndClass) {
        return dndClass == DndCharacter.RANGER ? Eligibility.PRIMARY : Eligibility.UNTRAINED;
    }

    @Override
    public boolean ownWhoText() {
        return true;
    }

    @Override
    public boolean breakable() {
        return false;
    }

    @Override
    public boolean disguised() {
        return true;
    }

    @Override
    public boolean protectsShell() {
        return false;
    }

    @Override
    public boolean hiddenFrom(BlockView world, BlockPos pos, PlayerEntity player) {
        if (player.isCreative()) {
            return false;
        }
        return !knows(world, pos, player.getUuid());
    }

    /** Whether the door at pos has been revealed to this player. */
    public static boolean knows(BlockView world, BlockPos pos, UUID player) {
        if (!(world.getBlockEntity(pos) instanceof ObstacleBlockEntity be)) {
            return false;
        }
        String id = player.toString();
        NbtList list = be.data().getList(REVEALED_KEY, NbtElement.STRING_TYPE);
        for (int i = 0; i < list.size(); i++) {
            if (id.equals(list.getString(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code finder} has found the door at pos: it's revealed to them and to every member of their party.
     * The finder earns the tier's XP (once per reset). Does nothing if they already knew.
     */
    public void reveal(ServerWorld world, BlockPos pos, ServerPlayerEntity finder, How how) {
        if (!ObstacleBlock.isSealed(world.getBlockState(pos)) || knows(world, pos, finder.getUuid())) {
            return;
        }
        Set<UUID> told = new LinkedHashSet<>();
        told.add(finder.getUuid());
        Party party = PartyManager.get(world.getServer()).getParty(finder.getUuid());
        if (party != null) {
            told.addAll(party.getMembers());
        }
        List<BlockPos> group = ObstacleGroups.group(world, pos);
        for (BlockPos part : group) {
            if (world.getBlockEntity(part) instanceof ObstacleBlockEntity be) {
                NbtList list = be.data().getList(REVEALED_KEY, NbtElement.STRING_TYPE);
                for (UUID id : told) {
                    if (!knows(world, part, id)) {
                        list.add(NbtString.of(id.toString()));
                    }
                }
                be.data().put(REVEALED_KEY, list);
                be.dataChanged();
            }
            world.spawnParticles(finder, new BlockStateParticleEffect(ParticleTypes.BLOCK, lookalike.getDefaultState()),
                    true, part.getX() + 0.5, part.getY() + 0.5, part.getZ() + 0.5, 8, 0.4, 0.4, 0.4, 0.05);
        }
        Tier tier = world.getBlockEntity(pos) instanceof ObstacleBlockEntity be ? be.tier() : Tier.MEDIUM;
        finder.sendMessage(Text.translatable(translationKey() + switch (how) {
            case PASSIVE -> ".noticed";
            case STONECUNNING -> ".stonecunning";
            default -> ".found";
        }), true);
        world.playSound(null, pos, SoundEvents.BLOCK_STONE_HIT, SoundCategory.BLOCKS, 1.0f, 0.6f);
        finder.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.PLAYERS, 0.6f, 1.4f);
        for (UUID id : told) {
            ServerPlayerEntity member = world.getServer().getPlayerManager().getPlayer(id);
            if (member != null && member != finder && member.getWorld() == world) {
                member.sendMessage(Text.translatable(translationKey() + ".party", finder.getDisplayName()), true);
            }
        }
        boolean rewarded = !finder.isCreative() && ObstacleGroups.claimReward(world, pos);
        int xp = rewarded ? xp(tier) : 0;
        DnDClasses.LOGGER.info("[Obstacle] {} found the {} at {} ({}, DC {}), revealed to {} player(s): +{} class XP",
                finder.getEntityName(), id(), pos.toShortString(), how.name().toLowerCase(), tier.dc, told.size(), xp);
        if (rewarded) {
            Progression.addXp(finder, xp);
            ObstacleEvents.SOLVED.invoker().onSolved(finder, this, tier, pos);
        }
    }

    /** Opening a found door earns nothing: the XP was for finding it. */
    @Override
    public boolean solve(ServerWorld world, BlockPos pos, ServerPlayerEntity solver) {
        ObstacleGroups.open(world, pos);
        return false;
    }

    @Override
    public void openEffects(ServerWorld world, BlockPos pos, boolean first) {
        world.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, lookalike.getDefaultState()),
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.1);
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_PISTON_CONTRACT, SoundCategory.BLOCKS, 1.0f, 0.5f);
            world.playSound(null, pos, SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.BLOCKS, 0.6f, 0.5f);
        }
    }

    @Override
    public void sealEffects(ServerWorld world, BlockPos pos, boolean first) {
        if (first) {
            world.playSound(null, pos, SoundEvents.BLOCK_PISTON_EXTEND, SoundCategory.BLOCKS, 1.0f, 0.5f);
        }
    }

    @Override
    public List<String> progressKeys() {
        return List.of(REVEALED_KEY);
    }
}

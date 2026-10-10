package mattonfire.dnd.entity;

import java.util.List;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.dungeon.DungeonEvents;
import mattonfire.dnd.dungeon.DungeonState;
import mattonfire.dnd.faction.Faction;
import mattonfire.dnd.faction.Factions;
import mattonfire.dnd.faction.Reputation;
import mattonfire.dnd.world.gen.dungeon.DungeonTheme;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/**
 * The Mountain Dwarves forgive whoever reclaims a lost hold. When a Dwarven Ruin is cleared
 * ({@link DungeonEvents#CLEARED}), each participant:
 * <ul>
 * <li>gains standing with the Mountain Dwarves: at least {@link #REPUTATION}, and enough to lift a
 * negative standing back to Neutral (their grudge over past thefts and blows is wiped);</li>
 * <li>is forgiven by every dwarf of the nearest dwarven fortress within {@link #FORTRESS_RANGE}
 * blocks of the Ruin ({@link SettlementGrudges#forgive}): dwarves angry at them calm down.</li>
 * </ul>
 */
public final class DwarfGrudges {
    /** A fortress this far (blocks, horizontally) from the Ruin hears of it. */
    public static final int FORTRESS_RANGE = 512;
    /** Dwarves this far (horizontally) from the fortress's start are its dwarves. */
    public static final int FORTRESS_RADIUS = 128;
    /** The least standing a clear gives with the Mountain Dwarves. */
    public static final int REPUTATION = 60;
    private static final Identifier FACTION = new Identifier(DnDClasses.MOD_ID, "mountain_dwarves");
    private static final TagKey<Structure> FORTRESSES = TagKey.of(RegistryKeys.STRUCTURE,
            new Identifier(DnDClasses.MOD_ID, "faction/mountain_dwarves"));

    private DwarfGrudges() {
    }

    public static void register() {
        DungeonEvents.CLEARED.register(DwarfGrudges::onDungeonCleared);
    }

    private static void onDungeonCleared(ServerWorld world, DungeonState dungeon, List<ServerPlayerEntity> participants) {
        if (dungeon.theme() != DungeonTheme.DWARVEN_RUIN || participants.isEmpty()) {
            return;
        }
        BlockPos ruin = dungeon.bounds().getCenter();
        BlockPos fortress = nearestFortress(world, ruin);
        Faction dwarves = Factions.get(FACTION);
        for (ServerPlayerEntity player : participants) {
            if (dwarves != null) {
                int standing = Reputation.get(player, dwarves);
                Reputation.add(player, dwarves, Math.max(REPUTATION, -standing), Reputation.Source.QUEST);
            }
            int calmed = 0;
            if (fortress != null) {
                Box area = new Box(fortress.getX() - FORTRESS_RADIUS, world.getBottomY(), fortress.getZ() - FORTRESS_RADIUS,
                        fortress.getX() + FORTRESS_RADIUS, world.getTopY(), fortress.getZ() + FORTRESS_RADIUS);
                calmed = SettlementGrudges.forgive(player, area);
            }
            player.sendMessage(Text.translatable(fortress != null ? "dungeon.dndclasses.dwarven_ruin.forgiven"
                    : "dungeon.dndclasses.dwarven_ruin.reclaimed").formatted(Formatting.GOLD), false);
            DnDClasses.LOGGER.info("[Dungeon] {} helped reclaim dwarven ruin {}: Mountain Dwarves standing now {}, fortress {}, {} dwarf grudge(s) dropped",
                    player.getEntityName(), dungeon.startKey(), dwarves == null ? "?" : Reputation.get(player, dwarves),
                    fortress == null ? "none within " + FORTRESS_RANGE : fortress.toShortString(), calmed);
        }
    }

    /** The nearest dwarven fortress's start within {@link #FORTRESS_RANGE} blocks of {@code pos}, or null. */
    @Nullable
    public static BlockPos nearestFortress(ServerWorld world, BlockPos pos) {
        BlockPos found = world.locateStructure(FORTRESSES, pos, FORTRESS_RANGE / 16, false);
        if (found == null) {
            return null;
        }
        double dx = found.getX() - pos.getX();
        double dz = found.getZ() - pos.getZ();
        return dx * dx + dz * dz <= (double) FORTRESS_RANGE * FORTRESS_RANGE ? found : null;
    }
}

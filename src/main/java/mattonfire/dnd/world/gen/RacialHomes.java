package mattonfire.dnd.world.gen;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.faction.Faction;
import mattonfire.dnd.faction.Reputation;
import mattonfire.dnd.world.gen.enclave.ElvenEnclaveStructures;
import mattonfire.dnd.world.gen.fortress.DwarvenFortressStructures;
import mattonfire.dnd.world.gen.village.HobbitVillageStructures;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/**
 * Every race's home settlement: which structure it is, whose home it is, which faction lives there and
 * which piece is the hearth. {@link HomeBonuses} gives the home race its bonuses there,
 * {@link mattonfire.dnd.entity.KinPrices} the kin discount and {@link mattonfire.dnd.entity.SettlementGrudges}
 * the kin trust. A new settlement registers one {@link Home} here.
 */
public final class RacialHomes {
    private static final Map<Identifier, Home> HOMES = new LinkedHashMap<>();

    public static final Home HOBBIT_VILLAGE = register(new Home(id("hobbit_village"), DndRace.HALFLING, Set.of(),
            id("hobbits"), HobbitVillageStructures.INN, HomeBonuses::shireWelcomeBasket));
    /** Gnomes get kin prices here until they have a home of their own. */
    public static final Home DWARVEN_FORTRESS = register(new Home(id("dwarven_fortress"), DndRace.DWARF,
            Set.of(DndRace.GNOME), id("mountain_dwarves"), DwarvenFortressStructures.HALL, null));
    /** The hearth is the Speaker's Hall up in the Heart Tree. */
    public static final Home ELVEN_ENCLAVE = register(new Home(id("elven_enclave"), DndRace.ELF, Set.of(),
            id("sylvan_court"), ElvenEnclaveStructures.HALL, null));

    private RacialHomes() {
    }

    /**
     * One race's home.
     *
     * @param structure  the structure id, e.g. {@code dndclasses:hobbit_village}
     * @param race       the home race: welcome, hearth, kin prices and kin trust
     * @param priceKin   other races that also get kin prices (interim homes)
     * @param faction    the faction that lives there; its members are the settlement's NPCs
     * @param hearth     the piece whose box is the hearth (the inn, the great hall)
     * @param firstVisit given to the home race on its first visit to each settlement, or null
     */
    public record Home(Identifier structure, DndRace race, Set<DndRace> priceKin, Identifier faction,
                       StructurePieceType hearth, @Nullable Consumer<ServerPlayerEntity> firstVisit) {
        public RegistryKey<Structure> key() {
            return RegistryKey.of(RegistryKeys.STRUCTURE, this.structure);
        }

        public boolean isHomeOf(DndRace race) {
            return race != DndRace.NONE && race == this.race;
        }

        public boolean givesKinPrices(DndRace race) {
            return this.isHomeOf(race) || this.priceKin.contains(race);
        }

        /** "Hobbit Village", "Dwarven Fortress". */
        public MutableText displayName() {
            return Text.translatable("home." + this.structure.getNamespace() + "." + this.structure.getPath());
        }
    }

    /** A player (or block) inside one particular settlement. */
    public record Visit(Home home, StructureStart start) {
        /** Identifies this settlement (its start chunk), for "first visit" bookkeeping. */
        public long id() {
            return this.start.getPos().toLong();
        }

        public boolean inHearth(BlockPos pos) {
            return this.pieceAt(pos, this.home.hearth()) != null;
        }

        /** The piece of {@code type} whose box holds {@code pos}, or null. */
        @Nullable
        public StructurePiece pieceAt(BlockPos pos, StructurePieceType type) {
            for (StructurePiece piece : this.start.getChildren()) {
                if (piece.getType() == type && piece.getBoundingBox().contains(pos)) {
                    return piece;
                }
            }
            return null;
        }

        /** The first piece whose box holds {@code pos}, or null. */
        @Nullable
        public StructurePiece pieceAt(BlockPos pos) {
            for (StructurePiece piece : this.start.getChildren()) {
                if (piece.getBoundingBox().contains(pos)) {
                    return piece;
                }
            }
            return null;
        }
    }

    public static Home register(Home home) {
        HOMES.put(home.structure(), home);
        return home;
    }

    public static Collection<Home> all() {
        return Collections.unmodifiableCollection(HOMES.values());
    }

    /** Blocks above a settlement's box that still count as inside it (roofs, hills, flying in). */
    public static final int HEADROOM = 16;

    /**
     * The racial home whose overall box (plus {@link #HEADROOM} above) holds {@code pos}, or null. The
     * whole box rather than single pieces, so the lanes and lawns between buildings count too.
     */
    @Nullable
    public static Visit homeAt(ServerWorld world, BlockPos pos) {
        Registry<Structure> structures = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        for (Home home : HOMES.values()) {
            Structure structure = structures.get(home.key());
            if (structure == null) {
                continue;
            }
            for (StructureStart start : world.getStructureAccessor().getStructureStarts(ChunkSectionPos.from(pos), structure)) {
                BlockBox box = start.getBoundingBox();
                if (start.hasChildren() && pos.getX() >= box.getMinX() && pos.getX() <= box.getMaxX()
                        && pos.getZ() >= box.getMinZ() && pos.getZ() <= box.getMaxZ()
                        && pos.getY() >= box.getMinY() && pos.getY() <= box.getMaxY() + HEADROOM) {
                    return new Visit(home, start);
                }
            }
        }
        return null;
    }

    /** The home {@code player} is standing in, if it's their own race's home; null otherwise. */
    @Nullable
    public static Visit ownHomeAt(ServerPlayerEntity player) {
        Visit visit = homeAt(player.getWorld(), player.getBlockPos());
        return visit != null && visit.home().isHomeOf(RaceLifecycle.activeRaceOf(player)) ? visit : null;
    }

    /**
     * Whether {@code player} is in the hearth of their own race's home (the Green Dragon inn for a
     * Halfling, the great hall for a Dwarf). Rests can treat this as a Safe Haven.
     */
    public static boolean isHearth(ServerPlayerEntity player) {
        Visit visit = ownHomeAt(player);
        return visit != null && visit.inHearth(player.getBlockPos());
    }

    /** The home whose faction {@code npc} belongs to (any hobbit or innkeeper: the Hobbit Village), or null. */
    @Nullable
    public static Home homeOfMember(Entity npc) {
        Faction faction = Reputation.factionOf(npc);
        if (faction == null) {
            return null;
        }
        for (Home home : HOMES.values()) {
            if (home.faction().equals(faction.id())) {
                return home;
            }
        }
        return null;
    }

    private static Identifier id(String path) {
        return new Identifier(DnDClasses.MOD_ID, path);
    }
}

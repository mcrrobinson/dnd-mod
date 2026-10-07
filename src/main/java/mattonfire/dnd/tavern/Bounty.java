package mattonfire.dnd.tavern;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The jobs hobbits pin to the tavern's bounty board. Each is either a hunt (slay a number of
 * creatures in an entity type tag) or an expedition (set foot inside a structure). Which
 * creatures count is data: the {@code dndclasses:goblins}, {@code dndclasses:wyverns} and
 * {@code dndclasses:bounty/*} entity type tags.
 */
public enum Bounty {
    GOBLIN_TROUBLE("goblin_trouble", hunt("goblins"), 10, 8, Tier.MINOR, 30, 50, 10),
    GOBLIN_WARLORD("goblin_warlord", hunt("bounty/goblin_warlords"), 1, 16, Tier.MAJOR, 60, 150, 4),
    WYVERN_HUNT("wyvern_hunt", hunt("wyverns"), 1, 12, Tier.MAJOR, 50, 120, 6),
    STORM_DRAGON("storm_dragon", hunt("bounty/storm_dragons"), 1, 24, Tier.MAJOR, 100, 250, 2),
    SPIDERS("spiders", hunt("bounty/spiders"), 8, 5, Tier.MINOR, 20, 30, 10),
    BARROW_WIGHTS("barrow_wights", hunt("bounty/undead"), 15, 6, Tier.MINOR, 25, 40, 10),
    BRIGANDS("brigands", hunt("bounty/brigands"), 5, 10, Tier.MINOR, 30, 60, 6),
    DWARF_FORTRESS("dwarf_fortress", explore("dwarven_fortress"), 1, 10, Tier.MAJOR, 40, 100, 6),
    DRAGON_LAIR("dragon_lair", explore("dragon_lair"), 1, 14, Tier.MAJOR, 50, 120, 3);

    /** How generous the spoils are: which loot table the reward is rolled from. */
    public enum Tier {
        MINOR("gameplay/bounty_minor"),
        MAJOR("gameplay/bounty_major");

        public final Identifier lootTable;

        Tier(String path) {
            this.lootTable = new Identifier(DnDClasses.MOD_ID, path);
        }
    }

    public final String id;
    @Nullable
    public final TagKey<EntityType<?>> targets;
    @Nullable
    public final RegistryKey<Structure> destination;
    /** Kills needed for a hunt; 1 for an expedition. */
    public final int count;
    public final int emeralds;
    public final Tier tier;
    /** Vanilla experience points. */
    public final int xp;
    /** Class-progression XP (see {@link BountyRewards#grantProgressionXp}). */
    public final int classXp;
    /** How often this bounty gets posted, relative to the others. */
    public final int weight;

    Bounty(String id, TagKey<EntityType<?>> targets, int count, int emeralds, Tier tier, int xp, int classXp, int weight) {
        this(id, targets, null, count, emeralds, tier, xp, classXp, weight);
    }

    Bounty(String id, RegistryKey<Structure> destination, int count, int emeralds, Tier tier, int xp, int classXp, int weight) {
        this(id, null, destination, count, emeralds, tier, xp, classXp, weight);
    }

    Bounty(String id, @Nullable TagKey<EntityType<?>> targets, @Nullable RegistryKey<Structure> destination, int count,
           int emeralds, Tier tier, int xp, int classXp, int weight) {
        this.id = id;
        this.targets = targets;
        this.destination = destination;
        this.count = count;
        this.emeralds = emeralds;
        this.tier = tier;
        this.xp = xp;
        this.classXp = classXp;
        this.weight = weight;
    }

    private static TagKey<EntityType<?>> hunt(String tag) {
        return TagKey.of(RegistryKeys.ENTITY_TYPE, new Identifier(DnDClasses.MOD_ID, tag));
    }

    private static RegistryKey<Structure> explore(String structure) {
        return RegistryKey.of(RegistryKeys.STRUCTURE, new Identifier(DnDClasses.MOD_ID, structure));
    }

    public boolean isHunt() {
        return this.targets != null;
    }

    public Text title() {
        return Text.translatable("bounty." + DnDClasses.MOD_ID + "." + this.id);
    }

    public Text description() {
        return Text.translatable("bounty." + DnDClasses.MOD_ID + "." + this.id + ".desc");
    }

    @Nullable
    public static Bounty byId(String id) {
        for (Bounty bounty : values()) {
            if (bounty.id.equals(id)) {
                return bounty;
            }
        }
        return null;
    }

    /** Draws {@code n} different bounties, weighted by {@link #weight}. */
    public static List<Bounty> draw(Random random, int n) {
        List<Bounty> pool = new ArrayList<>(List.of(values()));
        List<Bounty> drawn = new ArrayList<>();
        while (drawn.size() < n && !pool.isEmpty()) {
            int total = pool.stream().mapToInt(b -> b.weight).sum();
            int roll = random.nextInt(total);
            for (int i = 0; i < pool.size(); i++) {
                roll -= pool.get(i).weight;
                if (roll < 0) {
                    drawn.add(pool.remove(i));
                    break;
                }
            }
        }
        return drawn;
    }
}

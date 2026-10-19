package mattonfire.dnd.world.gen;

/**
 * How far gone a desolate settlement is ({@link Desolation}): each start rolls one, equally likely. The chances are
 * per block unless they say otherwise.
 */
public enum DesolationLevel {
    /** Sacked not long ago: walls breached, rubble everywhere, doors smashed, a few fires still smouldering. */
    RAIDED(new Builder().bareGround(0.25F).wildDie(0.35F).torches(0.35F).lanterns(0.5F).firesLit(0.35F).glass(0.6F)
            .cracked(0.15F).breaches(2.5F, 1.8F, 3.5F).floorHoles(0.8F).doorways(0.5F).rubble(0.6F).carpets(0.5F, 0.1F)
            .doors(0.5F).bridges(0.35F).siege(1.0F)),
    /** Left in a hurry: tidy, empty and cold, the crops gone to seed and the first cobwebs up. */
    ABANDONED(new Builder().bareGround(0.08F).wildDie(0.05F).torches(0.3F).lanterns(0.6F).glass(0.15F).cracked(0.08F)
            .mossy(0.04F).cobwebs(0.03F).breaches(0.6F, 1.2F, 2.2F).floorHoles(0.2F).doorways(0.1F).rubble(0.3F)
            .carpets(0.3F, 0.0F).doors(0.1F)
            .bridges(0.1F).flowers(0.04F).siege(0.2F)),
    /** Empty for years: the grass worn down to dirt, most plants and leaves dead, the walls crumbling. */
    FORSAKEN(new Builder().bareGround(0.6F).wildDie(0.6F).leaves(0.4F).torches(0.15F).lanterns(0.4F).glass(0.35F)
            .cracked(0.2F).mossy(0.05F).cobwebs(0.05F).breaches(1.5F, 1.5F, 3.0F).floorHoles(0.6F).doorways(0.35F)
            .rubble(0.45F).carpets(0.7F, 0.15F)
            .doors(0.3F).bridges(0.3F).siege(0.55F)),
    /** Lost long ago: the grass and trees have come back, moss and vines over everything, soil on the roofs. */
    OVERGROWN(new Builder().regrowth(0.5F).torches(0.0F).lanterns(0.2F).glass(0.7F).cracked(0.25F).mossy(0.4F)
            .cobwebs(0.1F).breaches(2.2F, 1.8F, 3.5F).floorHoles(1.0F).doorways(0.6F).rubble(0.5F).carpets(0.6F, 0.4F)
            .doors(0.6F).bridges(0.6F)
            .flowers(0.2F).vines(0.15F).roofs(0.3F).floorMoss(0.08F).trees(0.35F).siege(0.7F));

    /** Grass blocks that are bare dirt, in patches. */
    public final float bareGround;
    /** Dirt, coarse dirt and paths that have grassed over, in patches. */
    public final float regrowth;
    /** Wild plants (grass, flowers, saplings) that are dead. Crops always are. */
    public final float wildDie;
    public final float leavesKept;
    public final float torchesKept;
    public final float lanternsKept;
    /** Campfires still burning. Candles and furnaces are always out. */
    public final float firesLit;
    public final float glassBroken;
    public final float cracked;
    public final float mossy;
    /** Air under a solid ceiling with a cobweb. */
    public final float cobwebs;
    /**
     * Holes knocked in a piece's walls and roof, on average for a piece {@link Withering#PIECE_SIZE} blocks across
     * (more for a bigger one), and their radius.
     */
    public final float breaches;
    public final float breachMin;
    public final float breachMax;
    /** Holes through a piece's roof and floors, on average for a piece {@link Withering#PIECE_SIZE} across. */
    public final float floorHoles;
    /** Doorways that have fallen in: the door and the lintel over it, in a heap on the threshold. */
    public final float doorways;
    /** Blocks knocked out of a breach that lie on the ground below it. */
    public final float rubble;
    public final float carpetsTattered;
    public final float carpetsGone;
    public final float doorsBroken;
    /** Elven rope bridges that have fallen, each. */
    public final float bridgesFallen;
    /** Grass blocks in the open with a flower or tall grass on them. */
    public final float flowers;
    /** Air beside a wall with vines on it. */
    public final float vines;
    /** Roof blocks in the open with moss or soil on them. */
    public final float roofs;
    /** Floors with moss carpet on them. */
    public final float floorMoss;
    /** Trees grown up inside, per chunk on average. */
    public final float trees;
    /**
     * How battered a settlement's outside is, 0 to 1: a piece with its own ruin ({@link Ruinable}, the dwarven
     * fortress gate) scales its breaches, broken battlements, toppled pillars and heaps of rubble by it.
     */
    public final float siege;

    DesolationLevel(Builder b) {
        this.bareGround = b.bareGround;
        this.regrowth = b.regrowth;
        this.wildDie = b.wildDie;
        this.leavesKept = b.leavesKept;
        this.torchesKept = b.torchesKept;
        this.lanternsKept = b.lanternsKept;
        this.firesLit = b.firesLit;
        this.glassBroken = b.glassBroken;
        this.cracked = b.cracked;
        this.mossy = b.mossy;
        this.cobwebs = b.cobwebs;
        this.breaches = b.breaches;
        this.breachMin = b.breachMin;
        this.breachMax = b.breachMax;
        this.floorHoles = b.floorHoles;
        this.doorways = b.doorways;
        this.rubble = b.rubble;
        this.carpetsTattered = b.carpetsTattered;
        this.carpetsGone = b.carpetsGone;
        this.doorsBroken = b.doorsBroken;
        this.bridgesFallen = b.bridgesFallen;
        this.flowers = b.flowers;
        this.vines = b.vines;
        this.roofs = b.roofs;
        this.floorMoss = b.floorMoss;
        this.trees = b.trees;
        this.siege = b.siege;
    }

    /** The level for a start, from a number in [0, 1). */
    public static DesolationLevel pick(float roll) {
        DesolationLevel[] levels = values();
        return levels[Math.min(levels.length - 1, (int) (roll * levels.length))];
    }

    private static final class Builder {
        float bareGround;
        float regrowth;
        float wildDie;
        float leavesKept = 1.0F;
        float torchesKept = 1.0F;
        float lanternsKept = 1.0F;
        float firesLit;
        float glassBroken;
        float cracked;
        float mossy;
        float cobwebs;
        float breaches;
        float breachMin;
        float breachMax;
        float floorHoles;
        float doorways;
        float rubble;
        float carpetsTattered;
        float carpetsGone;
        float doorsBroken;
        float bridgesFallen;
        float flowers;
        float vines;
        float roofs;
        float floorMoss;
        float trees;
        float siege;

        Builder siege(float s) {
            this.siege = s;
            return this;
        }

        Builder bareGround(float p) {
            this.bareGround = p;
            return this;
        }

        Builder regrowth(float p) {
            this.regrowth = p;
            return this;
        }

        Builder wildDie(float p) {
            this.wildDie = p;
            return this;
        }

        Builder leaves(float kept) {
            this.leavesKept = kept;
            return this;
        }

        Builder torches(float kept) {
            this.torchesKept = kept;
            return this;
        }

        Builder lanterns(float kept) {
            this.lanternsKept = kept;
            return this;
        }

        Builder firesLit(float p) {
            this.firesLit = p;
            return this;
        }

        Builder glass(float broken) {
            this.glassBroken = broken;
            return this;
        }

        Builder cracked(float p) {
            this.cracked = p;
            return this;
        }

        Builder mossy(float p) {
            this.mossy = p;
            return this;
        }

        Builder cobwebs(float p) {
            this.cobwebs = p;
            return this;
        }

        Builder breaches(float perPiece, float minRadius, float maxRadius) {
            this.breaches = perPiece;
            this.breachMin = minRadius;
            this.breachMax = maxRadius;
            return this;
        }

        Builder floorHoles(float perPiece) {
            this.floorHoles = perPiece;
            return this;
        }

        Builder doorways(float p) {
            this.doorways = p;
            return this;
        }

        Builder rubble(float p) {
            this.rubble = p;
            return this;
        }

        Builder carpets(float tattered, float gone) {
            this.carpetsTattered = tattered;
            this.carpetsGone = gone;
            return this;
        }

        Builder doors(float broken) {
            this.doorsBroken = broken;
            return this;
        }

        Builder bridges(float fallen) {
            this.bridgesFallen = fallen;
            return this;
        }

        Builder flowers(float p) {
            this.flowers = p;
            return this;
        }

        Builder vines(float p) {
            this.vines = p;
            return this;
        }

        Builder roofs(float p) {
            this.roofs = p;
            return this;
        }

        Builder floorMoss(float p) {
            this.floorMoss = p;
            return this;
        }

        Builder trees(float perChunk) {
            this.trees = perChunk;
            return this;
        }
    }
}

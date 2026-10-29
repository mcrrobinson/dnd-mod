package mattonfire.dnd.world.gen.ancient;

import java.util.Locale;
import mattonfire.dnd.entity.dragon.ChromaticColor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

/**
 * Where everything in an Ancient dragon's lair goes (design 10, section 6), shared by its two pieces so they agree:
 * the {@link CalderaPiece} (the volcanic cone, its crater, the perch and the mouth) and the {@link HoardHallPiece}
 * (the stairwell under the perch, the kobold quarters and the domed hoard hall under the crater).
 * <p>
 * Positions are given in the lair's own frame: {@code along} runs from the crater's centre out through the
 * {@link #front() mouth} (negative towards the perch), {@code side} to the {@link #right() right}. The stairwell has
 * its own frame round the perch: {@code a} towards the back (away from the crater), {@code b} to the right, and
 * {@code h} up from the crater floor ({@link #stair}).
 *
 * @param center the crater's centre, at the crater floor's height (the floor block)
 * @param base   the average ground height round the cone's foot (radius {@value #BASE_RADIUS})
 * @param front  which way the mouth faces: the downhill side
 */
public record AncientLairLayout(ChromaticColor color, BlockPos center, int base, Direction front, long seed) {
    /** The cone's foot. */
    public static final int BASE_RADIUS = 44;
    /** The crest of the rim. */
    public static final int RIM_RADIUS = 30;
    /** The flat crater floor, open to the sky. */
    public static final int FLOOR_RADIUS = 26;
    /** How far the rim rises above the ground round the cone, at least. */
    public static final int RISE = 24;
    /** How far the crater floor sits below the rim. */
    public static final int CRATER_DEPTH = 6;

    /** The perch's middle is this far from the crater's centre, away from the mouth. */
    public static final int PERCH_OUT = 15;
    /** The perch's plateau is this many blocks above the crater floor. */
    public static final int PERCH_HEIGHT = 14;
    /** The plateau is 9x9: this far each way from its middle. */
    public static final int PERCH_HALF = 4;
    /** The plinth at the pillar's foot (and the stairwell under it) is 13x13. */
    public static final int PLINTH_HALF = 6;
    /** How tall the plinth stands above the crater floor. */
    public static final int PLINTH_HEIGHT = 4;

    /** The mouth is a gash 7 wide. */
    public static final int MOUTH_HALF = 3;
    /** The mouth's stair runs out this far at most. */
    public static final int MOUTH_REACH = 58;

    /** The hall's middle is this far towards the mouth from the crater's centre... */
    public static final int HALL_OUT = 9;
    /** ...and its floor this far below the crater floor. */
    public static final int HALL_DEPTH = 20;
    public static final int HALL_RADIUS = 18;
    public static final int HALL_HEIGHT = 14;

    public int floorY() {
        return this.center.getY();
    }

    public int rimY() {
        return this.floorY() + CRATER_DEPTH;
    }

    /** The hoard hall's floor block. */
    public int hallY() {
        return this.floorY() - HALL_DEPTH;
    }

    public Direction back() {
        return this.front.getOpposite();
    }

    public Direction right() {
        return this.front.rotateYClockwise();
    }

    /** The world position {@code along} the mouth's axis and {@code side} to the right of the crater's centre. */
    public BlockPos at(int along, int side, int y) {
        Direction right = this.right();
        return new BlockPos(this.center.getX() + this.front.getOffsetX() * along + right.getOffsetX() * side, y,
                this.center.getZ() + this.front.getOffsetZ() * along + right.getOffsetZ() * side);
    }

    /** How far {@code x, z} is along the mouth's axis from the crater's centre. */
    public int along(int x, int z) {
        return (x - this.center.getX()) * this.front.getOffsetX() + (z - this.center.getZ()) * this.front.getOffsetZ();
    }

    /** How far {@code x, z} is to the right of the mouth's axis. */
    public int side(int x, int z) {
        Direction right = this.right();
        return (x - this.center.getX()) * right.getOffsetX() + (z - this.center.getZ()) * right.getOffsetZ();
    }

    /** In the stairwell's frame: {@code a} back from the perch's middle, {@code b} to the right, {@code h} up from the crater floor. */
    public BlockPos stair(int a, int b, int h) {
        return this.at(-PERCH_OUT - a, b, this.floorY() + h);
    }

    /** Where the dragon stands, on the perch's plateau. */
    public BlockPos perchTop() {
        return this.at(-PERCH_OUT, 0, this.floorY() + PERCH_HEIGHT + 1);
    }

    /** The lair's centre for the dragon (its {@code Lair}): just above the middle of the crater floor. */
    public BlockPos lairCentre() {
        return this.center.up();
    }

    /** The middle of the hoard hall's floor (the floor block). */
    public BlockPos hallCenter() {
        return this.at(HALL_OUT, 0, this.hallY());
    }

    /** The top of the mouth's stair, on the crater floor. */
    public BlockPos mouthTop() {
        return this.at(RIM_RADIUS, 0, this.floorY() + 1);
    }

    /** A yaw that faces {@code direction}. */
    public static float yaw(Direction direction) {
        return direction.asRotation();
    }

    public void writeNbt(NbtCompound nbt) {
        nbt.putString("Color", this.color.id());
        nbt.put("Center", NbtHelper.fromBlockPos(this.center));
        nbt.putInt("Base", this.base);
        nbt.putString("Front", this.front.asString());
        nbt.putLong("Seed", this.seed);
    }

    public static AncientLairLayout readNbt(NbtCompound nbt) {
        Direction front = Direction.byName(nbt.getString("Front"));
        return new AncientLairLayout(colorOf(nbt.getString("Color")), NbtHelper.toBlockPos(nbt.getCompound("Center")),
                nbt.getInt("Base"), front == null || front.getAxis().isVertical() ? Direction.NORTH : front,
                nbt.getLong("Seed"));
    }

    /** The colour named {@code id} ("red"), or red. */
    public static ChromaticColor colorOf(String id) {
        for (ChromaticColor color : ChromaticColor.values()) {
            if (color.id().equals(id.toLowerCase(Locale.ROOT))) {
                return color;
            }
        }
        return ChromaticColor.RED;
    }

    // ---------------------------------------------------------------- noise

    /**
     * A number in [0, 1) from the lair's seed, a position and {@code salt}: the same in every chunk, so a lair built
     * a chunk at a time lines up.
     */
    public float hash(int x, int y, int z, int salt) {
        long h = this.seed + salt * 0x9E3779B97F4A7C15L + BlockPos.asLong(x, y, z) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        h ^= h >>> 31;
        return (h >>> 40) / (float) (1L << 24);
    }

    /** Smooth value noise in [0, 1) over x and z, with features about {@code scale} blocks across. */
    public float smooth(int x, int z, int scale, int salt) {
        int gx = Math.floorDiv(x, scale);
        int gz = Math.floorDiv(z, scale);
        float fx = (x - gx * scale) / (float) scale;
        float fz = (z - gz * scale) / (float) scale;
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        float a = this.hash(gx, 0, gz, salt);
        float b = this.hash(gx + 1, 0, gz, salt);
        float c = this.hash(gx, 0, gz + 1, salt);
        float d = this.hash(gx + 1, 0, gz + 1, salt);
        return MathHelper.lerp(fz, MathHelper.lerp(fx, a, b), MathHelper.lerp(fx, c, d));
    }
}

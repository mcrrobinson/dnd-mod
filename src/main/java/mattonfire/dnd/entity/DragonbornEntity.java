package mattonfire.dnd.entity;

import mattonfire.dnd.classes.Race.DragonAncestry;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A Dragonborn of the Dragon Temples. The base class holds what they all share: a scale colour (red, white,
 * blue, bronze or gold) that sets their ancestry, a name, a home spot they keep near, and the rule that
 * striking one brings every Templar nearby down on you. Templars ({@link DragonbornTemplarEntity}) guard the
 * temple; the Elder ({@link DragonbornElderEntity}) trades and gives quests. Neutral to everyone.
 */
public abstract class DragonbornEntity extends SettlementNpcEntity {
    /** Scale colours: red, white, blue, bronze, gold. */
    public static final int VARIANTS = 5;

    private static final String[] FIRST_NAMES = {
            "Arjhan", "Balasar", "Bharash", "Donaar", "Ghesh", "Heskan", "Kriv", "Medrash", "Mehen", "Nadarr",
            "Pandjed", "Patrin", "Rhogar", "Shamash", "Shedinn", "Tarhun", "Torinn", "Akra", "Biri", "Daar",
            "Farideh", "Harann", "Havilar", "Jheri", "Kava", "Korinn", "Mishann", "Nala", "Perra", "Raiann",
            "Sora", "Surina", "Thava", "Uadjit"
    };
    private static final String[] CLANS = {
            "Clethtinthiallor", "Daardendrian", "Delmirev", "Drachedandion", "Fenkenkabradon", "Kepeshkmolik",
            "Kerrhylon", "Kimbatuul", "Linxakasendalor", "Myastan", "Nemmonis", "Norixius", "Ophinshtalajiir",
            "Prexijandilin", "Shestendeliath", "Turnuroth", "Verthisathurgiesh", "Yarjerit"
    };

    protected DragonbornEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        this.folkGoals().escape(1.3D).fleePlayers(1.2D).wander(0.5D).add();
    }

    @Override
    protected int variants() {
        return VARIANTS;
    }

    /** The breath of this Dragonborn's colour: red and gold breathe fire, white frost, blue and bronze lightning. */
    public DragonAncestry getAncestry() {
        return ancestryOf(this.getVariant());
    }

    /** 0 red, 1 white, 2 blue, 3 bronze, 4 gold. */
    public static DragonAncestry ancestryOf(int variant) {
        return switch (Math.floorMod(variant, VARIANTS)) {
            case 1 -> DragonAncestry.FROST;
            case 2, 3 -> DragonAncestry.STORM;
            default -> DragonAncestry.EMBER;
        };
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        Random random = world.getRandom();
        this.setVariant(random.nextInt(VARIANTS));
        this.setCustomName(Text.literal(FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
                + CLANS[random.nextInt(CLANS.length)]));
        this.setPersistent();
        this.setHome(this.getBlockPos());
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    // A Templar's breath or stray blow doesn't hurt its own folk.
    @Override
    protected boolean isFriendlyFire(DamageSource source) {
        return source.getAttacker() instanceof DragonbornEntity;
    }

    /** Strike any Dragonborn and the Templars nearby turn on you. */
    @Override
    protected void callGuards(PlayerEntity player) {
        defend(this, player, DragonbornTemplarEntity.class);
    }

    @Override
    protected int dayRange() {
        return 20;
    }

    @Override
    protected int nightRange() {
        return 12;
    }

    // ---- Sounds: a villager's, deeper ----

    @Override
    public float getSoundPitch() {
        return super.getSoundPitch() * 0.75F;
    }
}

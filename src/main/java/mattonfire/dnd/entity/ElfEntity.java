package mattonfire.dnd.entity;

import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * An elf of the elven enclaves. This class is the Wood Elf, the enclave's ordinary folk: they wander the
 * platforms and the glade by day, keep near home at night, and run from monsters. Wardens
 * ({@link ElfWardenEntity}) guard them and the merchants ({@link ElfMerchantEntity}) trade. Strike any elf
 * and the Wardens nearby turn on you; what else angers them is Sylvan Law ({@link SylvanLaw}).
 */
public class ElfEntity extends SettlementNpcEntity {
    public static final int VARIANTS = 6;

    private static final int DAY_RANGE = 24;
    private static final int NIGHT_RANGE = 8;

    private static final String[] FIRST_NAMES = {
            "Aelar", "Arannis", "Berrian", "Caelynn", "Carric", "Enna", "Erevan", "Galinndan", "Hadarai", "Heian",
            "Ivellios", "Keyleth", "Laucian", "Lia", "Mialee", "Naivara", "Quelenna", "Quarion", "Riardon", "Sariel",
            "Shava", "Soveliss", "Thamior", "Tharivol", "Theren", "Thia", "Valanthe", "Varis", "Adrie", "Birel"
    };
    private static final String[] SURNAMES = {
            "Galanodel", "Amakiir", "Holimion", "Ilphelkiir", "Liadon", "Meliamne", "Nailo", "Siannodel",
            "Xiloscient", "Amastacia", "Brightwood", "Moonwhisper", "Starbloom", "Silverfrond", "Evenwood"
    };

    public ElfEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createElfAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 18.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void initGoals() {
        this.folkGoals().escape(1.4D).fleeMonsters(10.0F, 1.3D).fleePlayers(1.3D).walkTarget(0.8D).wander(0.6D)
                .lookAtKin(ElfEntity.class).add();
    }

    @Override
    protected int variants() {
        return VARIANTS;
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        Random random = world.getRandom();
        this.setVariant(random.nextInt(VARIANTS));
        this.setCustomName(Text.literal(FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
                + SURNAMES[random.nextInt(SURNAMES.length)]));
        this.setPersistent();
        this.setHome(this.getBlockPos());
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    // A Warden's stray arrow doesn't hurt its own folk.
    @Override
    protected boolean isFriendlyFire(DamageSource source) {
        return source.getSource() instanceof ProjectileEntity projectile && projectile.getOwner() instanceof ElfEntity;
    }

    /** Strike any elf and the Wardens nearby turn on you. */
    @Override
    protected void callGuards(PlayerEntity player) {
        defend(this, player, ElfWardenEntity.class);
    }

    /** How far from home this elf roams by day. */
    @Override
    protected int dayRange() {
        return DAY_RANGE;
    }

    /** How far from home this elf roams at night. */
    @Override
    protected int nightRange() {
        return NIGHT_RANGE;
    }

    // ---- Sounds: a villager's, lighter ----

    @Override
    public float getSoundPitch() {
        return super.getSoundPitch() * 1.2F;
    }
}

package mattonfire.dnd.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A Tiefling of the Brimstone Sanctums. The base class holds what they all share: a skin variant (crimson,
 * plum, ash, slate or ember), a name (an infernal name or a virtue name), a home spot they keep near, fire
 * immunity (from their entity type), Hellish Rebuke, and the rule that striking one brings every Exile nearby
 * down on you. Exiles ({@link TieflingExileEntity}) keep the sanctum; the Pactbroker
 * ({@link TieflingPactbrokerEntity}) trades. Neutral to everyone.
 */
public abstract class TieflingEntity extends SettlementNpcEntity {
    /** Skins: crimson, plum, ash, slate, ember. */
    public static final int VARIANTS = 5;
    /** Hellish Rebuke: a melee attacker burns for this long. */
    public static final int REBUKE_FIRE_SECONDS = 4;

    private static final String[] INFERNAL_NAMES = {
            "Akmenos", "Amnon", "Barakas", "Damakos", "Ekemon", "Iados", "Kairon", "Leucis", "Melech", "Mordai",
            "Morthos", "Pelaios", "Skamos", "Therai", "Akta", "Anakis", "Bryseis", "Criella", "Damaia", "Ea",
            "Kallista", "Lerissa", "Makaria", "Nemeia", "Orianna", "Phelaia", "Rieta"
    };
    private static final String[] VIRTUE_NAMES = {
            "Ash", "Carrion", "Despair", "Hope", "Ire", "Mercy", "Penance", "Quiet", "Sorrow", "Torment", "Vigil", "Weary"
    };

    /**
     * A Tiefling keeps within 16 blocks of home unless {@link #setHome(BlockPos, int)} says otherwise, day and night
     * alike (the Nether has no day and night).
     */
    protected TieflingEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world, 16);
    }

    @Override
    protected void initGoals() {
        this.folkGoals().escape(1.3D).fleePlayers(1.2D).wander(0.5D).add();
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
        String[] names = random.nextInt(3) == 0 ? VIRTUE_NAMES : INFERNAL_NAMES;
        this.setCustomName(Text.literal(names[random.nextInt(names.length)]));
        this.setPersistent();
        if (this.getHome() == null) {
            this.setHome(this.getBlockPos());
        }
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    // An Exile's stray fireball or blow doesn't hurt its own folk.
    @Override
    protected boolean isFriendlyFire(DamageSource source) {
        return source.getAttacker() instanceof TieflingEntity;
    }

    @Override
    protected void afterHurt(DamageSource source) {
        Entity attacker = source.getAttacker();
        if (attacker instanceof LivingEntity living && attacker == source.getSource() && isMelee(source)) {
            this.rebuke(living);
        }
    }

    /** Strike any Tiefling and the Exiles nearby turn on you. */
    @Override
    protected void callGuards(PlayerEntity player) {
        defend(this, player, TieflingExileEntity.class);
    }

    private static boolean isMelee(DamageSource source) {
        return source.isOf(DamageTypes.MOB_ATTACK) || source.isOf(DamageTypes.MOB_ATTACK_NO_AGGRO)
                || source.isOf(DamageTypes.PLAYER_ATTACK);
    }

    /** Hellish Rebuke: whoever struck this Tiefling in melee bursts into flame for {@link #REBUKE_FIRE_SECONDS} s. */
    void rebuke(LivingEntity attacker) {
        if (!attacker.isAlive() || attacker.isFireImmune()) {
            return;
        }
        attacker.setOnFireFor(REBUKE_FIRE_SECONDS);
        if (this.world instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.FLAME, attacker.getX(), attacker.getBodyY(0.5D), attacker.getZ(), 16,
                    attacker.getWidth() * 0.4D, attacker.getHeight() * 0.3D, attacker.getWidth() * 0.4D, 0.02D);
            world.playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(), SoundEvents.ITEM_FIRECHARGE_USE,
                    SoundCategory.HOSTILE, 0.8F, 0.9F);
        }
        mattonfire.dnd.classes.DnDClasses.LOGGER.info("[BrimstoneSanctum] {} rebuked {} (fire {} s)", this.getName().getString(),
                attacker.getName().getString(), REBUKE_FIRE_SECONDS);
    }

    // ---- Sounds: a villager's, lower and rougher ----

    @Override
    public float getSoundPitch() {
        return super.getSoundPitch() * 0.85F;
    }
}

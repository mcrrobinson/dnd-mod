package mattonfire.dnd.entity;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;
import org.joml.Vector3f;

/** The storm dragon of the mountain summits: fire breath and a lightning storm, guarding a dragon lair. */
public class LightningChaserEntity extends LairDragonEntity {
    /** Like a real bolt: 5 damage to anything within 3 blocks (and up to 6 above); half on a DEX save. */
    private static final float STRIKE_DAMAGE = 5.0F;
    private static final double STRIKE_RADIUS = 3.0;
    /** Seconds alight after a bolt (half on a successful DEX save). */
    private static final int STRIKE_BURN_SECONDS = 8;

    public static final RegistryKey<Structure> LAIR_STRUCTURE =
            RegistryKey.of(RegistryKeys.STRUCTURE, new Identifier(DnDClasses.MOD_ID, "dragon_lair"));

    public LightningChaserEntity(EntityType<? extends TameableEntity> entityType, World world) {
        super(entityType, world, BossBar.Color.YELLOW);
    }

    @Override
    protected FireBreath createBreath(DragonPart head, TrackedData<Integer> ticksKey, TrackedData<Vector3f> aimKey) {
        return new FireBreath(this, head, ticksKey, aimKey, 12.0, 4.0F);
    }

    @Override
    protected RegistryKey<Structure> lairStructure() {
        return LAIR_STRUCTURE;
    }

    // It calls the storm down itself, so its own lightning (and anyone else's) doesn't hurt it.
    @Override
    public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
    }

    /** Calls a storm down on the target: one bolt on it and two more close by. */
    @Override
    public void shoot(LivingEntity target) {
        if (this.world.isClient) {
            return;
        }
        this.strike(target.getX(), target.getY(), target.getZ());
        for (int i = 0; i < 2; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double distance = 1.5D + this.random.nextDouble() * STORM_SPREAD;
            this.strike(target.getX() + Math.cos(angle) * distance, target.getY(), target.getZ() + Math.sin(angle) * distance);
        }
    }

    /**
     * A cosmetic bolt plus damage the chaser deals itself: kills are credited to it, it scales with
     * difficulty like its other attacks, and it doesn't charge creepers, turn villagers into witches or burn
     * dropped items. Fire on the ground only with mobGriefing (and doFireTick, like real lightning).
     */
    private void strike(double x, double y, double z) {
        LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(this.world);
        if (lightning == null) {
            return;
        }
        lightning.refreshPositionAfterTeleport(x, y, z);
        lightning.setCosmetic(true);
        this.world.spawnEntity(lightning);

        DamageSource source = new DamageSource(this.world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE)
                .entryOf(DamageTypes.LIGHTNING_BOLT), this);
        Box area = new Box(x - STRIKE_RADIUS, y - STRIKE_RADIUS, z - STRIKE_RADIUS,
                x + STRIKE_RADIUS, y + 6.0D + STRIKE_RADIUS, z + STRIKE_RADIUS);
        for (LivingEntity victim : this.world.getEntitiesByClass(LivingEntity.class, area, this::isStormVictim)) {
            if (DragonSaves.isUnaffected(victim, source)) {
                continue;
            }
            // One DEX save for the whole storm: the three bolts land in the same tick
            SaveResult save = DragonSaves.save(victim, this, DragonSaves.LIGHTNING_STORM, DragonSaves.STORM_DC,
                    DragonSaves.LIGHTNING_STORM_EFFECT, "storm", DragonSaves.STORM_WINDOW);
            if (victim.damage(source, save.damage(STRIKE_DAMAGE))) {
                victim.setOnFireFor(save.duration(STRIKE_BURN_SECONDS));
            }
        }

        if (this.world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)
                && this.world.getGameRules().getBoolean(GameRules.DO_FIRE_TICK)) {
            BlockPos pos = BlockPos.ofFloored(x, y, z);
            BlockState fire = AbstractFireBlock.getState(this.world, pos);
            if (this.world.getBlockState(pos).isAir() && fire.canPlaceAt(this.world, pos)) {
                this.world.setBlockState(pos, fire);
            }
        }
    }
}

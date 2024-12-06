package mattonfire.dnd.classes;

import java.util.List;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.World.ExplosionSourceType;

public class CustomIceballEntity extends FireballEntity {
    private static final int explosionPower = 4; // Adjust explosion power (default is often 1 or 2)
    private static final int sphere_radius = 5;
    private static final float blast_damage = 2.F;
    private static final BlockState block_of_ice = Blocks.ICE.getDefaultState();
    private static final BlockState block_of_air = Blocks.AIR.getDefaultState();
    private static final BlockState block_of_bedrock = Blocks.BEDROCK.getDefaultState();
    private static final BlockState fire = Blocks.FIRE.getDefaultState();

    public CustomIceballEntity(EntityType<? extends FireballEntity> entityType, World world) {
        super(entityType, world);
    }

    public boolean validReplaceBlock(World world, BlockPos blockPosition) {
        BlockState blockState = world.getBlockState(blockPosition);
        if (blockState != block_of_air && blockState != block_of_bedrock) {
            return true;
        }
        return false;
    }

    public void setFireBlocks(World world, int rx, int blockPosY, int z, BlockState blockState, int radius) {
        if (world.getBlockState(new BlockPos(rx, blockPosY, z)).isAir()) {
            world.setBlockState(new BlockPos(rx, blockPosY, z), blockState);
        }
    }

    public void setIceBlocks(World world, int rx, int blockPosY, int z, BlockState blockState, int radius) {
        boolean belowBlock = false;
        BlockPos blockPos = new BlockPos(rx, blockPosY, z);
        for (int i = 0; i < radius; i++) {
            if (belowBlock != true) {
                blockPos = new BlockPos(rx, blockPosY - i, z);
                belowBlock = validReplaceBlock(world, blockPos);
            } else {
                world.setBlockState(blockPos, blockState);
                break;
            }
        }
    }

    public void setLightning(int posX, int posY, int posZ, World world) {

        // TODO: Not sure if this is the fix.
        LightningEntity lightningEntity = EntityType.LIGHTNING_BOLT.create(world);
        lightningEntity.setPos(posX, posY, posZ);
        world.spawnEntity(lightningEntity);
    }

    public void startStaffAction(BlockPos pos, World world, boolean entityPlacement) {
        int blockPosX = pos.getX();
        int blockPosZ = pos.getZ();
        int blockPosY = pos.getY();
        if (entityPlacement)
            blockPosY -= 1;

        System.out.println("Starting staff action at " + blockPosX + ", " + blockPosY + ", " + blockPosZ);

        int radius_sqr = sphere_radius * sphere_radius;
        for (int x = -sphere_radius; x <= sphere_radius; x++) {
            int hh = (int) Math.sqrt(radius_sqr - x * x);
            int rx = blockPosX + x;
            int ph = blockPosZ + hh;

            for (int z = blockPosZ - hh; z < ph; z++) {

                setIceBlocks(world, rx, blockPosY, z, block_of_ice, sphere_radius);

                // Retrieve entities in this block area
                List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class,
                        new Box(rx - 0.5, blockPosY, z - 0.5, rx + 0.5, blockPosY + 5, z + 0.5), // 5 blocks
                                                                                                 // above
                                                                                                 // base.
                        entity -> true); // Add filters here if necessary

                System.out.println(entities);
                for (LivingEntity entity : entities) {
                    entity.addStatusEffect(new StatusEffectInstance(ModEffects.FREEZE, 200));
                }
            }
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        super.onCollision(hitResult);

        System.out.println("Iceball collided with " + hitResult.getType());

        // Cause an explosion on impact
        if (!this.world.isClient) {
            world.createExplosion(this, this.getX(), this.getY(), this.getZ(), explosionPower,
                    ExplosionSourceType.NONE);

            startStaffAction(new BlockPos((int) this.getX(), (int) this.getY(), (int) this.getZ()), world, false);
            this.discard(); // Remove fireball after explosion
        }
    }
}

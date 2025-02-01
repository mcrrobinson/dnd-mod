package mattonfire.dnd.classes.Entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import mattonfire.dnd.classes.Registry.ModEffects;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import net.minecraft.world.World.ExplosionSourceType;

public class CustomIceballEntity extends FireballEntity {
    private static final int explosionPower = 4; // Adjust explosion power
    private static final int sphere_radius = 5;
    private static final BlockState block_of_ice = Blocks.ICE.getDefaultState();
    private static final BlockState block_of_air = Blocks.AIR.getDefaultState();
    private static final BlockState block_of_bedrock = Blocks.BEDROCK.getDefaultState();

    private boolean hasCollided = false;

    public double powerX;
    public double powerY;
    public double powerZ;

    // Map to store original block states for later restoration.
    private Map<BlockPos, BlockState> changedBlocks = new HashMap<>();
    // A counter to track when to revert blocks (20 ticks)
    private int revertTimer = -1; // -1 indicates no revert scheduled

    public CustomIceballEntity(EntityType<? extends FireballEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void tick() {
        if (this.revertTimer > 0) {
            this.revertTimer--;

            // Calculate the number of blocks to revert based on the remaining time.
            int totalBlocks = changedBlocks.size();
            int blocksToRevert = (int) Math.ceil(totalBlocks * Math.pow(1 - ((double) this.revertTimer / 100), 2));

            revertRandomBlocks(blocksToRevert);

            if (this.revertTimer == 0 || changedBlocks.isEmpty()) {
                System.out.println("Reverting blocks and discarding entity");
                this.discard();
                return;
            }
        }
        super.tick();
    }

    public void setIceBlocks(World world, int rx, int blockPosY, int z, BlockState blockState, int radius) {
        boolean belowBlock = false;
        BlockPos blockPos = new BlockPos(rx, blockPosY, z);
        for (int i = 0; i < radius; i++) {
            if (!belowBlock) {
                blockPos = new BlockPos(rx, blockPosY - i, z);
                BlockState currentBlockState = world.getBlockState(blockPos);
                belowBlock = currentBlockState != block_of_air && currentBlockState != block_of_bedrock;
            } else {
                // Store original block before replacing
                BlockState original = world.getBlockState(blockPos);
                changedBlocks.put(blockPos, original);
                world.setBlockState(blockPos, blockState);
                break;
            }
        }
    }

    public void setIceAction(BlockPos pos, World world, boolean entityPlacement) {
        int blockPosX = pos.getX();
        int blockPosZ = pos.getZ();
        int blockPosY = pos.getY();
        if (entityPlacement)
            blockPosY -= 1;

        int radius_sqr = sphere_radius * sphere_radius;
        for (int x = -sphere_radius; x <= sphere_radius; x++) {
            int hh = (int) Math.sqrt(radius_sqr - x * x);
            int rx = blockPosX + x;
            int ph = blockPosZ + hh;

            for (int z = blockPosZ - hh; z < ph; z++) {

                setIceBlocks(world, rx, blockPosY, z, block_of_ice, sphere_radius);

                // Retrieve entities in this block area and apply freeze effect
                List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class,
                        new Box(rx - 0.5, blockPosY, z - 0.5, rx + 0.5, blockPosY + 5, z + 0.5),
                        entity -> true);

                for (LivingEntity entity : entities) {
                    entity.addStatusEffect(new StatusEffectInstance(ModEffects.FREEZE, 200));
                }
            }
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        System.out.println("Collision detected");
        if (hasCollided)
            return;

        hasCollided = true;
        super.onCollision(hitResult);
        if (!this.world.isClient) {
            world.createExplosion(this, this.getX(), this.getY(), this.getZ(), explosionPower,
                    ExplosionSourceType.NONE);
            setIceAction(new BlockPos((int) this.getX(), (int) this.getY(), (int) this.getZ()), world, false);

            this.revertTimer = 120; // 20 ticks (1 second at 20 TPS)
        }
    }

    public void revertRandomBlocks(int blocksToRevert) {

        // TODO: I don't like this it doesn't seem efficient.
        Random random = new Random();
        List<Map.Entry<BlockPos, BlockState>> entries = new ArrayList<>(changedBlocks.entrySet());
        int count = 0;

        while (!entries.isEmpty() && count < blocksToRevert) {
            // Select a random index
            int randomIndex = random.nextInt(entries.size());
            Map.Entry<BlockPos, BlockState> entry = entries.get(randomIndex);

            // Check if the block is still ICE (to avoid overwriting changes made by
            // something else)
            if (this.world.getBlockState(entry.getKey()).getBlock() == Blocks.ICE) {
                this.world.setBlockState(entry.getKey(), entry.getValue());
            }

            // Remove the block from the list after processing
            changedBlocks.remove(entry.getKey()); // Update the original map
            entries.remove(randomIndex); // Update the local list
            count++;
        }
    }

    @Override
    protected void initDataTracker() {
    }
}

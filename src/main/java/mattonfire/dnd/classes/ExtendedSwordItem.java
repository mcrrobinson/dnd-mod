package mattonfire.dnd.classes;

import java.util.List;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class ExtendedSwordItem extends SwordItem {
    public static double reach = 0;
    private static LivingEntity liver = null;
    public static boolean active = false;
    public static BlockState block_of_ice = Blocks.ICE.getDefaultState();
    public static BlockState block_of_air = Blocks.AIR.getDefaultState();
    public static BlockState block_of_bedrock = Blocks.BEDROCK.getDefaultState();
    public static BlockState block_of_magma = Blocks.MAGMA_BLOCK.getDefaultState();
    public static BlockState fire = Blocks.FIRE.getDefaultState();
    public int sphere_radius = 5;
    public float blast_damage = 2.F;

    public ExtendedSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, 10, 1.2F, settings);
        reach = 40;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (active != (((LivingEntity) entity).getStackInHand(Hand.MAIN_HAND).getItem() == this)) {
            System.out.println("New enitiy reaching: " + entity);
            liver = (LivingEntity) entity;
            // SetRange(((LivingEntity) entity).getStackInHand(Hand.MAIN_HAND).getItem() ==
            // this);
        }
        active = ((LivingEntity) entity).getStackInHand(Hand.MAIN_HAND).getItem() == this;
    }

    public void SetRange(boolean change_range) {
        if (change_range) {
            System.out.println("On");
            liver.getAttributeInstance(ReachEntityAttributes.REACH)
                    .setBaseValue(reach);
        } else {
            System.out.println("Off");
            liver.getAttributeInstance(ReachEntityAttributes.REACH).setBaseValue(0.0);
        }
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
        if (!world.isClient) {
            int blockPosX = pos.getX();
            int blockPosZ = pos.getZ();
            int blockPosY = pos.getY();
            if (entityPlacement)
                blockPosY -= 1;

            world.createExplosion((Entity) null, blockPosX, blockPosY + 2, blockPosZ, blast_damage,
                    World.ExplosionSourceType.NONE);
            if (this.toString() == "staff_of_lightning") {
                setLightning(blockPosX, blockPosY, blockPosZ, world);
            } else if (this.toString() == "staff_of_ice" || this.toString() == "staff_of_fire") {
                int radius_sqr = sphere_radius * sphere_radius;
                for (int x = -sphere_radius; x <= sphere_radius; x++) {
                    int hh = (int) Math.sqrt(radius_sqr - x * x);
                    int rx = blockPosX + x;
                    int ph = blockPosZ + hh;

                    for (int z = blockPosZ - hh; z < ph; z++) {
                        if (this.toString() == "staff_of_fire") {
                            setFireBlocks(world, rx, blockPosY + 1, z, fire, sphere_radius);

                        } else {
                            setIceBlocks(world, rx, blockPosY, z, block_of_ice, sphere_radius);

                            // Retrieve entities in this block area
                            List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class,
                                    new Box(rx - 0.5, blockPosY, z - 0.5, rx + 0.5, blockPosY + 1, z + 0.5),
                                    entity -> true); // Add filters here if necessary

                            System.out.println(entities);
                            for (LivingEntity entity : entities) {
                                entity.addStatusEffect(new StatusEffectInstance(DnDClasses.FREEZE_EFFECT, 200));
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity playerEntity, Hand hand) {
        MinecraftClient client = MinecraftClient.getInstance();
        HitResult hit = client.crosshairTarget;
        HitResult.Type blockType = hit.getType();
        switch (blockType) {
            case BLOCK:
                startStaffAction(((BlockHitResult) hit).getBlockPos(), world, false);
                break;
            case ENTITY:
                startStaffAction(((EntityHitResult) hit).getEntity().getBlockPos(), world, true);
                break;

            default:
                break;
        }
        playerEntity.getItemCooldownManager().set(this, 50);
        return new TypedActionResult<ItemStack>(ActionResult.SUCCESS, playerEntity.getStackInHand(hand));
    }
}

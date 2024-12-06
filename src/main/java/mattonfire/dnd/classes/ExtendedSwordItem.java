package mattonfire.dnd.classes;

import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class ExtendedSwordItem extends SwordItem {

    public ExtendedSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, 10, 1.2F, settings);
    }

    private void spawnFireball(World world, Vec3d position, Vec3d direction) {
        // Create and configure the fireball entity
        CustomFireballEntity fireball = new CustomFireballEntity(EntityType.FIREBALL, world);
        fireball.setPosition(position.x, position.y, position.z);
        fireball.setVelocity(direction.x, direction.y, direction.z, 6.0F, 0.25F);

        // Add the fireball to the world
        world.spawnEntity(fireball);
    }

    private void spawnLightningBall(World world, Vec3d position, Vec3d direction) {
        // Create and configure the fireball entity
        CustomLightningEntity fireball = new CustomLightningEntity(EntityType.FIREBALL, world);
        fireball.setPosition(position.x, position.y, position.z);
        fireball.setVelocity(direction.x, direction.y, direction.z, 6.0F, 0.25F);

        // Add the fireball to the world
        world.spawnEntity(fireball);
    }

    private void spawnIceball(World world, Vec3d position, Vec3d direction) {
        // Create and configure the fireball entity
        CustomIceballEntity fireball = new CustomIceballEntity(EntityType.FIREBALL, world);
        fireball.setPosition(position.x, position.y, position.z);
        fireball.setVelocity(direction.x, direction.y, direction.z, 6.0F, 0.25F);

        // Add the fireball to the world
        world.spawnEntity(fireball);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity playerEntity, Hand hand) {
        switch (this.toString()) {
            case "staff_of_fire":
                spawnFireball(world, playerEntity.getPos(), playerEntity.getRotationVector());
                break;
            case "staff_of_lightning":
                spawnLightningBall(world, playerEntity.getPos(), playerEntity.getRotationVector());
                break;
            case "staff_of_ice":
                spawnIceball(world, playerEntity.getPos(), playerEntity.getRotationVector());
                break;
            default:
                System.out.println("No action for item: " + this.toString());
                break;
        }
        return new TypedActionResult<ItemStack>(ActionResult.SUCCESS, playerEntity.getStackInHand(hand));
    }
}

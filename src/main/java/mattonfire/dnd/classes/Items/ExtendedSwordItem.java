package mattonfire.dnd.classes.Items;

import mattonfire.dnd.classes.Entities.CustomFireballEntity;
import mattonfire.dnd.classes.Entities.CustomIceballEntity;
import mattonfire.dnd.classes.Entities.CustomLightningEntity;
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

    private void spawnFireball(World world, PlayerEntity player) {
        // Create and configure the fireball entity
        CustomFireballEntity fireball = new CustomFireballEntity(EntityType.FIREBALL, world);

        // Set the position and velocity of the fireball
        Vec3d position = player.getPos().add(0, 1, 0);
        Vec3d direction = player.getRotationVector().normalize();

        fireball.setPosition(position.x, position.y, position.z);
        fireball.setVelocity(direction.x, direction.y, direction.z, 3.0F, 0);

        // Add the fireball to the world
        world.spawnEntity(fireball);
    }

    private void spawnLightningBall(World world, PlayerEntity player) {
        // Create and configure the fireball entity
        CustomLightningEntity fireball = new CustomLightningEntity(EntityType.FIREBALL, world);

        // Set the position and velocity of the fireball
        Vec3d position = player.getPos().add(0, 1, 0);
        Vec3d direction = player.getRotationVector().normalize();

        fireball.setPosition(position.x, position.y, position.z);
        fireball.setVelocity(direction.x, direction.y, direction.z, 3.0F, 0);

        // Add the fireball to the world
        world.spawnEntity(fireball);
    }

    private void spawnIceball(World world, PlayerEntity player) {

        // Create the fireball entity
        CustomIceballEntity fireball = new CustomIceballEntity(EntityType.FIREBALL, world);

        // Set the position and velocity of the fireball
        Vec3d position = player.getPos().add(0, 1, 0);
        Vec3d direction = player.getRotationVector().normalize();

        fireball.setPosition(position.x, position.y, position.z);
        fireball.setVelocity(direction.x, direction.y, direction.z, 3.0F, 0);

        // Add the fireball to the world
        if (!world.isClient) {
            world.spawnEntity(fireball);
        }
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity playerEntity, Hand hand) {
        switch (this.toString()) {
            case "staff_of_fire":
                spawnFireball(world, playerEntity);
                break;
            case "staff_of_lightning":
                spawnLightningBall(world, playerEntity);
                break;
            case "staff_of_ice":
                spawnIceball(world, playerEntity);
                break;
            default:
                System.out.println("No action for item: " + this.toString());
                break;
        }
        return new TypedActionResult<ItemStack>(ActionResult.SUCCESS, playerEntity.getStackInHand(hand));
    }
}

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
        CustomIceballEntity fireball = new CustomIceballEntity(EntityType.FIREBALL, world);
        fireball.setPosition(position.x, position.y, position.z);
        fireball.setVelocity(direction.x, direction.y, direction.z, 6.0F, 0.25F);

        // Add the fireball to the world
        world.spawnEntity(fireball);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity playerEntity, Hand hand) {
        // MinecraftClient client = MinecraftClient.getInstance();

        spawnFireball(world, playerEntity.getPos(), playerEntity.getRotationVector());
        // HitResult hit = client.crosshairTarget;
        // HitResult.Type blockType = hit.getType();
        // switch (blockType) {
        // case BLOCK:
        // startStaffAction(((BlockHitResult) hit).getBlockPos(), world, false);
        // break;
        // case ENTITY:
        // startStaffAction(((EntityHitResult) hit).getEntity().getBlockPos(), world,
        // true);
        // break;

        // default:
        // break;
        // }
        // playerEntity.getItemCooldownManager().set(this, 50);
        return new TypedActionResult<ItemStack>(ActionResult.SUCCESS, playerEntity.getStackInHand(hand));
    }
}

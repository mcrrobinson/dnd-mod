package mattonfire.dnd.classes.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import mattonfire.dnd.classes.Registry.ModEffects;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class ExtendedSwordItem extends SwordItem {

    static int BEAM_RANGE = 40; // Range of the beam

    private static final List<ScheduledBlockRestore> scheduledRestores = new ArrayList<>();

    static {
        // Updated server tick handler
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Iterator<ScheduledBlockRestore> it = scheduledRestores.iterator();
            while (it.hasNext()) {
                ScheduledBlockRestore task = it.next();
                task.ticks--;

                // Start restoring blocks when time is up, or restore continuously
                if (task.ticks <= 0) {
                    boolean isComplete = task.restore(server);
                    if (isComplete) {
                        it.remove();
                    }
                }
            }
        });
    }

    public ExtendedSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, 10, 1.2F, settings);
    }

    private void scheduleBlockRestoration(ServerWorld world, Map<BlockPos, BlockState> blocks, int ticks) {
        scheduledRestores.add(new ScheduledBlockRestore(world, blocks, ticks));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {

        if (!world.isClient) {
            ServerWorld serverWorld = (ServerWorld) world;

            Vec3d start = player.getEyePos();
            Vec3d direction = player.getRotationVec(1.0F);
            Vec3d end = start.add(direction.multiply(BEAM_RANGE)); // Beam range

            ParticleEffect particleType = switch (this.toString()) {
                case "staff_of_fire" -> ParticleTypes.FLAME;
                case "staff_of_lightning" -> ParticleTypes.EXPLOSION;
                case "staff_of_ice" -> ParticleTypes.SNOWFLAKE;
                default -> null;
            };

            if (particleType != null) {
                for (int i = 0; i < BEAM_RANGE; i++) {
                    Vec3d point = start.add(direction.multiply(i));
                    serverWorld.spawnParticles(particleType, point.x, point.y, point.z, 1, 0, 0, 0, 0);
                }
            }

            // Block raycast
            BlockHitResult blockHit = world.raycast(new RaycastContext(
                    start, end,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    player));

            if (blockHit.getType() == HitResult.Type.MISS) {
                return TypedActionResult.fail(player.getStackInHand(hand));
            }

            BlockPos hitPos = blockHit.getBlockPos();

            // Save surrounding blocks before explosion
            Map<BlockPos, BlockState> savedBlocks = new HashMap<>();
            BlockPos.iterateOutwards(hitPos, 3, 3, 3).forEach(pos -> {

                BlockState blockState = serverWorld.getBlockState(pos);
                float hardness = blockState.getHardness(serverWorld, pos);

                // Ignores air and bedrock blocks
                if (blockState.getMaterial().isReplaceable() || hardness < 0 || hardness > 49.f) // Ignore bedrock and
                                                                                                 // obsidian
                    return;

                savedBlocks.put(pos.toImmutable(), blockState);
            });

            if (this.toString().equals("staff_of_ice")) {
                Box totalBox = new Box(hitPos).expand(3, 5, 3); // Encompasses entire area
                List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, totalBox,
                        LivingEntity::isAttackable);

                for (LivingEntity entity : entities) {
                    entity.addStatusEffect(new StatusEffectInstance(ModEffects.FREEZE, 200));
                }
            }

            int delayTicks = 100;

            switch (this.toString()) {
                case "staff_of_fire":
                    serverWorld.createExplosion(player, hitPos.getX(), hitPos.getY(), hitPos.getZ(),
                            3.0F, true, World.ExplosionSourceType.TNT);

                    // Schedule restoration after X ticks (e.g., 100 = 5 seconds)
                    scheduleBlockRestoration(serverWorld, savedBlocks, delayTicks);
                    break;

                case "staff_of_lightning":
                    serverWorld.createExplosion(player, hitPos.getX(), hitPos.getY(), hitPos.getZ(),
                            3.0F, false, World.ExplosionSourceType.NONE);

                    List<BlockPos> keyList = new ArrayList<>(savedBlocks.keySet()); // Convert to list to get
                                                                                    // indexed access
                    IntStream.range(0, keyList.size())
                            .filter(i -> i % 3 == 0)
                            .mapToObj(keyList::get)
                            .forEach(pos -> {
                                LightningEntity lightningEntity = EntityType.LIGHTNING_BOLT.create(world);
                                lightningEntity.setPos(pos.getX(), pos.getY(), pos.getZ());
                                serverWorld.spawnEntity(lightningEntity);
                            });

                    break;

                case "staff_of_ice":
                    // Change to ice blocks
                    serverWorld.createExplosion(player, hitPos.getX(), hitPos.getY(), hitPos.getZ(),
                            3.0F, false, World.ExplosionSourceType.NONE);

                    BlockState iceState = Blocks.ICE.getDefaultState();
                    for (BlockPos pos : savedBlocks.keySet()) {
                        serverWorld.setBlockState(pos, iceState);
                    }

                    // Schedule restoration after X ticks (e.g., 100 = 5 seconds)
                    scheduleBlockRestoration(serverWorld, savedBlocks, delayTicks);
                    break;
                default:
                    break;
            }
        }
        return TypedActionResult.success(player.getStackInHand(hand));
    }
}

// TODO: Whats most efficient?
// int blockPosX = pos.getX();
// int blockPosZ = pos.getZ();
// int blockPosY = pos.getY();
// if (entityPlacement)
// blockPosY -= 1;

// int radius_sqr = sphere_radius * sphere_radius;
// for (int x = -sphere_radius; x <= sphere_radius; x++) {
// int hh = (int) Math.sqrt(radius_sqr - x * x);
// int rx = blockPosX + x;
// int ph = blockPosZ + hh;

// for (int z = blockPosZ - hh; z < ph; z++) {

// setIceBlocks(world, rx, blockPosY, z, block_of_ice, sphere_radius);

// // Retrieve entities in this block area and apply freeze effect
// List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class,
// new Box(rx - 0.5, blockPosY, z - 0.5, rx + 0.5, blockPosY + 5, z + 0.5),
// LivingEntity::isAttackable);

// for (LivingEntity entity : entities) {
// entity.addStatusEffect(new StatusEffectInstance(ModEffects.FREEZE, 200));
// }
// }
// }

// Map<BlockPos, BlockState> savedBlocks = new HashMap<>();
// BlockPos.iterateOutwards(hitPos, 3, 3, 3).forEach(pos -> {
// if (serverWorld.getBlockState(pos).getMaterial().isReplaceable())
// return;
// savedBlocks.put(pos.toImmutable(), serverWorld.getBlockState(pos));
// });
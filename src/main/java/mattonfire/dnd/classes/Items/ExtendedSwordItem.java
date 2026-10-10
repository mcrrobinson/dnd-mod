package mattonfire.dnd.classes.Items;

import mattonfire.dnd.classes.Progression.Classes.WizardSkills;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Registry.ModEffects;
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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public class ExtendedSwordItem extends SwordItem {

    static int BEAM_RANGE = 40; // Range of the beam

    /** Cooldown after a cast, in ticks. */
    static final int CAST_COOLDOWN_TICKS = 20;
    /** Most lightning bolts one Staff of Lightning cast calls down. */
    static final int MAX_LIGHTNING_BOLTS = 5;
    /** Restored blocks come back over this window, after the rest of the 5 seconds. */
    static final int RESTORE_TICKS = 20;
    static final int RESTORE_DELAY_TICKS = 100 - RESTORE_TICKS;

    static {
        ScheduledBlockRestore.register();
    }

    public ExtendedSwordItem(ToolMaterial toolMaterial, Settings settings) {
        super(toolMaterial, 10, 1.2F, settings);
    }

    /** Only Wizards can wield the elemental staffs (cast with them or hit with them). */
    public static boolean canWield(PlayerEntity player) {
        return Progression.classOf(player) == DndCharacter.WIZARD;
    }

    /** Tells a non-Wizard they can't use the staff. */
    public static void sendCantWield(PlayerEntity player) {
        player.sendMessage(Text.literal("Only Wizards can wield elemental staffs!").formatted(Formatting.RED), true);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        if (!canWield(player)) {
            if (!world.isClient) {
                sendCantWield(player);
            }
            return TypedActionResult.fail(player.getStackInHand(hand));
        }


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

            // Blocks round the impact the staff may change: solid, breakable (no bedrock or
            // obsidian), and no block entities, so chests and their contents are never touched.
            Map<BlockPos, BlockState> savedBlocks = new HashMap<>();
            BlockPos.iterateOutwards(hitPos, 3, 3, 3).forEach(pos -> {
                BlockState blockState = serverWorld.getBlockState(pos);
                float hardness = blockState.getHardness(serverWorld, pos);
                if (blockState.getMaterial().isReplaceable() || hardness < 0 || hardness > 49.f
                        || blockState.hasBlockEntity())
                    return;
                savedBlocks.put(pos.toImmutable(), blockState);
            });

            if (this.toString().equals("staff_of_ice")) {
                Box totalBox = new Box(hitPos).expand(3, 5, 3); // Encompasses entire area
                // Sculpt Spells: an Evocation Wizard's freeze spares their party and pets
                boolean sculpt = WizardSkills.isEvoker(player);
                List<LivingEntity> entities = world.getEntitiesByClass(LivingEntity.class, totalBox,
                        e -> e.isAttackable() && !(sculpt && WizardSkills.spares(player, e)));

                for (LivingEntity entity : entities) {
                    entity.addStatusEffect(new StatusEffectInstance(ModEffects.FREEZE, 200));
                }
            }

            // None of the blasts break blocks themselves (a block-breaking explosion drops
            // the blocks as items, and restoring them afterwards duplicated them). The Fire
            // staff carves its own crater instead, with no drops, and puts it back later.
            switch (this.toString()) {
                case "staff_of_fire": {
                    Map<BlockPos, BlockState> crater = new HashMap<>();
                    savedBlocks.forEach((pos, state) -> {
                        if (pos.getSquaredDistance(hitPos) <= 9.0)
                            crater.put(pos, state);
                    });
                    BlockState air = Blocks.AIR.getDefaultState();
                    crater.keySet().forEach(pos -> serverWorld.setBlockState(pos, air, ScheduledBlockRestore.FLAGS));
                    serverWorld.createExplosion(player, hitPos.getX() + 0.5, hitPos.getY() + 0.5, hitPos.getZ() + 0.5,
                            3.0F, true, World.ExplosionSourceType.NONE);
                    ScheduledBlockRestore.schedule(serverWorld, crater, air, RESTORE_DELAY_TICKS, RESTORE_TICKS);
                    break;
                }

                case "staff_of_lightning": {
                    serverWorld.createExplosion(player, hitPos.getX(), hitPos.getY(), hitPos.getZ(),
                            3.0F, false, World.ExplosionSourceType.NONE);

                    // A few bolts on exposed blocks round the impact (it used to be every third
                    // block in the area, up to ~114 bolts a click).
                    List<BlockPos> targets = new ArrayList<>();
                    savedBlocks.keySet().forEach(pos -> {
                        if (serverWorld.getBlockState(pos.up()).getMaterial().isReplaceable())
                            targets.add(pos.up());
                    });
                    // Sculpt Spells: no bolts within 3 blocks of an Evocation Wizard's party members or pets
                    if (WizardSkills.isEvoker(player)) {
                        List<LivingEntity> spared = world.getEntitiesByClass(LivingEntity.class,
                                new Box(hitPos).expand(8), e -> WizardSkills.spares(player, e));
                        targets.removeIf(pos -> spared.stream()
                                .anyMatch(e -> e.squaredDistanceTo(Vec3d.ofBottomCenter(pos)) < 9.0));
                    }
                    Collections.shuffle(targets);
                    for (BlockPos pos : targets.subList(0, Math.min(MAX_LIGHTNING_BOLTS, targets.size()))) {
                        LightningEntity lightningEntity = EntityType.LIGHTNING_BOLT.create(world);
                        if (lightningEntity == null)
                            continue;
                        lightningEntity.refreshPositionAfterTeleport(Vec3d.ofBottomCenter(pos));
                        if (player instanceof ServerPlayerEntity serverPlayer)
                            lightningEntity.setChanneler(serverPlayer);
                        serverWorld.spawnEntity(lightningEntity);
                    }
                    break;
                }

                case "staff_of_ice": {
                    serverWorld.createExplosion(player, hitPos.getX(), hitPos.getY(), hitPos.getZ(),
                            3.0F, false, World.ExplosionSourceType.NONE);

                    BlockState iceState = Blocks.ICE.getDefaultState();
                    for (BlockPos pos : savedBlocks.keySet()) {
                        serverWorld.setBlockState(pos, iceState, ScheduledBlockRestore.FLAGS);
                    }
                    ScheduledBlockRestore.schedule(serverWorld, savedBlocks, iceState, RESTORE_DELAY_TICKS, RESTORE_TICKS);
                    break;
                }
                default:
                    break;
            }
            player.getItemCooldownManager().set(this, CAST_COOLDOWN_TICKS);
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
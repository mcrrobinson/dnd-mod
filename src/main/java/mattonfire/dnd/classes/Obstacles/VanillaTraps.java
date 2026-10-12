package mattonfire.dnd.classes.Obstacles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Party.Party;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.SkillChecks.Eligibility;
import mattonfire.dnd.classes.SkillChecks.PerceptionService;
import mattonfire.dnd.classes.SkillChecks.SkillCheck;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.PersistentState;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.World;

/**
 * The vanilla temple traps, treated as hidden traps: the desert pyramid's pressure plate over TNT and the
 * jungle temple's tripwires and arrow dispensers. No new blocks: a mixin on each temple piece's
 * {@code generate} records the trigger blocks (plates, tripwire hooks and string) and the payload blocks (TNT,
 * dispensers) it placed ({@link #recordGenerated}), so only temples generated after this change count.
 *
 * <ul>
 * <li><b>Spotting</b>, every {@value #SPOT_INTERVAL} ticks: within {@value #SPOT_RANGE} blocks of a trigger,
 * Rogues and Rangers always see it, anyone else if their passive Perception meets DC {@value #DC}, and
 * everyone whose {@code /party} has a member who has spotted it. A spotted trap's triggers are outlined in
 * orange for that player only.</li>
 * <li><b>Disarming:</b> a Rogue (or, as a secondary, an Artificer) sneak + right-clicks a trigger with an empty
 * hand and rolls Thieves' Tools (DC {@value #DC}, "Disarm" on the HUD). Success removes the payload (the TNT
 * becomes sandstone; the dispensers become mossy cobblestone and the string comes away), drops a redstone
 * dust (plate) or 2 arrows (tripwire), and earns Medium class XP once. Failure: wait and try again. A natural
 * 1 sets it off: the TNT is lit, or every dispenser looses an arrow at you.</li>
 * </ul>
 * Kept per dimension as a {@link PersistentState}; who has spotted what is in memory only.
 */
public final class VanillaTraps extends PersistentState {
    private static final String ID = "dndclasses_vanilla_traps";
    public static final int DC = Tier.MEDIUM.dc;
    public static final double SPOT_RANGE = 6.0;
    public static final int SPOT_INTERVAL = 10;
    public static final int FAIL_COOLDOWN_TICKS = 30;
    public static final int FUMBLE_COOLDOWN_TICKS = 100;
    public static final int TNT_FUSE = 40;
    public static final int TRIPWIRE_ARROWS = 2;
    /** How far below a temple piece's box its trap blocks can be (the desert pyramid's plate is 11 down). */
    private static final int BASEMENT_DEPTH = 16;
    private static final int SCAN_MARGIN = 1;
    private static final DustParticleEffect OUTLINE = new DustParticleEffect(new Vector3f(1.0f, 0.45f, 0.1f), 0.9f);

    public enum Kind {
        PRESSURE_PLATE("pressure_plate"), TRIPWIRE("tripwire");

        public final String id;

        Kind(String id) {
            this.id = id;
        }

        public String translationKey() {
            return "trap.dndclasses." + id;
        }

        static Kind byId(String id) {
            return "tripwire".equals(id) ? TRIPWIRE : PRESSURE_PLATE;
        }
    }

    /** One temple's traps. Triggers and payload are packed block positions. */
    public static final class Trap {
        public final long origin;
        public final Kind kind;
        final LongList triggers = new LongArrayList();
        final LongList payload = new LongArrayList();
        boolean disarmed;
        /** Who has spotted it (not saved). */
        final Set<UUID> spotted = new HashSet<>();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        Trap(long origin, Kind kind) {
            this.origin = origin;
            this.kind = kind;
        }

        public boolean disarmed() {
            return disarmed;
        }

        public List<BlockPos> triggers() {
            List<BlockPos> list = new ArrayList<>();
            for (long packed : triggers) {
                list.add(BlockPos.fromLong(packed));
            }
            return list;
        }

        void grow(BlockPos pos) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        boolean near(Vec3d at, double range) {
            return at.x >= minX - range && at.x <= maxX + 1 + range && at.y >= minY - range
                    && at.y <= maxY + 1 + range && at.z >= minZ - range && at.z <= maxZ + 1 + range;
        }
    }

    private final Map<Long, Trap> byOrigin = new LinkedHashMap<>();
    /** Trigger position to its trap's origin. */
    private final Long2LongOpenHashMap byTrigger = new Long2LongOpenHashMap();
    private static final Map<UUID, Long> RETRY_AT = new HashMap<>();

    public static VanillaTraps get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(VanillaTraps::fromNbt, VanillaTraps::new, ID);
    }

    public static void forget(UUID player) {
        RETRY_AT.remove(player);
    }

    static void register() {
        ServerTickEvents.END_WORLD_TICK.register(VanillaTraps::tick);
        UseBlockCallback.EVENT.register(VanillaTraps::onUse);
    }

    public List<Trap> all() {
        return new ArrayList<>(byOrigin.values());
    }

    @Nullable
    public Trap at(BlockPos trigger) {
        long key = trigger.asLong();
        return byTrigger.containsKey(key) ? byOrigin.get(byTrigger.get(key)) : null;
    }

    // ---- Generation ----

    /**
     * Called at the end of a temple piece's {@code generate} for one chunk: records the trap blocks the piece
     * placed inside {@code chunkBox}. Safe off the server thread.
     */
    public static void recordGenerated(StructureWorldAccess world, BlockBox pieceBox, BlockBox chunkBox, Kind kind) {
        // The desert pyramid's treasure room is built below its own bounding box
        int minX = Math.max(pieceBox.getMinX() - SCAN_MARGIN, chunkBox.getMinX());
        int minY = Math.max(pieceBox.getMinY() - BASEMENT_DEPTH, chunkBox.getMinY());
        int minZ = Math.max(pieceBox.getMinZ() - SCAN_MARGIN, chunkBox.getMinZ());
        int maxX = Math.min(pieceBox.getMaxX() + SCAN_MARGIN, chunkBox.getMaxX());
        int maxY = Math.min(pieceBox.getMaxY(), chunkBox.getMaxY());
        int maxZ = Math.min(pieceBox.getMaxZ() + SCAN_MARGIN, chunkBox.getMaxZ());
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            return;
        }
        LongList triggers = new LongArrayList();
        LongList payload = new LongArrayList();
        for (BlockPos pos : BlockPos.iterate(minX, minY, minZ, maxX, maxY, maxZ)) {
            BlockState state = world.getBlockState(pos);
            if (kind == Kind.PRESSURE_PLATE) {
                if (state.isOf(Blocks.STONE_PRESSURE_PLATE)) {
                    triggers.add(pos.asLong());
                } else if (state.isOf(Blocks.TNT)) {
                    payload.add(pos.asLong());
                }
            } else if (state.isOf(Blocks.TRIPWIRE_HOOK) || state.isOf(Blocks.TRIPWIRE)) {
                triggers.add(pos.asLong());
            } else if (state.isOf(Blocks.DISPENSER)) {
                payload.add(pos.asLong());
            }
        }
        if (triggers.isEmpty() && payload.isEmpty()) {
            return;
        }
        ServerWorld serverWorld = world.toServerWorld();
        MinecraftServer server = serverWorld.getServer();
        long origin = BlockPos.asLong(pieceBox.getMinX(), pieceBox.getMinY(), pieceBox.getMinZ());
        Runnable add = () -> get(serverWorld).add(origin, kind, triggers, payload);
        if (server.isOnThread()) {
            add.run();
        } else {
            server.execute(add);
        }
    }

    void add(long origin, Kind kind, LongList triggers, LongList payload) {
        Trap trap = byOrigin.computeIfAbsent(origin, o -> new Trap(o, kind));
        for (long packed : triggers) {
            if (!trap.triggers.contains(packed)) {
                trap.triggers.add(packed);
                trap.grow(BlockPos.fromLong(packed));
                byTrigger.put(packed, origin);
            }
        }
        for (long packed : payload) {
            if (!trap.payload.contains(packed)) {
                trap.payload.add(packed);
            }
        }
        DnDClasses.LOGGER.info("[Trap] Registered {} temple trap at {}: {} triggers, {} payload blocks so far",
                kind.id, BlockPos.fromLong(origin).toShortString(), trap.triggers.size(), trap.payload.size());
        markDirty();
    }

    // ---- Spotting ----

    private static void tick(ServerWorld world) {
        if (world.getTime() % SPOT_INTERVAL != 0) {
            return;
        }
        VanillaTraps traps = get(world);
        if (traps.byOrigin.isEmpty()) {
            return;
        }
        for (ServerPlayerEntity player : world.getPlayers(p -> p.isAlive() && !p.isSpectator())) {
            Vec3d at = player.getPos();
            for (Trap trap : traps.byOrigin.values()) {
                if (trap.disarmed || !trap.near(at, SPOT_RANGE)) {
                    continue;
                }
                List<BlockPos> close = new ArrayList<>();
                for (BlockPos trigger : trap.triggers()) {
                    if (player.squaredDistanceTo(Vec3d.ofCenter(trigger)) <= SPOT_RANGE * SPOT_RANGE) {
                        close.add(trigger);
                    }
                }
                if (close.isEmpty() || !spots(player, trap)) {
                    continue;
                }
                if (trap.spotted.add(player.getUuid())) {
                    player.sendMessage(Text.translatable("trap.dndclasses.spotted",
                            Text.translatable(trap.kind.translationKey())), true);
                    DnDClasses.LOGGER.info("[Trap] {} spots the {} temple trap at {}", player.getEntityName(),
                            trap.kind.id, close.get(0).toShortString());
                }
                for (BlockPos trigger : close) {
                    outline(world, player, trigger, world.getBlockState(trigger));
                }
            }
        }
    }

    /** Rogues and Rangers, a sharp enough passive Perception, or a party member who has spotted it. */
    private static boolean spots(ServerPlayerEntity player, Trap trap) {
        if (trap.spotted.contains(player.getUuid())) {
            return true;
        }
        DndCharacter dndClass = D20.classOf(player);
        if (dndClass == DndCharacter.ROGUE || dndClass == DndCharacter.RANGER
                || PerceptionService.noticesPassively(player, Skill.PERCEPTION, DC)) {
            return true;
        }
        Party party = PartyManager.get(player.getServer()).getParty(player.getUuid());
        if (party != null) {
            for (UUID member : trap.spotted) {
                if (party.contains(member)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void outline(ServerWorld world, ServerPlayerEntity player, BlockPos tile, BlockState state) {
        double y = tile.getY() + (state.isOf(Blocks.TRIPWIRE_HOOK) ? 0.5 : 0.12);
        for (int i = 0; i < 4; i++) {
            double t = i / 4.0;
            dust(world, player, tile.getX() + t, y, tile.getZ());
            dust(world, player, tile.getX() + 1, y, tile.getZ() + t);
            dust(world, player, tile.getX() + 1 - t, y, tile.getZ() + 1);
            dust(world, player, tile.getX(), y, tile.getZ() + 1 - t);
        }
    }

    private static void dust(ServerWorld world, ServerPlayerEntity player, double x, double y, double z) {
        world.spawnParticles(player, OUTLINE, true, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    // ---- Disarming ----

    /** Rogues are its primary disarmers, Artificers its secondary ones. */
    public static Eligibility eligibility(DndCharacter dndClass) {
        return dndClass == DndCharacter.ROGUE ? Eligibility.PRIMARY
                : dndClass == DndCharacter.ARTIFICER ? Eligibility.SECONDARY : Eligibility.NONE;
    }

    private static ActionResult onUse(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND || !player.isSneaking() || player.isSpectator()
                || !player.getMainHandStack().isEmpty() || !(world instanceof ServerWorld serverWorld)
                || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }
        VanillaTraps traps = get(serverWorld);
        Trap trap = traps.at(hit.getBlockPos());
        if (trap == null) {
            return ActionResult.PASS;
        }
        Text name = Text.translatable(trap.kind.translationKey());
        if (trap.disarmed || !traps.armed(serverWorld, trap)) {
            player.sendMessage(Text.translatable(trap.disarmed ? "trap.dndclasses.already_disarmed"
                    : "trap.dndclasses.already_spent"), true);
            return ActionResult.SUCCESS;
        }
        if (!eligibility(D20.classOf(player)).canTry() && !player.isCreative()) {
            if (spots(serverPlayer, trap)) {
                player.sendMessage(Text.translatable("trap.dndclasses.rogue_only"), true);
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        }
        long now = world.getTime();
        Long retryAt = RETRY_AT.get(player.getUuid());
        if (retryAt != null && now < retryAt) {
            player.sendMessage(Text.translatable("skill.dndclasses.lockpicking.steady"), true);
            return ActionResult.SUCCESS;
        }
        D20.Roll roll = SkillCheck.builder(player, Skill.THIEVES_TOOLS, DC, "trap").label(D20.DISARM).roll();
        DnDClasses.LOGGER.info("[Trap] {} disarms the {} temple trap at {}: {} + {} = {} vs DC {} -> {}",
                player.getEntityName(), trap.kind.id, hit.getBlockPos().toShortString(), roll.natural(),
                roll.modifier(), roll.total(), DC, roll.outcome());
        if (roll.outcome().succeeded()) {
            RETRY_AT.remove(player.getUuid());
            D20.show(serverPlayer, roll, Text.translatable(roll.outcome() == D20.Outcome.CRITICAL
                    ? "skill.dndclasses.disarm.critical" : "skill.dndclasses.disarm.success", name));
            traps.disarm(serverWorld, trap, serverPlayer);
            return ActionResult.SUCCESS;
        }
        boolean fumble = roll.outcome() == D20.Outcome.FUMBLE;
        RETRY_AT.put(player.getUuid(), now + (fumble ? FUMBLE_COOLDOWN_TICKS : FAIL_COOLDOWN_TICKS));
        if (fumble) {
            D20.show(serverPlayer, roll, Text.translatable("skill.dndclasses.disarm.failure", name));
            traps.setOff(serverWorld, trap, serverPlayer);
        } else {
            D20.show(serverPlayer, roll, Text.translatable("trap.dndclasses.temple.holds", name));
            world.playSound(null, hit.getBlockPos(), SoundEvents.BLOCK_TRIPWIRE_CLICK_ON, SoundCategory.BLOCKS, 0.6f,
                    0.8f);
        }
        return ActionResult.SUCCESS;
    }

    /** Whether any of its payload (TNT, dispensers) is still there. */
    boolean armed(ServerWorld world, Trap trap) {
        for (long packed : trap.payload) {
            if (isPayload(world.getBlockState(BlockPos.fromLong(packed)), trap.kind)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPayload(BlockState state, Kind kind) {
        return kind == Kind.PRESSURE_PLATE ? state.isOf(Blocks.TNT) : state.isOf(Blocks.DISPENSER);
    }

    /** A successful disarm: the payload is made safe, the trap is gone for good. */
    void disarm(ServerWorld world, Trap trap, ServerPlayerEntity player) {
        int removed = 0;
        for (long packed : trap.payload) {
            BlockPos pos = BlockPos.fromLong(packed);
            BlockState state = world.getBlockState(pos);
            if (!isPayload(state, trap.kind)) {
                continue;
            }
            if (world.getBlockEntity(pos) instanceof DispenserBlockEntity dispenser) {
                dispenser.clear();
            }
            Block safe = trap.kind == Kind.PRESSURE_PLATE ? Blocks.SANDSTONE : Blocks.MOSSY_COBBLESTONE;
            world.setBlockState(pos, safe.getDefaultState(), Block.NOTIFY_LISTENERS);
            removed++;
        }
        if (trap.kind == Kind.TRIPWIRE) {
            // Hooks first, so taking the string away can't pull the wire
            for (BlockPos pos : trap.triggers()) {
                if (world.getBlockState(pos).isOf(Blocks.TRIPWIRE_HOOK)) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                }
            }
            for (BlockPos pos : trap.triggers()) {
                if (world.getBlockState(pos).isOf(Blocks.TRIPWIRE)) {
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                }
            }
        }
        trap.disarmed = true;
        markDirty();
        player.getInventory().offerOrDrop(trap.kind == Kind.PRESSURE_PLATE ? new ItemStack(Items.REDSTONE)
                : new ItemStack(Items.ARROW, TRIPWIRE_ARROWS));
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_TRIPWIRE_DETACH, SoundCategory.BLOCKS, 1.0f,
                1.2f);
        int xp = player.isCreative() ? 0 : Tier.MEDIUM.xp;
        if (xp > 0) {
            Progression.addXp(player, xp);
        }
        DnDClasses.LOGGER.info("[Trap] {} disarmed the {} temple trap at {}: {} payload blocks made safe, +{} class XP",
                player.getEntityName(), trap.kind.id, BlockPos.fromLong(trap.origin).toShortString(), removed, xp);
    }

    /** A fumbled disarm sets it off on the player. */
    void setOff(ServerWorld world, Trap trap, ServerPlayerEntity player) {
        int fired = 0;
        for (long packed : trap.payload) {
            BlockPos pos = BlockPos.fromLong(packed);
            BlockState state = world.getBlockState(pos);
            if (!isPayload(state, trap.kind)) {
                continue;
            }
            if (trap.kind == Kind.PRESSURE_PLATE) {
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                TntEntity tnt = new TntEntity(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player);
                tnt.setFuse(TNT_FUSE);
                world.spawnEntity(tnt);
            } else {
                ArrowEntity arrow = new ArrowEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                arrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
                Vec3d to = player.getEyePos().subtract(arrow.getPos());
                arrow.setVelocity(to.x, to.y, to.z, 1.6f, 2.0f);
                world.spawnEntity(arrow);
                world.playSound(null, pos, SoundEvents.BLOCK_DISPENSER_LAUNCH, SoundCategory.BLOCKS, 1.0f, 1.0f);
            }
            fired++;
        }
        if (trap.kind == Kind.PRESSURE_PLATE) {
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }
        DnDClasses.LOGGER.info("[Trap] {} set off the {} temple trap at {} ({} payload blocks)", player.getEntityName(),
                trap.kind.id, BlockPos.fromLong(trap.origin).toShortString(), fired);
    }

    // ---- Saving ----

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (Trap trap : byOrigin.values()) {
            NbtCompound tag = new NbtCompound();
            tag.putLong("Origin", trap.origin);
            tag.putString("Kind", trap.kind.id);
            tag.putLongArray("Triggers", trap.triggers.toLongArray());
            tag.putLongArray("Payload", trap.payload.toLongArray());
            tag.putBoolean("Disarmed", trap.disarmed);
            list.add(tag);
        }
        nbt.put("Traps", list);
        return nbt;
    }

    private static VanillaTraps fromNbt(NbtCompound nbt) {
        VanillaTraps traps = new VanillaTraps();
        NbtList list = nbt.getList("Traps", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound tag = list.getCompound(i);
            long origin = tag.getLong("Origin");
            Trap trap = new Trap(origin, Kind.byId(tag.getString("Kind")));
            for (long packed : tag.getLongArray("Triggers")) {
                trap.triggers.add(packed);
                trap.grow(BlockPos.fromLong(packed));
                traps.byTrigger.put(packed, origin);
            }
            for (long packed : tag.getLongArray("Payload")) {
                trap.payload.add(packed);
            }
            trap.disarmed = tag.getBoolean("Disarmed");
            traps.byOrigin.put(origin, trap);
        }
        return traps;
    }
}

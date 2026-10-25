package mattonfire.dnd.world.underdark;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mattonfire.dnd.classes.Blocks.underdark.RiftAnchorBlock;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.RiftBlocks;
import mattonfire.dnd.entity.BeholderEntity;
import mattonfire.dnd.world.gen.beholder.BeholderCavernPiece;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/**
 * The live side of the Underdark Rifts (design 9, 2.6): Rift Veil contact (a second in the veil drops a player or
 * mob into the Underdark; items and XP orbs vanish), the Rift Anchor's 3 s pull back up (cancelled by moving more
 * than a block or being hurt), anchors beside floors found for a rift, and the rift a dying lair Beholder tears
 * open in its floor ({@link #tearOpen}). Travel itself is {@link UnderdarkTravel#riftDown} and
 * {@link UnderdarkTravel#riftUp}.
 */
public final class UnderdarkRifts {
    /** Ticks in the veil before you fall through (1 in creative). */
    public static final int VEIL_TICKS = 20;
    /** Ticks an anchor takes to pull you up. */
    public static final int ANCHOR_TICKS = 60;
    /** How far (squared, in blocks) you may drift from where you started an anchor's pull. */
    private static final double ANCHOR_DRIFT_SQ = 1.0;
    /** How far {@link #ensureAnchorNear} looks for an anchor before placing one. */
    private static final int ANCHOR_SEARCH = 12;
    private static final RegistryKey<Structure> BEHOLDER_LAIR = RegistryKey.of(RegistryKeys.STRUCTURE,
            new Identifier(DnDClasses.MOD_ID, "beholder_lair"));

    /** Who is in a veil: when they were last seen in one and for how many ticks running. */
    private static final Map<UUID, Contact> CONTACTS = new HashMap<>();
    /** Entities that have been in the veil long enough, moved at the end of the tick. */
    private static final List<Entity> FALLING = new ArrayList<>();
    private static final Map<UUID, Pull> PULLS = new HashMap<>();

    private static final class Contact {
        int last;
        int ticks;
    }

    private record Pull(BlockPos anchor, Vec3d start, int startTick, int[] hurtTime) {
    }

    private UnderdarkRifts() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(UnderdarkRifts::tick);
    }

    // ---------------------------------------------------------------- the veil

    /** Something is touching a Rift Veil block at {@code pos} (called every tick it does, per block). */
    public static void veilContact(ServerWorld world, BlockPos pos, Entity entity) {
        if (Underdark.isUnderdark(world) || entity.isRemoved()) {
            return; // a veil only leads down
        }
        if (entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity) {
            world.spawnParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + 0.2, entity.getZ(), 4, 0.1, 0.1, 0.1, 0.02);
            world.spawnParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + 0.2, entity.getZ(), 8, 0.2, 0.2, 0.2, 0.3);
            world.playSound(null, pos, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.BLOCKS, 0.5F, 0.5F);
            entity.discard();
            return;
        }
        if (!(entity instanceof LivingEntity) || entity.hasVehicle() || entity.hasPassengers() || !entity.canUsePortals()
                || entity.isSpectator()) {
            return;
        }
        int now = world.getServer().getTicks();
        Contact contact = CONTACTS.computeIfAbsent(entity.getUuid(), uuid -> new Contact());
        if (contact.last == now && contact.ticks > 0) {
            return; // already counted this tick (touching several veil blocks)
        }
        contact.ticks = contact.last == now - 1 ? contact.ticks + 1 : 1;
        contact.last = now;
        boolean creative = entity instanceof PlayerEntity player && player.isCreative();
        if (contact.ticks < (creative ? 1 : VEIL_TICKS)) {
            return;
        }
        if (entity instanceof ServerPlayerEntity player && UnderdarkTravel.onCooldown(player)) {
            return;
        }
        CONTACTS.remove(entity.getUuid());
        if (!FALLING.contains(entity)) {
            FALLING.add(entity);
        }
    }

    // ---------------------------------------------------------------- the anchor

    /** {@code player} right-clicked a Rift Anchor at {@code pos}: start its pull, if it can. */
    public static void useAnchor(ServerPlayerEntity player, BlockPos pos) {
        if (!Underdark.isUnderdark(player.getWorld())) {
            player.sendMessage(text("anchor.still").formatted(Formatting.GRAY), true);
            return;
        }
        if (PULLS.containsKey(player.getUuid())) {
            return;
        }
        if (UnderdarkTravel.onCooldown(player)) {
            player.sendMessage(text("anchor.cooldown").formatted(Formatting.GRAY), true);
            return;
        }
        PULLS.put(player.getUuid(), new Pull(pos.toImmutable(), player.getPos(), player.getServer().getTicks(),
                new int[] { player.hurtTime }));
        player.getWorld().playSound(null, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 0.8F, 0.6F);
        DnDClasses.LOGGER.info("[Underdark] {} starts an anchor pull at {} {} {}", player.getName().getString(),
                pos.getX(), pos.getY(), pos.getZ());
        progress(player, 0);
    }

    /** Whether {@code player} is in the middle of an anchor's pull. */
    public static boolean pulling(ServerPlayerEntity player) {
        return PULLS.containsKey(player.getUuid());
    }

    private static void tick(MinecraftServer server) {
        int now = server.getTicks();
        if (!FALLING.isEmpty()) {
            List<Entity> falling = new ArrayList<>(FALLING);
            FALLING.clear();
            for (Entity entity : falling) {
                if (entity.isRemoved()) {
                    continue;
                }
                if (entity instanceof ServerPlayerEntity player) {
                    UnderdarkTravel.riftDown(player);
                } else {
                    UnderdarkTravel.riftDown(entity);
                }
            }
        }
        CONTACTS.values().removeIf(contact -> now - contact.last > 40);

        Iterator<Map.Entry<UUID, Pull>> it = PULLS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Pull> entry = it.next();
            Pull pull = entry.getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player == null || player.isRemoved() || player.isDead() || !Underdark.isUnderdark(player.getWorld())) {
                it.remove();
                continue;
            }
            String why = null;
            if (player.getPos().squaredDistanceTo(pull.start()) > ANCHOR_DRIFT_SQ) {
                why = "moved";
            } else if (player.hurtTime > pull.hurtTime()[0]) {
                why = "hurt";
            }
            pull.hurtTime()[0] = player.hurtTime;
            if (why != null) {
                it.remove();
                player.sendMessage(text("anchor.cancel." + why).formatted(Formatting.GRAY), true);
                DnDClasses.LOGGER.info("[Underdark] {}'s anchor pull is cancelled ({})", player.getName().getString(), why);
                continue;
            }
            int elapsed = now - pull.startTick();
            if (elapsed >= ANCHOR_TICKS) {
                it.remove();
                UnderdarkTravel.riftUp(player);
            } else if (elapsed % 2 == 0) {
                progress(player, elapsed);
            }
        }
    }

    private static void progress(ServerPlayerEntity player, int elapsed) {
        int filled = Math.round(20.0F * elapsed / ANCHOR_TICKS);
        String bar = "|".repeat(filled) + ".".repeat(20 - filled);
        player.sendMessage(text("anchor.pull", bar).formatted(Formatting.LIGHT_PURPLE), true);
    }

    /**
     * Makes sure there's a Rift Anchor within {@value #ANCHOR_SEARCH} blocks of {@code feet} (a floor found for a
     * rift that has no landing), so the way back up is in reach: if there isn't, a column of up to three is set
     * up beside the feet.
     */
    public static void ensureAnchorNear(ServerWorld world, BlockPos feet) {
        for (BlockPos pos : BlockPos.iterate(feet.add(-ANCHOR_SEARCH, -4, -ANCHOR_SEARCH),
                feet.add(ANCHOR_SEARCH, 4, ANCHOR_SEARCH))) {
            if (world.getBlockState(pos).isOf(RiftBlocks.RIFT_ANCHOR)) {
                return;
            }
        }
        BlockPos base = null;
        for (int r = 1; r <= 2 && base == null; r++) {
            for (int dx = -r; dx <= r && base == null; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    BlockPos pos = feet.add(dx, 0, dz);
                    if (world.getBlockState(pos).isAir() && world.getBlockState(pos.up()).isAir()
                            && world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
                        base = pos;
                        break;
                    }
                }
            }
        }
        if (base == null) {
            base = feet.east();
        }
        int height = 1;
        while (height < 3 && world.getBlockState(base.up(height)).isAir()) {
            height++;
        }
        for (int i = 0; i < height; i++) {
            world.setBlockState(base.up(i), RiftBlocks.RIFT_ANCHOR.getDefaultState().with(RiftAnchorBlock.TOP, i == height - 1));
        }
    }

    /**
     * The piece of class {@code type} of a {@code key} structure that starts in the chunk of (x, z) or one of the 8
     * round it, nearest (x, z); or null. Reads structure starts only, so it doesn't generate blocks.
     */
    @Nullable
    public static <T extends StructurePiece> T nearbyPiece(ServerWorld world, RegistryKey<Structure> key, Class<T> type,
                                                           int x, int z) {
        Structure structure = world.getRegistryManager().get(RegistryKeys.STRUCTURE).get(key);
        if (structure == null) {
            return null;
        }
        T best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                StructureStart start = world.getChunk((x >> 4) + dx, (z >> 4) + dz, ChunkStatus.STRUCTURE_STARTS)
                        .getStructureStart(structure);
                if (start == null || !start.hasChildren()) {
                    continue;
                }
                for (StructurePiece piece : start.getChildren()) {
                    if (type.isInstance(piece)) {
                        BlockPos c = piece.getBoundingBox().getCenter();
                        double d = (c.getX() - x) * (double) (c.getX() - x) + (c.getZ() - z) * (double) (c.getZ() - z);
                        if (d < bestDistance) {
                            bestDistance = d;
                            best = type.cast(piece);
                        }
                    }
                }
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- Beholder rifts

    /**
     * A Beholder died: if its home is in a Beholder lair's cavern, the floor it floated over splits open into a
     * rift ({@link #tearOpen}). Works in lairs that generated before rifts existed, as it's done on death.
     */
    public static void onBeholderDeath(BeholderEntity beholder) {
        if (!(beholder.getWorld() instanceof ServerWorld world) || beholder.getHome() == null) {
            return;
        }
        StructureStart start = world.getStructureAccessor().getStructureContaining(beholder.getHome(), BEHOLDER_LAIR);
        if (start == null || !start.hasChildren()) {
            return;
        }
        for (StructurePiece piece : start.getChildren()) {
            if (piece instanceof BeholderCavernPiece cavern) {
                tearOpen(world, cavern.getFloorCentre());
                return;
            }
        }
    }

    /**
     * Turns the 3x3 middle of a Beholder lair's floor eye (centred on {@code floorCentre}) into Rift Veil, cracks
     * the iris round it, and tells everyone near. Does nothing if it's already open.
     */
    public static boolean tearOpen(ServerWorld world, BlockPos floorCentre) {
        if (world.getBlockState(floorCentre).isOf(RiftBlocks.RIFT_VEIL)) {
            return false;
        }
        BlockState veil = RiftBlocks.RIFT_VEIL.getDefaultState();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                BlockPos pos = floorCentre.add(dx, 0, dz);
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    world.setBlockState(pos, veil);
                    if (!world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
                        world.setBlockState(pos.down(), Blocks.DEEPSLATE.getDefaultState());
                    }
                } else if (world.random.nextFloat() < 0.5F) {
                    world.setBlockState(pos, Blocks.CRACKED_DEEPSLATE_TILES.getDefaultState());
                }
            }
        }
        Vec3d c = Vec3d.ofCenter(floorCentre);
        world.playSound(null, floorCentre, SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 2.0F, 0.5F);
        world.playSound(null, floorCentre, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 1.5F, 0.6F);
        world.spawnParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 6, 1.5, 0.2, 1.5, 0.0);
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 0.5, c.z, 120, 1.2, 0.3, 1.2, 0.05);
        for (ServerPlayerEntity player : world.getPlayers(p -> p.squaredDistanceTo(c) < 64 * 64)) {
            player.sendMessage(text("beholder_rift").formatted(Formatting.GRAY, Formatting.ITALIC), false);
        }
        DnDClasses.LOGGER.info("[Underdark] Beholder rift torn open at {} {} {}", floorCentre.getX(), floorCentre.getY(),
                floorCentre.getZ());
        return true;
    }

    private static MutableText text(String key, Object... args) {
        return Text.translatable("underdark.dndclasses." + key, args);
    }
}

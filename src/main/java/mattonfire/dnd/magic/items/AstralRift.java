package mattonfire.dnd.magic.items;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.boss.BossFight;
import mattonfire.dnd.magic.MagicData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.Vec3d;

/**
 * A Bag of Holding inside a Portable Hole, or the other way round (design 10, 8.4): a rift to the Astral Plane.
 * Both items and everything in both are gone; every creature within {@value #RADIUS} blocks takes
 * {@value #DAMAGE} magic damage and is pulled {@value #PULL} blocks in. The screens accept the item and schedule
 * the rift here, for the end of the tick.
 */
public final class AstralRift {
    public static final double RADIUS = 4.0;
    public static final float DAMAGE = 8.0F;
    public static final double PULL = 2.0;
    public static final double MESSAGE_RANGE = 16.0;
    public static final Identifier ADVANCEMENT = new Identifier(DnDClasses.MOD_ID, "astral_accident");

    public enum Cause {
        BAG_IN_HOLE, HOLE_IN_BAG
    }

    private record Pending(ServerPlayerEntity player, Cause cause, @Nullable UUID hole, @Nullable ItemStack bag) {
    }

    private static final Map<UUID, Pending> PENDING = new LinkedHashMap<>();

    private AstralRift() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(AstralRift::tick);
    }

    /** A Bag went into the Hole {@code hole}. */
    public static void schedule(PlayerEntity player, Cause cause, UUID hole) {
        if (player instanceof ServerPlayerEntity server)
            PENDING.putIfAbsent(player.getUuid(), new Pending(server, cause, hole, null));
    }

    /** A Hole went into the Bag {@code bag}. */
    public static void scheduleBag(PlayerEntity player, ItemStack bag) {
        if (player instanceof ServerPlayerEntity server)
            PENDING.putIfAbsent(player.getUuid(), new Pending(server, Cause.HOLE_IN_BAG, null, bag));
    }

    private static void tick(MinecraftServer server) {
        if (PENDING.isEmpty())
            return;
        Pending[] all = PENDING.values().toArray(Pending[]::new);
        PENDING.clear();
        for (Pending pending : all) {
            if (!pending.player().isRemoved())
                tear(pending);
        }
    }

    private static void tear(Pending pending) {
        ServerPlayerEntity player = pending.player();
        MinecraftServer server = player.getServer();
        // Close first: the Bag's screen writes its contents back on close
        player.closeHandledScreen();
        PortableHoleStorage storage = PortableHoleStorage.get(server);
        int lost = 0;
        if (pending.cause() == Cause.BAG_IN_HOLE) {
            lost += storage.destroy(pending.hole());
            lost += removeHoles(player, pending.hole());
        } else {
            ItemStack bag = pending.bag();
            DefaultedList<ItemStack> inside = BagOfHoldingItem.load(bag);
            for (ItemStack stack : inside) {
                if (!stack.isEmpty())
                    lost++;
                UUID hole = PortableHoleItem.isHole(stack) ? MagicData.uuid(stack) : null;
                if (hole != null) {
                    lost += storage.destroy(hole);
                    PortableHolePassages.closeHole(server, hole, "astral rift");
                }
            }
            lost += removeStack(player, bag);
        }
        ServerWorld world = player.getWorld();
        Vec3d center = player.getPos().add(0, 1.0, 0);
        for (LivingEntity victim : world.getEntitiesByClass(LivingEntity.class,
                player.getBoundingBox().expand(RADIUS), e -> e.isAlive() && e.squaredDistanceTo(center) <= RADIUS * RADIUS * 1.5)) {
            victim.damage(world.getDamageSources().magic(), DAMAGE);
            if (victim != player) {
                Vec3d pull = center.subtract(victim.getPos());
                if (pull.horizontalLength() > 0.01) {
                    Vec3d step = new Vec3d(pull.x, 0, pull.z).normalize().multiply(Math.min(PULL, pull.horizontalLength()));
                    victim.requestTeleport(victim.getX() + step.x, victim.getY(), victim.getZ() + step.z);
                }
            }
        }
        world.spawnParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 200, 1.5, 1.5, 1.5, 0.4);
        world.spawnParticles(ParticleTypes.PORTAL, center.x, center.y, center.z, 120, 2.0, 1.0, 2.0, 1.0);
        world.playSound(null, player.getBlockPos(), SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 0.8F, 1.3F);
        Text message = Text.translatable("item.dndclasses.portable_hole.rift").formatted(Formatting.DARK_PURPLE);
        for (ServerPlayerEntity near : world.getPlayers(p -> p.squaredDistanceTo(player) <= MESSAGE_RANGE * MESSAGE_RANGE))
            near.sendMessage(message, false);
        BossFight.grantAdvancement(player, ADVANCEMENT);
        DnDClasses.LOGGER.info("[Magic] Astral rift at {} ({}): {} stacks lost", player.getBlockPos().toShortString(),
                pending.cause(), lost);
    }

    /** Removes every Portable Hole with this uuid from the player (inventory and cursor). */
    private static int removeHoles(ServerPlayerEntity player, UUID hole) {
        int removed = 0;
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (PortableHoleItem.isHole(stack) && hole.equals(MagicData.uuid(stack))) {
                inv.setStack(i, ItemStack.EMPTY);
                removed++;
            }
        }
        ItemStack cursor = player.currentScreenHandler.getCursorStack();
        if (PortableHoleItem.isHole(cursor) && hole.equals(MagicData.uuid(cursor)))
            player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
        return removed;
    }

    private static int removeStack(ServerPlayerEntity player, ItemStack target) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i) == target) {
                inv.setStack(i, ItemStack.EMPTY);
                return 1;
            }
        }
        return 0;
    }
}

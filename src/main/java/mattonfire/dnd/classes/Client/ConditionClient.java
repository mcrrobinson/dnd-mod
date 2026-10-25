package mattonfire.dnd.classes.Client;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.ConditionSync;
import mattonfire.dnd.classes.Conditions.Conditions;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * The client's copy of its own conditions ({@link ConditionSync#S2C_CONDITIONS}), counted down between packets:
 * the HUD reads it, Frightened clamps movement toward its source, Prone stands up on jump, and an incapacitated
 * player lets go of whatever they were using.
 */
@Environment(EnvType.CLIENT)
public final class ConditionClient {
    /** One instance as the server described it. */
    public static final class Entry {
        public final Condition condition;
        public int remaining;
        /** The longest {@link #remaining} seen for it, for the HUD's time bar. */
        public int total;
        public final int sourceId;
        @Nullable
        public final Text sourceName;
        @Nullable
        public final Ability saveAbility;
        public final int dc;
        public int nextSave;

        Entry(Condition condition, int remaining, int sourceId, @Nullable Text sourceName,
                @Nullable Ability saveAbility, int dc, int nextSave) {
            this.condition = condition;
            this.remaining = remaining;
            this.total = remaining;
            this.sourceId = sourceId;
            this.sourceName = sourceName;
            this.saveAbility = saveAbility;
            this.dc = dc;
            this.nextSave = nextSave;
        }
    }

    /** Frightened: movement toward the source is removed while it's this close and in sight. */
    public static final double FRIGHTENED_RANGE = 32.0D;

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private ConditionClient() {
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ConditionSync.S2C_CONDITIONS, (client, handler, buf, sender) -> {
            int count = buf.readVarInt();
            List<Entry> read = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Condition condition = Condition.values()[buf.readVarInt()];
                int remaining = buf.readVarInt();
                int sourceId = -1;
                Text sourceName = null;
                if (buf.readBoolean()) {
                    sourceId = buf.readVarInt();
                    sourceName = buf.readText();
                }
                int abilityIndex = buf.readByte();
                Ability ability = abilityIndex < 0 ? null : Ability.values()[abilityIndex];
                int dc = 0;
                int next = 0;
                if (ability != null) {
                    dc = buf.readVarInt();
                    next = buf.readVarInt();
                }
                read.add(new Entry(condition, remaining, sourceId, sourceName, ability, dc, next));
            }
            client.execute(() -> accept(read));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ENTRIES::clear));
        ClientTickEvents.END_CLIENT_TICK.register(ConditionClient::tick);
    }

    private static void accept(List<Entry> read) {
        for (Entry entry : read) {
            for (Entry old : ENTRIES) {
                if (old.condition == entry.condition && old.sourceId == entry.sourceId && old.total >= entry.remaining
                        && entry.remaining >= 0) {
                    entry.total = old.total; // same instance counting down: keep its full length for the bar
                    break;
                }
            }
        }
        ENTRIES.clear();
        ENTRIES.addAll(read);
    }

    private static void tick(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        if (!client.isPaused()) {
            for (Entry entry : ENTRIES) {
                if (entry.remaining > 0) {
                    entry.remaining--;
                }
                if (entry.saveAbility != null && entry.nextSave > 0) {
                    entry.nextSave--;
                }
            }
        }
        ClientPlayerEntity player = client.player;
        if (player.isUsingItem() && !Conditions.canAct(player) && client.interactionManager != null) {
            client.interactionManager.stopUsingItem(player); // a raised shield drops
        }
    }

    /** Called by {@link GrappleClient} when jump was pressed this tick: a Prone player stands up. */
    static void onJumpPressed(MinecraftClient client) {
        if (client.player != null && client.currentScreen == null
                && Conditions.has(client.player, Condition.PRONE)) {
            ClientPlayNetworking.send(ConditionSync.C2S_STAND_UP, PacketByteBufs.empty());
        }
    }

    /**
     * Frightened: removes any part of the movement input that points toward a visible source within
     * {@value #FRIGHTENED_RANGE} blocks. Called from {@code KeyboardInputMixin} after the keys are read.
     */
    public static void clampFrightened(Input input) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || !Conditions.has(player, Condition.FRIGHTENED)) {
            return;
        }
        float yaw = player.getYaw() * MathHelper.RADIANS_PER_DEGREE;
        float sin = MathHelper.sin(yaw);
        float cos = MathHelper.cos(yaw);
        double x = input.movementSideways * cos - input.movementForward * sin;
        double z = input.movementForward * cos + input.movementSideways * sin;
        boolean changed = false;
        for (Entry entry : ENTRIES) {
            if (entry.condition != Condition.FRIGHTENED || entry.sourceId < 0) {
                continue;
            }
            Entity source = client.world.getEntityById(entry.sourceId);
            if (source == null || source.squaredDistanceTo(player) > FRIGHTENED_RANGE * FRIGHTENED_RANGE
                    || !player.canSee(source)) {
                continue;
            }
            double dx = source.getX() - player.getX();
            double dz = source.getZ() - player.getZ();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length < 1.0E-4) {
                continue;
            }
            dx /= length;
            dz /= length;
            double toward = x * dx + z * dz;
            if (toward > 0.0D) {
                x -= dx * toward;
                z -= dz * toward;
                changed = true;
            }
        }
        if (!changed) {
            return;
        }
        input.movementForward = (float) (z * cos - x * sin);
        input.movementSideways = (float) (x * cos + z * sin);
        input.pressingForward = input.movementForward > 0.01F;
        input.pressingBack = input.movementForward < -0.01F;
        input.pressingLeft = input.movementSideways > 0.01F;
        input.pressingRight = input.movementSideways < -0.01F;
    }

    /** For DevScript's {@code conditions} step: {@code stunned(57t, INT 14 in 17t) prone(-1)}. */
    public static String describe(MinecraftClient client) {
        StringBuilder out = new StringBuilder();
        for (Entry entry : ENTRIES) {
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(entry.condition.id()).append('(').append(entry.remaining < 0 ? "-1" : entry.remaining + "t");
            if (entry.saveAbility != null) {
                out.append(", ").append(entry.saveAbility.shortKey()).append(' ').append(entry.dc).append(" in ")
                        .append(entry.nextSave).append('t');
            }
            out.append(')');
        }
        if (out.length() == 0) {
            out.append("none");
        }
        if (client.player != null) {
            out.append(" mask=").append(Conditions.mask(client.player));
        }
        return out.toString();
    }
}

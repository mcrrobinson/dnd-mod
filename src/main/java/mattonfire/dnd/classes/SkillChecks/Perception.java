package mattonfire.dnd.classes.SkillChecks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.Skill;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

/**
 * Active Perception: the Search/Study key ({@code key.dnd-classes.study}, V).
 *
 * The client sends {@link #C2S_STUDY} with the creature under its crosshair (up to {@link #STUDY_RANGE}
 * blocks) or none. Looking at a creature asks the {@link TargetHandler}s in order (a Lore/Insight Study is
 * meant to register one); if none handles it, or there is no creature, the player Searches:
 * a Perception roll on the main HUD lane, on a {@link #SEARCH_COOLDOWN}-tick cooldown, that reveals every
 * {@link Perceivable} within {@link PerceptionService#SEARCH_RANGE} blocks whose DC the total meets. A
 * natural 20 finds everything in range, a natural 1 nothing.
 *
 * {@link #S2C_PERCEIVED} tells one client to mark an entity or block it noticed (the mimic's breathing, the
 * red outline after a Search); only that player sees it.
 */
public final class Perception {
    public static final Identifier C2S_STUDY = new Identifier(DnDClasses.MOD_ID, "study");
    public static final Identifier S2C_PERCEIVED = new Identifier(DnDClasses.MOD_ID, "perceived");
    /** HUD label of the Search roll. */
    public static final String SEARCH_LABEL = "skill.dndclasses.search";
    /** Ticks between Searches (10 s). */
    public static final int SEARCH_COOLDOWN = 200;
    /** How far away a creature can be looked at with the key. */
    public static final double STUDY_RANGE = 16.0;
    /** How long a Search outlines what it found. */
    public static final int OUTLINE_TICKS = 100;

    /** How the client marks something it perceived. */
    public enum Mark {
        /** Faint breathing puffs over a dormant mimic, while it stays dormant. */
        BREATH,
        /** A red outline (entities) or a red box of particles (blocks). */
        OUTLINE
    }

    /** Something the key does when the player looks at a creature (a Study). */
    @FunctionalInterface
    public interface TargetHandler {
        /** @return true if it handled the key press; false lets the next handler (or a Search) have it */
        boolean handle(ServerPlayerEntity player, LivingEntity target);
    }

    private static final List<TargetHandler> TARGET_HANDLERS = new CopyOnWriteArrayList<>();
    private static final Map<UUID, Integer> LAST_SEARCH = new HashMap<>();

    private Perception() {
    }

    /** Adds a handler for the key pressed while looking at a creature (registration order is asking order). */
    public static void registerTargetHandler(TargetHandler handler) {
        TARGET_HANDLERS.add(handler);
    }

    static void register() {
        PerceptionService.register();
        ServerPlayNetworking.registerGlobalReceiver(C2S_STUDY, (server, player, handler, buf, sender) -> {
            int targetId = buf.readVarInt();
            server.execute(() -> onKey(player, targetId));
        });
    }

    /** Drops the Search cooldown; called on disconnect. */
    public static void forget(UUID player) {
        LAST_SEARCH.remove(player);
    }

    private static void onKey(ServerPlayerEntity player, int targetId) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        LivingEntity target = validTarget(player, targetId);
        if (target != null) {
            for (TargetHandler handler : TARGET_HANDLERS) {
                if (handler.handle(player, target)) {
                    return;
                }
            }
        }
        search(player);
    }

    /** The creature the client says it's looking at, if the server agrees it could be. */
    @Nullable
    private static LivingEntity validTarget(ServerPlayerEntity player, int targetId) {
        if (targetId < 0) {
            return null;
        }
        Entity entity = player.getWorld().getEntityById(targetId);
        if (!(entity instanceof LivingEntity living) || !living.isAlive() || living == player
                || living.squaredDistanceTo(player) > (STUDY_RANGE + 2) * (STUDY_RANGE + 2) || !player.canSee(living)) {
            return null;
        }
        // A creature still in disguise (a dormant mimic) is not a creature yet: Search it instead
        if (living instanceof Perceivable hidden && hidden.hiddenFrom(player)) {
            return null;
        }
        return living;
    }

    /** Ticks until the player can Search again (0: now). */
    public static int cooldown(ServerPlayerEntity player) {
        Integer last = LAST_SEARCH.get(player.getUuid());
        int now = player.getServer().getTicks();
        return last == null || now < last ? 0 : Math.max(0, SEARCH_COOLDOWN - (now - last));
    }

    /**
     * A Search: rolls Perception and reveals what it finds nearby. Returns the roll, or null while on
     * cooldown.
     */
    @Nullable
    public static D20.Roll search(ServerPlayerEntity player) {
        int wait = cooldown(player);
        if (wait > 0) {
            player.sendMessage(Text.translatable("search.dndclasses.cooldown", (wait + 19) / 20), true);
            return null;
        }
        LAST_SEARCH.put(player.getUuid(), player.getServer().getTicks());
        D20.Roll roll = SkillCheck.builder(player, Skill.PERCEPTION, 0, "search").label(SEARCH_LABEL).roll();
        // Per-roll bonuses on top of the sheet's Perception (none yet, but they carry over to Investigation)
        int extra = roll.modifier() - AbilityScores.sheet(player).check(Skill.PERCEPTION);
        ServerWorld world = (ServerWorld) player.getWorld();
        int found = 0;
        for (Perceivable thing : PerceptionService.near(world, player.getPos(), PerceptionService.SEARCH_RANGE)) {
            if (!thing.hiddenFrom(player)) {
                continue;
            }
            int total = roll.natural() + PerceptionService.searchBonus(player, thing.perceptionSkill()) + extra;
            boolean spotted = roll.outcome() == D20.Outcome.CRITICAL
                    || roll.outcome() != D20.Outcome.FUMBLE && total >= thing.perceptionDc();
            DnDClasses.LOGGER.info("[Perception] {} searches: {} total {} vs DC {} -> {}", player.getEntityName(),
                    PerceptionService.describe(thing), total, thing.perceptionDc(), spotted ? "found" : "missed");
            if (spotted) {
                thing.perceive(player, true);
                found++;
            }
        }
        D20.show(player, roll, found == 0 ? Text.translatable("search.dndclasses.nothing")
                : found == 1 ? Text.translatable("search.dndclasses.found_one")
                : Text.translatable("search.dndclasses.found", found));
        return roll;
    }

    // ---------------------------------------------------------------- marks

    /** Marks an entity on one player's client for {@code ticks} (BREATH lasts while the entity stays hidden). */
    public static void mark(ServerPlayerEntity player, Entity entity, Mark mark, int ticks) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeEnumConstant(mark);
        buf.writeBoolean(true);
        buf.writeVarInt(entity.getId());
        buf.writeVarInt(ticks);
        ServerPlayNetworking.send(player, S2C_PERCEIVED, buf);
    }

    /** Marks a block on one player's client for {@code ticks}. */
    public static void mark(ServerPlayerEntity player, BlockPos pos, Mark mark, int ticks) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeEnumConstant(mark);
        buf.writeBoolean(false);
        buf.writeBlockPos(pos);
        buf.writeVarInt(ticks);
        ServerPlayNetworking.send(player, S2C_PERCEIVED, buf);
    }
}

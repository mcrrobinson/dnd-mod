package mattonfire.dnd.classes;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.Registry.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

/**
 * Monk rules:
 * - Can only attack with the Monk Staff or bare fists (unarmed strikes). Any
 * other held item cancels the attack.
 * - Damage output is scaled by worn armor: unarmored monks hit for 75% of
 * normal damage and every armor point lowers that further (full diamond is
 * 25%). Recomputed every tick so it follows equipment changes, and removed
 * as soon as the player is no longer a monk.
 * - Double jump: the client does the jump (DoubleJumpMixin) and tells the
 * server, which checks it, resets the fall distance so the landing is
 * measured from the second jump, and shows the effect to nearby players.
 */
public class MonkHandler {

    private static final UUID DAMAGE_MODIFIER_ID = UUID.fromString("5b0e7c2a-3d6f-4c1e-9a8b-6f2d1e0c4a71");
    private static final String DAMAGE_MODIFIER_NAME = "Monk armor damage penalty";

    /** Damage multiplier when wearing no armor. */
    private static final double UNARMORED_MULTIPLIER = 0.75;
    /** Armor points that halve the unarmored multiplier. */
    private static final double ARMOR_HALVING_POINTS = 10.0;

    /** Air jumps allowed before touching the ground again (matches DoubleJumpMixin). */
    public static final int AIR_JUMPS = 2;
    /** Air jumps used since each monk last stood on the ground or climbed. */
    private static final Map<UUID, Integer> AIR_JUMPS_USED = new HashMap<>();

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(DnDClasses.C2S_DOUBLEJUMP_EFFECTS_REQUEST_PACKET_ID,
                (server, player, handler, buf, responseSender) -> server.execute(() -> onDoubleJump(player)));
        ServerPlayConnectionEvents.DISCONNECT
                .register((handler, server) -> AIR_JUMPS_USED.remove(handler.getPlayer().getUuid()));
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player.isSpectator() || !isMonk(player)) {
                return ActionResult.PASS;
            }
            if (canAttackWith(player)) {
                return ActionResult.PASS;
            }
            // Runs on the client first (which then never sends the attack), and on the
            // server only if the client let it through, so the message shows once.
            player.sendMessage(Text.literal("Monks can only fight with a staff or bare fists!")
                    .formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                updateDamageModifier(player);
            }
            if (!AIR_JUMPS_USED.isEmpty()) {
                AIR_JUMPS_USED.keySet().removeIf(uuid -> {
                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                    return player == null || player.isOnGround() || player.isClimbing();
                });
            }
        });
    }

    private static boolean isMonk(PlayerEntity player) {
        return player instanceof PlayerEntityExt ext && ext.getDndClass() == DndCharacter.MONK;
    }

    private static boolean canAttackWith(PlayerEntity player) {
        return player.getMainHandStack().isEmpty() || player.getMainHandStack().isOf(ModItems.MONK_STAFF);
    }

    /** Total damage multiplier for a monk wearing the given armor points. */
    public static double damageMultiplier(int armor) {
        return UNARMORED_MULTIPLIER / (1.0 + Math.max(0, armor) / ARMOR_HALVING_POINTS);
    }

    private static void updateDamageModifier(ServerPlayerEntity player) {
        EntityAttributeInstance attackDamage = player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attackDamage == null) {
            return;
        }
        EntityAttributeModifier current = attackDamage.getModifier(DAMAGE_MODIFIER_ID);

        if (!isMonk(player)) {
            if (current != null) {
                attackDamage.removeModifier(DAMAGE_MODIFIER_ID);
            }
            return;
        }

        // Older monk saves persisted a base damage of 10 from the old formula.
        if (attackDamage.getBaseValue() != 1.0D) {
            attackDamage.setBaseValue(1.0D);
        }

        // MULTIPLY_TOTAL multiplies the final value by (1 + amount).
        double amount = damageMultiplier(player.getArmor()) - 1.0;
        if (current != null && current.getValue() == amount) {
            return;
        }
        if (current != null) {
            attackDamage.removeModifier(DAMAGE_MODIFIER_ID);
        }
        attackDamage.addTemporaryModifier(new EntityAttributeModifier(DAMAGE_MODIFIER_ID, DAMAGE_MODIFIER_NAME,
                amount, EntityAttributeModifier.Operation.MULTIPLY_TOTAL));
    }

    /** A monk's client says it double jumped. The client's word is only taken within these limits. */
    private static void onDoubleJump(ServerPlayerEntity player) {
        if (!isMonk(player) || !player.isAlive() || player.isSpectator() || player.getAbilities().allowFlying
                || player.isFallFlying() || player.hasVehicle() || player.isTouchingWater()
                || player.hasStatusEffect(StatusEffects.LEVITATION)
                || mattonfire.dnd.classes.Effects.AntiMagicEffect.isSuppressed(player)) {
            return;
        }
        int used = AIR_JUMPS_USED.getOrDefault(player.getUuid(), 0);
        if (used >= AIR_JUMPS) {
            return;
        }
        AIR_JUMPS_USED.put(player.getUuid(), used + 1);

        // Fall damage counts from the top of the second jump, not from where the first one started.
        player.fallDistance = 0;

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(player.getUuid());
        for (ServerPlayerEntity other : PlayerLookup.tracking(player)) {
            ServerPlayNetworking.send(other, DnDClasses.S2C_DOUBLEJUMP_EFFECTS_PACKET_ID, buf);
        }
    }
}

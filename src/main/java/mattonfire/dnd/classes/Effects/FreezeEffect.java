package mattonfire.dnd.classes.Effects;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.network.packet.s2c.play.EntityStatusEffectS2CPacket;
import net.minecraft.network.packet.s2c.play.RemoveEntityStatusEffectS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

public class FreezeEffect extends StatusEffect {
    public FreezeEffect(StatusEffectCategory statusEffectCategory, int color) {
        super(statusEffectCategory, color);
    }

    @Override
    public void applyUpdateEffect(LivingEntity pLivingEntity, int pAmplifier) {
        if (!pLivingEntity.world.isClient()) {
            double x = pLivingEntity.getX();
            double y = pLivingEntity.getY();
            double z = pLivingEntity.getZ();

            pLivingEntity.teleport(x, y, z);
            pLivingEntity.setVelocity(0, 0, 0);
        }

        super.applyUpdateEffect(pLivingEntity, pAmplifier);
    }

    @Override
    public boolean canApplyUpdateEffect(int pDuration, int pAmplifier) {
        return true;
    }

    // Vanilla only sends a mob's effects when a player starts tracking it, so the client-side
    // freeze tint (LivingEntityRendererMixin) would miss a mob frozen while in view. Tell every
    // player tracking the entity when the effect starts and ends. A player's own effects are
    // already sent to them by vanilla; PlayerLookup.tracking doesn't include the entity itself.
    @Override
    public void onApplied(LivingEntity entity, AttributeContainer attributes, int amplifier) {
        super.onApplied(entity, attributes, amplifier);
        if (entity.world.isClient()) {
            return;
        }
        StatusEffectInstance instance = entity.getStatusEffect(this);
        if (instance == null) {
            return;
        }
        EntityStatusEffectS2CPacket packet = new EntityStatusEffectS2CPacket(entity.getId(), instance);
        for (ServerPlayerEntity player : PlayerLookup.tracking(entity)) {
            player.networkHandler.sendPacket(packet);
        }
    }

    @Override
    public void onRemoved(LivingEntity entity, AttributeContainer attributes, int amplifier) {
        super.onRemoved(entity, attributes, amplifier);
        if (entity.world.isClient()) {
            return;
        }
        RemoveEntityStatusEffectS2CPacket packet = new RemoveEntityStatusEffectS2CPacket(entity.getId(), this);
        for (ServerPlayerEntity player : PlayerLookup.tracking(entity)) {
            player.networkHandler.sendPacket(packet);
        }
    }
}

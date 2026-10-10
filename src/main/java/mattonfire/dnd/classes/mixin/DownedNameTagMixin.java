package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import mattonfire.dnd.classes.Downed.Downed;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** "<name> (Downed)" over a Downed player, from the synced Downed bit. */
@Mixin(PlayerEntityRenderer.class)
public abstract class DownedNameTagMixin {
    @ModifyVariable(method = "renderLabelIfPresent(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/text/Text;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"), argsOnly = true)
    private Text dndclasses$downedNameTag(Text text, AbstractClientPlayerEntity player, Text original,
            MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        if (!Downed.is(player)) {
            return text;
        }
        return text.copy().append(Text.translatable("hud.dndclasses.downed.name_tag").formatted(Formatting.RED));
    }
}

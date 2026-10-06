package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;

/**
 * Class vision cons: pulls the fog in close for classes that can't see far.
 * Only ever shortens the fog, so water, lava, blindness etc. still win when
 * they're already closer.
 */
@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {
    // Barbarian: "Limited vision" - you can't see very far.
    private static final float BARBARIAN_FOG_START = 4.0F;
    private static final float BARBARIAN_FOG_END = 24.0F;
    // Cleric: "Shorter viewing distance" - milder than the Barbarian.
    private static final float CLERIC_FOG_START = 16.0F;
    private static final float CLERIC_FOG_END = 48.0F;

    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void dnd$limitClassVision(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance,
            boolean thickFog, float tickDelta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(client.player instanceof PlayerEntityExt) || client.player.isSpectator()) {
            return;
        }

        float start;
        float end;
        DndCharacter dndClass = ((PlayerEntityExt) client.player).getDndClass();
        if (dndClass == DndCharacter.BARBARIAN) {
            start = BARBARIAN_FOG_START;
            end = BARBARIAN_FOG_END;
        } else if (dndClass == DndCharacter.CLERIC) {
            start = CLERIC_FOG_START;
            end = CLERIC_FOG_END;
        } else {
            return;
        }

        if (end < RenderSystem.getShaderFogEnd()) {
            RenderSystem.setShaderFogStart(Math.min(start, RenderSystem.getShaderFogStart()));
            RenderSystem.setShaderFogEnd(end);
        }
    }
}

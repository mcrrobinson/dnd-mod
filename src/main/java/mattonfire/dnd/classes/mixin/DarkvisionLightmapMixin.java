package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import mattonfire.dnd.classes.Race.RacePassives;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;

/**
 * Darkvision: the lightmap's night vision strength (local 8 of {@code update}: night vision, or the underwater
 * visibility, or 0) is at least {@link RacePassives#DARKVISION_STRENGTH} for a race with darkvision.
 */
@Mixin(LightmapTextureManager.class)
public abstract class DarkvisionLightmapMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @ModifyVariable(method = "update", at = @At("STORE"), index = 8)
    private float dnd$darkvision(float strength) {
        if (this.client.player != null
                && RacePassives.hasDarkvision(RacePassives.raceOf(this.client.player))) {
            return Math.max(strength, RacePassives.DARKVISION_STRENGTH);
        }
        return strength;
    }
}

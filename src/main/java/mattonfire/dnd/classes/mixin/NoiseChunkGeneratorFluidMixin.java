package mattonfire.dnd.classes.mixin;

import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla fills every open cell below {@code min(-54, sea level)} with lava (the Overworld's deep lava lakes). The
 * Underdark's whole world is below y -65 and its sea level is -222, so its lakes would all be lava. For noise
 * settings whose sea level is below -54 (only the Underdark's: the Overworld-like presets never are), the fluid below
 * the sea level is the settings' default fluid (water) instead, so the Darklake has water. Lava comes from springs.
 */
@Mixin(NoiseChunkGenerator.class)
public abstract class NoiseChunkGeneratorFluidMixin {
    private static final int VANILLA_LAVA_LEVEL = -54;

    @Inject(method = "createFluidLevelSampler", at = @At("HEAD"), cancellable = true)
    private static void dnd$waterBelowTheBedrock(ChunkGeneratorSettings settings,
            CallbackInfoReturnable<AquiferSampler.FluidLevelSampler> cir) {
        if (settings.seaLevel() < VANILLA_LAVA_LEVEL) {
            AquiferSampler.FluidLevel water = new AquiferSampler.FluidLevel(settings.seaLevel(),
                    settings.defaultFluid());
            cir.setReturnValue((x, y, z) -> water);
        }
    }
}

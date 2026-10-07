package mattonfire.dnd.classes.mixin;

import mattonfire.dnd.classes.Client.Music.MusicStings;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundSystem;
import net.minecraft.sound.SoundCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Turns the background music down while a class special's sting plays (see MusicStings). */
@Mixin(SoundSystem.class)
public abstract class SoundSystemMusicDuckMixin {

    @Inject(method = "getAdjustedVolume(Lnet/minecraft/client/sound/SoundInstance;)F", at = @At("RETURN"), cancellable = true)
    private void dndclasses$duckMusic(SoundInstance sound, CallbackInfoReturnable<Float> cir) {
        if (sound.getCategory() == SoundCategory.MUSIC && !MusicStings.isSting(sound)) {
            cir.setReturnValue(cir.getReturnValue() * MusicStings.getDuckFactor());
        }
    }
}

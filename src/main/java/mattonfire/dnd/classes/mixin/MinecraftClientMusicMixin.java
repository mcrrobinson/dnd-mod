package mattonfire.dnd.classes.mixin;

import mattonfire.dnd.classes.Client.Music.EventMusic;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.MusicSound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMusicMixin {

    @Inject(method = "getMusicType", at = @At("RETURN"), cancellable = true)
    private void dndclasses$eventMusic(CallbackInfoReturnable<MusicSound> cir) {
        MusicSound music = EventMusic.getMusic(cir.getReturnValue());
        if (music != null) {
            cir.setReturnValue(music);
        }
    }
}

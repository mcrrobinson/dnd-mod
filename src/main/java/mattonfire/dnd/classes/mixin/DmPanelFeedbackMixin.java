package mattonfire.dnd.classes.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import mattonfire.dnd.dm.DmPanelPackets;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;

/**
 * A command run from the DM panel has its output wrapped ({@link DmPanelPackets.Capture}) so the panel can show the
 * feedback. When the feedback is broadcast to ops ("[Dev: ...]"), vanilla skips the player whose output it is and
 * logs it to the console unless the output is the server; unwrap it there, so the DM isn't sent their own line
 * twice and the log matches the typed command.
 */
@Mixin(ServerCommandSource.class)
public abstract class DmPanelFeedbackMixin {
    @Shadow
    @Final
    private CommandOutput output;

    @Redirect(method = "sendToOps", at = @At(value = "FIELD",
            target = "Lnet/minecraft/server/command/ServerCommandSource;output:Lnet/minecraft/server/command/CommandOutput;"))
    private CommandOutput dnd$unwrapPanelOutput(ServerCommandSource source) {
        return this.output instanceof DmPanelPackets.Capture capture ? capture.player() : this.output;
    }
}

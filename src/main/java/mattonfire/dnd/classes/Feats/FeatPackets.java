package mattonfire.dnd.classes.Feats;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.Progression;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Feat packets: C2S {@link #C2S_CHOOSE_FEAT} {@code {id, ability or ""}} (the picker's confirm, handled by
 * {@link Progression#chooseFeat}, which checks the table and payment again), C2S {@link #C2S_TOGGLE} (N: Power
 * Attack / Steady Aim), and S2C {@link #S2C_CLEAVE} (Cleave readied the swing: reset the client's cooldown too).
 */
public final class FeatPackets {
    public static final Identifier C2S_CHOOSE_FEAT = new Identifier(DnDClasses.MOD_ID, "choose_feat");
    public static final Identifier C2S_TOGGLE = new Identifier(DnDClasses.MOD_ID, "feat_toggle");
    public static final Identifier S2C_CLEAVE = new Identifier(DnDClasses.MOD_ID, "feat_cleave");

    private FeatPackets() {
    }

    static void register() {
        ServerPlayNetworking.registerGlobalReceiver(C2S_CHOOSE_FEAT, (server, player, handler, buf, sender) -> {
            String id = buf.readString(64);
            String abilityId = buf.readString(16);
            server.execute(() -> {
                Feat feat = Feat.byId(id);
                if (feat == null) {
                    DnDClasses.LOGGER.warn("{} sent an unknown feat {}", player.getEntityName(), id);
                    return;
                }
                Ability ability = abilityId.isEmpty() ? null : Ability.byId(abilityId);
                Progression.chooseFeat(player, feat, ability, false);
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(C2S_TOGGLE,
                (server, player, handler, buf, sender) -> server.execute(() -> toggle(player)));
    }

    /** N: flips Power Attack and Steady Aim together, for a player with Great Weapon Master or Sharpshooter. */
    public static void toggle(ServerPlayerEntity player) {
        ClassProgress progress = Progression.current(player);
        boolean gwm = progress.hasFeat(Feat.GREAT_WEAPON_MASTER);
        boolean sharp = progress.hasFeat(Feat.SHARPSHOOTER);
        if (!gwm && !sharp) {
            player.sendMessage(Text.literal("N toggles Power Attack (Great Weapon Master) and Steady Aim"
                    + " (Sharpshooter); you have neither feat.").formatted(Formatting.GRAY), true);
            return;
        }
        boolean on = !Feats.toggleOn(player);
        Feats.setToggle(player, on);
        String name = gwm && sharp ? "Power Attack and Steady Aim" : gwm ? "Power Attack" : "Steady Aim";
        player.sendMessage(Text.literal(name + ": " + (on ? "on" : "off"))
                .formatted(on ? Formatting.GOLD : Formatting.GRAY), true);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(),
                SoundCategory.PLAYERS, 0.4F, on ? 1.4F : 0.9F);
        DnDClasses.LOGGER.info("[Feats] {} {}: {}", player.getEntityName(), name, on ? "on" : "off");
    }

    static void sendCleave(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, S2C_CLEAVE, PacketByteBufs.empty());
    }
}

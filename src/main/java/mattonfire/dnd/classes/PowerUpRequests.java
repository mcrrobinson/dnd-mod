package mattonfire.dnd.classes;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.Misc.PowerUpEffect;
import mattonfire.dnd.classes.Progression.Abilities;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.SkillNode;
import mattonfire.dnd.classes.Rest.Charges;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The power-up key (C2S_POWERUP_EFFECTS_REQUEST_PACKET_ID): fires the equipped active skill (or the
 * class's power-up for a class without a tree) if the player can: not frozen or Downed, enough mana and
 * charges, not inside a Beholder's anti-magic cone. Sneak + power-up cycles the Druid's Wild Shape form.
 */
public final class PowerUpRequests {
        private PowerUpRequests() {
        }

        public static void register() {
                ServerPlayNetworking.registerGlobalReceiver(ClassPackets.C2S_POWERUP_EFFECTS_REQUEST_PACKET_ID,
                                PowerUpRequests::handle);
        }

        private static void handle(MinecraftServer server, ServerPlayerEntity player,
                        ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
                server.execute(() -> {
                        // Held by the Dungeon Master's freeze: no powers.
                        if (mattonfire.dnd.dm.DmFreeze.isFrozen(player)) {
                                return;
                        }
                        // Downed: no specials (holding the key gives up instead, see DownedEvents).
                        if (mattonfire.dnd.classes.Downed.Downed.is(player)) {
                                return;
                        }
                        // Stunned, Paralyzed, Petrified, Incapacitated: no specials.
                        if (!mattonfire.dnd.classes.Conditions.Conditions.canAct(player)) {
                                return;
                        }
                        // Firing a power gets you up from a short rest.
                        mattonfire.dnd.classes.Rest.RestSession.cancel(player, "you used a power");
                        // The equipped active skill; classes without a tree yet use their power-up at full mana.
                        mattonfire.dnd.classes.Progression.ClassProgress progress = Progression.current(player);
                        SkillNode skill = progress.activeNode();
                        int cost = skill == null ? DnDClasses.MANA_ICONS
                                        : mattonfire.dnd.classes.Progression.ClassSkills.manaCostFor(progress, skill);
                        // Sneak + power-up picks the Druid's Wild Shape form; free, so before the mana check.
                        if (DruidRules.cycleForm(player)) {
                                return;
                        }
                        int mana = ManaManager.getMana(player);
                        if (mana < cost) {
                                return;
                        }
                        // Major actives also cost charges, which only rests restore (off with dndRests false)
                        if (!Charges.canAfford(player, skill)) {
                                player.sendMessage(Text.literal("No charges left: rest to recover")
                                                .formatted(Formatting.RED), true);
                                return;
                        }
                        // A Beholder's anti-magic cone: the power fizzles and the mana is kept
                        if (mattonfire.dnd.classes.Effects.AntiMagicEffect.blocks(player)) {
                                return;
                        }

                        // Heals the skill lands on others count as outside heals: they revive Downed players
                        boolean success = mattonfire.dnd.classes.Downed.Revives.asHealer(player, () -> skill == null
                                        ? PowerUpEffect.play(server, player, Progression.classOf(player))
                                        : Abilities.activate(player, skill));
                        if (success) {
                                ManaManager.setMana(player, mana - cost);
                                ManaManager.sync(player);
                                Charges.spend(player, skill);
                                mattonfire.dnd.classes.Progression.ClassSkills skills = mattonfire.dnd.classes.Progression.ClassTrees
                                                .skills(progress.dndClass);
                                if (skills != null) {
                                        skills.afterActivate(player, progress, skill);
                                }
                                ServerPlayNetworking.send(player,
                                                ClassPackets.S2C_POWERUP_EFFECTS_PACKET_ID,
                                                new PacketByteBuf(Unpooled.buffer()));
                        }
                });

        }
}

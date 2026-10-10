package mattonfire.dnd.classes.SkillChecks;

import io.netty.buffer.Unpooled;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.PlayerEntityExt;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * d20 skill checks: roll a d20, add the player's class modifier and beat a DC. A natural 20 always
 * succeeds and a natural 1 always fails. The roll is shown to the player on the HUD with a sound
 * ({@code DiceRollHud} on the client, sent with {@link #show}).
 *
 * <ul>
 * <li>{@link Lockpicking}: Rogues pick the locks of dungeon and lair loot chests</li>
 * <li>{@link Persuasion}: Bards talk villagers into better prices</li>
 * <li>{@link AttackRolls}: every full-strength melee swing rolls; natural 20 crits, natural 1 fumbles</li>
 * <li>{@code Obstacles.ObstacleInteractions}: class-gated obstacles (Arcane Seals), with modifiers from
 * {@link SkillModifiers}</li>
 * </ul>
 */
public final class D20 {
    public static final Identifier S2C_ROLL = new Identifier(DnDClasses.MOD_ID, "d20_roll");

    private D20() {
    }

    public enum Skill {
        LOCKPICKING,
        PERSUASION,
        ATTACK,
        /** Class-gated obstacles: dispelling Arcane Seals ({@code classes/Obstacles}). */
        ARCANA;

        public String translationKey() {
            return "skill.dndclasses." + name().toLowerCase();
        }
    }

    public enum Outcome {
        SUCCESS,
        FAILURE,
        /** Natural 20 (or the top of an extended crit range). */
        CRITICAL,
        /** Natural 1. */
        FUMBLE;

        public boolean succeeded() {
            return this == SUCCESS || this == CRITICAL;
        }

        public String translationKey() {
            return "skill.dndclasses.outcome." + name().toLowerCase();
        }
    }

    /** A rolled check. {@code dc} is 0 for rolls without a target number (attack rolls). */
    public record Roll(Skill skill, int natural, int modifier, int dc, Outcome outcome) {
        public int total() {
            return natural + modifier;
        }

        public void write(PacketByteBuf buf) {
            buf.writeEnumConstant(skill);
            buf.writeVarInt(natural);
            buf.writeVarInt(modifier);
            buf.writeVarInt(dc);
            buf.writeEnumConstant(outcome);
        }

        public static Roll read(PacketByteBuf buf) {
            return new Roll(buf.readEnumConstant(Skill.class), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                    buf.readEnumConstant(Outcome.class));
        }
    }

    public static int d20(PlayerEntity player) {
        return 1 + player.getRandom().nextInt(20);
    }

    /** Rolls d20 + modifier against a DC. */
    public static Roll check(PlayerEntity player, Skill skill, int modifier, int dc) {
        int natural = d20(player);
        Outcome outcome;
        if (natural == 20) {
            outcome = Outcome.CRITICAL;
        } else if (natural == 1) {
            outcome = Outcome.FUMBLE;
        } else {
            outcome = natural + modifier >= dc ? Outcome.SUCCESS : Outcome.FAILURE;
        }
        return new Roll(skill, natural, modifier, dc, outcome);
    }

    /** Shows the roll on the player's HUD, with a line saying what came of it. */
    public static void show(PlayerEntity player, Roll roll, Text detail) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        roll.write(buf);
        buf.writeText(detail);
        ServerPlayNetworking.send(serverPlayer, S2C_ROLL, buf);
    }

    public static DndCharacter classOf(PlayerEntity player) {
        DndCharacter c = player instanceof PlayerEntityExt ext ? ext.getDndClass() : null;
        return c == null ? DndCharacter.NONE : c;
    }

    public static void register() {
        Lockpicking.register();
        Persuasion.register();
        AttackRolls.register();
    }
}

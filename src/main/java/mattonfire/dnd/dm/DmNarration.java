package mattonfire.dnd.dm;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModSounds;
import mattonfire.dnd.entity.boss.BossMusic;
import mattonfire.dnd.faction.Faction;
import mattonfire.dnd.faction.Reputation;
import mattonfire.dnd.particle.ModParticles;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

/**
 * The DM's storytelling commands:
 *
 * <pre>
 * /dm narrate [@selector] &lt;text&gt;              gold italic "✦" chat line with a page-turn sound; *word* is bold
 * /dm title [@selector] &lt;title&gt; [| &lt;subtitle&gt;]  on-screen title with a low drum hit
 * /dm say &lt;entity&gt; &lt;text&gt;                     "&lt;Name&gt; text" in the speaker's faction colour, speech bubble over it
 * /dm mood combat|tavern|dungeon|boss|off      (op only) force a music track for players within 96 blocks
 * </pre>
 *
 * Narration and titles go to every player unless the text starts with a target selector ({@code @a[distance=..20]},
 * {@code @p}, ...). Player names need a selector too ({@code @a[name=Alice]}), so a line can start with any word.
 */
final class DmNarration {
    public static final int TITLE_FADE_IN = 10;
    public static final int TITLE_STAY = 70;
    public static final int TITLE_FADE_OUT = 20;
    public static final double MOOD_RANGE = 96.0;
    /** The fake boss bar a mood track is filed under on the client ({@link BossMusic}). */
    public static final UUID MOOD_BAR = BossMusic.MOOD_BAR;

    private static final SimpleCommandExceptionType NO_PLAYERS = new SimpleCommandExceptionType(
            Text.literal("No players to tell"));
    private static final SimpleCommandExceptionType EMPTY = new SimpleCommandExceptionType(
            Text.literal("Nothing to say"));

    /** Forced music tracks. Each plays like a boss fight track: straight away, looping, over everything else. */
    enum Mood {
        COMBAT(ModSounds.MUSIC_GOBLIN_RAID),
        TAVERN(ModSounds.SONG_LUTE),
        DUNGEON(ModSounds.MUSIC_DUNGEON),
        BOSS(ModSounds.MUSIC_DRAGON_FIGHT),
        OFF(null);

        final SoundEvent track;

        Mood(SoundEvent track) {
            this.track = track;
        }

        String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private DmNarration() {
    }

    // ------------------------------------------------------------ commands

    static LiteralArgumentBuilder<ServerCommandSource> narrate() {
        return CommandManager.literal("narrate")
                .then(CommandManager.argument("text", StringArgumentType.greedyString())
                        .executes(DmNarration::narrate));
    }

    static LiteralArgumentBuilder<ServerCommandSource> title() {
        return CommandManager.literal("title")
                .then(CommandManager.argument("text", StringArgumentType.greedyString())
                        .executes(DmNarration::title));
    }

    static LiteralArgumentBuilder<ServerCommandSource> say() {
        return CommandManager.literal("say")
                .then(CommandManager.argument("entity", EntityArgumentType.entity())
                        .then(CommandManager.argument("text", StringArgumentType.greedyString())
                                .executes(DmNarration::say)));
    }

    static LiteralArgumentBuilder<ServerCommandSource> mood() {
        LiteralArgumentBuilder<ServerCommandSource> node = CommandManager.literal("mood")
                .requires(source -> source.hasPermissionLevel(2));
        for (Mood mood : Mood.values()) {
            node.then(CommandManager.literal(mood.id()).executes(context -> mood(context, mood)));
        }
        return node;
    }

    // ------------------------------------------------------------ narrate / title

    /** Who a line goes to, and the line without its leading selector. */
    private record Audience(List<ServerPlayerEntity> players, String text) {
    }

    private static Audience audience(ServerCommandSource source, String raw) throws CommandSyntaxException {
        String text = raw.strip();
        List<ServerPlayerEntity> players;
        if (text.startsWith("@")) {
            StringReader reader = new StringReader(text);
            EntitySelector selector = EntityArgumentType.players().parse(reader);
            players = selector.getPlayers(source);
            text = text.substring(reader.getCursor()).strip();
        } else {
            players = new ArrayList<>(source.getServer().getPlayerManager().getPlayerList());
        }
        if (text.isEmpty()) {
            throw EMPTY.create();
        }
        if (players.isEmpty()) {
            throw NO_PLAYERS.create();
        }
        return new Audience(players, text);
    }

    private static int narrate(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Audience audience = audience(source, StringArgumentType.getString(context, "text"));
        Text line = narration(audience.text());
        for (ServerPlayerEntity player : audience.players()) {
            player.sendMessage(line);
            player.playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.MASTER, 1.0F, 0.9F);
        }
        DnDClasses.LOGGER.info("[DM] narrate to {}: {}", names(audience.players()), audience.text());
        source.sendFeedback(Text.literal("Narrated to " + count(audience.players())).formatted(Formatting.GRAY),
                false);
        return audience.players().size();
    }

    /** "✦ text" in gold italics, with {@code *emphasis*} in bold. */
    static MutableText narration(String text) {
        MutableText line = Text.literal("✦ ");
        boolean bold = false;
        StringBuilder part = new StringBuilder();
        for (int i = 0; i <= text.length(); i++) {
            if (i == text.length() || text.charAt(i) == '*') {
                if (!part.isEmpty()) {
                    MutableText piece = Text.literal(part.toString());
                    line.append(bold ? piece.formatted(Formatting.BOLD) : piece);
                    part.setLength(0);
                }
                bold = !bold;
            } else {
                part.append(text.charAt(i));
            }
        }
        return line.formatted(Formatting.GOLD, Formatting.ITALIC);
    }

    private static int title(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Audience audience = audience(source, StringArgumentType.getString(context, "text"));
        String titleText = audience.text();
        String subtitleText = null;
        int bar = titleText.indexOf('|');
        if (bar >= 0) {
            subtitleText = titleText.substring(bar + 1).strip();
            titleText = titleText.substring(0, bar).strip();
        }
        Text title = Text.literal(titleText).formatted(Formatting.GOLD);
        Text subtitle = subtitleText == null || subtitleText.isEmpty() ? null
                : Text.literal(subtitleText).formatted(Formatting.YELLOW, Formatting.ITALIC);
        for (ServerPlayerEntity player : audience.players()) {
            player.networkHandler.sendPacket(new TitleFadeS2CPacket(TITLE_FADE_IN, TITLE_STAY, TITLE_FADE_OUT));
            if (subtitle != null) {
                player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
            }
            player.networkHandler.sendPacket(new TitleS2CPacket(title));
            // A low drum hit, with a deeper one under it
            player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value(), SoundCategory.MASTER, 1.0F, 0.5F);
            player.playSound(SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.MASTER, 1.0F, 0.6F);
        }
        DnDClasses.LOGGER.info("[DM] title to {}: {}{}", names(audience.players()), titleText,
                subtitle == null ? "" : " | " + subtitleText);
        source.sendFeedback(Text.literal("Title shown to " + count(audience.players())).formatted(Formatting.GRAY),
                false);
        return audience.players().size();
    }

    // ------------------------------------------------------------ say

    private static int say(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        Entity speaker = EntityArgumentType.getEntity(context, "entity");
        String text = StringArgumentType.getString(context, "text").strip();
        if (text.isEmpty()) {
            throw EMPTY.create();
        }
        Text line = Text.literal("<").append(speakerName(speaker)).append("> ").append(Text.literal(text));
        List<ServerPlayerEntity> players = source.getServer().getPlayerManager().getPlayerList();
        for (ServerPlayerEntity player : players) {
            player.sendMessage(line);
        }
        if (speaker.world instanceof ServerWorld world) {
            world.spawnParticles(ModParticles.SPEECH_BUBBLE, speaker.getX(), speaker.getY() + speaker.getHeight() + 0.6,
                    speaker.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        }
        DnDClasses.LOGGER.info("[DM] say as {}: {}", speaker.getName().getString(), text);
        return 1;
    }

    /** The speaker's name in its faction's colour (yellow for anyone outside a faction). */
    static MutableText speakerName(Entity speaker) {
        MutableText name = Text.literal(speaker.getName().getString());
        Faction faction = Reputation.factionOf(speaker);
        if (faction != null) {
            return name.styled(style -> style.withColor(TextColor.fromRgb(faction.color())));
        }
        return name.formatted(Formatting.YELLOW);
    }

    // ------------------------------------------------------------ mood

    private static int mood(CommandContext<ServerCommandSource> context, Mood mood) {
        ServerCommandSource source = context.getSource();
        List<ServerPlayerEntity> players = new ArrayList<>();
        Vec3d center = source.getPosition();
        for (ServerPlayerEntity player : source.getServer().getPlayerManager().getPlayerList()) {
            // Off reaches everyone, so nobody keeps a mood after walking away
            boolean inRange = player.getWorld() == source.getWorld()
                    && player.squaredDistanceTo(center) <= MOOD_RANGE * MOOD_RANGE;
            if (mood == Mood.OFF || inRange) {
                BossMusic.send(player, MOOD_BAR, mood.track);
                players.add(player);
            }
        }
        DnDClasses.LOGGER.info("[DM] mood {} ({}) for {}", mood.id(),
                mood.track == null ? "none" : mood.track.getId(), names(players));
        source.sendFeedback(Text.literal(mood == Mood.OFF ? "Mood music off"
                : "Mood " + mood.id() + " for " + count(players)).formatted(Formatting.GRAY), false);
        return players.size();
    }

    // ------------------------------------------------------------ helpers

    private static String count(List<ServerPlayerEntity> players) {
        return players.size() + (players.size() == 1 ? " player" : " players");
    }

    private static String names(List<ServerPlayerEntity> players) {
        return String.join(", ", players.stream().map(ServerPlayerEntity::getEntityName).toList());
    }
}

package mattonfire.dnd.magic.items;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.Proficiency;
import mattonfire.dnd.classes.Abilities.RollFilter;
import mattonfire.dnd.classes.Abilities.RollKind;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.Conditions;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.entity.boss.BossFight;
import mattonfire.dnd.magic.RemoveCurse;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * What the Deck of Many Things leaves on a player (design 10, 8.5), in persistent player data {@value #KEY}
 * (copied on respawn like the rest of the mod's data): Balance, Euryale, the Idiot, the Star, Fates, Moon wishes,
 * the Flames' nights, the Comet, the Throne's Persuasion and the Vizier's insight. This class turns them into the
 * character sheet (a dynamic contributor), a d20 {@link D20.Reroll} (wishes), a death hook (Fates), a boss-death
 * hook (Comet), a {@link RemoveCurse.Lifter} (Euryale, Void) and a nightly tick (the Flames' blazes).
 */
public final class DeckCurses {
    public static final String KEY = "dndDeck";
    public static final int EURYALE_SAVE_PENALTY = -2;
    public static final int STAR_BONUS = 2;
    public static final int STAR_CEILING = 24;
    public static final int IDIOT_FLOOR = 3;
    public static final int FATES_RESISTANCE_TICKS = 40;
    public static final int FLAMES_NIGHTS = 3;
    public static final int FLAMES_BLAZES = 3;
    public static final String VOID_KEY = "deck_void";
    public static final int VOID_TICKS = 24000;

    private DeckCurses() {
    }

    public static NbtCompound data(PlayerEntity player) {
        NbtCompound root = ((IEntityDataSaver) player).getPersistentData();
        if (!root.contains(KEY))
            root.put(KEY, new NbtCompound());
        return root.getCompound(KEY);
    }

    /** Call after changing the data: the sheet reads it. */
    static void changed(PlayerEntity player) {
        AbilityScores.invalidate(player);
    }

    public static void register() {
        AbilityScores.register(new Identifier(DnDClasses.MOD_ID, "magic/deck_of_many_things"), DeckCurses::contribute,
                true);

        // Moon: each wish turns the next failed d20 roll into a natural 20
        D20.registerReroll((player, kind, outcome) -> {
            NbtCompound d = data(player);
            int wishes = d.getInt("Wishes");
            if (wishes <= 0)
                return null;
            d.putInt("Wishes", wishes - 1);
            D20.wishNext(player);
            player.sendMessage(Text.translatable("deck.dndclasses.moon.answers", wishes - 1)
                    .formatted(Formatting.AQUA), false);
            DnDClasses.LOGGER.info("[Deck] {} spends a Moon wish ({} left)", player.getEntityName(), wishes - 1);
            return "spends a Moon wish";
        });

        // Remove Curse lifts Euryale; Void too, when someone else casts or reads it
        RemoveCurse.registerLifter((caster, target) -> {
            boolean lifted = false;
            NbtCompound d = data(target);
            if (d.getBoolean("Euryale")) {
                d.remove("Euryale");
                changed(target);
                target.sendMessage(Text.translatable("deck.dndclasses.euryale.lifted").formatted(Formatting.AQUA), false);
                DnDClasses.LOGGER.info("[Deck] Remove Curse ({}) lifts Euryale from {}", caster.getEntityName(),
                        target.getEntityName());
                lifted = true;
            }
            if (caster != target && Conditions.removeKey(target, Condition.PARALYZED, VOID_KEY)) {
                target.sendMessage(Text.translatable("deck.dndclasses.void.lifted").formatted(Formatting.AQUA), false);
                DnDClasses.LOGGER.info("[Deck] Remove Curse ({}) frees {}'s soul", caster.getEntityName(),
                        target.getEntityName());
                lifted = true;
            }
            return lifted;
        });

        // Fates: the next blow that would drop you to 0 leaves you on 1 HP (before the Downed rules and the locket)
        Identifier fatesPhase = new Identifier(DnDClasses.MOD_ID, "deck_fates");
        ServerLivingEntityEvents.ALLOW_DEATH.addPhaseOrdering(fatesPhase, Event.DEFAULT_PHASE);
        ServerLivingEntityEvents.ALLOW_DEATH.register(fatesPhase, (entity, source, amount) -> {
            if (!(entity instanceof ServerPlayerEntity player) || player.isCreative() || player.isSpectator()
                    || source.isOf(net.minecraft.entity.damage.DamageTypes.OUT_OF_WORLD))
                return true;
            NbtCompound d = data(player);
            if (!d.getBoolean("Fates"))
                return true;
            d.remove("Fates");
            player.setHealth(1.0F);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, FATES_RESISTANCE_TICKS, 4));
            player.getWorld().spawnParticles(ParticleTypes.ENCHANT, player.getX(), player.getBodyY(0.5),
                    player.getZ(), 40, 0.4, 0.8, 0.4, 0.5);
            player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK,
                    SoundCategory.PLAYERS, 1.0F, 0.7F);
            player.sendMessage(Text.translatable("deck.dndclasses.fates.saved").formatted(Formatting.LIGHT_PURPLE), false);
            DnDClasses.LOGGER.info("[Deck] Fates spares {}", player.getEntityName());
            return false;
        });

        // Comet: the next boss that dies in a fight you're in
        BossFight.registerDeathListener((fight, players) -> {
            for (ServerPlayerEntity player : players) {
                NbtCompound d = data(player);
                if (!d.getBoolean("Comet"))
                    continue;
                d.remove("Comet");
                int xp = DeckCards.levelXp(player);
                mattonfire.dnd.classes.Progression.Progression.addXp(player, xp);
                player.sendMessage(Text.translatable("deck.dndclasses.comet.done", xp).formatted(Formatting.GOLD), false);
                DnDClasses.LOGGER.info("[Deck] Comet: {} gets {} class XP for {}", player.getEntityName(), xp,
                        fight.getBoss().getName().getString());
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 100 == 0)
                flamesTick(server);
        });
    }

    // ---- The sheet ----

    private static void contribute(PlayerEntity player, mattonfire.dnd.classes.Abilities.Contribution c) {
        NbtCompound root = ((IEntityDataSaver) player).getPersistentData();
        if (!root.contains(KEY))
            return;
        NbtCompound d = root.getCompound(KEY);
        if (d.contains("BalanceHigh")) {
            Ability high = Ability.values()[d.getInt("BalanceHigh")];
            Ability low = Ability.values()[d.getInt("BalanceLow")];
            int gap = d.getInt("BalanceGap");
            c.shift(high, -gap, 1, 30, "Balance (Deck of Many Things)");
            c.shift(low, gap, 1, 30, "Balance (Deck of Many Things)");
        }
        if (d.getBoolean("Euryale"))
            c.saveBonus(null, EURYALE_SAVE_PENALTY, "Euryale (Deck of Many Things)");
        int intLoss = d.getInt("IntLoss");
        if (intLoss > 0)
            c.shift(Ability.INT, -intLoss, IDIOT_FLOOR, 30, "The Idiot (Deck of Many Things)");
        int star = d.getInt("StarBonus");
        if (star > 0 && d.contains("StarAbility"))
            c.shift(Ability.values()[d.getInt("StarAbility")], star, 1, STAR_CEILING, "The Star (Deck of Many Things)");
        if (d.getBoolean("Throne"))
            c.proficiency(Skill.PERSUASION, Proficiency.EXPERTISE, "The Throne (Deck of Many Things)");
        World world = player.getWorld();
        if (d.getLong("VizierUntil") > world.getTime())
            c.advantage(RollFilter.kind(RollKind.CHECK), "all ability checks", "The Vizier (Deck of Many Things)");
    }

    // ---- Moon wishes against curses ----

    /** Whether the player carries a deck curse a wish can lift. */
    static @Nullable String firstCurse(ServerPlayerEntity player) {
        NbtCompound d = data(player);
        if (Conditions.get(player, Condition.PARALYZED) != null && hasVoid(player))
            return "void";
        if (d.getBoolean("Euryale"))
            return "euryale";
        if (d.contains("BalanceHigh"))
            return "balance";
        if (d.getInt("IntLoss") > 0)
            return "idiot";
        if (d.getInt("FlamesNights") > 0)
            return "flames";
        return null;
    }

    private static boolean hasVoid(ServerPlayerEntity player) {
        return Conditions.all(player).stream().anyMatch(i -> i.condition() == Condition.PARALYZED
                && VOID_KEY.equals(i.key()));
    }

    /** Spends wishes on deck curses, one wish each, while there are both. */
    static void wishAwayCurses(ServerPlayerEntity player) {
        NbtCompound d = data(player);
        while (d.getInt("Wishes") > 0) {
            String curse = firstCurse(player);
            if (curse == null)
                return;
            switch (curse) {
                case "void" -> Conditions.removeKey(player, Condition.PARALYZED, VOID_KEY);
                case "euryale" -> d.remove("Euryale");
                case "balance" -> {
                    d.remove("BalanceHigh");
                    d.remove("BalanceLow");
                    d.remove("BalanceGap");
                }
                case "idiot" -> d.remove("IntLoss");
                case "flames" -> d.remove("FlamesNights");
                default -> {
                }
            }
            d.putInt("Wishes", d.getInt("Wishes") - 1);
            changed(player);
            player.sendMessage(Text.translatable("deck.dndclasses.moon.lifts",
                    Text.translatable("deck.dndclasses.curse." + curse)).formatted(Formatting.AQUA), false);
            DnDClasses.LOGGER.info("[Deck] {} wishes away {} ({} wishes left)", player.getEntityName(), curse,
                    d.getInt("Wishes"));
        }
    }

    /** Lifts every deck curse (admin: {@code /dndmagic deck <player> clear}). */
    public static void clear(ServerPlayerEntity player) {
        ((IEntityDataSaver) player).getPersistentData().remove(KEY);
        Conditions.removeKey(player, Condition.PARALYZED, VOID_KEY);
        changed(player);
    }

    // ---- The Flames' blazes ----

    private static void flamesTick(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        if (!overworld.isNight())
            return;
        long night = overworld.getTimeOfDay() / 24000L;
        for (ServerPlayerEntity player : overworld.getPlayers()) {
            NbtCompound d = data(player);
            int nights = d.getInt("FlamesNights");
            if (nights <= 0 || (d.contains("FlamesLastNight") && d.getLong("FlamesLastNight") == night)
                    || player.isSpectator() || player.isCreative())
                continue;
            int spawned = spawnBlazes(overworld, player);
            if (spawned == 0)
                continue;
            d.putLong("FlamesLastNight", night);
            d.putInt("FlamesNights", nights - 1);
            player.sendMessage(Text.translatable("deck.dndclasses.flames.blazes").formatted(Formatting.RED), false);
            DnDClasses.LOGGER.info("[Deck] Flames: {} blazes hunt {} ({} nights left)", spawned,
                    player.getEntityName(), nights - 1);
        }
    }

    private static int spawnBlazes(ServerWorld world, ServerPlayerEntity player) {
        var random = player.getRandom();
        int spawned = 0;
        for (int attempt = 0; attempt < 40 && spawned < FLAMES_BLAZES; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = 8 + random.nextDouble() * 8;
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
            BlockPos spot = null;
            for (int dy = 4; dy >= -6 && spot == null; dy--) {
                BlockPos p = new BlockPos(x, player.getBlockY() + dy, z);
                if (world.getBlockState(p.down()).isSolidBlock(world, p.down()) && world.isAir(p) && world.isAir(p.up()))
                    spot = p;
            }
            if (spot == null)
                continue;
            BlazeEntity blaze = EntityType.BLAZE.create(world);
            if (blaze == null)
                continue;
            blaze.refreshPositionAndAngles(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, random.nextFloat() * 360, 0);
            blaze.initialize(world, world.getLocalDifficulty(spot), SpawnReason.EVENT, null, null);
            blaze.setTarget(player);
            world.spawnEntity(blaze);
            spawned++;
        }
        return spawned;
    }
}

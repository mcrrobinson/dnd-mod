package mattonfire.dnd.magic.items;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Pair;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.CharacterSheet;
import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.Conditions;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.entity.HirelingEntity;
import mattonfire.dnd.entity.hireling.HirelingRace;
import mattonfire.dnd.entity.hireling.HirelingRole;
import mattonfire.dnd.entity.hireling.Hirelings;
import mattonfire.dnd.faction.Faction;
import mattonfire.dnd.faction.Factions;
import mattonfire.dnd.faction.FactionIds;
import mattonfire.dnd.faction.Reputation;
import mattonfire.dnd.magic.Attunement;
import mattonfire.dnd.magic.MagicData;
import mattonfire.dnd.magic.MagicItemLootFunction;
import mattonfire.dnd.magic.MagicItems;
import mattonfire.dnd.magic.MagicTier;
import net.minecraft.entity.ItemEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.structure.Structure;

/**
 * The 22 cards of the Deck of Many Things and what each does (design 10, 8.5). The first 13 make the
 * 13-card deck; the {@link DeckCurses} keep what lasts.
 */
public final class DeckCards {
    public static final TagKey<Structure> BOSS_LAIRS = TagKey.of(RegistryKeys.STRUCTURE,
            new Identifier(DnDClasses.MOD_ID, "boss_lairs"));
    public static final net.minecraft.registry.tag.TagKey<net.minecraft.item.Item> RUIN_WEALTH = TagKey.of(
            RegistryKeys.ITEM, new Identifier(DnDClasses.MOD_ID, "deck_ruin_wealth"));
    public static final Identifier KEY_TABLE = new Identifier(DnDClasses.MOD_ID, "magic/deck_key");
    public static final Identifier SUN_TABLE = new Identifier(DnDClasses.MOD_ID, "magic/deck_sun");
    public static final Identifier RARE_TABLE = new Identifier(DnDClasses.MOD_ID, "magic/rare");
    /** The Vizier's search radius, in blocks. */
    public static final int VIZIER_RANGE = 2000;
    public static final int VIZIER_TICKS = 24000;
    public static final int ROGUE_LOSS = 300;
    public static final int THRONE_GAIN = 250;
    public static final int FLAMES_FLOOR = -600;
    public static final int GEM_DIAMONDS = 20;
    public static final int GEM_EMERALDS = 10;

    public enum Card {
        SUN(false), MOON(false), STAR(false), THRONE(false), KEY(false), KNIGHT(false), VOID(true), FLAMES(true),
        SKULL(true), RUIN(true), EURYALE(true), ROGUE(true), JESTER(false),
        // The 22-card deck adds these
        VIZIER(false), COMET(false), FATES(false), GEM(false), TALONS(true), IDIOT(true), DONJON(true),
        BALANCE(true), FOOL(true);

        public static final int SMALL_DECK = 13;
        public final boolean bad;

        Card(boolean bad) {
            this.bad = bad;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Text title() {
            return Text.translatable("deck.dndclasses.card." + id());
        }

        public Text line() {
            return Text.translatable("deck.dndclasses.card." + id() + ".line");
        }

        /** The Fool and the Jester leave the deck when drawn; the rest go back in. */
        public boolean leavesDeck() {
            return this == FOOL || this == JESTER;
        }

        public static @Nullable Card byId(String id) {
            for (Card card : values())
                if (card.id().equals(id))
                    return card;
            return null;
        }
    }

    /** What a draw did to the deck itself. */
    public enum Outcome {
        /** Nothing special: the draw counts. */
        NORMAL,
        /** The Fool: one more card, not counted against the declared number. */
        EXTRA_DRAW,
        /** The Donjon: the remaining draws are lost and the deck vanishes. */
        DECK_ENDS
    }

    private DeckCards() {
    }

    /** One level's worth of class XP: the gap from the player's level to the next (the last gap at the cap). */
    public static int levelXp(ServerPlayerEntity player) {
        ClassProgress progress = Progression.current(player);
        int level = progress.level();
        int[] xp = ClassProgress.LEVEL_XP;
        return level < ClassProgress.MAX_LEVEL ? xp[level + 1] - xp[level] : xp[level] - xp[level - 1];
    }

    public static Outcome apply(ServerPlayerEntity player, Card card) {
        ServerWorld world = player.getWorld();
        NbtCompound d = DeckCurses.data(player);
        Outcome outcome = Outcome.NORMAL;
        switch (card) {
            case SUN -> {
                gainLevel(player);
                give(player, roll(player, SUN_TABLE));
            }
            case MOON -> {
                int wishes = 1 + player.getRandom().nextInt(3);
                d.putInt("Wishes", d.getInt("Wishes") + wishes);
                say(player, Text.translatable("deck.dndclasses.moon.wishes", wishes), Formatting.AQUA);
            }
            case STAR -> {
                Ability best = highest(AbilityScores.sheet(player));
                if (d.contains("StarAbility") && d.getInt("StarAbility") != best.ordinal()) {
                    // A second Star adds to the first one's score
                    best = Ability.values()[d.getInt("StarAbility")];
                }
                d.putInt("StarAbility", best.ordinal());
                d.putInt("StarBonus", d.getInt("StarBonus") + DeckCurses.STAR_BONUS);
                DeckCurses.changed(player);
                say(player, Text.translatable("deck.dndclasses.star.done", Text.translatable(best.translationKey()),
                        AbilityScores.score(player, best)), Formatting.GOLD);
            }
            case THRONE -> {
                Faction best = bestFaction(player);
                if (best != null)
                    Reputation.add(player, best, THRONE_GAIN, Reputation.Source.COMMAND);
                d.putBoolean("Throne", true);
                DeckCurses.changed(player);
            }
            case KEY -> give(player, roll(player, KEY_TABLE));
            case KNIGHT -> knight(player);
            case VOID -> {
                Conditions.of(player, Condition.PARALYZED).key(DeckCurses.VOID_KEY).ticks(DeckCurses.VOID_TICKS)
                        .sourceName(Text.translatable("item.dndclasses.deck_of_many_things")).apply();
            }
            case FLAMES -> {
                Faction covenant = Factions.get(FactionIds.BRIMSTONE_COVENANT);
                if (covenant != null && Reputation.get(player, covenant) > FLAMES_FLOOR)
                    Reputation.set(player, covenant, FLAMES_FLOOR);
                d.putInt("FlamesNights", DeckCurses.FLAMES_NIGHTS);
                d.remove("FlamesLastNight");
            }
            case SKULL -> DeckAvatarEntity.rise(player, null);
            case RUIN -> ruin(player);
            case EURYALE -> {
                d.putBoolean("Euryale", true);
                DeckCurses.changed(player);
            }
            case ROGUE -> {
                Faction best = bestFaction(player);
                if (best != null) {
                    Reputation.add(player, best, -ROGUE_LOSS, Reputation.Source.COMMAND);
                    say(player, Text.translatable("deck.dndclasses.rogue.done", best.displayName()), Formatting.RED);
                }
            }
            case JESTER -> gainLevel(player);
            case VIZIER -> vizier(player);
            case COMET -> d.putBoolean("Comet", true);
            case FATES -> d.putBoolean("Fates", true);
            case GEM -> {
                drop(player, new ItemStack(Items.DIAMOND, GEM_DIAMONDS));
                drop(player, new ItemStack(Items.EMERALD, GEM_EMERALDS));
            }
            case TALONS -> talons(player);
            case IDIOT -> {
                int loss = 1 + player.getRandom().nextInt(4);
                d.putInt("IntLoss", d.getInt("IntLoss") + loss);
                DeckCurses.changed(player);
                say(player, Text.translatable("deck.dndclasses.idiot.done", loss, AbilityScores.score(player, Ability.INT)),
                        Formatting.RED);
            }
            case DONJON -> {
                DonjonCell.build(player);
                outcome = Outcome.DECK_ENDS;
            }
            case BALANCE -> balance(player, d);
            case FOOL -> {
                loseLevel(player);
                outcome = Outcome.EXTRA_DRAW;
            }
        }
        // A wish in hand lifts a fresh curse at once
        DeckCurses.wishAwayCurses(player);
        return outcome;
    }

    // ---- Card helpers ----

    private static void say(ServerPlayerEntity player, Text text, Formatting color) {
        player.sendMessage(text.copy().formatted(color), false);
    }

    private static void gainLevel(ServerPlayerEntity player) {
        if (Progression.classOf(player) == DndCharacter.NONE)
            return;
        Progression.addXp(player, levelXp(player));
    }

    private static void loseLevel(ServerPlayerEntity player) {
        DndCharacter dndClass = Progression.classOf(player);
        if (dndClass == DndCharacter.NONE)
            return;
        ClassProgress progress = Progression.current(player);
        int level = progress.level();
        int[] xp = ClassProgress.LEVEL_XP;
        int loss = level > 0 ? xp[level] - xp[level - 1] : xp[1];
        Progression.setXp(player, dndClass, Math.max(0, progress.xp - loss));
        AbilityScores.invalidate(player);
    }

    static Ability highest(CharacterSheet sheet) {
        Ability best = Ability.STR;
        for (Ability a : Ability.values())
            if (sheet.score(a) > sheet.score(best))
                best = a;
        return best;
    }

    private static Ability lowest(CharacterSheet sheet) {
        Ability worst = Ability.STR;
        for (Ability a : Ability.values())
            if (sheet.score(a) < sheet.score(worst))
                worst = a;
        return worst;
    }

    private static void balance(ServerPlayerEntity player, NbtCompound d) {
        if (d.contains("BalanceHigh")) {
            // A second Balance swaps them back
            d.remove("BalanceHigh");
            d.remove("BalanceLow");
            d.remove("BalanceGap");
            DeckCurses.changed(player);
            say(player, Text.translatable("deck.dndclasses.balance.back"), Formatting.LIGHT_PURPLE);
            return;
        }
        CharacterSheet sheet = AbilityScores.sheet(player);
        Ability high = highest(sheet);
        Ability low = lowest(sheet);
        int gap = sheet.score(high) - sheet.score(low);
        if (gap <= 0)
            return;
        d.putInt("BalanceHigh", high.ordinal());
        d.putInt("BalanceLow", low.ordinal());
        d.putInt("BalanceGap", gap);
        DeckCurses.changed(player);
        say(player, Text.translatable("deck.dndclasses.balance.done", Text.translatable(high.translationKey()),
                Text.translatable(low.translationKey())), Formatting.LIGHT_PURPLE);
    }

    /** The faction that likes the player best (the first of equals), or null with no factions. */
    static @Nullable Faction bestFaction(ServerPlayerEntity player) {
        Faction best = null;
        int bestRep = Integer.MIN_VALUE;
        for (Faction faction : Factions.all()) {
            int rep = Reputation.get(player, faction);
            if (rep > bestRep) {
                best = faction;
                bestRep = rep;
            }
        }
        return best;
    }

    /** One roll on a loot table, identified (known goods: a random curse is lifted). */
    static List<ItemStack> roll(ServerPlayerEntity player, Identifier tableId) {
        ServerWorld world = player.getWorld();
        LootTable table = world.getServer().getLootManager().getTable(tableId);
        List<ItemStack> loot = table.generateLoot(new LootContext.Builder(world)
                .parameter(LootContextParameters.ORIGIN, player.getPos())
                .parameter(LootContextParameters.THIS_ENTITY, player)
                .random(player.getRandom())
                .build(LootContextTypes.GIFT));
        for (ItemStack stack : loot) {
            MagicItemLootFunction.apply(stack, null, null, true);
            MagicItemLootFunction.rollCurse(stack, -1.0F, true, player.getRandom());
        }
        return loot;
    }

    private static void give(ServerPlayerEntity player, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            say(player, Text.translatable("deck.dndclasses.gift", stack.toHoverableText()), Formatting.GOLD);
            player.getInventory().offerOrDrop(stack);
        }
    }

    private static void drop(ServerPlayerEntity player, ItemStack stack) {
        ItemEntity item = new ItemEntity(player.getWorld(), player.getX(), player.getY() + 0.2, player.getZ(), stack);
        item.setToDefaultPickupDelay();
        player.getWorld().spawnEntity(item);
    }

    private static void knight(ServerPlayerEntity player) {
        var server = player.getServer();
        if (Hirelings.count(server, player.getUuid()) >= Hirelings.MAX_PER_PLAYER
                || Hirelings.partyCount(server, player.getUuid()) >= Hirelings.MAX_PER_PARTY) {
            say(player, Text.translatable("deck.dndclasses.knight.full"), Formatting.GOLD);
            give(player, roll(player, RARE_TABLE));
            return;
        }
        HirelingRace[] races = HirelingRace.values();
        HirelingEntity knight = Hirelings.spawnHired(player, HirelingRole.TANK,
                races[player.getRandom().nextInt(races.length)], null);
        knight.setSworn(true);
        say(player, Text.translatable("deck.dndclasses.knight.done", knight.getDisplayName()), Formatting.GOLD);
    }

    /** Every piece of wealth in the inventory and the ender chest vanishes (never a magic item). */
    private static void ruin(ServerPlayerEntity player) {
        int lost = strip(player.getInventory()) + strip(player.getEnderChestInventory());
        DnDClasses.LOGGER.info("[Deck] Ruin takes {} items from {}", lost, player.getEntityName());
        say(player, Text.translatable("deck.dndclasses.ruin.done", lost), Formatting.RED);
    }

    private static int strip(Inventory inventory) {
        int lost = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && stack.isIn(RUIN_WEALTH) && MagicItems.info(stack) == null) {
                lost += stack.getCount();
                inventory.setStack(i, ItemStack.EMPTY);
            }
        }
        if (inventory instanceof EnderChestInventory)
            inventory.markDirty();
        return lost;
    }

    /** Every magic item carried or worn crumbles, except Legendary ones; their bonds end. */
    private static void talons(ServerPlayerEntity player) {
        var inventory = player.getInventory();
        int lost = 0;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            MagicItems.Info info = MagicItems.info(stack);
            if (info == null || info.tier() == MagicTier.LEGENDARY)
                continue;
            var uuid = MagicData.uuid(stack);
            if (uuid != null)
                Attunement.release(player, uuid, true);
            DnDClasses.LOGGER.info("[Deck] Talons: {}'s {} crumbles", player.getEntityName(), stack.getName().getString());
            inventory.setStack(i, ItemStack.EMPTY);
            lost++;
        }
        Attunement.sync(player);
        say(player, Text.translatable("deck.dndclasses.talons.done", lost), Formatting.RED);
    }

    /** The nearest boss lair within range, and advantage on checks for a day. */
    private static void vizier(ServerPlayerEntity player) {
        ServerWorld world = player.getWorld();
        NbtCompound d = DeckCurses.data(player);
        d.putLong("VizierUntil", world.getTime() + VIZIER_TICKS);
        DeckCurses.changed(player);
        var lairs = world.getRegistryManager().get(RegistryKeys.STRUCTURE).getEntryList(BOSS_LAIRS);
        Pair<BlockPos, RegistryEntry<Structure>> found = null;
        if (lairs.isPresent()) {
            found = world.getChunkManager().getChunkGenerator().locateStructure(world,
                    (RegistryEntryList<Structure>) lairs.get(), player.getBlockPos(), VIZIER_RANGE / 16, false);
        }
        if (found == null) {
            say(player, Text.translatable("deck.dndclasses.vizier.none", VIZIER_RANGE), Formatting.GRAY);
            DnDClasses.LOGGER.info("[Deck] Vizier: no boss lair within {} of {}", VIZIER_RANGE, player.getEntityName());
            return;
        }
        BlockPos pos = found.getFirst();
        String id = found.getSecond().getKey().map(k -> k.getValue().getPath()).orElse("lair");
        Text name = Text.translatable("deck.dndclasses.lair." + id);
        int distance = MathHelper.floor(Math.sqrt(pos.getSquaredDistance(player.getBlockPos().withY(pos.getY()))));
        say(player, Text.translatable("deck.dndclasses.vizier.done", name, pos.getX(), pos.getZ(), distance),
                Formatting.AQUA);
        DnDClasses.LOGGER.info("[Deck] Vizier: {} is told of {} at {} {}", player.getEntityName(), id, pos.getX(),
                pos.getZ());
    }
}

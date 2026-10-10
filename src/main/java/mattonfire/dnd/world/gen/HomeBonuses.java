package mattonfire.dnd.world.gen;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.entity.MountainDwarfEntity;
import mattonfire.dnd.tavern.Tavern;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.village.VillageGossipType;
import net.minecraft.village.VillagerProfession;

/**
 * What a race gets at its own home ({@link RacialHomes}), checked once a second for each player:
 * <ul>
 * <li>Welcome: a "Welcome home" title on arriving, again after 5 minutes away.</li>
 * <li>Hearth: Regeneration I in the hearth piece while no hostile mob is within 16 blocks.</li>
 * <li>A first-visit gift per settlement (the Shire welcome basket).</li>
 * </ul>
 * Plus the per-settlement extras: Halflings get hobbit gifts every 2 minutes, a Dwarf can ask the Dwarf
 * King for an audience once a day, and Humans count vanilla villages as home (villagers gossip well of
 * them the first time they trade). Kin prices are {@link mattonfire.dnd.entity.KinPrices}, kin trust
 * {@link mattonfire.dnd.entity.SettlementGrudges}.
 */
public final class HomeBonuses {
    /** Ticks away from home before the welcome shows again: 5 minutes. */
    public static final int WELCOME_REARM = 20 * 60 * 5;
    /** The hearth refreshes Regeneration every 3 s, for 5 s, so it never runs out while you stay. */
    public static final int HEARTH_INTERVAL = 60;
    public static final int HEARTH_DURATION = 100;
    public static final double HEARTH_HOSTILE_RANGE = 16.0D;
    /** How long a hobbit makes a Halfling wait between gifts at Neutral (everyone else waits 5 minutes). */
    public static final int HALFLING_GIFT_COOLDOWN = 20 * 60 * 2;
    /** The Neutral wait the Halfling one is scaled from: Halflings wait 2/5 of their tier's wait. */
    private static final int NORMAL_GIFT_COOLDOWN = 20 * 60 * 5;
    /** Gossip a villager gains for a Human the first time they trade. */
    public static final int HUMAN_KIN_GOSSIP = 10;

    private static final String BASKETS_KEY = "DndHomeBaskets";
    private static final String AUDIENCE_KEY = "DndKingAudienceDay";
    private static final String GREETED_TAG = "dndclasses.kin_greeted.";
    private static final Item[] KING_TOOLS = {Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_SWORD};

    /** Server tick each player was last seen in their own home. */
    private static final Map<UUID, Long> lastAtHome = new HashMap<>();

    /** Called after each Dwarf King audience (a quest hook: "seek an audience with the king"). */
    public static BiConsumer<ServerPlayerEntity, MountainDwarfEntity> onKingAudience = (player, king) -> {
    };

    private HomeBonuses() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 == 0) {
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    secondTick(server, player);
                }
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> lastAtHome.remove(handler.getPlayer().getUuid()));
        // Before the villager's own interaction, so the gossip already counts for the first trade screen.
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!world.isClient && hand == Hand.MAIN_HAND && entity instanceof VillagerEntity villager) {
                greetHuman(player, villager);
            }
            return ActionResult.PASS;
        });
    }

    private static void secondTick(MinecraftServer server, ServerPlayerEntity player) {
        if (player.isSpectator() || !player.isAlive()) {
            return;
        }
        RacialHomes.Visit visit = RacialHomes.ownHomeAt(player);
        if (visit == null) {
            return;
        }
        long now = server.getTicks();
        Long last = lastAtHome.put(player.getUuid(), now);
        if (last == null || now - last > WELCOME_REARM) {
            welcome(player, visit);
        }
        if (now % HEARTH_INTERVAL == 0 && visit.inHearth(player.getBlockPos()) && !hostileNear(player)) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, HEARTH_DURATION, 0, true, true, true));
        }
    }

    private static boolean hostileNear(PlayerEntity player) {
        return !player.world.getOtherEntities(player, new Box(player.getBlockPos()).expand(HEARTH_HOSTILE_RANGE),
                entity -> entity instanceof Monster && entity.isAlive()).isEmpty();
    }

    private static void welcome(ServerPlayerEntity player, RacialHomes.Visit visit) {
        DndRace race = visit.home().race();
        player.networkHandler.sendPacket(new TitleFadeS2CPacket(10, 60, 20));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(visit.home().displayName().formatted(Formatting.GRAY)));
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.translatable("message.dndclasses.home.welcome",
                Text.translatable("race.dndclasses." + race.id())).formatted(Formatting.GOLD)));
        DnDClasses.LOGGER.info("[RacialHomes] Welcomed {} home to {} at chunk {}", player.getEntityName(),
                visit.home().structure(), visit.start().getPos());
        if (visit.home().firstVisit() != null && firstVisit(player, visit)) {
            visit.home().firstVisit().accept(player);
        }
    }

    /** Whether this is {@code player}'s first visit to this settlement; remembers it. */
    private static boolean firstVisit(ServerPlayerEntity player, RacialHomes.Visit visit) {
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        long[] seen = data.getLongArray(BASKETS_KEY);
        for (long id : seen) {
            if (id == visit.id()) {
                return false;
            }
        }
        long[] more = java.util.Arrays.copyOf(seen, seen.length + 1);
        more[seen.length] = visit.id();
        data.putLongArray(BASKETS_KEY, more);
        return true;
    }

    /** A Halfling's first visit to a hobbit village: 1 cake, 4 cookies and 2 mugs of ale. */
    static void shireWelcomeBasket(ServerPlayerEntity player) {
        give(player, new ItemStack(Items.CAKE));
        give(player, new ItemStack(Items.COOKIE, 4));
        give(player, new ItemStack(Tavern.ALE, 2));
        player.sendMessage(Text.translatable("message.dndclasses.home.shire_basket").formatted(Formatting.GREEN), false);
        player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.6F, 1.4F);
    }

    // ---- Per-settlement extras ----

    /**
     * How long a hobbit makes {@code player} wait between gifts, given the wait for their reputation tier:
     * Halflings wait 2/5 of it (2 minutes instead of 5 at Neutral or Friendly).
     */
    public static int giftCooldown(PlayerEntity player, int normal) {
        return RacialHomes.HOBBIT_VILLAGE.isHomeOf(RaceLifecycle.activeRaceOf(player))
                ? (int) ((long) normal * HALFLING_GIFT_COOLDOWN / NORMAL_GIFT_COOLDOWN) : normal;
    }

    /**
     * A Dwarf asks the Dwarf King for an audience (an empty hand): once per in-game day he grants a random
     * enchanted iron tool or 3 gold ingots. Stored as {@code DndKingAudienceDay} in persistent data.
     */
    public static void kingAudience(ServerPlayerEntity player, MountainDwarfEntity king) {
        if (king.getTarget() != null || king.shouldAngerAt(player)) {
            king.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, king.getSoundPitch());
            return;
        }
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        long today = player.getServer().getOverworld().getTimeOfDay() / 24000L;
        if (data.contains(AUDIENCE_KEY) && data.getLong(AUDIENCE_KEY) == today) {
            king.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, king.getSoundPitch());
            player.sendMessage(Text.translatable("message.dndclasses.home.king_busy", king.getDisplayName())
                    .formatted(Formatting.GRAY), true);
            return;
        }
        data.putLong(AUDIENCE_KEY, today);
        ItemStack gift;
        if (king.getRandom().nextBoolean()) {
            gift = new ItemStack(Items.GOLD_INGOT, 3);
        } else {
            gift = new ItemStack(KING_TOOLS[king.getRandom().nextInt(KING_TOOLS.length)]);
            gift = EnchantmentHelper.enchant(king.getRandom(), gift, 15, false);
        }
        Text message = Text.translatable("message.dndclasses.home.king_audience", king.getDisplayName(),
                gift.getCount(), gift.getName()).formatted(Formatting.GOLD);
        give(player, gift);
        king.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0F, king.getSoundPitch());
        player.sendMessage(message, false);
        onKingAudience.accept(player, king);
    }

    /** A Human's first trade with a villager: +10 minor positive gossip (vanilla villages are the Human home). */
    private static void greetHuman(PlayerEntity player, VillagerEntity villager) {
        if (RaceLifecycle.activeRaceOf(player) != DndRace.HUMAN || villager.isBaby()) {
            return;
        }
        VillagerProfession profession = villager.getVillagerData().getProfession();
        if (profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) {
            return;
        }
        String tag = GREETED_TAG + player.getUuidAsString();
        if (villager.getCommandTags().contains(tag) || !villager.addCommandTag(tag)) {
            return;
        }
        villager.getGossip().startGossip(player.getUuid(), VillageGossipType.MINOR_POSITIVE, HUMAN_KIN_GOSSIP);
    }

    private static void give(PlayerEntity player, ItemStack stack) {
        if (!player.giveItemStack(stack)) {
            player.dropItem(stack, false);
        }
    }
}

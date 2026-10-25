package mattonfire.dnd.classes.SkillChecks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.Abilities.RollKind;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.entity.FrostDrakeEntity;
import mattonfire.dnd.entity.LichEntity;
import mattonfire.dnd.entity.ModEntityTypes;
import mattonfire.dnd.entity.MountainDwarfEntity;
import mattonfire.dnd.entity.OwlbearEntity;
import mattonfire.dnd.entity.boss.Boss;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSources;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.EntityTypeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Study: the Search/Study key (V) pressed while looking at a creature, registered as a
 * {@link Perception.TargetHandler}. The player makes a Lore check picked by the creature's group
 * ({@link Lore}: Religion for undead, Nature for beasts, Arcana for aberrations and magic, Insight for
 * humanoids) against {@link #dc}: {@value #DC_VANILLA} for vanilla mobs, {@value #DC_MOD} for this mod's,
 * {@value #DC_BOSS} for bosses.
 *
 * <ul>
 * <li>Success: a chat card with health, armour, the saves its attacks call for ({@link MobSaveInfo}) and its
 * immunities. The creature type is then known for good (player persistent data, {@link #KNOWN_KEY}): later
 * presses show the card again without a roll.</li>
 * <li>Natural 20: the card also shows its weakness ({@link #registerWeakness}).</li>
 * <li>Failure: that creature type can't be studied again for {@link #FAIL_COOLDOWN} ticks.</li>
 * <li>Insight on a humanoid adds what you read in it ({@link #registerInsight}): a villager's opinion of you
 * and whether you've already tried to persuade it today, or a Mountain Dwarf's grudge.</li>
 * </ul>
 */
public final class Study {
    public static final int DC_VANILLA = 10;
    public static final int DC_MOD = 13;
    public static final int DC_BOSS = 15;
    /** How long a failed Study keeps the player from trying that creature type again (60 s). */
    public static final int FAIL_COOLDOWN = 20 * 60;
    /** Player persistent data: creature type id to {@link #KNOWN} or {@link #KNOWN_WEAKNESS}. */
    public static final String KNOWN_KEY = "dndKnownCreatures";
    public static final byte KNOWN = 1;
    /** Known, weakness included (learned on a natural 20). */
    public static final byte KNOWN_WEAKNESS = 2;

    /** Creature groups and the skill that knows about them, from the {@code #dndclasses:lore/*} tags. */
    public enum Lore {
        UNDEAD(Skill.RELIGION),
        HUMANOID(Skill.INSIGHT),
        ARCANE(Skill.ARCANA),
        BEASTS(Skill.NATURE);

        private final Skill skill;
        private final TagKey<EntityType<?>> tag;

        Lore(Skill skill) {
            this.skill = skill;
            this.tag = TagKey.of(RegistryKeys.ENTITY_TYPE,
                    new Identifier(DnDClasses.MOD_ID, "lore/" + name().toLowerCase(java.util.Locale.ROOT)));
        }

        public Skill skill() {
            return skill;
        }

        public TagKey<EntityType<?>> tag() {
            return tag;
        }
    }

    /** Extra lines an Insight Study reads from a humanoid (empty if it has nothing to say). */
    @FunctionalInterface
    public interface InsightReader {
        List<Text> read(ServerPlayerEntity player, LivingEntity target);
    }

    private static final Map<EntityType<?>, List<Function<LivingEntity, Text>>> WEAKNESSES = new LinkedHashMap<>();
    private static final List<InsightReader> INSIGHT_READERS = new CopyOnWriteArrayList<>();
    /** Player to (creature type to the server tick its fail cooldown ends). */
    private static final Map<UUID, Map<EntityType<?>, Integer>> FAILED = new HashMap<>();

    private Study() {
    }

    // ---------------------------------------------------------------- registration

    /** Adds a weakness line to a creature type's card, shown once the player has rolled a natural 20 on it. */
    public static synchronized void registerWeakness(EntityType<?> type, Function<LivingEntity, Text> line) {
        WEAKNESSES.computeIfAbsent(type, k -> new ArrayList<>()).add(line);
    }

    public static void registerWeakness(EntityType<?> type, Text line) {
        registerWeakness(type, target -> line);
    }

    /** Adds something an Insight Study reads from humanoids (area 7 detect lies, for example). */
    public static void registerInsight(InsightReader reader) {
        INSIGHT_READERS.add(reader);
    }

    static void register() {
        Perception.registerTargetHandler(Study::handle);

        registerWeakness(ModEntityTypes.FROST_DRAKE, Text.translatable("study.dndclasses.weakness.fire",
                Math.round((FrostDrakeEntity.FIRE_WEAKNESS - 1.0F) * 100)));
        registerWeakness(ModEntityTypes.LICH, Text.translatable("study.dndclasses.weakness.phylactery"));
        registerWeakness(ModEntityTypes.LICH, Text.translatable("study.dndclasses.weakness.necromancer",
                Math.round((LichEntity.NECROMANCER_DAMAGE_BONUS - 1.0F) * 100)));
        registerWeakness(ModEntityTypes.OWLBEAR, Text.translatable("study.dndclasses.weakness.owlbear_hug",
                (int) OwlbearEntity.HUG_BREAK_DAMAGE));
        registerWeakness(ModEntityTypes.TROLL, Text.translatable("study.dndclasses.weakness.troll"));
        registerWeakness(ModEntityTypes.MIND_FLAYER, Text.translatable("study.dndclasses.weakness.mind_flayer"));

        registerInsight((player, target) -> {
            if (!(target instanceof VillagerEntity villager)) {
                return List.of();
            }
            int reputation = villager.getReputation(player);
            String mood = reputation >= 30 ? "fond" : reputation > 0 ? "warm" : reputation == 0 ? "neutral"
                    : reputation > -30 ? "wary" : "hostile";
            List<Text> lines = new ArrayList<>();
            lines.add(Text.translatable("study.dndclasses.insight.reputation",
                    Text.translatable("study.dndclasses.insight.mood." + mood), reputation));
            lines.add(Text.translatable(!Persuasion.hasTrade(villager) ? "study.dndclasses.insight.unpersuadable"
                    : Persuasion.triedToday(player, villager)
                    ? "study.dndclasses.insight.persuaded" : "study.dndclasses.insight.persuadable"));
            return lines;
        });
        registerInsight((player, target) -> {
            if (!(target instanceof MountainDwarfEntity dwarf)) {
                return List.of();
            }
            return List.of(Text.translatable(dwarf.hasGrudgeAgainst(player)
                    ? "study.dndclasses.insight.grudge" : "study.dndclasses.insight.no_grudge"));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> dispatcher.register(
                CommandManager.literal("dndclass")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("lore")
                                .then(CommandManager.argument("player", EntityArgumentType.player())
                                        .then(CommandManager.literal("list").executes(Study::listCommand))
                                        .then(CommandManager.literal("forget")
                                                .executes(context -> forgetCommand(context, null))
                                                .then(CommandManager.argument("entity", StringArgumentType.greedyString())
                                                        .suggests((context, builder) -> CommandSource.suggestIdentifiers(
                                                                Registries.ENTITY_TYPE.getIds(), builder))
                                                        .executes(context -> forgetCommand(context,
                                                                StringArgumentType.getString(context, "entity")))))))));
    }

    /** Drops the player's fail cooldowns; called on disconnect. */
    public static void forget(UUID player) {
        FAILED.remove(player);
    }

    // ---------------------------------------------------------------- rules

    /** The group a creature belongs to: its {@code #dndclasses:lore/*} tag, else a guess from its kind. */
    public static Lore loreOf(LivingEntity target) {
        EntityType<?> type = target.getType();
        for (Lore lore : Lore.values()) {
            if (type.isIn(lore.tag())) {
                return lore;
            }
        }
        if (target.getGroup() == EntityGroup.UNDEAD) {
            return Lore.UNDEAD;
        }
        if (target.getGroup() == EntityGroup.ILLAGER || target instanceof MerchantEntity) {
            return Lore.HUMANOID;
        }
        return target instanceof HostileEntity || target instanceof Monster ? Lore.ARCANE : Lore.BEASTS;
    }

    /** The Study DC: bosses {@value #DC_BOSS}, this mod's creatures {@value #DC_MOD}, vanilla {@value #DC_VANILLA}. */
    public static int dc(LivingEntity target) {
        EntityType<?> type = target.getType();
        if (target instanceof Boss || type == EntityType.ENDER_DRAGON || type == EntityType.WITHER
                || type == EntityType.WARDEN || type == EntityType.ELDER_GUARDIAN) {
            return DC_BOSS;
        }
        return DnDClasses.MOD_ID.equals(EntityType.getId(type).getNamespace()) ? DC_MOD : DC_VANILLA;
    }

    /** How well the player knows a creature type: 0, {@link #KNOWN} or {@link #KNOWN_WEAKNESS}. */
    public static int knowledge(PlayerEntity player, EntityType<?> type) {
        NbtCompound known = ((IEntityDataSaver) player).getPersistentData().getCompound(KNOWN_KEY);
        return known.getByte(EntityType.getId(type).toString());
    }

    /** Records what the player learned (never lowers it). */
    public static void learn(PlayerEntity player, EntityType<?> type, byte level) {
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        NbtCompound known = data.getCompound(KNOWN_KEY);
        String id = EntityType.getId(type).toString();
        if (known.getByte(id) < level) {
            known.putByte(id, level);
        }
        data.put(KNOWN_KEY, known);
    }

    /** Ticks until the player may study this creature type again after a failure (0: now). */
    public static int cooldown(ServerPlayerEntity player, EntityType<?> type) {
        Map<EntityType<?>, Integer> failed = FAILED.get(player.getUuid());
        Integer until = failed == null ? null : failed.get(type);
        return until == null ? 0 : Math.max(0, until - player.getServer().getTicks());
    }

    // ---------------------------------------------------------------- the key

    private static boolean handle(ServerPlayerEntity player, LivingEntity target) {
        if (!(target instanceof MobEntity)) {
            return false; // players, armour stands: Search instead
        }
        EntityType<?> type = target.getType();
        Lore lore = loreOf(target);
        int known = knowledge(player, type);
        if (known > 0) {
            DnDClasses.LOGGER.info("[Study] {} recalls {} (known {})", player.getEntityName(), EntityType.getId(type),
                    known);
            sendCard(player, target, lore, known >= KNOWN_WEAKNESS);
            return true;
        }
        int wait = cooldown(player, type);
        if (wait > 0) {
            player.sendMessage(Text.translatable("study.dndclasses.cooldown", type.getName(), (wait + 19) / 20), true);
            return true;
        }

        int dc = dc(target);
        D20.Roll roll = SkillCheck.builder(player, lore.skill(), dc, "study", lore.name().toLowerCase(java.util.Locale.ROOT))
                .kind(RollKind.STUDY).roll();
        DnDClasses.LOGGER.info("[Study] {} studies {} with {}: {} vs DC {} -> {}", player.getEntityName(),
                EntityType.getId(type), lore.skill().id(), roll.total(), dc, roll.outcome());
        if (roll.outcome().succeeded()) {
            boolean critical = roll.outcome() == D20.Outcome.CRITICAL;
            learn(player, type, critical ? KNOWN_WEAKNESS : KNOWN);
            D20.show(player, roll, Text.translatable(critical ? "study.dndclasses.critical" : "study.dndclasses.success",
                    type.getName()));
            sendCard(player, target, lore, critical);
        } else {
            FAILED.computeIfAbsent(player.getUuid(), k -> new HashMap<>())
                    .put(type, player.getServer().getTicks() + FAIL_COOLDOWN);
            D20.show(player, roll, Text.translatable(roll.outcome() == D20.Outcome.FUMBLE
                    ? "study.dndclasses.fumble" : "study.dndclasses.failure", type.getName()));
        }
        return true;
    }

    // ---------------------------------------------------------------- the card

    /** The chat card for a creature, with its weakness when {@code weakness} is set. */
    public static List<Text> card(ServerPlayerEntity player, LivingEntity target, Lore lore, boolean weakness) {
        List<Text> lines = new ArrayList<>();
        EntityType<?> type = target.getType();
        lines.add(Text.translatable("study.dndclasses.card.title", target.getDisplayName(),
                Text.translatable(lore.skill().translationKey())).formatted(Formatting.GOLD));
        lines.add(Text.translatable("study.dndclasses.card.stats", (int) Math.ceil(target.getHealth()),
                (int) Math.ceil(target.getMaxHealth()), target.getArmor()).formatted(Formatting.GRAY));

        List<MobSaveInfo.Entry> saves = MobSaveInfo.of(type);
        if (saves.isEmpty()) {
            lines.add(Text.translatable("study.dndclasses.card.no_saves").formatted(Formatting.GRAY));
        } else {
            for (MobSaveInfo.Entry save : saves) {
                lines.add(Text.literal(" - ").append(save.describe()).formatted(Formatting.YELLOW));
            }
        }

        List<Text> immunities = immunities(target);
        if (!immunities.isEmpty()) {
            MutableText line = Text.translatable("study.dndclasses.card.immune").formatted(Formatting.AQUA);
            for (int i = 0; i < immunities.size(); i++) {
                line.append(i == 0 ? Text.literal(" ") : Text.literal(", ")).append(immunities.get(i));
            }
            lines.add(line);
        }

        if (weakness) {
            List<Text> weaknesses = weaknesses(target);
            if (weaknesses.isEmpty()) {
                lines.add(Text.translatable("study.dndclasses.card.no_weakness").formatted(Formatting.GRAY));
            }
            for (Text w : weaknesses) {
                lines.add(Text.translatable("study.dndclasses.card.weakness", w).formatted(Formatting.RED));
            }
        }

        if (lore == Lore.HUMANOID) {
            for (InsightReader reader : INSIGHT_READERS) {
                for (Text t : reader.read(player, target)) {
                    lines.add(Text.literal(" ").append(t).formatted(Formatting.LIGHT_PURPLE));
                }
            }
        }
        return lines;
    }

    private static void sendCard(ServerPlayerEntity player, LivingEntity target, Lore lore, boolean weakness) {
        for (Text line : card(player, target, lore, weakness)) {
            player.sendMessage(line, false);
        }
        player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 0.8F, 1.0F);
    }

    /** What can't hurt it or can't take hold of it, probed from the creature itself. */
    private static List<Text> immunities(LivingEntity target) {
        List<Text> list = new ArrayList<>();
        DamageSources sources = target.getWorld().getDamageSources();
        if (target.isFireImmune() || target.isInvulnerableTo(sources.inFire())) {
            list.add(Text.translatable("study.dndclasses.immunity.fire"));
        }
        if (target.isInvulnerableTo(sources.freeze()) || target.getType().isIn(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES)) {
            list.add(Text.translatable("study.dndclasses.immunity.freezing"));
        }
        if (target.canBreatheInWater()) {
            list.add(Text.translatable("study.dndclasses.immunity.drowning"));
        }
        if (!target.canHaveStatusEffect(new StatusEffectInstance(StatusEffects.POISON))) {
            list.add(Text.translatable("study.dndclasses.immunity.poison"));
        }
        if (!target.canHaveStatusEffect(new StatusEffectInstance(StatusEffects.WITHER))) {
            list.add(Text.translatable("study.dndclasses.immunity.wither"));
        }
        return list;
    }

    private static synchronized List<Text> weaknesses(LivingEntity target) {
        List<Text> list = new ArrayList<>();
        for (Function<LivingEntity, Text> line : WEAKNESSES.getOrDefault(target.getType(), List.of())) {
            list.add(line.apply(target));
        }
        if (target.isUndead()) {
            list.add(Text.translatable("study.dndclasses.weakness.undead"));
        }
        return list;
    }

    // ---------------------------------------------------------------- commands

    private static int listCommand(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        NbtCompound known = ((IEntityDataSaver) player).getPersistentData().getCompound(KNOWN_KEY);
        StringBuilder text = new StringBuilder(player.getEntityName()).append(" knows ").append(known.getSize())
                .append(" creature type(s)");
        for (String id : known.getKeys()) {
            text.append("\n  ").append(id).append(known.getByte(id) >= KNOWN_WEAKNESS ? " (weakness)" : "");
        }
        context.getSource().sendFeedback(Text.literal(text.toString()), false);
        return known.getSize();
    }

    private static int forgetCommand(CommandContext<ServerCommandSource> context, @Nullable String entity)
            throws CommandSyntaxException {
        ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        NbtCompound known = data.getCompound(KNOWN_KEY);
        int dropped;
        if (entity == null) {
            dropped = known.getSize();
            data.remove(KNOWN_KEY);
            forget(player.getUuid());
        } else {
            Identifier id = Identifier.tryParse(entity.trim());
            if (id == null || !Registries.ENTITY_TYPE.containsId(id)) {
                context.getSource().sendError(Text.literal("Unknown entity " + entity));
                return 0;
            }
            dropped = known.contains(id.toString()) ? 1 : 0;
            known.remove(id.toString());
            data.put(KNOWN_KEY, known);
            Map<EntityType<?>, Integer> failed = FAILED.get(player.getUuid());
            if (failed != null) {
                failed.remove(Registries.ENTITY_TYPE.get(id));
            }
        }
        context.getSource().sendFeedback(Text.literal(player.getEntityName() + " forgot " + dropped
                + " creature type(s)"), true);
        return dropped;
    }
}

package mattonfire.dnd.quest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mattonfire.dnd.dungeon.DungeonMaps;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.entity.boss.BossFight;
import mattonfire.dnd.faction.Faction;
import mattonfire.dnd.faction.Factions;
import mattonfire.dnd.faction.Reputation;
import mattonfire.dnd.quest.QuestJson.StructureMatch;
import mattonfire.dnd.world.underdark.Underdark;
import mattonfire.dnd.world.underdark.UnderdarkMaps;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapIcon;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
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
import net.minecraft.world.gen.structure.Structure;
import org.jetbrains.annotations.Nullable;

/**
 * Something a quest does: on accept, when a stage completes, or as the final reward. Most actions
 * run once for each participant ("per hero"); {@link #perInstance()} ones run once for the whole
 * quest instance.
 */
public sealed interface QuestAction {
    /**
     * Where an action runs.
     *
     * @param player   who it's for; for per-instance actions any online participant, or null
     * @param instance the quest instance, or null when paying out a finished quest's rewards
     */
    record Context(QuestManager manager, MinecraftServer server, ServerPlayerEntity player, QuestDefinition quest,
                   @Nullable QuestInstance instance) {
    }

    void run(Context context);

    default boolean perInstance() {
        return false;
    }

    record Give(Identifier item, int count, @Nullable NbtCompound nbt) implements QuestAction {
        @Override
        public void run(Context context) {
            ItemStack stack = new ItemStack(Registries.ITEM.get(this.item), this.count);
            if (this.nbt != null) {
                stack.setNbt(this.nbt.copy());
            }
            context.player().getInventory().offerOrDrop(stack);
        }
    }

    record Loot(Identifier table) implements QuestAction {
        @Override
        public void run(Context context) {
            loot(context.player(), this.table);
        }
    }

    record MagicItem(String rarity) implements QuestAction {
        @Override
        public void run(Context context) {
            ItemStack stack = QuestHooks.magicItem.apply(context.player(), this.rarity);
            if (stack != null && !stack.isEmpty()) {
                context.player().getInventory().offerOrDrop(stack);
            } else {
                loot(context.player(), new Identifier(DnDClasses.MOD_ID, "gameplay/quest_reward_" + this.rarity));
            }
        }
    }

    record Emeralds(int count) implements QuestAction {
        @Override
        public void run(Context context) {
            int left = this.count;
            while (left > 0) {
                int stack = Math.min(left, 64);
                context.player().getInventory().offerOrDrop(new ItemStack(Items.EMERALD, stack));
                left -= stack;
            }
        }
    }

    record Xp(int amount) implements QuestAction {
        @Override
        public void run(Context context) {
            context.player().addExperience(this.amount);
        }
    }

    record ClassXp(int amount) implements QuestAction {
        @Override
        public void run(Context context) {
            Progression.addXp(context.player(), this.amount);
        }
    }

    /**
     * Faction deltas, applied as one change (rivals react to gains as usual). With {@code race} set
     * ({@code "race": "elf"} in the file) only players of that race get it, e.g. a bonus for elves.
     */
    record Rep(Map<Identifier, Integer> deltas, @Nullable DndRace race) implements QuestAction {
        @Override
        public void run(Context context) {
            if (this.race != null && ((PlayerEntityExt) context.player()).getDndRace() != this.race) {
                return;
            }
            Reputation.Change change = Reputation.change(context.player(), Reputation.Source.QUEST);
            for (Map.Entry<Identifier, Integer> entry : this.deltas.entrySet()) {
                Faction faction = Factions.get(entry.getKey());
                if (faction == null) {
                    DnDClasses.LOGGER.warn("[Quests] {}: rep action names unknown faction {}", context.quest().id(), entry.getKey());
                    continue;
                }
                change.add(faction, entry.getValue());
            }
            change.apply();
        }
    }

    /**
     * An explorer map to the nearest matching structure (like a cartographer's), searching
     * {@code radius} chunks. If there's none, the player gets {@code fallback} (or a generic line).
     *
     * <p>With {@code dimension} set, it searches that dimension from the player's x/z instead. A player
     * standing in it gets the usual map; anyone else is told where it lies "below you" and, for the
     * Underdark, gets a Rift Map to the nearest Underdark Rift with the target's coordinates in its lore.
     */
    record Reveal(StructureMatch structure, int radius, @Nullable Text fallback, @Nullable RegistryKey<World> dimension)
            implements QuestAction {
        @Override
        public void run(Context context) {
            ServerPlayerEntity player = context.player();
            ServerWorld here = player.getWorld();
            ServerWorld world = this.dimension == null ? here : context.server().getWorld(this.dimension);
            if (world == null) {
                DnDClasses.LOGGER.warn("[Quests] {}: reveal names unknown dimension {}", context.quest().id(), this.dimension);
                this.none(context, player);
                return;
            }
            BlockPos from = world == here ? player.getBlockPos()
                    : new BlockPos(player.getBlockX(), (world.getBottomY() + world.getTopY()) / 2, player.getBlockZ());
            Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
            RegistryEntryList<Structure> targets = this.structure.tag() != null
                    ? registry.getEntryList(this.structure.tag()).orElse(null)
                    : registry.getEntry(this.structure.key()).<RegistryEntryList<Structure>>map(RegistryEntryList::of).orElse(null);
            Pair<BlockPos, RegistryEntry<Structure>> found = targets == null ? null
                    : world.getChunkManager().getChunkGenerator().locateStructure(world, targets, from, this.radius, false);
            if (found == null) {
                this.none(context, player);
                return;
            }
            BlockPos pos = found.getFirst();
            if (world != here) {
                this.below(context, player, world, pos);
                return;
            }
            ItemStack map = DungeonMaps.explorerMap(world, pos, "+", MapIcon.Type.TARGET_X,
                    Text.translatable("quest.dndclasses.reveal.map", this.structure.name()).styled(style -> style.withItalic(false)));
            DnDClasses.LOGGER.info("[Quests] {}: revealed {} at {} for {}", context.quest().id(), this.structure,
                    pos.toShortString(), player.getEntityName());
            player.getInventory().offerOrDrop(map);
        }

        private void none(Context context, ServerPlayerEntity player) {
            DnDClasses.LOGGER.info("[Quests] {}: no {} within {} chunks of {}", context.quest().id(), this.structure,
                    this.radius, player.getEntityName());
            player.sendMessage(this.fallback != null ? this.fallback
                    : Text.translatable("quest.dndclasses.reveal.none", this.structure.name()).formatted(Formatting.GRAY), false);
        }

        /** The target is in another dimension: say where, and hand over the way down if there is one. */
        private void below(Context context, ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
            Text where = QuestObjective.dimensionName(world.getRegistryKey());
            player.sendMessage(Text.translatable("quest.dndclasses.reveal.below", this.structure.name(), where,
                    pos.getX(), pos.getZ()).formatted(Formatting.GRAY), false);
            ItemStack map = Underdark.WORLD.equals(world.getRegistryKey())
                    ? UnderdarkMaps.riftMap(player.getWorld(), player.getBlockPos(), this.structure.name(), pos)
                    : null;
            DnDClasses.LOGGER.info("[Quests] {}: revealed {} at {} in {} for {} ({})", context.quest().id(), this.structure,
                    pos.toShortString(), world.getRegistryKey().getValue(), player.getEntityName(),
                    map != null ? "rift map" : "no map");
            if (map != null) {
                player.getInventory().offerOrDrop(map);
            }
        }
    }

    /** A line of story in chat, styled like DM narration. */
    record Narrate(Text text) implements QuestAction {
        @Override
        public void run(Context context) {
            context.player().sendMessage(Text.literal("✦ ").append(this.text.copy())
                    .formatted(Formatting.GOLD, Formatting.ITALIC), false);
            context.player().playSound(SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
    }

    /** Starts another quest for the same participants (the next link in a chain). */
    record StartQuest(Identifier quest) implements QuestAction {
        @Override
        public void run(Context context) {
            if (context.instance() != null) {
                context.manager().startFollowUp(context.server(), this.quest, context.instance());
            } else if (context.player() != null) {
                // Paying out a finished quest: start it for this player (and their nearby party).
                context.manager().startFromAction(context.player(), this.quest);
            }
        }

        @Override
        public boolean perInstance() {
            return true;
        }
    }

    /** Sets a flag on the quest instance (for branching later stages and dialogue). */
    record SetFlag(String flag) implements QuestAction {
        @Override
        public void run(Context context) {
            if (context.instance() != null) {
                context.instance().flags().add(this.flag);
            }
        }

        @Override
        public boolean perInstance() {
            return true;
        }
    }

    /**
     * Finishes the quest instance's current stage (optionally only if it's on {@code stage}, 1-based,
     * or 0 for any), running its {@code on_complete}. For dialogue branches where every path moves on.
     */
    record CompleteStage(int stage) implements QuestAction {
        @Override
        public void run(Context context) {
            if (context.instance() != null) {
                context.manager().completeStageFromAction(context.server(), context.instance(), context.quest(), this.stage);
            }
        }

        @Override
        public boolean perInstance() {
            return true;
        }
    }

    record Advancement(Identifier advancement) implements QuestAction {
        @Override
        public void run(Context context) {
            BossFight.grantAdvancement(context.player(), this.advancement);
        }
    }

    static void loot(ServerPlayerEntity player, Identifier tableId) {
        ServerWorld world = player.getWorld();
        LootTable table = world.getServer().getLootManager().getTable(tableId);
        if (table == LootTable.EMPTY) {
            DnDClasses.LOGGER.warn("[Quests] loot table {} is missing or empty", tableId);
            return;
        }
        List<ItemStack> spoils = table.generateLoot(new LootContext.Builder(world)
                .parameter(LootContextParameters.ORIGIN, player.getPos())
                .parameter(LootContextParameters.THIS_ENTITY, player)
                .random(player.getRandom())
                .build(LootContextTypes.GIFT));
        spoils.forEach(stack -> player.getInventory().offerOrDrop(stack));
    }

    public static QuestAction parse(JsonElement element) {
        JsonObject json = QuestJson.object(element, "action");
        String type = QuestJson.string(json, "type");
        return switch (type) {
            case "give" -> {
                Identifier item = QuestJson.id(json, "item");
                if (!Registries.ITEM.containsId(item)) {
                    throw new IllegalArgumentException("unknown item \"" + item + "\"");
                }
                NbtCompound nbt = null;
                String snbt = QuestJson.optString(json, "nbt");
                if (snbt != null) {
                    try {
                        nbt = StringNbtReader.parse(snbt);
                    } catch (CommandSyntaxException e) {
                        throw new IllegalArgumentException("bad nbt: " + e.getMessage());
                    }
                }
                yield new Give(item, QuestJson.integer(json, "count", 1, 1, 64), nbt);
            }
            case "loot" -> new Loot(QuestJson.id(json, "table"));
            case "magic_item" -> {
                String rarity = QuestJson.string(json, "rarity");
                if (!List.of("common", "uncommon", "rare", "very_rare", "legendary").contains(rarity)) {
                    throw new IllegalArgumentException("unknown rarity \"" + rarity + "\"");
                }
                yield new MagicItem(rarity);
            }
            case "emeralds" -> new Emeralds(QuestJson.integer(json, "count", 1, 1, 64 * 36));
            case "xp" -> new Xp(QuestJson.integer(json, "amount", 0, 0, 100000));
            case "class_xp" -> new ClassXp(QuestJson.integer(json, "amount", 0, 0, 100000));
            case "rep" -> {
                JsonObject factions = QuestJson.object(json.get("factions"), "rep \"factions\"");
                Map<Identifier, Integer> deltas = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> entry : factions.entrySet()) {
                    JsonObject wrapper = new JsonObject();
                    wrapper.add("v", entry.getValue());
                    deltas.put(QuestJson.id(entry.getKey(), "rep faction"),
                            QuestJson.integer(wrapper, "v", 0, -2000, 2000));
                }
                DndRace race = null;
                String raceName = QuestJson.optString(json, "race");
                if (raceName != null) {
                    race = DndRace.byId(raceName.substring(raceName.indexOf(':') + 1));
                    if (race == null || race == DndRace.NONE) {
                        throw new IllegalArgumentException("rep: unknown race \"" + raceName + "\"");
                    }
                }
                yield new Rep(deltas, race);
            }
            case "reveal" -> new Reveal(StructureMatch.parse(QuestJson.string(json, "structure")),
                    QuestJson.integer(json, "radius", 100, 1, 300), QuestJson.optText(json, "fallback"),
                    json.has("dimension") ? QuestObjective.dimension(json) : null);
            case "narrate" -> new Narrate(QuestJson.text(json.get("text"), "text"));
            case "start_quest" -> new StartQuest(QuestJson.id(json, "quest"));
            case "set_flag" -> new SetFlag(QuestJson.string(json, "flag"));
            case "complete_stage" -> new CompleteStage(QuestJson.integer(json, "stage", 0, 0, 64));
            case "advancement" -> new Advancement(QuestJson.id(json, "advancement"));
            default -> throw new IllegalArgumentException("unknown action type \"" + type + "\"");
        };
    }
}

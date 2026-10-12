package mattonfire.dnd.classes.Race;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.RollFilter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Registry.ModEffects;
import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.classes.Obstacles.ObstacleBlockEntity;
import mattonfire.dnd.classes.Obstacles.ObstacleIndex;
import mattonfire.dnd.classes.Obstacles.Tier;
import mattonfire.dnd.classes.Obstacles.types.HiddenDoorType;
import mattonfire.dnd.classes.Rest.CampWatch;
import mattonfire.dnd.classes.SkillChecks.PerceptionService;
import mattonfire.dnd.dungeon.DungeonRegistry;
import mattonfire.dnd.world.gen.dungeon.DungeonPiece;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

/**
 * The always-on racial traits (the d20 and combat ones are in {@link RaceTraits}). Every trait reads the
 * active race ({@link RaceLifecycle#activeRaceOf}, NONE while {@code dndRaces} is off) on the server, and the
 * synced body race ({@link PlayerEntityExt#getBodyRace}, the same value) on the client.
 * <ul>
 * <li><b>Darkvision</b> (Elf, Dwarf, Gnome, Half-Orc, Tiefling): the client's lightmap gets night vision at
 * {@value #DARKVISION_STRENGTH} strength ({@code DarkvisionLightmapMixin}).</li>
 * <li><b>Resistances</b> ({@link #resist}): Tiefling fire x{@value #HELLISH_RESISTANCE}, Gnome magic
 * x{@value #GNOME_CUNNING} ({@link #MAGIC_DAMAGE}), Dragonborn ancestry element x{@value #DRACONIC_RESISTANCE}.
 * Dwarven Resilience: poison and (for Frost Dragonborn) the Freeze effect last half as long
 * ({@link #shortenEffect}), and a Dwarf's poison ticks deal {@value #DWARF_POISON_TICK} ({@link #poisonTick}).</li>
 * <li><b>Keen Senses</b> (Elf): while sneaking, hostile mobs within {@value #KEEN_SENSES_RANGE} blocks get an
 * outline only you see ({@code KeenSensesOutlineMixin}).</li>
 * <li><b>Trance</b> (Elf): the time-since-rest stat is reset every second, so phantoms never come.</li>
 * <li><b>Second Breakfast</b> (Halfling): food restores {@value #SECOND_BREAKFAST} extra hunger point.</li>
 * <li><b>Stonecunning</b> (Dwarf): x{@value #STONECUNNING_SPEED} break speed on {@link #STONECUNNING_BLOCKS};
 * while sneaking, the bricked-up hidden doorways of a dungeon within {@value #SECRET_DOOR_RANGE} blocks are
 * outlined (when they're stonework, {@link #SECRET_DOOR}), and once a second each Hidden Door obstacle within that
 * range that's still hidden from you is checked against your passive score + {@value #STONECUNNING_PASSIVE_BONUS}
 * (advantage): if it meets the DC you find it, as a Ranger would ({@link #stonecunningDoors}).</li>
 * <li><b>Tinker</b> (Gnome): crafting an item in {@link #TINKER_CRAFTS} has a {@value #TINKER_CHANCE} chance to
 * make one extra.</li>
 * <li><b>Ambitious</b> (Human): +{@value #AMBITIOUS_PERCENT}% class XP ({@link #ambitiousXp}), with the
 * fractions carried over so small awards add up. <b>Diplomat</b>: +{@value #DIPLOMAT_BONUS} on Persuasion,
 * Deception and Intimidation; a villager's first trade seeds kin gossip ({@code HomeBonuses.greetHuman}).</li>
 * <li>Advantage on the sheet: a Dwarf on saves tagged {@code poison} and on camp watches underground
 * ({@link CampWatch#TAG_UNDERGROUND}); a Gnome on INT, WIS and CHA saves tagged {@value #MAGIC_TAG}.</li>
 * </ul>
 */
public final class RacePassives {
    public static final Identifier CONTRIBUTOR_ID = new Identifier(DnDClasses.MOD_ID, "race_passives");

    public static final float DARKVISION_STRENGTH = 0.15F;
    public static final double KEEN_SENSES_RANGE = 12.0;

    public static final float HELLISH_RESISTANCE = 0.5F;
    public static final float DRACONIC_RESISTANCE = 0.5F;
    public static final float GNOME_CUNNING = 0.7F;
    public static final float DWARF_EFFECT_DURATION = 0.5F;
    /** Damage of a Dwarf's poison tick (vanilla: 1). */
    public static final float DWARF_POISON_TICK = 0.5F;

    public static final int SECOND_BREAKFAST = 1;

    public static final float STONECUNNING_SPEED = 1.2F;
    public static final double SECRET_DOOR_RANGE = 8.0;
    /** Stonecunning's bonus to the passive check against a Hidden Door obstacle: advantage, as on a passive score. */
    public static final int STONECUNNING_PASSIVE_BONUS = 5;
    private static final DustParticleEffect SECRET_DOOR_DUST = new DustParticleEffect(new Vector3f(0.95F, 0.8F, 0.35F),
            1.2F);

    public static final float TINKER_CHANCE = 0.25F;

    public static final int AMBITIOUS_PERCENT = 10;
    public static final int DIPLOMAT_BONUS = 2;

    /** Save tag for spells and other magic; a Gnome has advantage on INT/WIS/CHA saves with it. */
    public static final String MAGIC_TAG = "magic";
    public static final String POISON_TAG = "poison";

    public static final TagKey<DamageType> MAGIC_DAMAGE = TagKey.of(RegistryKeys.DAMAGE_TYPE,
            new Identifier(DnDClasses.MOD_ID, "magic_damage"));
    public static final TagKey<Item> TINKER_CRAFTS = TagKey.of(RegistryKeys.ITEM,
            new Identifier(DnDClasses.MOD_ID, "tinker_crafts"));
    public static final TagKey<net.minecraft.block.Block> STONECUNNING_BLOCKS = TagKey.of(RegistryKeys.BLOCK,
            new Identifier(DnDClasses.MOD_ID, "stonecunning"));
    public static final TagKey<net.minecraft.block.Block> SECRET_DOOR = TagKey.of(RegistryKeys.BLOCK,
            new Identifier(DnDClasses.MOD_ID, "secret_door"));

    /** Tenths of a class XP point a Human is owed, in the race compound. */
    private static final String AMBITIOUS_CARRY_KEY = "ambitiousCarry";

    private RacePassives() {
    }

    /** Called from {@link RaceLifecycle#register()}. */
    public static void register() {
        AbilityScores.register(CONTRIBUTOR_ID, (player, c) -> {
            switch (RaceLifecycle.activeRaceOf(player)) {
                case HUMAN -> {
                    for (Skill skill : new Skill[] { Skill.PERSUASION, Skill.DECEPTION, Skill.INTIMIDATION }) {
                        c.skillBonus(skill, DIPLOMAT_BONUS, "Human (Diplomat)");
                    }
                }
                case DWARF -> {
                    c.advantage(RollFilter.allSaves().and(RollFilter.tag(POISON_TAG)), "saves against poison",
                            "Dwarf (Dwarven Resilience)");
                    c.advantage(RollFilter.tag(CampWatch.TAG_WATCH).and(RollFilter.tag(CampWatch.TAG_UNDERGROUND)),
                            "camp watches underground", "Dwarf (Stonecunning)");
                }
                case GNOME -> c.advantage(RollFilter.save(Ability.INT).or(RollFilter.save(Ability.WIS))
                        .or(RollFilter.save(Ability.CHA)).and(RollFilter.tag(MAGIC_TAG)),
                        "INT, WIS and CHA saves against magic", "Gnome (Gnome Cunning)");
                default -> {
                }
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(RacePassives::tick);
    }

    /** The race whose traits apply to this player, on either side. */
    public static DndRace raceOf(PlayerEntity player) {
        if (player.getWorld().isClient) {
            DndRace race = player instanceof PlayerEntityExt ext ? ext.getBodyRace() : null;
            return race == null ? DndRace.NONE : race;
        }
        return RaceLifecycle.activeRaceOf(player);
    }

    public static boolean hasDarkvision(DndRace race) {
        return race == DndRace.ELF || race == DndRace.DWARF || race == DndRace.GNOME || race == DndRace.HALFORC
                || race == DndRace.TIEFLING;
    }

    // --- resistances ---

    /** Damage after racial resistances (Hellish, Draconic, Gnome Cunning). Server side, before armour. */
    public static float resist(LivingEntity entity, DamageSource source, float amount) {
        if (!(entity instanceof PlayerEntity player) || amount <= 0) {
            return amount;
        }
        DndRace race = RaceLifecycle.activeRaceOf(player);
        float result = amount;
        switch (race) {
            case TIEFLING -> {
                if (source.isIn(DamageTypeTags.IS_FIRE)) {
                    result *= HELLISH_RESISTANCE;
                }
            }
            case GNOME -> {
                if (source.isIn(MAGIC_DAMAGE)) {
                    result *= GNOME_CUNNING;
                }
            }
            case DRAGONBORN -> {
                boolean element = switch (RaceLifecycle.ancestryOf(player)) {
                    case EMBER -> source.isIn(DamageTypeTags.IS_FIRE);
                    case FROST -> source.isIn(DamageTypeTags.IS_FREEZING);
                    case STORM -> source.isIn(DamageTypeTags.IS_LIGHTNING);
                    default -> false;
                };
                if (element) {
                    result *= DRACONIC_RESISTANCE;
                }
            }
            default -> {
            }
        }
        return result;
    }

    /**
     * The effect a player actually gets: a Dwarf's poison and a Frost Dragonborn's Freeze last half as long.
     * Server side, from {@code addStatusEffect}.
     */
    public static StatusEffectInstance shortenEffect(LivingEntity entity, StatusEffectInstance effect) {
        if (effect == null || entity.getWorld().isClient || !(entity instanceof PlayerEntity player)
                || effect.isInfinite() || effect.getDuration() <= 1) {
            return effect;
        }
        StatusEffect type = effect.getEffectType();
        DndRace race = RaceLifecycle.activeRaceOf(player);
        boolean shorten = (race == DndRace.DWARF && type == StatusEffects.POISON)
                || (race == DndRace.DRAGONBORN && type == ModEffects.FREEZE
                        && RaceLifecycle.ancestryOf(player) == DragonAncestry.FROST);
        if (!shorten) {
            return effect;
        }
        int duration = Math.max(1, Math.round(effect.getDuration() * DWARF_EFFECT_DURATION));
        return new StatusEffectInstance(type, duration, effect.getAmplifier(), effect.isAmbient(),
                effect.shouldShowParticles(), effect.shouldShowIcon());
    }

    /**
     * A Dwarf's poison tick: {@value #DWARF_POISON_TICK} damage instead of 1 (still never below 1 health).
     * Returns true when it handled the tick, so vanilla's is skipped. Bards are healed by poison instead
     * ({@code StatusEffectMixin}), so they're left alone.
     */
    public static boolean poisonTick(LivingEntity entity) {
        if (entity.getWorld().isClient || !(entity instanceof PlayerEntity player)
                || RaceLifecycle.activeRaceOf(player) != DndRace.DWARF
                || (player instanceof PlayerEntityExt ext && ext.getDndClass() == DndCharacter.BARD)) {
            return false;
        }
        if (player.getHealth() > 1.0F) {
            player.damage(player.getDamageSources().magic(), DWARF_POISON_TICK);
        }
        return true;
    }

    // --- Stonecunning ---

    /** Break speed after Stonecunning. Both sides, so the client's mining prediction matches the server. */
    public static float breakSpeed(PlayerEntity player, BlockState state, float speed) {
        return raceOf(player) == DndRace.DWARF && state.isIn(STONECUNNING_BLOCKS) ? speed * STONECUNNING_SPEED : speed;
    }

    /**
     * The hidden doorways of the dungeon at {@code pos} with at least one stonework block still in place, each as
     * the blocks still standing, nearest first. Empty outside a dungeon.
     */
    public static List<List<BlockPos>> secretDoorways(ServerWorld world, BlockPos pos, double range) {
        List<List<BlockPos>> found = new ArrayList<>();
        Vec3d at = Vec3d.ofCenter(pos);
        StructureStart start = DungeonRegistry.findStart(world, pos);
        if (start == null || !start.hasChildren()) {
            // /place structure leaves no start behind: find the doorways in the recorded rooms' walls instead
            DungeonRegistry.find(world, pos).filter(dungeon -> dungeon.contains(pos))
                    .ifPresent(dungeon -> scanWalls(world, dungeon, at, range, found));
            found.sort((a, b) -> Double.compare(a.get(0).getSquaredDistance(at), b.get(0).getSquaredDistance(at)));
            return found;
        }
        List<BlockPos> seen = new ArrayList<>();
        for (StructurePiece piece : start.getChildren()) {
            if (!(piece instanceof DungeonPiece dungeonPiece)) {
                continue;
            }
            for (List<BlockPos> doorway : dungeonPiece.hiddenDoorways()) {
                // The room and the corridor on either side both list the same doorway
                if (doorway.isEmpty() || seen.contains(doorway.get(0))) {
                    continue;
                }
                seen.add(doorway.get(0));
                List<BlockPos> standing = new ArrayList<>();
                for (BlockPos block : doorway) {
                    if (Vec3d.ofCenter(block).distanceTo(at) <= range && world.isChunkLoaded(block)
                            && world.getBlockState(block).isIn(SECRET_DOOR)) {
                        standing.add(block);
                    }
                }
                if (!standing.isEmpty()) {
                    found.add(standing);
                }
            }
        }
        found.sort((a, b) -> Double.compare(a.get(0).getSquaredDistance(at), b.get(0).getSquaredDistance(at)));
        return found;
    }

    /**
     * Hidden doorways in the walls of a dungeon's rooms, by their shape: a room wall is two blocks thick, and a
     * hidden doorway leaves its outer skin open and bricks up the inner wall ({@code DungeonPiece.Builder.doorway}),
     * so a column of the room's inner wall that's secret-door stonework, with air behind it in the skin, is one.
     */
    private static void scanWalls(ServerWorld world, mattonfire.dnd.dungeon.DungeonState dungeon, Vec3d at,
            double range, List<List<BlockPos>> found) {
        int floorY = dungeon.floorY();
        for (mattonfire.dnd.dungeon.DungeonState.Room room : dungeon.rooms()) {
            BlockBox box = room.box();
            for (Direction side : Direction.Type.HORIZONTAL) {
                List<BlockPos> doorway = new ArrayList<>();
                boolean alongX = side.getAxis() == Direction.Axis.Z;
                int from = alongX ? box.getMinX() + 2 : box.getMinZ() + 2;
                int to = alongX ? box.getMaxX() - 2 : box.getMaxZ() - 2;
                for (int a = from; a <= to; a++) {
                    BlockPos inner = switch (side) {
                        case NORTH -> new BlockPos(a, floorY, box.getMinZ() + 1);
                        case SOUTH -> new BlockPos(a, floorY, box.getMaxZ() - 1);
                        case WEST -> new BlockPos(box.getMinX() + 1, floorY, a);
                        default -> new BlockPos(box.getMaxX() - 1, floorY, a);
                    };
                    if (Vec3d.ofCenter(inner).distanceTo(at) > range + DungeonPiece.DOOR_HEIGHT
                            || !world.isChunkLoaded(inner)) {
                        continue;
                    }
                    BlockPos skin = inner.offset(side);
                    boolean column = true;
                    for (int y = 1; y <= DungeonPiece.DOOR_HEIGHT && column; y++) {
                        column = world.getBlockState(inner.up(y)).isIn(SECRET_DOOR)
                                && world.getBlockState(skin.up(y)).isAir();
                    }
                    if (column) {
                        for (int y = 1; y <= DungeonPiece.DOOR_HEIGHT; y++) {
                            if (Vec3d.ofCenter(inner.up(y)).distanceTo(at) <= range) {
                                doorway.add(inner.up(y));
                            }
                        }
                    }
                }
                if (!doorway.isEmpty()) {
                    found.add(doorway);
                }
            }
        }
    }

    /**
     * A sneaking Dwarf's Stonecunning against the Hidden Door obstacles round them: each one within
     * {@value #SECRET_DOOR_RANGE} blocks, still hidden from them, is found (for them and their party, with the
     * finder's XP once per reset) if their passive score + {@value #STONECUNNING_PASSIVE_BONUS} meets its DC.
     */
    private static void stonecunningDoors(ServerWorld world, ServerPlayerEntity player) {
        double rangeSq = SECRET_DOOR_RANGE * SECRET_DOOR_RANGE;
        int passive = PerceptionService.passive(player, Skill.INVESTIGATION) + STONECUNNING_PASSIVE_BONUS;
        for (BlockPos pos : ObstacleIndex.get(world).within(player.getBlockPos(), (int) Math.ceil(SECRET_DOOR_RANGE))) {
            if (player.squaredDistanceTo(pos.toCenterPos()) > rangeSq) {
                continue;
            }
            BlockState state = world.getBlockState(pos);
            if (!ObstacleBlock.isSealed(state) || !(((ObstacleBlock) state.getBlock()).type() instanceof HiddenDoorType door)
                    || !door.hiddenFrom(world, pos, player)) {
                continue;
            }
            Tier tier = world.getBlockEntity(pos) instanceof ObstacleBlockEntity be ? be.tier() : Tier.MEDIUM;
            if (passive >= tier.dc) {
                door.reveal(world, pos, player, HiddenDoorType.How.STONECUNNING);
            }
        }
    }

    /** Draws a doorway's outline in dust, on both faces of the wall, for this player only. */
    private static void outline(ServerWorld world, ServerPlayerEntity player, List<BlockPos> blocks) {
        BlockBox box = BlockBox.encompassPositions(blocks).orElse(null);
        if (box == null) {
            return;
        }
        // The plug is one block thick; its wall runs along the longer horizontal axis.
        Direction.Axis normal = box.getBlockCountX() <= box.getBlockCountZ() ? Direction.Axis.X : Direction.Axis.Z;
        double minA = normal == Direction.Axis.X ? box.getMinZ() : box.getMinX();
        double maxA = (normal == Direction.Axis.X ? box.getMaxZ() : box.getMaxX()) + 1;
        double minY = box.getMinY();
        double maxY = box.getMaxY() + 1;
        double[] faces = normal == Direction.Axis.X
                ? new double[] { box.getMinX() - 0.05, box.getMaxX() + 1.05 }
                : new double[] { box.getMinZ() - 0.05, box.getMaxZ() + 1.05 };
        for (double face : faces) {
            for (double a = minA; a <= maxA + 1e-6; a += 0.2) {
                dust(world, player, normal, face, a, minY);
                dust(world, player, normal, face, a, maxY);
            }
            for (double y = minY + 0.2; y < maxY - 1e-6; y += 0.2) {
                dust(world, player, normal, face, minA, y);
                dust(world, player, normal, face, maxA, y);
            }
        }
    }

    private static void dust(ServerWorld world, ServerPlayerEntity player, Direction.Axis normal, double face, double a,
            double y) {
        double x = normal == Direction.Axis.X ? face : a;
        double z = normal == Direction.Axis.X ? a : face;
        world.spawnParticles(player, SECRET_DOOR_DUST, true, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    // --- Keen Senses (client) ---

    /** Whether {@code viewer}'s Keen Senses outline {@code entity}: an Elf sneaking near a hostile mob. */
    public static boolean keenSenses(PlayerEntity viewer, Entity entity) {
        return viewer != null && entity != viewer && entity instanceof Monster && entity.isAlive()
                && viewer.isSneaking() && raceOf(viewer) == DndRace.ELF
                && entity.squaredDistanceTo(viewer) <= KEEN_SENSES_RANGE * KEEN_SENSES_RANGE;
    }

    // --- Second Breakfast, Tinker, Ambitious ---

    /** Called as a player eats: a Halfling gets an extra hunger point. Server side. */
    public static void secondBreakfast(PlayerEntity player, ItemStack food) {
        if (!player.getWorld().isClient && food.isFood() && RaceLifecycle.activeRaceOf(player) == DndRace.HALFLING) {
            player.getHungerManager().add(SECOND_BREAKFAST, 0.0F);
        }
    }

    /**
     * Called once per craft taken from a crafting result slot (a click, or each repeat of a shift-click) with
     * the crafted stack: a Gnome crafting a redstone component may get one extra, straight into the inventory.
     */
    public static void tinker(PlayerEntity player, ItemStack crafted) {
        if (player == null || player.getWorld().isClient || crafted.isEmpty() || !crafted.isIn(TINKER_CRAFTS)
                || RaceLifecycle.activeRaceOf(player) != DndRace.GNOME
                || player.getRandom().nextFloat() >= TINKER_CHANCE) {
            return;
        }
        ItemStack extra = crafted.copy();
        extra.setCount(1);
        DnDClasses.LOGGER.info("[Race] {} Tinker: one extra {}", player.getEntityName(),
                net.minecraft.registry.Registries.ITEM.getId(extra.getItem()));
        player.getInventory().offerOrDrop(extra);
    }

    /** Class XP after Ambitious: +10% for a Human, carrying the tenths over. */
    public static int ambitiousXp(ServerPlayerEntity player, int amount) {
        if (amount <= 0 || RaceLifecycle.activeRaceOf(player) != DndRace.HUMAN) {
            return amount;
        }
        NbtCompound data = data(player);
        // amount * 10% in tenths of a point is just amount
        int tenths = data.getInt(AMBITIOUS_CARRY_KEY) + amount * AMBITIOUS_PERCENT / 10;
        data.putInt(AMBITIOUS_CARRY_KEY, tenths % 10);
        return amount + tenths / 10;
    }

    // --- every tick ---

    private static void tick(MinecraftServer server) {
        int ticks = server.getTicks();
        if (ticks % 10 != 0) {
            return;
        }
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            DndRace race = RaceLifecycle.activeRaceOf(player);
            if (race == DndRace.ELF && ticks % 20 == 0) {
                // Trance: an Elf never goes without rest, so no phantoms
                player.resetStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST));
            } else if (race == DndRace.DWARF && player.isSneaking()) {
                ServerWorld world = player.getWorld();
                for (List<BlockPos> doorway : secretDoorways(world, player.getBlockPos(), SECRET_DOOR_RANGE)) {
                    outline(world, player, doorway);
                }
                if (ticks % 20 == 0 && !player.isCreative()) {
                    stonecunningDoors(world, player);
                }
            }
        }
    }

    private static NbtCompound data(ServerPlayerEntity player) {
        NbtCompound persistent = ((IEntityDataSaver) player).getPersistentData();
        if (!persistent.contains(BreathWeapon.DATA_KEY)) {
            persistent.put(BreathWeapon.DATA_KEY, new NbtCompound());
        }
        return persistent.getCompound(BreathWeapon.DATA_KEY);
    }
}

package mattonfire.dnd.dm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.ClassInfo;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.classes.Race.RaceInfo;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.classes.Registry.ModItemGroup;
import mattonfire.dnd.classes.Registry.ModItems;
import mattonfire.dnd.dm.encounter.Encounter;
import mattonfire.dnd.dm.encounter.EncounterSpawner;
import mattonfire.dnd.dm.encounter.Encounters;
import mattonfire.dnd.faction.Faction;
import mattonfire.dnd.faction.Factions;
import mattonfire.dnd.faction.Reputation;
import mattonfire.dnd.quest.QuestDefinition;
import mattonfire.dnd.quest.QuestInstance;
import mattonfire.dnd.quest.QuestManager;
import mattonfire.dnd.quest.Quests;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * What the DM Wand ({@code DmWandItem}) does. Everything runs on the server from a 64-block raycast out of the DM's
 * eye, so the wand reaches as far as {@code /dm encounter spawn} does, whatever the client's crosshair hit.
 * <ul>
 * <li>Sneak + right-click air: next mode.</li>
 * <li>{@link Mode#ENCOUNTER}: right-click a block spawns the selected encounter there; sneak + right-click a block
 * selects the next encounter.</li>
 * <li>{@link Mode#FREEZE}: right-click a mob or player toggles its freeze; a block toggles every mob within 16
 * blocks of it.</li>
 * <li>{@link Mode#INSPECT}: right-click a mob or player prints what the DM needs to know, to the DM only.</li>
 * <li>{@link Mode#BLINK}: right-click a block teleports there (mobs in the way are ignored); sneak + right-click
 * toggles the veil.</li>
 * </ul>
 * Non-DMs get "The wand is cold in your hand." and nothing happens. The mode and the selected encounter live in the
 * stack's NBT, so two wands can be set up differently. The static actions are public so a DM panel can reuse them.
 */
public final class DmWand {
    public static final int RANGE = 64;
    public static final int FREEZE_AREA = 16;
    private static final String NBT_MODE = "DmWandMode";
    private static final String NBT_ENCOUNTER = "DmWandEncounter";
    private static final Identifier DEFAULT_ENCOUNTER = new Identifier(DnDClasses.MOD_ID, "goblin_patrol");

    /** Last tick each player used the wand, so one click handled by two paths only acts once. */
    private static final Map<UUID, Long> LAST_USE = new HashMap<>();

    public enum Mode {
        ENCOUNTER("Encounter", Formatting.RED, "block: spawn, sneak: next encounter"),
        FREEZE("Freeze", Formatting.AQUA, "mob, player or block: freeze / thaw"),
        INSPECT("Inspect", Formatting.YELLOW, "mob or player: details"),
        BLINK("Blink", Formatting.LIGHT_PURPLE, "block: teleport, sneak: veil");

        public final String label;
        public final Formatting color;
        public final String hint;

        Mode(String label, Formatting color, String hint) {
            this.label = label;
            this.color = color;
            this.hint = hint;
        }

        public Mode next() {
            return values()[(this.ordinal() + 1) % values().length];
        }

        public MutableText text() {
            return Text.literal(this.label).formatted(this.color);
        }
    }

    private DmWand() {
    }

    public static void register() {
        // Claim clicks on blocks and entities with the wand before the block or mob reacts (no chest opening, no
        // villager trades). The client can't tell who is a DM, so it always claims them and lets the server decide.
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!isWand(player.getStackInHand(hand)) || player.isSpectator()) {
                return ActionResult.PASS;
            }
            if (player instanceof ServerPlayerEntity serverPlayer) {
                use(serverPlayer, player.getStackInHand(hand));
            }
            return ActionResult.SUCCESS;
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!isWand(player.getStackInHand(hand)) || player.isSpectator()) {
                return ActionResult.PASS;
            }
            if (player instanceof ServerPlayerEntity serverPlayer) {
                use(serverPlayer, player.getStackInHand(hand));
            }
            return ActionResult.SUCCESS;
        });
        ItemGroupEvents.modifyEntriesEvent(ModItemGroup.DND_CLASSES_ITEMGROUP).register(entries -> entries.add(ModItems.DM_WAND));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> LAST_USE.remove(handler.getPlayer().getUuid()));
    }

    public static boolean isWand(ItemStack stack) {
        return stack.isOf(ModItems.DM_WAND);
    }

    public static Mode mode(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        int index = nbt == null ? 0 : nbt.getInt(NBT_MODE);
        return Mode.values()[Math.floorMod(index, Mode.values().length)];
    }

    public static void setMode(ItemStack stack, Mode mode) {
        stack.getOrCreateNbt().putInt(NBT_MODE, mode.ordinal());
    }

    /** The encounter the wand spawns: the remembered one, else the goblin patrol, else the first loaded. Null if none are loaded. */
    @Nullable
    public static Encounter encounter(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(NBT_ENCOUNTER)) {
            Identifier id = Identifier.tryParse(nbt.getString(NBT_ENCOUNTER));
            Encounter encounter = id == null ? null : Encounters.get(id);
            if (encounter != null) {
                return encounter;
            }
        }
        Encounter fallback = Encounters.get(DEFAULT_ENCOUNTER);
        return fallback != null ? fallback : Encounters.all().stream().findFirst().orElse(null);
    }

    /** One right-click with the wand, on the server. */
    public static void use(ServerPlayerEntity player, ItemStack stack) {
        long now = player.getWorld().getTime();
        Long last = LAST_USE.put(player.getUuid(), now);
        if (last != null && last == now) {
            return;
        }
        if (!DungeonMaster.isDm(player)) {
            player.sendMessage(Text.literal("The wand is cold in your hand.").formatted(Formatting.GRAY), true);
            return;
        }
        HitResult hit = raycast(player);
        boolean sneaking = player.isSneaking();
        if (hit.getType() == HitResult.Type.MISS) {
            if (sneaking) {
                cycleMode(player, stack);
            } else {
                showMode(player, stack);
            }
            return;
        }
        switch (mode(stack)) {
            case ENCOUNTER -> encounterMode(player, stack, hit, sneaking);
            case FREEZE -> freezeMode(player, hit);
            case INSPECT -> inspectMode(player, hit);
            case BLINK -> blinkMode(player, hit, sneaking);
        }
    }

    public static void cycleMode(ServerPlayerEntity player, ItemStack stack) {
        setMode(stack, mode(stack).next());
        player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.PLAYERS, 0.4F, 1.6F);
        showMode(player, stack);
    }

    private static void showMode(ServerPlayerEntity player, ItemStack stack) {
        Mode mode = mode(stack);
        MutableText line = Text.literal("Wand: ").formatted(Formatting.GOLD).append(mode.text());
        if (mode == Mode.ENCOUNTER) {
            Encounter encounter = encounter(stack);
            line.append(Text.literal(" (" + (encounter == null ? "no encounters" : encounter.name()) + ")")
                    .formatted(Formatting.WHITE));
        }
        line.append(Text.literal(" - " + mode.hint).formatted(Formatting.GRAY));
        player.sendMessage(line, true);
        DnDClasses.LOGGER.info("[DmWand] {} mode {}", player.getEntityName(), mode.label);
    }

    /** The nearest mob or player within 64 blocks along the DM's look, else the nearest block, else a miss. */
    public static HitResult raycast(ServerPlayerEntity player) {
        HitResult block = player.raycast(RANGE, 1.0F, false);
        Vec3d eye = player.getEyePos();
        double reach = block.getType() == HitResult.Type.MISS ? RANGE : block.getPos().distanceTo(eye);
        Vec3d end = eye.add(player.getRotationVec(1.0F).multiply(reach));
        Box box = player.getBoundingBox().stretch(player.getRotationVec(1.0F).multiply(reach)).expand(1.0D);
        EntityHitResult entity = ProjectileUtil.raycast(player, eye, end, box,
                e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator() && e != player, reach * reach);
        return entity != null ? entity : block;
    }

    // ------------------------------------------------------------ encounter

    private static void encounterMode(ServerPlayerEntity player, ItemStack stack, HitResult hit, boolean sneaking) {
        if (sneaking) {
            List<Encounter> all = new ArrayList<>(Encounters.all());
            if (all.isEmpty()) {
                player.sendMessage(Text.literal("No encounters loaded.").formatted(Formatting.RED), true);
                return;
            }
            Encounter current = encounter(stack);
            int index = current == null ? -1 : all.indexOf(current);
            Encounter next = all.get((index + 1) % all.size());
            stack.getOrCreateNbt().putString(NBT_ENCOUNTER, next.id().toString());
            player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.PLAYERS, 0.4F, 1.2F);
            showMode(player, stack);
            return;
        }
        Encounter encounter = encounter(stack);
        if (encounter == null) {
            player.sendMessage(Text.literal("No encounters loaded.").formatted(Formatting.RED), true);
            return;
        }
        spawnEncounter(player, encounter, groundAt(hit));
    }

    /** Where a hit puts something: the block in front of the face that was hit, or the block an entity stands in. */
    private static BlockPos groundAt(HitResult hit) {
        if (hit instanceof BlockHitResult blockHit) {
            return blockHit.getBlockPos().offset(blockHit.getSide());
        }
        return BlockPos.ofFloored(hit.getPos());
    }

    /** Same as {@code /dm encounter spawn <id> <pos>}: numbered, tagged and listed for {@code /dm encounter}. */
    public static int spawnEncounter(ServerPlayerEntity player, Encounter encounter, BlockPos pos) {
        ServerWorld world = player.getWorld();
        DungeonMaster state = DungeonMaster.get(player.getServer());
        int number = state.nextEncounterNumber();
        EncounterSpawner.Result result = EncounterSpawner.spawn(world, encounter, pos,
                List.of(DmCommand.ENCOUNTER_TAG, DmCommand.ENCOUNTER_TAG_PREFIX + number));
        List<UUID> uuids = new ArrayList<>();
        for (Entity entity : result.entities()) {
            uuids.add(entity.getUuid());
        }
        state.addEncounter(new DungeonMaster.EncounterRecord(number, encounter.id(), encounter.name(),
                world.getRegistryKey().getValue(), pos, uuids));
        int mobs = result.entities().size();
        String chests = result.chests().isEmpty() ? "" : ", " + result.chests().size() + " chests";
        player.sendMessage(Text.literal("Encounter #" + number + " \"" + encounter.name() + "\": " + mobs
                + (mobs == 1 ? " mob" : " mobs") + chests + " at " + pos.toShortString()).formatted(Formatting.GOLD),
                false);
        return number;
    }

    // ------------------------------------------------------------ freeze

    private static void freezeMode(ServerPlayerEntity player, HitResult hit) {
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            if (!DmFreeze.freezable(target)) {
                player.sendMessage(Text.literal("That can't be frozen.").formatted(Formatting.GRAY), true);
                return;
            }
            boolean freeze = !DmFreeze.isFrozen(target);
            if (freeze) {
                DmFreeze.freeze(target);
            } else {
                DmFreeze.unfreeze(target);
            }
            player.sendMessage(Text.literal((freeze ? "Froze " : "Unfroze ")).formatted(Formatting.AQUA)
                    .append(target.getDisplayName()), true);
            return;
        }
        BlockPos center = ((BlockHitResult) hit).getBlockPos();
        Vec3d middle = Vec3d.ofCenter(center);
        List<MobEntity> mobs = player.getWorld().getEntitiesByClass(MobEntity.class,
                new Box(center).expand(FREEZE_AREA),
                mob -> mob.isAlive() && mob.squaredDistanceTo(middle) <= FREEZE_AREA * FREEZE_AREA);
        // Any mob still moving: freeze the lot. All already frozen: let them go.
        boolean freeze = mobs.stream().anyMatch(mob -> !DmFreeze.isFrozen(mob));
        int changed = 0;
        for (MobEntity mob : mobs) {
            if (freeze ? DmFreeze.freeze(mob) : DmFreeze.unfreeze(mob)) {
                changed++;
            }
        }
        player.sendMessage(Text.literal((freeze ? "Froze " : "Unfroze ") + changed
                + (changed == 1 ? " creature" : " creatures") + " within " + FREEZE_AREA + " blocks")
                .formatted(Formatting.AQUA), true);
    }

    // ------------------------------------------------------------ inspect

    private static void inspectMode(ServerPlayerEntity player, HitResult hit) {
        if (!(hit instanceof EntityHitResult entityHit)) {
            player.sendMessage(Text.literal("Inspect needs a mob or player.").formatted(Formatting.GRAY), true);
            return;
        }
        for (Text line : inspect(entityHit.getEntity())) {
            player.sendMessage(line, false);
        }
    }

    /** What Inspect prints about an entity: one header and a few detail lines. */
    public static List<Text> inspect(Entity entity) {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal("== ").formatted(Formatting.GOLD).append(entity.getDisplayName().copy())
                .append(Text.literal(" (" + Registries.ENTITY_TYPE.getId(entity.getType()) + ") ==")
                        .formatted(Formatting.GOLD)));
        if (entity instanceof ServerPlayerEntity target) {
            inspectPlayer(target, lines);
        } else if (entity instanceof LivingEntity living) {
            inspectMob(living, lines);
        }
        return lines;
    }

    private static void inspectMob(LivingEntity living, List<Text> lines) {
        MutableText line = detail("HP ", hp(living));
        if (living instanceof MobEntity mob) {
            LivingEntity target = mob.getTarget();
            line.append(detail(" | target ", target == null ? "none" : target.getDisplayName().getString()));
        }
        line.append(detail(" | frozen ", DmFreeze.isFrozen(living) ? "yes" : "no"));
        String encounter = "none";
        List<String> tags = new ArrayList<>();
        for (String tag : living.getCommandTags()) {
            if (tag.startsWith(DmCommand.ENCOUNTER_TAG_PREFIX)) {
                encounter = "#" + tag.substring(DmCommand.ENCOUNTER_TAG_PREFIX.length());
            }
            tags.add(tag);
        }
        line.append(detail(" | encounter ", encounter));
        lines.add(line);
        Faction faction = Reputation.factionOf(living);
        if (faction != null) {
            lines.add(detail("Faction ", faction.displayName().getString()));
        }
        tags.sort(String::compareTo);
        lines.add(detail("Tags ", tags.isEmpty() ? "none" : String.join(", ", tags)));
    }

    private static void inspectPlayer(ServerPlayerEntity target, List<Text> lines) {
        DndCharacter dndClass = Progression.classOf(target);
        ClassProgress progress = Progression.current(target);
        String className = dndClass == DndCharacter.NONE ? "No class" : Progression.name(dndClass);
        MutableText line = detail("Class ", dndClass == DndCharacter.NONE ? className
                : className + " " + progress.level());
        ClassInfo info = dndClass == DndCharacter.NONE ? null : ClassInfo.get(dndClass);
        if (info != null && info.role() != null) {
            line.append(detail(" | role ", info.role().text().getString()));
        }
        DndRace race = RaceLifecycle.raceOf(target);
        RaceInfo raceInfo = race == DndRace.NONE ? null : RaceInfo.get(race);
        if (raceInfo != null) {
            line.append(detail(" | race ", raceInfo.name()));
        }
        line.append(detail(" | HP ", hp(target)));
        line.append(detail(" | frozen ", DmFreeze.isFrozen(target) ? "yes" : "no"));
        lines.add(line);

        MutableText rep = Text.literal("Rep ").formatted(Formatting.GRAY);
        boolean first = true;
        for (Faction faction : Factions.all()) {
            if (!first) {
                rep.append(Text.literal(", ").formatted(Formatting.GRAY));
            }
            first = false;
            rep.append(faction.displayName().copy().formatted(Formatting.WHITE)).append(" ")
                    .append(Reputation.tier(target, faction).displayName())
                    .append(Text.literal(" (" + Reputation.get(target, faction) + ")").formatted(Formatting.GRAY));
        }
        lines.add(first ? detail("Rep ", "no factions") : rep);

        List<String> quests = new ArrayList<>();
        for (QuestInstance instance : QuestManager.get(target.getServer()).instancesOf(target.getUuid())) {
            QuestDefinition quest = Quests.get(instance.quest());
            if (quest == null) {
                quests.add(instance.quest().toString());
            } else {
                quests.add(quest.title().getString() + " (stage " + (instance.stage() + 1) + "/"
                        + quest.stages().size() + ")");
            }
        }
        lines.add(detail("Quests ", quests.isEmpty() ? "none" : String.join(", ", quests)));
    }

    private static String hp(LivingEntity living) {
        return String.format(java.util.Locale.ROOT, "%.1f/%.1f", living.getHealth(), living.getMaxHealth());
    }

    private static MutableText detail(String label, String value) {
        return Text.literal(label).formatted(Formatting.GRAY).append(Text.literal(value).formatted(Formatting.WHITE));
    }

    // ------------------------------------------------------------ blink

    private static void blinkMode(ServerPlayerEntity player, HitResult hit, boolean sneaking) {
        if (sneaking) {
            DungeonMaster state = DungeonMaster.get(player.getServer());
            boolean veiled = !state.isVeiled(player.getUuid());
            state.setVeiled(player, veiled);
            DmVeil.apply(player);
            player.sendMessage(Text.literal(veiled ? "You draw the veil." : "You step out from behind the veil.")
                    .formatted(Formatting.LIGHT_PURPLE), true);
            return;
        }
        // Blink goes to blocks only: a mob in the way doesn't stop the ray.
        HitResult block = player.raycast(RANGE, 1.0F, false);
        if (!(block instanceof BlockHitResult blockHit) || block.getType() != HitResult.Type.BLOCK) {
            player.sendMessage(Text.literal("No block within " + RANGE + " blocks to blink to.")
                    .formatted(Formatting.GRAY), true);
            return;
        }
        ServerWorld world = player.getWorld();
        BlockPos pos = standable(world, groundAt(blockHit));
        world.spawnParticles(ParticleTypes.PORTAL, player.getX(), player.getBodyY(0.5D), player.getZ(), 20, 0.3D,
                0.6D, 0.3D, 0.1D);
        player.teleport(world, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, player.getYaw(), player.getPitch());
        player.fallDistance = 0.0F;
        world.spawnParticles(ParticleTypes.PORTAL, player.getX(), player.getBodyY(0.5D), player.getZ(), 20, 0.3D,
                0.6D, 0.3D, 0.1D);
        if (!DungeonMaster.isVeiled(player)) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                    SoundCategory.PLAYERS, 0.6F, 1.2F);
        }
    }

    /** The first spot at or above {@code pos} (up to 8 blocks) with room for a player. */
    private static BlockPos standable(ServerWorld world, BlockPos pos) {
        for (int dy = 0; dy <= 8; dy++) {
            BlockPos feet = pos.up(dy);
            if (world.getBlockState(feet).getCollisionShape(world, feet).isEmpty()
                    && world.getBlockState(feet.up()).getCollisionShape(world, feet.up()).isEmpty()) {
                return feet;
            }
        }
        return pos;
    }

    /** The wand's name with its mode: "DM Wand (Freeze)". */
    public static Text name(ItemStack stack, Text base) {
        return base.copy().append(Text.literal(" (").formatted(Formatting.GRAY)).append(mode(stack).text())
                .append(Text.literal(")").formatted(Formatting.GRAY));
    }
}

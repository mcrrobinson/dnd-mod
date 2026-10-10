package mattonfire.dnd.classes.Race;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.CharacterSheet;
import mattonfire.dnd.classes.Damages.ModDamageTypes;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.classes.Registry.ModEffects;
import mattonfire.dnd.classes.Rest.RestEvents;
import mattonfire.dnd.classes.SkillChecks.SaveResult;
import mattonfire.dnd.entity.DragonSaves;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.RaycastContext;

/**
 * The Dragonborn Breath Weapon, fired with the racial key ({@code key.dnd-classes.racial}, R).
 * <ul>
 * <li>A 6-block, 60° cone from the eyes. Damage 6 + ⌊class level / 2⌋. Line of sight is checked to each
 * target's eyes. Party members, the breather's pets and the breather are never hit.</li>
 * <li>Each target rolls a DEX save (DC 8 + CON mod + proficiency) through {@link DragonSaves#save}; a success
 * halves the damage and shortens or avoids the rider.</li>
 * <li>Riders by {@link DragonAncestry}: Ember burns (and may light flammable blocks), Frost freezes and turns
 * water in the cone to frosted ice, Storm arcs to one more target.</li>
 * <li>60 s cooldown, stored as an overworld time in the persistent compound under {@code dndRace}, so it
 * survives death and rejoining. A short or long rest recharges it.</li>
 * </ul>
 * Later features (the Dragon Temple's Blessing of the Ancestors) change damage and cooldown through
 * {@link #MODIFY}.
 */
public final class BreathWeapon {
    public static final Identifier C2S_RACIAL_POWER = new Identifier(DnDClasses.MOD_ID, "racial_power");
    /** Breath visuals for everyone nearby: breather id, ancestry, origin, direction. */
    public static final Identifier S2C_BREATH = new Identifier(DnDClasses.MOD_ID, "racial_breath");
    /** The player's own breath state for the HUD: usable, ancestry, cooldown ticks left, cooldown total. */
    public static final Identifier S2C_COOLDOWN = new Identifier(DnDClasses.MOD_ID, "racial_breath_cooldown");

    public static final double RANGE = 6.0;
    public static final double HALF_ANGLE_DEG = 30.0;
    public static final float BASE_DAMAGE = 6.0F;
    public static final int COOLDOWN_TICKS = 60 * 20;
    public static final int BURN_TICKS = 5 * 20;
    public static final int FREEZE_TICKS = 3 * 20;
    public static final double ARC_RANGE = 4.0;
    public static final int GLOW_TICKS = 2 * 20;
    /** Chance per block a ray of Ember breath hits to set it alight. */
    public static final float IGNITE_CHANCE = 0.15F;

    public static final String DATA_KEY = "dndRace";
    public static final String READY_AT_KEY = "breathReadyAt";
    /** The length of the last cooldown, for the HUD's sweep. */
    public static final String TOTAL_KEY = "breathCooldown";
    private static final String SAVE_TAG = "racial_breath";

    public static final String SAVE_LABEL = "save.dndclasses.dragonborn_breath";
    public static final String SAVE_EFFECT = "save.dndclasses.effect.dragonborn_breath";

    public static final RegistryKey<DamageType> FIRE_DAMAGE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE,
            new Identifier(DnDClasses.MOD_ID, "dragonborn_fire"));
    public static final RegistryKey<DamageType> LIGHTNING_DAMAGE = RegistryKey.of(RegistryKeys.DAMAGE_TYPE,
            new Identifier(DnDClasses.MOD_ID, "dragonborn_lightning"));

    private static final double COS_HALF_ANGLE = Math.cos(Math.toRadians(HALF_ANGLE_DEG));

    /** Damage and cooldown of one breath, before saves. */
    public record Stats(float damage, int cooldownTicks) {
    }

    /** Lets other features change a breath (e.g. the shrine's blessing: half cooldown, +2 damage). */
    @FunctionalInterface
    public interface Modifier {
        Stats modify(ServerPlayerEntity player, DragonAncestry ancestry, Stats stats);
    }

    public static final Event<Modifier> MODIFY = EventFactory.createArrayBacked(Modifier.class,
            listeners -> (player, ancestry, stats) -> {
                for (Modifier listener : listeners) {
                    stats = listener.modify(player, ancestry, stats);
                }
                return stats;
            });

    private BreathWeapon() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(C2S_RACIAL_POWER,
                (server, player, handler, buf, sender) -> server.execute(() -> useRacialPower(player)));
        // Breath Weapon recharges on any rest (design: short rest, or 60 s, whichever comes first).
        RestEvents.AFTER_REST.register((player, kind, source) -> {
            if (remainingTicks(player) > 0) {
                setReadyAt(player, 0);
                player.sendMessage(Text.translatable("message.dndclasses.breath.recharged")
                        .formatted(Formatting.GOLD), true);
            }
            sync(player);
        });
    }

    // --- the key ---

    /** The racial key: fires the race's active power. Only Dragonborn have one. */
    public static void useRacialPower(ServerPlayerEntity player) {
        if (!player.isAlive() || player.isSpectator()) {
            return;
        }
        DndRace race = RaceLifecycle.activeRaceOf(player);
        DragonAncestry ancestry = RaceLifecycle.ancestryOf(player);
        if (race != DndRace.DRAGONBORN || ancestry == DragonAncestry.NONE) {
            player.sendMessage(Text.translatable("message.dndclasses.racial.none").formatted(Formatting.GRAY), true);
            return;
        }
        long left = remainingTicks(player);
        if (left > 0 && !player.isCreative()) {
            player.sendMessage(Text.translatable("message.dndclasses.breath.cooldown", (left + 19) / 20)
                    .formatted(Formatting.RED), true);
            return;
        }
        breathe(player, ancestry);
    }

    /** Breathes now, whatever the cooldown, and starts the cooldown. */
    public static void breathe(ServerPlayerEntity player, DragonAncestry ancestry) {
        ServerWorld world = (ServerWorld) player.getWorld();
        Stats stats = MODIFY.invoker().modify(player, ancestry, new Stats(baseDamage(player), COOLDOWN_TICKS));
        setReadyAt(player, now(player) + stats.cooldownTicks());
        data(player).putInt(TOTAL_KEY, stats.cooldownTicks());

        Vec3d eye = player.getEyePos();
        Vec3d look = player.getRotationVec(1.0F);
        int dc = saveDc(player);

        List<LivingEntity> targets = targets(player, eye, look);
        LivingEntity first = targets.isEmpty() ? null : targets.get(0);
        for (LivingEntity target : targets) {
            hit(player, ancestry, target, stats.damage(), dc);
        }
        if (ancestry == DragonAncestry.STORM && first != null) {
            LivingEntity arc = arcTarget(player, first, targets);
            if (arc != null) {
                hit(player, ancestry, arc, stats.damage() / 2.0F, dc);
                arcParticles(world, first, arc);
            }
        }
        affectBlocks(world, player, ancestry, eye, look);

        playSound(world, player, ancestry);
        sendVisuals(player, ancestry, eye, look);
        sync(player);
    }

    /** 6 + ⌊class level / 2⌋ (6 to 11). */
    public static float baseDamage(ServerPlayerEntity player) {
        return BASE_DAMAGE + AbilityScores.sheet(player).level() / 2;
    }

    /** 8 + CON modifier + proficiency bonus. */
    public static int saveDc(ServerPlayerEntity player) {
        CharacterSheet sheet = AbilityScores.sheet(player);
        return 8 + sheet.modifier(Ability.CON) + sheet.proficiencyBonus();
    }

    // --- targets ---

    /** Living things in the cone with a clear line to their eyes, nearest first. */
    private static List<LivingEntity> targets(ServerPlayerEntity player, Vec3d eye, Vec3d look) {
        Box area = new Box(eye, eye).expand(RANGE + 1.0);
        List<LivingEntity> found = new ArrayList<>(player.getWorld().getEntitiesByClass(LivingEntity.class, area,
                e -> e != player && e.isAlive() && !e.isSpectator() && !isFriendly(player, e)
                        && inCone(eye, look, e) && hasLineOfSight(player, eye, e.getEyePos())));
        found.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(eye)));
        return found;
    }

    /** In range and within the cone's half-angle, aiming at the eyes or the middle of the body. */
    private static boolean inCone(Vec3d eye, Vec3d look, LivingEntity e) {
        Box box = e.getBoundingBox();
        Vec3d closest = new Vec3d(MathHelper.clamp(eye.x, box.minX, box.maxX),
                MathHelper.clamp(eye.y, box.minY, box.maxY), MathHelper.clamp(eye.z, box.minZ, box.maxZ));
        if (closest.squaredDistanceTo(eye) > RANGE * RANGE) {
            return false;
        }
        return withinAngle(eye, look, e.getEyePos()) || withinAngle(eye, look, box.getCenter())
                || box.contains(eye.add(look.multiply(0.5)));
    }

    private static boolean withinAngle(Vec3d eye, Vec3d look, Vec3d point) {
        Vec3d to = point.subtract(eye);
        double length = to.length();
        return length < 1.0E-4 || to.dotProduct(look) / length >= COS_HALF_ANGLE;
    }

    private static boolean hasLineOfSight(Entity breather, Vec3d from, Vec3d to) {
        return breather.getWorld().raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, breather)).getType() == HitResult.Type.MISS;
    }

    /** Party members and the breather's own pets don't get hit. */
    public static boolean isFriendly(ServerPlayerEntity player, Entity entity) {
        return PartyManager.areInSameParty(player, entity)
                || entity instanceof Tameable pet && player.getUuid().equals(pet.getOwnerUuid());
    }

    /** Storm: the nearest other living thing within 4 blocks of the first target, not already hit. */
    @Nullable
    private static LivingEntity arcTarget(ServerPlayerEntity player, LivingEntity first, List<LivingEntity> hit) {
        Box area = first.getBoundingBox().expand(ARC_RANGE);
        return player.getWorld().getEntitiesByClass(LivingEntity.class, area,
                e -> e != player && e != first && e.isAlive() && !e.isSpectator() && !hit.contains(e)
                        && !isFriendly(player, e) && e.squaredDistanceTo(first) <= ARC_RANGE * ARC_RANGE
                        && hasLineOfSight(first, first.getEyePos(), e.getEyePos()))
                .stream().min(Comparator.comparingDouble(e -> e.squaredDistanceTo(first))).orElse(null);
    }

    // --- hitting ---

    private static void hit(ServerPlayerEntity player, DragonAncestry ancestry, LivingEntity target, float damage,
            int dc) {
        ServerWorld world = (ServerWorld) player.getWorld();
        DamageSource source = switch (ancestry) {
            case FROST -> ModDamageTypes.of(world, ModDamageTypes.FROST_BREATH, player);
            case STORM -> ModDamageTypes.of(world, LIGHTNING_DAMAGE, player);
            default -> ModDamageTypes.of(world, FIRE_DAMAGE, player);
        };
        if (DragonSaves.isUnaffected(target, source)) {
            return;
        }
        SaveResult save = DragonSaves.save(target, player, SAVE_LABEL, dc, SAVE_EFFECT, SAVE_TAG, 20);
        if (!target.damage(source, save.damage(damage))) {
            return;
        }
        switch (ancestry) {
            case EMBER -> target.setFireTicks(Math.max(target.getFireTicks(), save.duration(BURN_TICKS)));
            case FROST -> {
                target.extinguish();
                if (save.failed()) {
                    target.addStatusEffect(new StatusEffectInstance(ModEffects.FREEZE, FREEZE_TICKS, 0), player);
                }
            }
            case STORM -> target.addStatusEffect(
                    new StatusEffectInstance(StatusEffects.GLOWING, GLOW_TICKS, 0, false, false), player);
            default -> {
            }
        }
    }

    /** Ember lights flammable blocks it hits (15% each); Frost freezes water sources in the cone. */
    private static void affectBlocks(ServerWorld world, ServerPlayerEntity player, DragonAncestry ancestry,
            Vec3d eye, Vec3d look) {
        if (ancestry == DragonAncestry.FROST) {
            freezeWater(world, player, eye, look);
        } else if (ancestry == DragonAncestry.EMBER && world.getGameRules().getBoolean(GameRules.DO_FIRE_TICK)
                && world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
            igniteBlocks(world, player);
        }
    }

    private static void igniteBlocks(ServerWorld world, ServerPlayerEntity player) {
        Vec3d eye = player.getEyePos();
        for (int dPitch = -20; dPitch <= 20; dPitch += 10) {
            for (int dYaw = -30; dYaw <= 30; dYaw += 10) {
                Vec3d dir = Vec3d.fromPolar(player.getPitch() + dPitch, player.getYaw() + dYaw);
                BlockHitResult hit = world.raycast(new RaycastContext(eye, eye.add(dir.multiply(RANGE)),
                        RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, player));
                if (hit.getType() != HitResult.Type.BLOCK || world.random.nextFloat() >= IGNITE_CHANCE) {
                    continue;
                }
                BlockState state = world.getBlockState(hit.getBlockPos());
                if (!state.getMaterial().isBurnable()) {
                    continue;
                }
                BlockPos firePos = hit.getBlockPos().offset(hit.getSide());
                if (world.getBlockState(firePos).isAir()
                        && AbstractFireBlock.canPlaceAt(world, firePos, player.getHorizontalFacing())) {
                    world.setBlockState(firePos, AbstractFireBlock.getState(world, firePos));
                }
            }
        }
    }

    /** Water source blocks with air above, in range and in the cone, turn to frosted ice (a bridge). */
    private static void freezeWater(ServerWorld world, ServerPlayerEntity player, Vec3d eye, Vec3d look) {
        BlockState water = Blocks.WATER.getDefaultState();
        BlockState ice = Blocks.FROSTED_ICE.getDefaultState();
        int r = (int) Math.ceil(RANGE);
        BlockPos center = BlockPos.ofFloored(eye);
        for (BlockPos pos : BlockPos.iterate(center.add(-r, -r, -r), center.add(r, r, r))) {
            Vec3d top = new Vec3d(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            if (top.squaredDistanceTo(eye) > RANGE * RANGE || !withinAngle(eye, look, top)) {
                continue;
            }
            if (world.getBlockState(pos) == water && world.getBlockState(pos.up()).isAir()
                    && world.canPlace(ice, pos, ShapeContext.absent())) {
                world.setBlockState(pos, ice);
                world.scheduleBlockTick(pos, Blocks.FROSTED_ICE, MathHelper.nextInt(world.random, 60, 120));
            }
        }
    }

    // --- effects ---

    private static void playSound(ServerWorld world, ServerPlayerEntity player, DragonAncestry ancestry) {
        switch (ancestry) {
            case FROST -> world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 1.0F, 0.6F);
            case STORM -> world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 0.8F, 1.4F);
            default -> world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0F, 0.7F);
        }
    }

    /** A line of sparks from the first target to the arc target. */
    private static void arcParticles(ServerWorld world, LivingEntity from, LivingEntity to) {
        Vec3d a = from.getBoundingBox().getCenter();
        Vec3d b = to.getBoundingBox().getCenter();
        int steps = Math.max(4, (int) (a.distanceTo(b) * 4));
        for (int i = 0; i <= steps; i++) {
            Vec3d p = a.lerp(b, i / (double) steps);
            world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
        }
    }

    /** Everyone who can see the breather (and the breather) draws the cone on their own client. */
    private static void sendVisuals(ServerPlayerEntity player, DragonAncestry ancestry, Vec3d eye, Vec3d look) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(player.getId());
        buf.writeVarInt(ancestry.getValue());
        buf.writeDouble(eye.x);
        buf.writeDouble(eye.y);
        buf.writeDouble(eye.z);
        buf.writeFloat((float) look.x);
        buf.writeFloat((float) look.y);
        buf.writeFloat((float) look.z);
        for (ServerPlayerEntity viewer : PlayerLookup.tracking(player)) {
            if (viewer != player) {
                ServerPlayNetworking.send(viewer, S2C_BREATH, PacketByteBufs.copy(buf));
            }
        }
        ServerPlayNetworking.send(player, S2C_BREATH, buf);
    }

    // --- cooldown ---

    private static long now(ServerPlayerEntity player) {
        return player.getServer().getOverworld().getTime();
    }

    private static NbtCompound data(ServerPlayerEntity player) {
        NbtCompound persistent = ((IEntityDataSaver) player).getPersistentData();
        if (!persistent.contains(DATA_KEY)) {
            persistent.put(DATA_KEY, new NbtCompound());
        }
        return persistent.getCompound(DATA_KEY);
    }

    private static void setReadyAt(ServerPlayerEntity player, long readyAt) {
        data(player).putLong(READY_AT_KEY, readyAt);
    }

    /** Ticks until the breath is ready again, 0 when ready. */
    public static long remainingTicks(ServerPlayerEntity player) {
        long left = data(player).getLong(READY_AT_KEY) - now(player);
        // Longer than a cooldown can be means the world clock went back (another save); don't lock it.
        return left <= 0 || left > COOLDOWN_TICKS * 2L ? 0 : left;
    }

    /** Ready the breath now (admin, rests). */
    public static void reset(ServerPlayerEntity player) {
        setReadyAt(player, 0);
        sync(player);
    }

    /** Tells the client whether it has a breath, which, and the cooldown for the HUD icon. */
    public static void sync(ServerPlayerEntity player) {
        if (player.networkHandler == null) {
            return;
        }
        boolean usable = RaceLifecycle.activeRaceOf(player) == DndRace.DRAGONBORN
                && RaceLifecycle.ancestryOf(player) != DragonAncestry.NONE;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(usable);
        buf.writeVarInt(RaceLifecycle.ancestryOf(player).getValue());
        buf.writeVarInt((int) remainingTicks(player));
        int total = data(player).getInt(TOTAL_KEY);
        buf.writeVarInt(total > 0 ? total : COOLDOWN_TICKS);
        ServerPlayNetworking.send(player, S2C_COOLDOWN, buf);
    }
}

package mattonfire.dnd.entity.hireling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.PartyRole;
import mattonfire.dnd.classes.Conditions.Condition;
import mattonfire.dnd.classes.Conditions.Conditions;
import mattonfire.dnd.classes.Downed.Downed;
import mattonfire.dnd.classes.Downed.Revives;
import mattonfire.dnd.classes.Downed.Stabilise;
import mattonfire.dnd.classes.Party.Party;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.classes.Party.PartySynergies;
import mattonfire.dnd.classes.SkillChecks.Perceivable;
import mattonfire.dnd.classes.SkillChecks.PerceptionService;
import mattonfire.dnd.dm.DungeonMaster;
import mattonfire.dnd.entity.HirelingEntity;
import mattonfire.dnd.entity.boss.Boss;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Each hireling role's automatic ability ({@code docs/systems/hirelings.md}, "Role abilities"):
 * <ul>
 * <li><b>Tank, Taunt</b>: every {@value #TAUNT_COOLDOWN} ticks, hostiles within {@value #TAUNT_RANGE} blocks that
 * target its owner or a party member turn on it for {@value #TAUNT_TICKS} ticks (a boss only
 * {@code 30%} of the time).</li>
 * <li><b>Healer, Cure Wounds</b>: every {@value #CURE_COOLDOWN} ticks, heals the most hurt ally under half health
 * (or Poisoned or Frightened) within {@value #CURE_RANGE} blocks for {@value #CURE_HEAL} (x1.25 Triage on players)
 * and ends Poisoned and Frightened. <b>Revivify</b>: stabilises the Downed in {@value #REVIVIFY_HOLD_TICKS} ticks
 * and, with Cure Wounds ready, stands them up.</li>
 * <li><b>Damage, Hunter's Mark</b>: every {@value #MARK_COOLDOWN} ticks, its next hit on a mob that targets
 * someone else deals +{@value #MARK_DAMAGE} and makes it glow for {@value #MARK_GLOW_TICKS} ticks.</li>
 * <li><b>Support, Rallying Song</b>: every {@value #SONG_COOLDOWN} ticks, when an ally within
 * {@value #SONG_RANGE} blocks is under half health or Frightened: Regeneration I for {@value #SONG_REGEN_TICKS}
 * ticks to the allies there, and it ends Frightened.</li>
 * <li><b>Utility, Scout and Torchbearer</b>: passive Perception {@value #SCOUT_PASSIVE} for hidden things
 * within {@value #SCOUT_RANGE} blocks, revealed to its owner and party; in the dark it lays a torch every
 * {@value #TORCH_EVERY} blocks walked, from a stock of {@value #TORCH_STOCK}.</li>
 * </ul>
 * Devoted hirelings heal and hit {@code 10%} harder. Abilities stop while the hireling can't act
 * ({@link Conditions#canAct}). Logs: {@code [Hireling] <name> <ability> -> <target>}.
 *
 * <p>Allies are the owner, the owner's online party members and the hirelings of any of them.
 */
public final class HirelingAbilities {
    public static final int TAUNT_COOLDOWN = 200;
    public static final double TAUNT_RANGE = 8.0D;
    public static final int TAUNT_TICKS = 100;
    public static final float TAUNT_BOSS_CHANCE = 0.30F;

    public static final int CURE_COOLDOWN = 300;
    public static final double CURE_RANGE = 12.0D;
    public static final float CURE_HEAL = 6.0F;
    /** Revivify: a Healer stabilises the Downed in this many ticks instead of {@link Stabilise#HOLD_TICKS}. */
    public static final int REVIVIFY_HOLD_TICKS = 20;

    public static final int MARK_COOLDOWN = 160;
    public static final float MARK_DAMAGE = 6.0F;
    public static final int MARK_GLOW_TICKS = 100;

    public static final int SONG_COOLDOWN = 400;
    public static final double SONG_RANGE = 8.0D;
    public static final int SONG_REGEN_TICKS = 100;

    public static final int SCOUT_PASSIVE = 15;
    public static final double SCOUT_RANGE = 8.0D;
    public static final int SCOUT_INTERVAL = 20;
    public static final int TORCH_LIGHT = 3;
    public static final double TORCH_EVERY = 10.0D;
    public static final int TORCH_STOCK = 16;

    /** Below this share of max health an ally needs healing (or a song). */
    public static final float HURT = 0.5F;

    /** Taunted mob -> the Tank it must stay on, and until when. */
    private static final Map<UUID, Taunt> TAUNTS = new HashMap<>();

    private record Taunt(ServerWorld world, UUID tank, long until) {
    }

    private HirelingAbilities() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(HirelingAbilities::holdTaunts);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> TAUNTS.clear());
    }

    // ---- Who's on whose side ----

    /** Whether the player is the hireling's owner or in its owner's party. */
    public static boolean alliedWith(HirelingEntity hireling, @Nullable PlayerEntity player) {
        return player != null && (hireling.isOwner(player)
                || Hirelings.sameParty(hireling.getServer(), hireling.ownerUuid(), player.getUuid()));
    }

    /** Whether two hirelings serve the same side (the same owner or owners in one party). */
    public static boolean sameSide(HirelingEntity a, HirelingEntity b) {
        return a.ownerUuid() != null && (a.ownerUuid().equals(b.ownerUuid())
                || Hirelings.sameParty(a.getServer(), a.ownerUuid(), b.ownerUuid()));
    }

    /**
     * Active hirelings within {@code radius} of the player that serve them or their party: the hirelings that
     * count for the player's party synergies.
     */
    public static List<HirelingEntity> sideHirelings(ServerPlayerEntity player, double radius) {
        return player.getWorld().getEntitiesByClass(HirelingEntity.class, player.getBoundingBox().expand(radius),
                h -> h.isActive() && h.squaredDistanceTo(player) <= radius * radius && alliedWith(h, player));
    }

    /** The owner and their online party members in the hireling's world (not DMs, not spectators). */
    private static List<ServerPlayerEntity> sidePlayers(HirelingEntity hireling) {
        List<ServerPlayerEntity> people = new ArrayList<>();
        ServerPlayerEntity owner = hireling.ownerPlayer();
        if (owner == null) {
            return people;
        }
        people.add(owner);
        Party party = PartyManager.get(owner.getServer()).getParty(owner.getUuid());
        if (party != null) {
            for (ServerPlayerEntity member : party.getOnlineMembers(owner.getServer())) {
                if (member != owner) {
                    people.add(member);
                }
            }
        }
        people.removeIf(p -> p.world != hireling.world || !p.isAlive() || p.isSpectator() || DungeonMaster.isDm(p));
        return people;
    }

    /** Allies within {@code range} of the hireling that are on their feet: side players and hirelings (itself too). */
    private static List<LivingEntity> allies(HirelingEntity hireling, double range) {
        List<LivingEntity> list = new ArrayList<>();
        for (ServerPlayerEntity player : sidePlayers(hireling)) {
            if (!Downed.is(player) && player.squaredDistanceTo(hireling) <= range * range) {
                list.add(player);
            }
        }
        list.addAll(hireling.world.getEntitiesByClass(HirelingEntity.class, hireling.getBoundingBox().expand(range),
                h -> h.isActive() && h.squaredDistanceTo(hireling) <= range * range && sameSide(h, hireling)));
        return list;
    }

    public static boolean isBoss(Entity entity) {
        return entity instanceof Boss || entity instanceof EnderDragonEntity || entity instanceof WitherEntity;
    }

    private static String nameOf(Entity entity) {
        if (entity instanceof HirelingEntity hireling) {
            return hireling.firstName();
        }
        return entity instanceof PlayerEntity player ? player.getEntityName() : entity.getName().getString();
    }

    // ---- The tick ----

    /** Called every server tick for an active (hired, conscious) hireling. */
    public static void tick(HirelingEntity hireling) {
        if (hireling.abilityCooldown() > 0) {
            hireling.setAbilityCooldown(hireling.abilityCooldown() - 1);
        }
        if (!Conditions.canAct(hireling) || hireling.ownerPlayer() == null) {
            return;
        }
        boolean ready = hireling.abilityCooldown() <= 0;
        switch (hireling.role()) {
            case TANK -> {
                if (ready && hireling.age % 5 == 0) {
                    taunt(hireling);
                }
            }
            case HEALER -> {
                if (ready && hireling.age % 5 == 0) {
                    cureWounds(hireling);
                }
            }
            case SUPPORT -> {
                if (ready && hireling.age % 5 == 0) {
                    rallyingSong(hireling);
                }
            }
            case UTILITY -> {
                if (hireling.age % SCOUT_INTERVAL == 0) {
                    scout(hireling);
                }
                torchbearer(hireling);
            }
            case DAMAGE -> {
                // Hunter's Mark fires on its next hit (modifyDamage).
            }
        }
    }

    // ---- Tank: Taunt ----

    private static boolean taunt(HirelingEntity tank) {
        List<MobEntity> mobs = tank.world.getEntitiesByClass(MobEntity.class, tank.getBoundingBox().expand(TAUNT_RANGE),
                mob -> mob instanceof Monster && mob.isAlive() && mob.squaredDistanceTo(tank) <= TAUNT_RANGE * TAUNT_RANGE
                        && mob.getTarget() instanceof PlayerEntity victim && alliedWith(tank, victim));
        if (mobs.isEmpty()) {
            return false;
        }
        long until = tank.world.getTime() + TAUNT_TICKS;
        int taunted = 0;
        for (MobEntity mob : mobs) {
            String was = nameOf(mob.getTarget());
            if (isBoss(mob)) {
                float roll = tank.getRandom().nextFloat();
                if (roll >= TAUNT_BOSS_CHANCE) {
                    DnDClasses.LOGGER.info("[Hireling] {} Taunt -> {} resists (roll {} vs {})", tank.firstName(),
                            mob.getName().getString(), String.format("%.2f", roll), TAUNT_BOSS_CHANCE);
                    continue;
                }
            }
            mob.setTarget(tank);
            TAUNTS.put(mob.getUuid(), new Taunt((ServerWorld) tank.world, tank.getUuid(), until));
            taunted++;
            DnDClasses.LOGGER.info("[Hireling] {} Taunt -> {} (was on {})", tank.firstName(), mob.getName().getString(), was);
        }
        tank.setAbilityCooldown(TAUNT_COOLDOWN);
        tank.swingHand(Hand.MAIN_HAND);
        tank.world.playSound(null, tank.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.NEUTRAL, 1.0F, 0.6F);
        ((ServerWorld) tank.world).spawnParticles(ParticleTypes.ANGRY_VILLAGER, tank.getX(), tank.getBodyY(1.1),
                tank.getZ(), 3, 0.3D, 0.2D, 0.3D, 0.0D);
        if (taunted > 0) {
            ServerPlayerEntity owner = tank.ownerPlayer();
            if (owner != null) {
                owner.sendMessage(Text.translatable("hireling.dndclasses.ability.taunt", tank.firstName(), taunted)
                        .formatted(Formatting.GOLD), true);
            }
        }
        return taunted > 0;
    }

    /** Keeps taunted mobs on their Tank until the taunt runs out or the Tank goes down. */
    private static void holdTaunts(MinecraftServer server) {
        if (TAUNTS.isEmpty()) {
            return;
        }
        for (Iterator<Map.Entry<UUID, Taunt>> it = TAUNTS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Taunt> entry = it.next();
            Taunt taunt = entry.getValue();
            Entity mobEntity = taunt.world.getEntity(entry.getKey());
            Entity tankEntity = taunt.world.getEntity(taunt.tank);
            if (!(mobEntity instanceof MobEntity mob) || !mob.isAlive() || taunt.world.getTime() >= taunt.until
                    || !(tankEntity instanceof HirelingEntity tank) || !tank.isActive()) {
                it.remove();
                continue;
            }
            if (mob.getTarget() != tank) {
                mob.setTarget(tank);
            }
        }
    }

    /** Whether the mob is held on a Tank hireling by a Taunt right now (for tests and the docs). */
    public static boolean isTaunted(MobEntity mob) {
        return TAUNTS.containsKey(mob.getUuid());
    }

    // ---- Healer: Cure Wounds and Revivify ----

    private static boolean curable(LivingEntity entity) {
        return Conditions.has(entity, Condition.POISONED) || Conditions.has(entity, Condition.FRIGHTENED)
                || entity.hasStatusEffect(StatusEffects.POISON);
    }

    private static boolean cureWounds(HirelingEntity healer) {
        LivingEntity best = null;
        float bestRatio = Float.MAX_VALUE;
        for (LivingEntity ally : allies(healer, CURE_RANGE)) {
            float ratio = ally.getHealth() / Math.max(1.0F, ally.getMaxHealth());
            if (ratio >= HURT && !curable(ally)) {
                continue;
            }
            if (ratio < bestRatio) {
                best = ally;
                bestRatio = ratio;
            }
        }
        if (best == null) {
            return false;
        }
        float amount = CURE_HEAL * healer.devotedBonus();
        if (best instanceof ServerPlayerEntity player) {
            float boosted = amount * PartySynergies.TRIAGE_MULTIPLIER;
            DnDClasses.LOGGER.info("[Party] Triage: {} (hireling) heals {} for {} instead of {}", healer.firstName(),
                    player.getEntityName(), boosted, amount);
            amount = boosted;
            Revives.healFrom(null, player, amount);
        } else {
            best.heal(amount);
        }
        List<String> ended = new ArrayList<>();
        if (Conditions.remove(best, Condition.POISONED)) {
            ended.add("poisoned");
        }
        if (best.removeStatusEffect(StatusEffects.POISON) && !ended.contains("poisoned")) {
            ended.add("poison");
        }
        if (Conditions.remove(best, Condition.FRIGHTENED)) {
            ended.add("frightened");
        }
        healer.setAbilityCooldown(CURE_COOLDOWN);
        healer.swingHand(Hand.MAIN_HAND);
        healer.getLookControl().lookAt(best);
        ServerWorld world = (ServerWorld) healer.world;
        world.spawnParticles(ParticleTypes.HEART, best.getX(), best.getBodyY(1.0), best.getZ(), 5, 0.4D, 0.3D, 0.4D, 0.0D);
        world.playSound(null, best.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.NEUTRAL, 0.6F, 1.4F);
        DnDClasses.LOGGER.info("[Hireling] {} Cure Wounds -> {} ({} HP{})", healer.firstName(), nameOf(best),
                amount, ended.isEmpty() ? "" : ", ends " + String.join(", ", ended));
        if (best instanceof ServerPlayerEntity player) {
            player.sendMessage(Text.translatable("hireling.dndclasses.ability.cure", healer.firstName())
                    .formatted(Formatting.GREEN), true);
        }
        return true;
    }

    /** How long this hireling holds a Downed ally before they're stable: 20 ticks for a Healer (Revivify). */
    public static int stabiliseTicks(HirelingEntity hireling) {
        return hireling.role() == HirelingRole.HEALER ? REVIVIFY_HOLD_TICKS : Stabilise.HOLD_TICKS;
    }

    /** After a Healer stabilises a Downed ally: with Cure Wounds ready, it stands them up with 6 HP. */
    public static void revivify(HirelingEntity healer, ServerPlayerEntity patient) {
        if (healer.role() != HirelingRole.HEALER || healer.abilityCooldown() > 0 || !Conditions.canAct(healer)
                || !Downed.is(patient)) {
            return;
        }
        float amount = CURE_HEAL * healer.devotedBonus();
        healer.setAbilityCooldown(CURE_COOLDOWN);
        DnDClasses.LOGGER.info("[Hireling] {} Revivify -> {} ({} HP)", healer.firstName(), patient.getEntityName(), amount);
        Revives.healFrom(null, patient, amount);
        ((ServerWorld) healer.world).spawnParticles(ParticleTypes.HEART, patient.getX(), patient.getBodyY(1.0),
                patient.getZ(), 8, 0.4D, 0.3D, 0.4D, 0.0D);
    }

    // ---- Damage: Hunter's Mark (and hireling Opportunist) ----

    /**
     * The damage hook ({@code ProgressionEvents.modifyDamage}): a Damage hireling's Hunter's Mark, and
     * Opportunist for its hits on a mob that's on a Tank of its side.
     */
    public static float modifyDamage(LivingEntity target, DamageSource source, float amount) {
        if (!(source.getAttacker() instanceof HirelingEntity hireling) || !hireling.isActive()
                || hireling.role() != HirelingRole.DAMAGE || !(target instanceof MobEntity mob) || amount <= 0) {
            return amount;
        }
        LivingEntity mobTarget = mob.getTarget();
        if (hireling.abilityCooldown() <= 0 && mobTarget != null && mobTarget != hireling && Conditions.canAct(hireling)) {
            float bonus = MARK_DAMAGE * hireling.devotedBonus();
            amount += bonus;
            hireling.setAbilityCooldown(MARK_COOLDOWN);
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, MARK_GLOW_TICKS, 0, false, false), hireling);
            DnDClasses.LOGGER.info("[Hireling] {} Hunter's Mark -> {} (+{}, it was on {})", hireling.firstName(),
                    mob.getName().getString(), bonus, nameOf(mobTarget));
        }
        if (mobTarget != null && isSideTank(hireling, mobTarget)) {
            float boosted = amount * PartySynergies.OPPORTUNIST_MULTIPLIER;
            DnDClasses.LOGGER.info("[Party] Opportunist: {} (hireling) hits {} (on {}) for {} instead of {}",
                    hireling.firstName(), mob.getName().getString(), nameOf(mobTarget), boosted, amount);
            amount = boosted;
        }
        return amount;
    }

    /** A Tank on the hireling's side: a Tank hireling of its side, or its owner or a party member with the Tank role. */
    private static boolean isSideTank(HirelingEntity hireling, LivingEntity entity) {
        if (entity instanceof HirelingEntity other) {
            return other != hireling && other.isActive() && other.role() == HirelingRole.TANK && sameSide(other, hireling);
        }
        return entity instanceof ServerPlayerEntity player && alliedWith(hireling, player) && !Downed.is(player)
                && !DungeonMaster.isDm(player) && PartySynergies.roleOf(player) == PartyRole.TANK;
    }

    // ---- Support: Rallying Song ----

    private static boolean rallyingSong(HirelingEntity bard) {
        List<LivingEntity> near = allies(bard, SONG_RANGE);
        boolean needed = false;
        for (LivingEntity ally : near) {
            if (ally.getHealth() / Math.max(1.0F, ally.getMaxHealth()) < HURT || Conditions.has(ally, Condition.FRIGHTENED)) {
                needed = true;
                break;
            }
        }
        if (!needed) {
            return false;
        }
        int ticks = Math.round(SONG_REGEN_TICKS * bard.devotedBonus());
        List<String> names = new ArrayList<>();
        for (LivingEntity ally : near) {
            ally.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, ticks, 0), bard);
            boolean calmed = Conditions.remove(ally, Condition.FRIGHTENED);
            names.add(nameOf(ally) + (calmed ? " (no longer frightened)" : ""));
            if (ally instanceof ServerPlayerEntity player) {
                player.sendMessage(Text.translatable("hireling.dndclasses.ability.song", bard.firstName())
                        .formatted(Formatting.LIGHT_PURPLE), true);
            }
        }
        bard.setAbilityCooldown(SONG_COOLDOWN);
        bard.swingHand(Hand.MAIN_HAND);
        ServerWorld world = (ServerWorld) bard.world;
        world.spawnParticles(ParticleTypes.NOTE, bard.getX(), bard.getBodyY(1.2), bard.getZ(), 8, 0.6D, 0.3D, 0.6D, 1.0D);
        world.playSound(null, bard.getBlockPos(), SoundEvents.BLOCK_NOTE_BLOCK_HARP.value(), SoundCategory.NEUTRAL, 1.0F, 1.2F);
        DnDClasses.LOGGER.info("[Hireling] {} Rallying Song -> {} (Regeneration I, {} ticks)", bard.firstName(),
                String.join(", ", names), ticks);
        return true;
    }

    // ---- Utility: Scout and Torchbearer ----

    private static void scout(HirelingEntity guide) {
        List<ServerPlayerEntity> people = sidePlayers(guide);
        if (people.isEmpty()) {
            return;
        }
        ServerWorld world = (ServerWorld) guide.world;
        for (Perceivable thing : PerceptionService.near(world, guide.getPos(), SCOUT_RANGE)) {
            if (thing.perceptionDc() > SCOUT_PASSIVE) {
                continue;
            }
            for (ServerPlayerEntity player : people) {
                if (!thing.hiddenFrom(player)) {
                    continue;
                }
                DnDClasses.LOGGER.info("[Perception] {} (hireling) spots {} for {} (passive {} vs DC {}) -> revealed",
                        guide.firstName(), PerceptionService.describe(thing), player.getEntityName(), SCOUT_PASSIVE,
                        thing.perceptionDc());
                // As if they'd found it themselves: traps get outlined, hidden doors revealed to the party.
                thing.perceive(player, true);
                if (player == guide.ownerPlayer()) {
                    player.sendMessage(Text.translatable("hireling.dndclasses.ability.scout", guide.firstName())
                            .formatted(Formatting.AQUA), false);
                }
            }
        }
    }

    private static void torchbearer(HirelingEntity guide) {
        Vec3d now = guide.getPos();
        Vec3d last = guide.lastTorchPos;
        guide.lastTorchPos = now;
        if (last == null || guide.isWaiting() || guide.ownerHere() == null || guide.torches() <= 0) {
            return;
        }
        double step = Math.sqrt(now.squaredDistanceTo(last.x, now.y, last.z));
        if (step > 4.0D) {
            return; // a teleport, not a walk
        }
        BlockPos at = guide.getBlockPos();
        if (guide.world.getLightLevel(at) > TORCH_LIGHT) {
            return;
        }
        guide.torchWalked += step;
        if (guide.torchWalked < TORCH_EVERY) {
            return;
        }
        BlockState torch = Blocks.TORCH.getDefaultState();
        if (!guide.world.getBlockState(at).isAir() || !torch.canPlaceAt(guide.world, at) || !guide.isOnGround()) {
            return; // try again next step
        }
        guide.world.setBlockState(at, torch);
        guide.world.playSound(null, at, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 0.8F, 1.0F);
        guide.swingHand(Hand.OFF_HAND);
        guide.setTorches(guide.torches() - 1);
        guide.torchWalked = 0.0D;
        DnDClasses.LOGGER.info("[Hireling] {} Torchbearer -> torch at {} ({} left)", guide.firstName(),
                at.toShortString(), guide.torches());
        if (guide.torches() == 0) {
            ServerPlayerEntity owner = guide.ownerPlayer();
            if (owner != null) {
                owner.sendMessage(Text.translatable("hireling.dndclasses.ability.torches_out", guide.firstName())
                        .formatted(Formatting.YELLOW), false);
            }
        }
    }

    /** The owner right-clicks the Guide with torches: they go into its stock, up to {@value #TORCH_STOCK}. */
    public static boolean refillTorches(ServerPlayerEntity player, HirelingEntity guide, ItemStack held) {
        if (guide.role() != HirelingRole.UTILITY || !held.isOf(Items.TORCH)) {
            return false;
        }
        int take = Math.min(held.getCount(), TORCH_STOCK - guide.torches());
        if (take <= 0) {
            player.sendMessage(Text.translatable("hireling.dndclasses.ability.torches_full", guide.firstName(), TORCH_STOCK)
                    .formatted(Formatting.YELLOW), true);
            return true;
        }
        if (!player.getAbilities().creativeMode) {
            held.decrement(take);
        }
        guide.setTorches(guide.torches() + take);
        player.sendMessage(Text.translatable("hireling.dndclasses.ability.torches", guide.firstName(), guide.torches())
                .formatted(Formatting.GREEN), true);
        DnDClasses.LOGGER.info("[Hireling] {} takes {} torches ({} now)", guide.firstName(), take, guide.torches());
        return true;
    }
}

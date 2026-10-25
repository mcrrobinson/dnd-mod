package mattonfire.dnd.magic.items;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.WarlockRules;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.AbilityScores;
import mattonfire.dnd.classes.Abilities.RollKind;
import mattonfire.dnd.classes.Damages.ModDamageTypes;
import mattonfire.dnd.classes.Downed.Downed;
import mattonfire.dnd.classes.Downed.DownedEvents;
import mattonfire.dnd.classes.Registry.ModEffects;
import mattonfire.dnd.classes.Registry.ModItems;
import mattonfire.dnd.classes.Rest.RestEvents;
import mattonfire.dnd.classes.Rest.RestKind;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.magic.Curse;
import mattonfire.dnd.magic.MagicEffect;
import mattonfire.dnd.magic.MagicEffects;
import mattonfire.dnd.magic.MagicItemDef;
import mattonfire.dnd.magic.MagicItems;
import mattonfire.dnd.magic.MagicKind;
import mattonfire.dnd.magic.MagicTier;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * The boss and monster magic items (design 5, section 3.7, items 10-18 and 20): their tiers, passive effects and
 * the hooks they need. Active items (Warhorn, Eye, Locket) are their own classes in this package; the drops are in
 * {@link BossItemDrops}.
 */
public final class BossItems {
    // Heart of the Furnace
    public static final int HEART_IGNITE_SECONDS = 3;
    public static final int HEART_WET_INTERVAL_SECONDS = 2;
    public static final float HEART_WET_DAMAGE = 1.0F;
    /** Like the Warlock's: water never takes you below this. */
    public static final float HEART_WET_MIN_HEALTH = 2.0F;
    // Wyvern-Scale Cloak
    public static final float CLOAK_FALL_MULTIPLIER = 0.5F;
    // Belt of Owlbear Might
    public static final double BELT_ATTACK_DAMAGE = 2.0;
    public static final double BELT_KNOCKBACK_RESISTANCE = 0.3;
    public static final int BELT_STR = 21;
    // Mace of Disruption
    public static final float MACE_UNDEAD_BONUS = 8.0F;
    public static final float MACE_DISRUPT_HEALTH = 25.0F;
    public static final int MACE_SAVE_DC = 15;
    public static final int MACE_SAVE_BONUS = 2;
    // Frostbrand
    public static final int FROSTBRAND_SLOW_TICKS = 60;
    public static final int FROSTBRAND_SLOW_AMPLIFIER = 1;
    public static final float FROSTBRAND_FREEZE_CHANCE = 0.15F;
    public static final int FROSTBRAND_FREEZE_TICKS = 40;
    // The Burglar's Ring
    /** Mobs further than this lose track of an invisible Ring wearer. */
    public static final double RING_NOTICE_RANGE = 4.0;
    private static final double RING_FORGET_SCAN = 32.0;

    private static final UUID BELT_DAMAGE_ID = UUID.fromString("0f3c2a1e-7d4b-4e55-9b0a-6c1d2e3f4c01");
    private static final UUID BELT_KNOCKBACK_ID = UUID.fromString("0f3c2a1e-7d4b-4e55-9b0a-6c1d2e3f4c02");

    /** Seconds since the Heart's last water damage, per player. */
    private static final Map<UUID, Integer> HEART_WET = new HashMap<>();
    /** Where a sneaking Ring wearer stood a second ago. */
    private static final Map<UUID, Vec3d> RING_STILL = new HashMap<>();
    /** Ring wearers who are hidden now; true if the Ring gave them their Invisibility (so it takes it back). */
    private static final Map<UUID, Boolean> RING_HIDDEN = new HashMap<>();

    private BossItems() {
    }

    public static void register() {
        registerDefs();
        registerEffects();
        registerHooks();
        DrowItems.register();
        BossItemDrops.register();
    }

    private static void registerDefs() {
        MagicItems.register(MagicItemDef.of(ModItems.WARLORDS_WARHORN, MagicTier.RARE, MagicKind.WONDROUS, "horn"));
        MagicItems.register(MagicItemDef.of(ModItems.HEART_OF_THE_FURNACE, MagicTier.RARE, MagicKind.WONDROUS,
                "amulet").withAttunement());
        MagicItems.register(MagicItemDef.of(ModItems.STORM_CHASER_BOOTS, MagicTier.RARE, MagicKind.ARMOR, "boots")
                .withAttunement());
        MagicItems.register(MagicItemDef.of(ModItems.WYVERN_SCALE_CLOAK, MagicTier.RARE, MagicKind.WONDROUS, "cloak")
                .withAttunement());
        MagicItems.register(MagicItemDef.of(ModItems.BELT_OF_OWLBEAR_MIGHT, MagicTier.RARE, MagicKind.WONDROUS,
                "belt").withAttunement());
        MagicItems.register(MagicItemDef.of(ModItems.MACE_OF_DISRUPTION, MagicTier.RARE, MagicKind.WEAPON, "mace")
                .withAttunement().onlyFor(DndCharacter.CLERIC, DndCharacter.PALADIN));
        MagicItems.register(MagicItemDef.of(ModItems.FROSTBRAND, MagicTier.VERY_RARE, MagicKind.WEAPON, "sword")
                .withAttunement());
        MagicItems.register(MagicItemDef.of(ModItems.PHYLACTERY_LOCKET, MagicTier.VERY_RARE, MagicKind.WONDROUS,
                "amulet").withAttunement().onlyFor(DndCharacter.WIZARD, DndCharacter.NECROMANCER,
                        DndCharacter.WARLOCK));
        MagicItems.register(MagicItemDef.of(ModItems.BURGLARS_RING, MagicTier.VERY_RARE, MagicKind.RING, "ring")
                .withAttunement().withCurse(Curse.RINGS_CALL));
        MagicItems.register(MagicItemDef.of(ModItems.EYE_OF_THE_TYRANT, MagicTier.LEGENDARY, MagicKind.WONDROUS,
                "wondrous_item").withAttunement());
        MagicItems.register(MagicItemDef.of(ModItems.GORGONS_GAZE, MagicTier.RARE, MagicKind.WONDROUS,
                "wondrous_item").withAttunement());
    }

    private static boolean melee(PlayerEntity player, DamageSource source) {
        return source.getSource() == player && source.isOf(DamageTypes.PLAYER_ATTACK);
    }

    private static void registerEffects() {
        MagicEffects.register(ModItems.HEART_OF_THE_FURNACE, new MagicEffect() {
            @Override
            public float modifyDealtDamage(PlayerEntity player, ItemStack stack, LivingEntity target,
                    DamageSource source, float amount) {
                if (melee(player, source))
                    target.setOnFireFor(HEART_IGNITE_SECONDS);
                return amount;
            }

            @Override
            public void secondTick(ServerPlayerEntity player, ItemStack stack) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 40, 0, true, false,
                        true));
                heartWet(player);
            }
        });
        MagicEffects.register(ModItems.STORM_CHASER_BOOTS, new MagicEffect() {
            @Override
            public void secondTick(ServerPlayerEntity player, ItemStack stack) {
                boolean storm = player.getWorld().isRaining() || player.getWorld().isThundering();
                StatusEffectInstance speed = player.getStatusEffect(StatusEffects.SPEED);
                int amplifier = storm ? 1 : 0;
                // Don't trample a stronger or longer Speed from elsewhere (a potion, the Warhorn)
                if (speed == null || (speed.isAmbient() && speed.getDuration() <= 40)
                        || speed.getAmplifier() < amplifier) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, amplifier, true, false,
                            true));
                }
            }
        });
        MagicEffects.register(ModItems.WYVERN_SCALE_CLOAK, new MagicEffect() {
            @Override
            public float modifyTakenDamage(PlayerEntity player, ItemStack stack, DamageSource source, float amount) {
                return source.isIn(DamageTypeTags.IS_FALL) ? amount * CLOAK_FALL_MULTIPLIER : amount;
            }
        });
        List<MagicEffect.Bonus> belt = List.of(
                new MagicEffect.Bonus(EntityAttributes.GENERIC_ATTACK_DAMAGE, BELT_DAMAGE_ID, BELT_ATTACK_DAMAGE,
                        EntityAttributeModifier.Operation.ADDITION),
                new MagicEffect.Bonus(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, BELT_KNOCKBACK_ID,
                        BELT_KNOCKBACK_RESISTANCE, EntityAttributeModifier.Operation.ADDITION));
        MagicEffects.register(ModItems.BELT_OF_OWLBEAR_MIGHT, new MagicEffect() {
            @Override
            public List<Bonus> attributeBonuses() {
                return belt;
            }
        });
        MagicEffects.register(ModItems.MACE_OF_DISRUPTION, new MagicEffect() {
            @Override
            public float modifyDealtDamage(PlayerEntity player, ItemStack stack, LivingEntity target,
                    DamageSource source, float amount) {
                if (!melee(player, source) || target.getGroup() != EntityGroup.UNDEAD)
                    return amount;
                amount += MACE_UNDEAD_BONUS;
                if (target.getHealth() <= MACE_DISRUPT_HEALTH && disrupt(player, target))
                    amount = Math.max(amount, (target.getHealth() + target.getAbsorptionAmount()) * 10.0F + 100.0F);
                return amount;
            }
        });
        MagicEffects.register(ModItems.FROSTBRAND, new MagicEffect() {
            @Override
            public float modifyDealtDamage(PlayerEntity player, ItemStack stack, LivingEntity target,
                    DamageSource source, float amount) {
                if (!melee(player, source))
                    return amount;
                target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, FROSTBRAND_SLOW_TICKS,
                        FROSTBRAND_SLOW_AMPLIFIER), player);
                if (player.getRandom().nextFloat() < FROSTBRAND_FREEZE_CHANCE) {
                    target.addStatusEffect(new StatusEffectInstance(ModEffects.FREEZE, FROSTBRAND_FREEZE_TICKS, 0),
                            player);
                    target.getWorld().playSound(null, target.getBlockPos(), SoundEvents.BLOCK_GLASS_BREAK,
                            SoundCategory.PLAYERS, 0.8F, 1.6F);
                    DnDClasses.LOGGER.info("[Magic] {}'s Frostbrand froze {}", player.getEntityName(),
                            target.getEntityName());
                }
                if (target.getWorld() instanceof ServerWorld world) {
                    world.spawnParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getBodyY(0.6), target.getZ(),
                            8, 0.3, 0.4, 0.3, 0.02);
                }
                return amount;
            }
        });
        // No passive effect, but registered so has() finds them while active
        MagicEffects.register(ModItems.PHYLACTERY_LOCKET, new MagicEffect() {
        });
        MagicEffects.register(ModItems.EYE_OF_THE_TYRANT, new MagicEffect() {
        });
        MagicEffects.register(ModItems.GORGONS_GAZE, new MagicEffect() {
        });
        MagicEffects.register(ModItems.BURGLARS_RING, new MagicEffect() {
            @Override
            public void secondTick(ServerPlayerEntity player, ItemStack stack) {
                ringStillness(player);
            }
        });
    }

    private static void registerHooks() {
        WarlordsWarhornItem.registerCooldownMessage();

        // Phylactery Locket: before the Downed rules (which would down or kill you), like a totem
        Identifier locketPhase = new Identifier(DnDClasses.MOD_ID, "phylactery_locket");
        ServerLivingEntityEvents.ALLOW_DEATH.addPhaseOrdering(locketPhase, Event.DEFAULT_PHASE);
        ServerLivingEntityEvents.ALLOW_DEATH.register(locketPhase, (entity, source, amount) -> !(entity
                instanceof ServerPlayerEntity player) || !locketSaves(player, source, amount));

        // Storm-Chaser Boots: real lightning bolts too (dragon storms go through DragonSaves.isUnaffected)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player && source.isOf(DamageTypes.LIGHTNING_BOLT)
                    && WondrousItems.has(player, ModItems.STORM_CHASER_BOOTS)) {
                player.extinguish();
                return false;
            }
            return true;
        });

        // Belt of Owlbear Might: Strength 21
        AbilityScores.register(new Identifier(DnDClasses.MOD_ID, "magic/boss_items"), (player, c) -> {
            if (WondrousItems.has(player, ModItems.BELT_OF_OWLBEAR_MIGHT))
                c.setAtLeast(Ability.STR, BELT_STR, "Belt of Owlbear Might");
        }, true);

        // Long rest: the Eye regains a charge and the Locket recharges
        RestEvents.AFTER_REST.register((player, kind, source) -> {
            if (kind != RestKind.LONG)
                return;
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.isOf(ModItems.EYE_OF_THE_TYRANT))
                    EyeOfTheTyrantItem.recharge(stack);
                else if (stack.isOf(ModItems.PHYLACTERY_LOCKET))
                    PhylacteryLocketItem.recharge(stack);
                else if (stack.isOf(ModItems.GORGONS_GAZE))
                    GorgonsGazeItem.recharge(stack);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(BossItems::tick);
        // Attacking breaks the Ring's invisibility (before the hit lands)
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity server)
                reveal(server);
            return ActionResult.PASS;
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (source.getAttacker() instanceof ServerPlayerEntity player && player != entity)
                reveal(player); // arrows and spells count as attacking too
            return true;
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID id = handler.player.getUuid();
            HEART_WET.remove(id);
            RING_STILL.remove(id);
            reveal(handler.player);
        });
    }

    // ---- Immunities ----

    /**
     * Damage a dragon attack can't do to this creature because of its items: the Storm-Chaser Boots' lightning
     * immunity and Frostbrand's immunity to the Frost Drake's breath. Called from {@code DragonSaves.isUnaffected}.
     */
    public static boolean immune(LivingEntity victim, DamageSource source) {
        if (!(victim instanceof PlayerEntity player) || player.getWorld().isClient)
            return false;
        if (source.isOf(DamageTypes.LIGHTNING_BOLT))
            return WondrousItems.has(player, ModItems.STORM_CHASER_BOOTS);
        if (source.isOf(ModDamageTypes.FROST_BREATH))
            return WondrousItems.has(player, ModItems.FROSTBRAND);
        return false;
    }

    /** Status effects an item makes its bearer immune to: Freeze (Frostbrand), anti-magic (Eye of the Tyrant). */
    public static boolean blocksEffect(LivingEntity entity, StatusEffect effect) {
        if (!(entity instanceof ServerPlayerEntity player))
            return false;
        if (effect == ModEffects.FREEZE)
            return WondrousItems.has(player, ModItems.FROSTBRAND);
        if (effect == ModEffects.ANTI_MAGIC)
            return WondrousItems.has(player, ModItems.EYE_OF_THE_TYRANT);
        return false;
    }

    // ---- Heart of the Furnace ----

    /** Water and rain hurt the Heart's bearer: 1 damage every 2 s, never below 2 HP (Warlocks already suffer). */
    private static void heartWet(ServerPlayerEntity player) {
        if (!player.isTouchingWaterOrRain() || player.isCreative() || player.isSpectator()
                || WarlockRules.isWarlock(player)) {
            HEART_WET.remove(player.getUuid());
            return;
        }
        int seconds = HEART_WET.merge(player.getUuid(), 1, Integer::sum);
        if (seconds < HEART_WET_INTERVAL_SECONDS)
            return;
        HEART_WET.put(player.getUuid(), 0);
        if (player.getHealth() > HEART_WET_MIN_HEALTH) {
            player.damage(ModDamageTypes.of(player.getWorld(), ModDamageTypes.WARLOCK_WET_DAMAGE_SOURCE),
                    Math.min(HEART_WET_DAMAGE, player.getHealth() - HEART_WET_MIN_HEALTH));
        }
    }

    // ---- Mace of Disruption ----

    /** An undead at 25 HP or less saves (DC 15, d20 + 2) or crumbles to ash. True if it crumbles. */
    private static boolean disrupt(PlayerEntity player, LivingEntity undead) {
        D20.Roll roll = D20.roll(undead).label(Text.translatable("item.dndclasses.mace_of_disruption.save"))
                .ability(Ability.WIS).kind(RollKind.SAVE).modifier(MACE_SAVE_BONUS).dc(MACE_SAVE_DC).roll();
        boolean crumbles = !roll.outcome().succeeded();
        D20.show(player, roll, crumbles
                ? Text.translatable("item.dndclasses.mace_of_disruption.crumbles", undead.getDisplayName())
                : Text.translatable("item.dndclasses.mace_of_disruption.resists", undead.getDisplayName()));
        if (crumbles && undead.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.ASH, undead.getX(), undead.getBodyY(0.5), undead.getZ(), 60, 0.4,
                    0.6, 0.4, 0.05);
            world.spawnParticles(ParticleTypes.END_ROD, undead.getX(), undead.getBodyY(0.5), undead.getZ(), 12, 0.3,
                    0.5, 0.3, 0.05);
            world.playSound(null, undead.getBlockPos(), SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.PLAYERS,
                    0.8F, 1.6F);
            DnDClasses.LOGGER.info("[Magic] {}'s Mace of Disruption turned {} to ash", player.getEntityName(),
                    undead.getEntityName());
        }
        return crumbles;
    }

    // ---- Phylactery Locket ----

    private static boolean locketSaves(ServerPlayerEntity player, DamageSource source, float amount) {
        if (player.isCreative() || player.isSpectator() || Downed.is(player) || DownedEvents.bypasses(source, amount))
            return false;
        // A held totem goes first, as it does for the Downed rules
        if (player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING) || player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING))
            return false;
        ItemStack locket = activeStack(player, ModItems.PHYLACTERY_LOCKET);
        if (locket == null || !PhylacteryLocketItem.isCharged(locket, player.getWorld()))
            return false;
        PhylacteryLocketItem.save(player, locket);
        return true;
    }

    private static @Nullable ItemStack activeStack(PlayerEntity player, Item item) {
        for (MagicEffects.Active active : MagicEffects.active(player)) {
            if (active.stack().isOf(item))
                return active.stack();
        }
        return null;
    }

    // ---- Per tick: the Cloak's glide and the Ring's invisibility ----

    private static void tick(MinecraftServer server) {
        boolean second = server.getTicks() % 20 == 0;
        boolean scan = server.getTicks() % 5 == 0;
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.isSneaking() && !player.isOnGround() && player.getVelocity().y < -0.1
                    && !player.getAbilities().flying && !player.isFallFlying() && !player.isTouchingWater()
                    && !player.hasStatusEffect(StatusEffects.SLOW_FALLING)
                    && WondrousItems.has(player, ModItems.WYVERN_SCALE_CLOAK)) {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 10, 0, true, false, false));
            }
            if (!RING_HIDDEN.containsKey(player.getUuid()))
                continue;
            if (!player.isSneaking() || !player.isAlive() || player.isSpectator()
                    || (second && !WondrousItems.has(player, ModItems.BURGLARS_RING))) {
                reveal(player);
            } else if (scan) {
                loseTrack(player);
            }
        }
    }

    /** Sneaking without moving for a second turns the Ring's wearer invisible. */
    private static void ringStillness(ServerPlayerEntity player) {
        if (!player.isSneaking() || RING_HIDDEN.containsKey(player.getUuid())) {
            RING_STILL.remove(player.getUuid());
            return;
        }
        Vec3d last = RING_STILL.put(player.getUuid(), player.getPos());
        if (last != null && last.squaredDistanceTo(player.getPos()) < 0.01)
            hide(player);
    }

    private static void hide(ServerPlayerEntity player) {
        RING_STILL.remove(player.getUuid());
        boolean added = !player.hasStatusEffect(StatusEffects.INVISIBILITY);
        if (added) {
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, StatusEffectInstance.INFINITE,
                    0, false, false, true));
        }
        RING_HIDDEN.put(player.getUuid(), added);
        player.sendMessage(Text.translatable("item.dndclasses.burglars_ring.hidden").formatted(Formatting.DARK_GRAY),
                true);
        loseTrack(player);
    }

    private static void reveal(ServerPlayerEntity player) {
        Boolean added = RING_HIDDEN.remove(player.getUuid());
        if (added == null)
            return;
        if (added)
            player.removeStatusEffect(StatusEffects.INVISIBILITY);
        player.sendMessage(Text.translatable("item.dndclasses.burglars_ring.revealed").formatted(Formatting.GRAY),
                true);
    }

    /** Mobs chasing a hidden Ring wearer from more than 4 blocks away lose them. */
    private static void loseTrack(ServerPlayerEntity player) {
        for (MobEntity mob : player.getWorld().getEntitiesByClass(MobEntity.class,
                player.getBoundingBox().expand(RING_FORGET_SCAN), m -> m.getTarget() == player)) {
            if (mob.squaredDistanceTo(player) > RING_NOTICE_RANGE * RING_NOTICE_RANGE) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
        }
    }
}

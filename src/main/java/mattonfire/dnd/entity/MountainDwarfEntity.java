package mattonfire.dnd.entity;

import java.util.List;
import java.util.UUID;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.faction.ReputationTier;
import mattonfire.dnd.faction.TierEffects;
import mattonfire.dnd.world.gen.HomeBonuses;
import mattonfire.dnd.world.gen.RacialHomes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.brain.task.LookTargetUtil;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.GoToWalkTargetGoal;
import net.minecraft.entity.ai.goal.LongDoorInteractGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A stout, bearded, armoured dwarf from the mountain fortresses. Neutral, like an iron golem:
 * it leaves players alone, hunts monsters near its home, and if a player hits it (or one of its
 * kin nearby) it and its kin fight back with their axes. Hand one a gold ingot and it trades
 * something from the mountain's depths for it. What angers them is in {@link SettlementGrudges}.
 */
public class MountainDwarfEntity extends PathAwareEntity implements GuardAnger {
    public static final int VARIANTS = 6;
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(MountainDwarfEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public static final Identifier BARTER_LOOT = new Identifier(DnDClasses.MOD_ID, "gameplay/dwarf_barter");
    /** What dwarves barter to players they honour (Honored and Exalted). */
    public static final Identifier HONORED_BARTER_LOOT = new Identifier(DnDClasses.MOD_ID, "gameplay/dwarf_barter_honored");
    /** Kin stock: an extra roll for Dwarves only, usually nothing, sometimes Dwarven Plate or the Waraxe. */
    public static final Identifier KIN_STOCK_LOOT = new Identifier(DnDClasses.MOD_ID, "gameplay/dwarf_kin_stock");
    /** How long after hitting a dwarf a player still counts as its kin's enemy. */
    private static final int KIN_GRUDGE_TICKS = 20 * 10;
    private static final int HOME_RANGE = 32;
    private static final int BARTER_COOLDOWN = 40;

    private static final String[] FIRST_NAMES = {
            "Thorin", "Balin", "Dwalin", "Gimli", "Gloin", "Oin", "Bombur", "Bofur", "Bifur", "Dori", "Nori", "Ori",
            "Fili", "Kili", "Dain", "Thror", "Thrain", "Fundin", "Groin", "Nain", "Borin", "Farin", "Floi", "Frar",
            "Loni", "Nali", "Narvi", "Durin", "Telchar", "Hilda", "Brunhild", "Dagna", "Kathra", "Helga"
    };
    private static final String[] SURNAMES = {
            "Ironfoot", "Stonebeard", "Goldhand", "Deepdelver", "Anvilborn", "Hammerfell", "Coppervein", "Blackforge",
            "Granitebrow", "Firebeard", "Longbeard", "Stoneshield", "Runecarver", "Ambershard", "Oreheart",
            "Flintaxe", "Bronzebelly", "Deepmantle", "Gemcutter", "Ironbraid"
    };

    @Nullable
    private BlockPos home;
    private final GuardAnger.State anger = new GuardAnger.State();
    private int barterCooldown;
    /** Game time this dwarf was last saved (0 if unknown), read back on load for {@link FortressForgiveness}. */
    private long savedAt;
    private boolean checkForgiveness;

    public MountainDwarfEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        ((MobNavigation) this.getNavigation()).setCanPathThroughDoors(true);
        this.experiencePoints = 8;
    }

    public static DefaultAttributeContainer.Builder createMountainDwarfAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 32.0D)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0D)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.27D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.add(3, new LongDoorInteractGoal(this, true));
        this.goalSelector.add(4, new GoToWalkTargetGoal(this, 0.8D));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.6D));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(6, new LookAtEntityGoal(this, MountainDwarfEntity.class, 6.0F));
        this.goalSelector.add(7, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt));
        // Exalted with the dwarves: they fight at your side.
        this.targetSelector.add(2, new mattonfire.dnd.entity.ai.goal.DwarfFriendGoal(this));
        // Rust Monsters eat dwarven steel: every dwarf goes for one on sight.
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, RustMonsterEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, MobEntity.class, 5, true, false,
                entity -> entity instanceof Monster && !(entity instanceof CreeperEntity)
                        // A sleeping mimic passes for a chest, even to a dwarf.
                        && !(entity instanceof MimicEntity mimic && mimic.isDormant())));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(VARIANT, 0);
    }

    public int getVariant() {
        return this.dataTracker.get(VARIANT);
    }

    /** Ties the dwarf to the hall it guards. */
    public void setHome(BlockPos home) {
        this.home = home.toImmutable();
        this.setPositionTarget(this.home, HOME_RANGE);
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        Random random = world.getRandom();
        this.dataTracker.set(VARIANT, random.nextInt(VARIANTS));
        this.setCustomName(Text.literal(FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
                + SURNAMES[random.nextInt(SURNAMES.length)]));
        this.equip(random);
        this.setPersistent();
        this.setHome(this.getBlockPos());
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    private void equip(Random random) {
        float weapon = random.nextFloat();
        Item mainHand = weapon < 0.05F ? Items.NETHERITE_AXE
                : weapon < 0.15F ? Items.DIAMOND_AXE
                : weapon < 0.35F ? Items.GOLDEN_AXE
                : weapon < 0.5F ? Items.IRON_PICKAXE
                : Items.IRON_AXE;
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(mainHand));

        float helmet = random.nextFloat();
        if (helmet < 0.45F) {
            this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        } else if (helmet < 0.75F) {
            this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        } else if (helmet < 0.85F) {
            this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.CHAINMAIL_HELMET));
        }
        // The skins already wear mail; a few veterans have plate over it.
        if (random.nextFloat() < 0.2F) {
            this.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        }
    }

    /** Command tag on the fortress's king (faction files match it as {@code @dndclasses.role.dwarf_king}). */
    public static final String KING_TAG = "dndclasses.role.dwarf_king";

    /** Makes this dwarf the fortress's king: a crown, a netherite axe and twice the health. */
    public void crown() {
        String name = this.getCustomName() != null ? this.getCustomName().getString() : FIRST_NAMES[0];
        this.setCustomName(Text.literal("King " + name));
        this.addCommandTag(KING_TAG);
        this.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET));
        this.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_AXE));
        this.setEquipmentDropChance(EquipmentSlot.HEAD, 1.0F);
        this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(64.0D);
        this.setHealth(this.getMaxHealth());
    }

    /** The dwarf's growl as it turns on a player meddling with the fortress (see {@link SettlementGrudges}). */
    @Override
    public void provokeShout() {
        this.playSound(SoundEvents.ENTITY_VINDICATOR_AMBIENT, 1.0F, this.getSoundPitch());
    }

    /**
     * Dwarves who can see {@code player} watched them break dwarven work at {@code pos} (a smashed Clockwork
     * Door): it's judged like breaking a protected block of the fortress ({@link SettlementGrudges#witness}),
     * so they turn on the player (a Dwarf gets one warning growl a day) and the player's standing drops.
     */
    public static void witness(PlayerEntity player, BlockPos pos) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        SettlementGrudges.witness(SettlementGrudges.MOUNTAIN_DWARVES, player, pos, SettlementGrudges.Offence.BREAK);
    }

    // The mod gives every mob a player-targeting goal; dwarves only turn on players who wronged them.
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target instanceof PlayerEntity player && !this.hasGrudgeAgainst(player)) {
            return;
        }
        super.setTarget(target);
    }

    /** Whether this dwarf holds a grudge against {@code player} right now (also read by an Insight Study). */
    public boolean hasGrudgeAgainst(PlayerEntity player) {
        if (player == this.getAttacker() || this.shouldAngerAt(player)) {
            return true;
        }
        // Group revenge: a player who just struck one of us.
        return player.getAttacking() instanceof MountainDwarfEntity
                && player.age - player.getLastAttackTime() < KIN_GRUDGE_TICKS;
    }

    /** Angry at the player it holds a grudge against, and at anyone Hostile with the dwarves, on sight. */
    @Override
    public boolean shouldAngerAt(LivingEntity entity) {
        return GuardAnger.super.shouldAngerAt(entity) || SettlementGuard.attacksOnSight(this, entity);
    }

    @Override
    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.barterCooldown > 0) {
            this.barterCooldown--;
        }
        if (!this.world.isClient) {
            if (this.checkForgiveness) {
                this.checkForgiveness = false;
                this.applyForgiveness((ServerWorld) this.world);
            }
            this.tickAngerLogic((ServerWorld) this.world, true);
        }
    }

    /**
     * Just loaded: drop a grudge held when this dwarf was saved against a player its fortress has forgiven
     * since (a Dwarven Ruin cleared while the fortress was unloaded).
     */
    private void applyForgiveness(ServerWorld world) {
        UUID grudge = this.getAngryAt();
        if (grudge != null && FortressForgiveness.get(world).forgivenSince(this.getBlockPos(), grudge, this.savedAt)) {
            this.stopAnger();
            DnDClasses.LOGGER.info("[Grudges] {} at {} forgives {} (its fortress was told while it was unloaded)",
                    this.getName().getString(), this.getBlockPos().toShortString(), grudge);
        }
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (hand == Hand.MAIN_HAND && stack.isEmpty() && this.getCommandTags().contains(KING_TAG)
                && RacialHomes.DWARVEN_FORTRESS.isHomeOf(RaceLifecycle.activeRaceOf(player))) {
            if (!this.world.isClient) {
                this.getLookControl().lookAt(player);
                HomeBonuses.kingAudience((ServerPlayerEntity) player, this);
            }
            return ActionResult.success(this.world.isClient);
        }
        // Exalted with the dwarves: sneak + right-click holding an unidentified magic item, and they name it free.
        if (hand == Hand.MAIN_HAND && player.isSneaking() && mattonfire.dnd.magic.Identify.isUnidentified(stack)) {
            if (!this.world.isClient) {
                this.getLookControl().lookAt(player);
                if (this.getTarget() == null && TierEffects.dwarfFriend(TierEffects.tierWith(player, this))) {
                    mattonfire.dnd.magic.Identify.reveal((ServerPlayerEntity) player, stack, "dwarf");
                    this.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0F, this.getSoundPitch());
                } else {
                    player.sendMessage(Text.translatable("entity.dndclasses.mountain_dwarf.no_identify",
                            this.getDisplayName()).formatted(net.minecraft.util.Formatting.GRAY), true);
                    this.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, this.getSoundPitch());
                }
            }
            return ActionResult.success(this.world.isClient);
        }
        if (!stack.isOf(Items.GOLD_INGOT)) {
            return super.interactMob(player, hand);
        }
        if (this.world.isClient) {
            return ActionResult.SUCCESS;
        }
        this.getLookControl().lookAt(player);
        if (this.getTarget() != null || this.shouldAngerAt(player) || this.barterCooldown > 0) {
            this.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, this.getSoundPitch());
            return ActionResult.CONSUME;
        }
        ReputationTier tier = TierEffects.tierWith(player, this);
        this.barterCooldown = BARTER_COOLDOWN;
        if (TierEffects.refusesBarter(tier, this.random)) {
            // Unfriendly: a shake of the head, and the gold stays with the player.
            player.sendMessage(Text.translatable("entity.dndclasses.mountain_dwarf.refuses", this.getDisplayName())
                    .formatted(net.minecraft.util.Formatting.RED), true);
            this.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, this.getSoundPitch());
            return ActionResult.CONSUME;
        }
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        this.barter(player, tier);
        if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            mattonfire.dnd.faction.FactionEvents.traded(serverPlayer, this);
        }
        return ActionResult.CONSUME;
    }

    private void barter(PlayerEntity player, ReputationTier tier) {
        ServerWorld world = (ServerWorld) this.world;
        Identifier tableId = TierEffects.honoredBarter(tier) ? HONORED_BARTER_LOOT : BARTER_LOOT;
        LootTable table = world.getServer().getLootManager().getTable(tableId);
        // Kin (Dwarves) get the rarer of two rolls.
        List<ItemStack> loot = new java.util.ArrayList<>(
                KinPrices.barter(world, player, this, tableId, () -> this.rollBarter(world, table)));
        // Friends of the mountain sometimes get a second helping.
        if (this.random.nextFloat() < TierEffects.secondBarterRollChance(tier)) {
            loot.addAll(this.rollBarter(world, table));
        }
        // Kin stock (Dwarves): the racial gear.
        if (KinStock.isKin(player, this)) {
            loot.addAll(this.rollBarter(world, world.getServer().getLootManager().getTable(KIN_STOCK_LOOT)));
        }
        this.swingHand(Hand.OFF_HAND);
        for (ItemStack stack : loot) {
            LookTargetUtil.give(this, stack, player.getPos().add(0.0D, 1.0D, 0.0D));
        }
        this.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0F, this.getSoundPitch());
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getEyeY() + 0.3D, this.getZ(),
                5, 0.3D, 0.2D, 0.3D, 0.0D);
    }

    private List<ItemStack> rollBarter(ServerWorld world, LootTable table) {
        return table.generateLoot(new LootContext.Builder(world)
                .parameter(LootContextParameters.THIS_ENTITY, this)
                .random(this.random)
                .build(LootContextTypes.BARTER));
    }

    // ---- Angerable ----

    @Override
    public GuardAnger.State angerState() {
        return this.anger;
    }

    // ---- Sounds: a villager, but deeper and gruffer ----

    @Override
    public float getSoundPitch() {
        return super.getSoundPitch() * 0.7F;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return this.getTarget() != null ? SoundEvents.ENTITY_VINDICATOR_AMBIENT : SoundEvents.ENTITY_VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_VILLAGER_DEATH;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("Variant", this.getVariant());
        this.writeAngerToNbt(nbt);
        nbt.putLong("DndSavedAt", this.world.getTime());
        if (this.home != null) {
            nbt.put("Home", NbtHelper.fromBlockPos(this.home));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.dataTracker.set(VARIANT, Math.floorMod(nbt.getInt("Variant"), VARIANTS));
        this.readAngerFromNbt(this.world, nbt);
        this.savedAt = nbt.getLong("DndSavedAt");
        this.checkForgiveness = true;
        if (nbt.contains("Home")) {
            this.setHome(NbtHelper.toBlockPos(nbt.getCompound("Home")));
        }
        // Kings crowned before the tag existed.
        if (this.getCustomName() != null && this.getCustomName().getString().startsWith("King ")
                && this.getEquippedStack(EquipmentSlot.HEAD).isOf(Items.GOLDEN_HELMET)) {
            this.addCommandTag(KING_TAG);
        }
    }
}

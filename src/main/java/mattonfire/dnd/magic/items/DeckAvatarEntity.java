package mattonfire.dnd.magic.items;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.DnDClasses;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.WitherSkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.DyeableItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The Deck of Many Things' Skull: an Avatar of Death, a black-robed wither skeleton with a scythe. It hunts only
 * the player who drew it and ignores damage from anyone else; the first time someone else hits it, a second
 * Avatar rises (5e). It goes when its quarry dies or leaves, or after {@value #LIFE_TICKS} ticks, and drops
 * nothing.
 */
public class DeckAvatarEntity extends WitherSkeletonEntity {
    public static final double HEALTH = 60.0;
    public static final double DAMAGE = 8.0;
    public static final double SPEED = 0.3;
    public static final int LIFE_TICKS = 6000;
    public static final int ROBE_COLOR = 0x16121C;

    @Nullable
    private UUID quarry;
    private boolean second;
    private boolean calledSecond;
    private int lifeTicks;

    public DeckAvatarEntity(EntityType<? extends WitherSkeletonEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 0;
        this.setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAvatarAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, HEALTH)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, DAMAGE)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5);
    }

    /** An Avatar rises 3 blocks from the player (a second one when {@code first} was struck by someone else). */
    public static @Nullable DeckAvatarEntity rise(ServerPlayerEntity player, @Nullable DeckAvatarEntity first) {
        ServerWorld world = player.getWorld();
        DeckAvatarEntity avatar = PlanarItems.DECK_AVATAR.create(world);
        if (avatar == null)
            return null;
        Vec3d look = player.getRotationVector().multiply(1, 0, 1);
        if (look.lengthSquared() < 1.0E-4)
            look = new Vec3d(0, 0, 1);
        Vec3d at = first != null ? first.getPos().add(1.5, 0, 0) : player.getPos().add(look.normalize().multiply(3));
        avatar.refreshPositionAndAngles(at.x, player.getY(), at.z, player.getYaw() + 180.0F, 0.0F);
        avatar.quarry = player.getUuid();
        avatar.second = first != null;
        avatar.dress();
        avatar.setCustomName(Text.translatable("entity.dndclasses.deck_avatar.named", player.getDisplayName()));
        world.spawnEntity(avatar);
        avatar.setTarget(player);
        world.spawnParticles(ParticleTypes.SOUL, at.x, player.getY() + 1.0, at.z, 40, 0.4, 1.0, 0.4, 0.05);
        world.playSound(null, avatar.getBlockPos(), SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.HOSTILE, 0.6F, 1.4F);
        player.sendMessage(Text.translatable(first != null ? "deck.dndclasses.skull.second" : "deck.dndclasses.skull.done")
                .formatted(Formatting.DARK_RED), false);
        DnDClasses.LOGGER.info("[Deck] An Avatar of Death rises for {}{}", player.getEntityName(),
                first != null ? " (the second)" : "");
        return avatar;
    }

    private void dress() {
        ItemStack robe = new ItemStack(Items.LEATHER_CHESTPLATE);
        ((DyeableItem) Items.LEATHER_CHESTPLATE).setColor(robe, ROBE_COLOR);
        ItemStack skirt = new ItemStack(Items.LEATHER_LEGGINGS);
        ((DyeableItem) Items.LEATHER_LEGGINGS).setColor(skirt, ROBE_COLOR);
        ItemStack hood = new ItemStack(Items.LEATHER_HELMET);
        ((DyeableItem) Items.LEATHER_HELMET).setColor(hood, ROBE_COLOR);
        this.equipStack(EquipmentSlot.HEAD, hood);
        this.equipStack(EquipmentSlot.CHEST, robe);
        this.equipStack(EquipmentSlot.LEGS, skirt);
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_HOE));
        for (EquipmentSlot slot : EquipmentSlot.values())
            this.setEquipmentDropChance(slot, 0.0F);
    }

    /** Only movement goals: its one target is set every tick, and it never answers anyone else's blows. */
    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(6, new LookAroundGoal(this));
    }

    public @Nullable UUID quarry() {
        return quarry;
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (++lifeTicks > LIFE_TICKS) {
            vanish("time");
            return;
        }
        PlayerEntity target = quarry == null ? null : this.getWorld().getPlayerByUuid(quarry);
        if (target == null || !target.isAlive() || target.isSpectator()) {
            vanish(target == null ? "quarry gone" : "quarry died");
            return;
        }
        if (this.getTarget() != target)
            this.setTarget(target);
    }

    private void vanish(String why) {
        if (this.getWorld() instanceof ServerWorld world)
            world.spawnParticles(ParticleTypes.SMOKE, getX(), getBodyY(0.5), getZ(), 30, 0.3, 0.8, 0.3, 0.02);
        DnDClasses.LOGGER.info("[Deck] An Avatar of Death fades ({})", why);
        this.discard();
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY))
            return super.damage(source, amount);
        Entity attacker = source.getAttacker();
        if (attacker != null && quarry != null && quarry.equals(attacker.getUuid()))
            return super.damage(source, amount);
        if (!this.getWorld().isClient && attacker != null) {
            DnDClasses.LOGGER.info("[Deck] The Avatar of Death ignores {}'s blow", attacker.getName().getString());
            if (!second && !calledSecond && quarry != null
                    && this.getWorld().getPlayerByUuid(quarry) instanceof ServerPlayerEntity player) {
                calledSecond = true;
                rise(player, this);
            }
            this.getWorld().playSound(null, getBlockPos(), SoundEvents.ENTITY_WITHER_SKELETON_AMBIENT,
                    SoundCategory.HOSTILE, 1.0F, 0.5F);
        }
        return false;
    }

    @Override
    public void setTarget(@Nullable net.minecraft.entity.LivingEntity target) {
        if (target != null && (quarry == null || !quarry.equals(target.getUuid())))
            return;
        super.setTarget(target);
    }

    @Override
    protected Identifier getLootTableId() {
        return LootTables.EMPTY;
    }

    @Override
    protected void dropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops) {
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (quarry != null)
            nbt.putUuid("Quarry", quarry);
        nbt.putBoolean("Second", second);
        nbt.putBoolean("CalledSecond", calledSecond);
        nbt.putInt("LifeTicks", lifeTicks);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        quarry = nbt.containsUuid("Quarry") ? nbt.getUuid("Quarry") : null;
        second = nbt.getBoolean("Second");
        calledSecond = nbt.getBoolean("CalledSecond");
        lifeTicks = nbt.getInt("LifeTicks");
    }
}

package mattonfire.dnd.entity;

import mattonfire.dnd.faction.TierEffects;
import mattonfire.dnd.world.gen.HomeBonuses;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A peaceful halfling from the hobbit villages. Wanders its village by day, goes home at night,
 * keeps away from monsters and is always snacking. Right-click one with an empty hand and it
 * shares some of its food.
 */
public class HobbitEntity extends SettlementNpcEntity {
    public static final int VARIANTS = 6;

    /** Command tag of the village's elder, the Thain: a quest giver (see {@code DialogueManager}). */
    public static final String ELDER_TAG = "dndclasses.role.hobbit_elder";

    private static final int DAY_RANGE = 24;
    private static final int NIGHT_RANGE = 3;

    private static final Item[] SNACKS = {
            Items.BREAD, Items.APPLE, Items.COOKIE, Items.PUMPKIN_PIE, Items.BAKED_POTATO,
            Items.CARROT, Items.COOKED_CHICKEN, Items.SWEET_BERRIES, Items.MUSHROOM_STEW, Items.HONEY_BOTTLE
    };
    private static final String[] FIRST_NAMES = {
            "Bilbo", "Frodo", "Samwise", "Meriadoc", "Peregrin", "Rosie", "Lobelia", "Bungo", "Belladonna",
            "Hamfast", "Fredegar", "Lotho", "Primula", "Drogo", "Daisy", "Marigold", "Elanor", "Tolman",
            "Ponto", "Holman", "Poppy", "Odo", "Otho", "Mungo", "Ruby", "Pansy", "Hob", "Wilcome", "Dudo", "Esmeralda"
    };
    private static final String[] SURNAMES = {
            "Baggins", "Took", "Brandybuck", "Gamgee", "Proudfoot", "Bolger", "Cotton", "Boffin", "Bracegirdle",
            "Burrows", "Goodbody", "Hornblower", "Sackville", "Underhill", "Greenhand", "Chubb", "Grubb", "Maggot",
            "Brockhouse", "Twofoot", "Puddifoot", "Sandyman", "Banks", "Longhole"
    };

    /** The wait a hobbit without a saved {@code GiftWait} assumes (the Neutral 5 minutes). */
    private static final int DEFAULT_GIFT_WAIT = 20 * 60 * 5;
    /** Ticks left of the wait set by the last gift (that player's wait). */
    private int giftCooldown;
    /** The wait the last gift set, so {@code giftWait - giftCooldown} is the time since it. */
    private int giftWait = DEFAULT_GIFT_WAIT;

    public HobbitEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createHobbitAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 14.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void initGoals() {
        this.folkGoals().escape(1.4D).fleeMonsters(10.0F, 1.3D).fleePlayers(1.3D).walkTarget(0.8D).wander(0.6D)
                .lookAtKin(HobbitEntity.class).add();
    }

    @Override
    protected int variants() {
        return VARIANTS;
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        Random random = world.getRandom();
        this.setVariant(random.nextInt(VARIANTS));
        this.setCustomName(Text.literal(FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " "
                + SURNAMES[random.nextInt(SURNAMES.length)]));
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(SNACKS[random.nextInt(SNACKS.length)]));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 1.0F);
        this.setPersistent();
        this.setHome(this.getBlockPos());
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    /** Makes this hobbit the village's elder: "Thain <surname>", with the hobbit_elder role. */
    public void makeElder() {
        String name = this.getCustomName() != null ? this.getCustomName().getString() : SURNAMES[0];
        String surname = name.contains(" ") ? name.substring(name.lastIndexOf(' ') + 1) : name;
        this.setCustomName(Text.translatable("entity.dndclasses.hobbit.elder", surname));
        this.addCommandTag(ELDER_TAG);
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.giftCooldown > 0) {
            this.giftCooldown--;
        }
        if (this.random.nextInt(900) == 0 && !this.getMainHandStack().isEmpty()) {
            this.nibble();
        }
    }

    // Stay near home: the whole village by day, indoors at night.
    @Override
    protected int dayRange() {
        return DAY_RANGE;
    }

    @Override
    protected int nightRange() {
        return NIGHT_RANGE;
    }

    private void nibble() {
        this.playSound(SoundEvents.ENTITY_GENERIC_EAT, 0.6F, this.getSoundPitch());
        if (this.world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(new ItemStackParticleEffect(ParticleTypes.ITEM, this.getMainHandStack()),
                    this.getX(), this.getEyeY() - 0.1D, this.getZ(), 6, 0.1D, 0.05D, 0.1D, 0.05D);
        }
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND || !player.getStackInHand(hand).isEmpty()) {
            return super.interactMob(player, hand);
        }
        if (this.world.isClient) {
            return ActionResult.SUCCESS;
        }
        this.getLookControl().lookAt(player);
        // The wait before the next gift depends on how the hobbits feel about this player (Hostile gets none),
        // and Halflings wait less. Each asker needs their own wait to have passed since the last gift.
        int tierWait = TierEffects.giftCooldown(TierEffects.tierWith(player, this));
        int cooldown = tierWait < 0 ? tierWait : HomeBonuses.giftCooldown(player, tierWait);
        if (cooldown < 0) {
            player.sendMessage(Text.translatable("entity.dndclasses.hobbit.no_gift", this.getDisplayName())
                    .formatted(net.minecraft.util.Formatting.RED), true);
        }
        if (cooldown < 0 || this.giftCooldown > 0 && this.giftWait - this.giftCooldown < cooldown) {
            this.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, this.getSoundPitch());
            return ActionResult.CONSUME;
        }
        this.giftCooldown = cooldown;
        this.giftWait = cooldown;
        ItemStack gift = new ItemStack(SNACKS[this.random.nextInt(SNACKS.length)], 1 + this.random.nextInt(3));
        if (!player.giveItemStack(gift)) {
            player.dropItem(gift, false);
        }
        this.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0F, this.getSoundPitch());
        ((ServerWorld) this.world).spawnParticles(ParticleTypes.HEART, this.getX(), this.getEyeY() + 0.4D, this.getZ(),
                3, 0.3D, 0.2D, 0.3D, 0.0D);
        return ActionResult.CONSUME;
    }

    @Override
    public float getSoundPitch() {
        return super.getSoundPitch() * 1.35F;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("GiftCooldown", this.giftCooldown);
        nbt.putInt("GiftWait", this.giftWait);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.giftCooldown = nbt.getInt("GiftCooldown");
        this.giftWait = nbt.contains("GiftWait") ? nbt.getInt("GiftWait") : DEFAULT_GIFT_WAIT;
    }
}

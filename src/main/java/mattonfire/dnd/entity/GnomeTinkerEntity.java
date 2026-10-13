package mattonfire.dnd.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * A Gnome Tinker of Cogsdeep: a Small (0.65) gnome in an apron with brass goggles pushed up on the forehead, who
 * tinkers at a workshop bench and trades ({@link TinkerTrades}): spyglasses, clocks, compasses and redstone
 * components, and to Gnomes only (kin stock) the Gnomish Repeater and Tinker's Goggles. Every Tinker keeps a shop;
 * wares restock every morning and Gnomes pay kin prices ({@link KinPrices}). Tinkers never fight: they flee
 * monsters, and striking one brings every Clockwork Sentinel nearby down on you. Quest-giver role
 * {@value #ROLE_TAG}.
 */
public class GnomeTinkerEntity extends SettlementNpcEntity implements SettlementMerchant {
    /** Skins: fair, tan, ruddy, brown, pale (see tools/cogsdeep_textures.py). */
    public static final int VARIANTS = 5;
    /** Command tag: the quest-giver role, matched by the faction file as {@code @dndclasses.role.gnome_tinker}. */
    public static final String ROLE_TAG = "dndclasses.role.gnome_tinker";

    private static final String[] NAMES = {
            "Alston", "Boddynock", "Brocc", "Burgell", "Dimble", "Eldon", "Erky", "Fonkin", "Frug", "Gerbo", "Gimble",
            "Glim", "Jebeddo", "Kellen", "Namfoodle", "Orryn", "Roondar", "Seebo", "Sindri", "Warryn", "Wrenn", "Zook",
            "Bimpnottin", "Breena", "Caramip", "Carlin", "Donella", "Duvamil", "Ella", "Ellyjobell", "Ellywick", "Lilli",
            "Loopmottin", "Lorilla", "Mardnab", "Nissa", "Nyx", "Oda", "Orla", "Roywyn", "Shamil", "Tana", "Waywocket",
            "Zanna"
    };
    private static final String[] CLANS = {
            "Beren", "Daergel", "Folkor", "Garrick", "Nackle", "Murnig", "Ningel", "Raulnor", "Scheppen", "Timbers",
            "Turen", "Cogwhistle", "Sprocketson", "Gearloose"
    };

    private final NpcMerchant merchant = new NpcMerchant(this, world -> TinkerTrades.tinker(),
            "entity.dndclasses.gnome_tinker.refuses")
            .stocks(new KinStock(KinStockOffers::cogsdeep));

    public GnomeTinkerEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world, 8);
    }

    public static DefaultAttributeContainer.Builder createTinkerAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 18.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.27D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D);
    }

    @Override
    protected void initGoals() {
        this.folkGoals().escape(1.3D).fleeMonsters(8.0F, 1.25D).fleePlayers(1.2D).wander(0.5D).add();
    }

    @Override
    protected int variants() {
        return VARIANTS;
    }

    @Override
    public NpcMerchant merchant() {
        return this.merchant;
    }

    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason,
                                 @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
        Random random = world.getRandom();
        this.setVariant(random.nextInt(VARIANTS));
        String name = NAMES[random.nextInt(NAMES.length)] + " " + CLANS[random.nextInt(CLANS.length)];
        this.setCustomName(Text.translatable("entity.dndclasses.gnome_tinker.named", name));
        this.addCommandTag(ROLE_TAG);
        this.setPersistent();
        if (this.getHome() == null) {
            this.setHome(this.getBlockPos());
        }
        ItemStack tool = new ItemStack(random.nextBoolean() ? Items.CLOCK : random.nextBoolean() ? Items.COMPASS : Items.SPYGLASS);
        this.equipStack(EquipmentSlot.MAINHAND, tool);
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0F);
        return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
    }

    /** Striking a Tinker brings every Clockwork Sentinel nearby down on you. */
    @Override
    protected void callGuards(PlayerEntity player) {
        defend(this, player);
    }

    /** Every Sentinel near {@code npc} turns on {@code player}, who struck a gnome or a Sentinel. */
    static void defend(Entity npc, PlayerEntity player) {
        defend(npc, player, ClockworkSentinelEntity.class);
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        this.merchant.tick();
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        return this.merchant.interact(player, hand, () -> super.interactMob(player, hand));
    }

    // ---- Sounds: a villager's, higher ----

    @Override
    public float getSoundPitch() {
        return super.getSoundPitch() * 1.35F;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return this.merchant.customer() != null ? null : super.getAmbientSound();
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        this.merchant.write(nbt);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.merchant.read(nbt);
    }
}

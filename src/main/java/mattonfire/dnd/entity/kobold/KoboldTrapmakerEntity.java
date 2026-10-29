package mattonfire.dnd.entity.kobold;

import java.util.ArrayList;
import java.util.List;
import mattonfire.dnd.entity.NpcMerchant;
import mattonfire.dnd.entity.SettlementMerchant;
import mattonfire.dnd.entity.kobold.ai.KoboldRearmGoal;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A Kobold Trapmaker: 9 health, 2 armour, speed 0.30, a hook-knife (2). In a fight it hides a snare in the
 * target's path every {@value KoboldSnares#INTERVAL} ticks ({@link KoboldSnares}, at most
 * {@value KoboldSnares#MAX_ACTIVE} at once). At Friendly or better with the Kobold Tribes it trades
 * ({@link KoboldTrades}); worse, it hisses and won't. Out of a fight it re-arms its tribe's spent and disarmed traps
 * ({@link KoboldRearmGoal}).
 */
public class KoboldTrapmakerEntity extends KoboldEntity implements SettlementMerchant {
    private final NpcMerchant merchant = new NpcMerchant(this, world -> KoboldTrades.trapmaker(),
            "entity.dndclasses.kobold_trapmaker.refuses")
            .serves(KoboldHostility::trades)
            .sounds(SoundEvents.ENTITY_FOX_AMBIENT, 1.5F, SoundEvents.ENTITY_FOX_AGGRO, SoundEvents.ENTITY_FOX_AGGRO);
    /** Snares it has set that may still be there. */
    private final List<BlockPos> snares = new ArrayList<>();
    private int nextSnareAt;

    public KoboldTrapmakerEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
    }

    @Override
    protected void initGoals() {
        super.initGoals();
        // Out of a fight it fixes its tribe's spent and disarmed traps (K2, design 10, 4.2).
        this.goalSelector.add(4, new KoboldRearmGoal(this));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return koboldAttributes(9.0D, 2.0D, 0.30D, 2.0D);
    }

    @Override
    public NpcMerchant merchant() {
        return this.merchant;
    }

    List<BlockPos> snares() {
        return this.snares;
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (this.world.isClient) {
            return;
        }
        this.merchant.tick();
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && this.age >= this.nextSnareAt && this.world instanceof ServerWorld server) {
            if (KoboldSnares.placeNear(server, this, target)) {
                this.nextSnareAt = this.age + KoboldSnares.INTERVAL;
            } else {
                this.nextSnareAt = this.age + 20; // nowhere to put one: look again in a second
            }
        }
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        return this.merchant.interact(player, hand, () -> super.interactMob(player, hand));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        this.merchant.write(nbt);
        NbtList list = new NbtList();
        for (BlockPos pos : this.snares) {
            list.add(NbtHelper.fromBlockPos(pos));
        }
        nbt.put("Snares", list);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        this.merchant.read(nbt);
        this.snares.clear();
        for (NbtElement element : nbt.getList("Snares", NbtElement.COMPOUND_TYPE)) {
            this.snares.add(NbtHelper.toBlockPos((NbtCompound) element));
        }
    }
}

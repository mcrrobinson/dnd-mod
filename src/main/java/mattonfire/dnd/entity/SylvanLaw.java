package mattonfire.dnd.entity;

import java.util.List;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.faction.TierEffects;
import mattonfire.dnd.world.gen.RacialHomes;
import mattonfire.dnd.world.gen.enclave.ElvenEnclaveStructures;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Sylvan Law, the elven enclaves' grudge rules ({@link SettlementGrudges}). Every elf within 16 blocks who
 * can see you takes offence, and the Wardens among them turn on you, if inside an enclave you:
 * <ul>
 * <li>break a log or leaf block of the Heart Tree or a talan (a block inside one of their pieces' boxes)</li>
 * <li>kill an animal</li>
 * <li>use flint and steel or a fire charge (fire, lit TNT) or empty a lava bucket</li>
 * <li>open the Heart Tree chest (any chest in the Speaker's Hall)</li>
 * </ul>
 * Kin trust: an Elf's first tree-block offence each in-game day is forgiven with a warning. Helping drive
 * off a goblin raid on the enclave forgives everything ({@link SettlementGrudges#forgive}).
 */
final class SylvanLaw implements SettlementGrudges.Rules {
    SylvanLaw() {
    }

    /** Hooks the offences the block break and open events don't cover: animal kills and fire. */
    static void register() {
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, killer, killed) -> {
            PlayerEntity player = killer instanceof PlayerEntity p ? p
                    : killer instanceof ProjectileEntity projectile && projectile.getOwner() instanceof PlayerEntity p ? p : null;
            if (player != null && killed instanceof AnimalEntity && inEnclave(world, killed.getBlockPos())) {
                offence(player, killed.getBlockPos(), SettlementGrudges.Offence.HARM);
            }
        });
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!world.isClient && (stack.isOf(Items.FLINT_AND_STEEL) || stack.isOf(Items.FIRE_CHARGE))) {
                BlockPos pos = hit.getBlockPos();
                if (inEnclave((ServerWorld) world, pos)) {
                    offence(player, pos, SettlementGrudges.Offence.FIRE);
                }
            }
            return ActionResult.PASS;
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!world.isClient && stack.isOf(Items.LAVA_BUCKET) && inEnclave((ServerWorld) world, player.getBlockPos())) {
                offence(player, player.getBlockPos(), SettlementGrudges.Offence.FIRE);
            }
            return TypedActionResult.pass(stack);
        });
    }

    private static void offence(PlayerEntity player, BlockPos pos, SettlementGrudges.Offence offence) {
        if (!player.isCreative() && !player.isSpectator()) {
            SettlementGrudges.witness(SettlementGrudges.SYLVAN_COURT, player, pos, offence);
        }
    }

    @Nullable
    private static RacialHomes.Visit enclave(ServerWorld world, BlockPos pos) {
        RacialHomes.Visit visit = RacialHomes.homeAt(world, pos);
        return visit != null && visit.home() == RacialHomes.ELVEN_ENCLAVE ? visit : null;
    }

    static boolean inEnclave(ServerWorld world, BlockPos pos) {
        return enclave(world, pos) != null;
    }

    /** Whether {@code pos} is inside one of an enclave's living trees (the Heart Tree or a talan). */
    static boolean inTree(ServerWorld world, BlockPos pos) {
        RacialHomes.Visit visit = enclave(world, pos);
        if (visit == null) {
            return false;
        }
        for (StructurePieceType tree : ElvenEnclaveStructures.TREES) {
            if (visit.pieceAt(pos, tree) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String id() {
        return "dndclasses:sylvan_court";
    }

    @Override
    public boolean protects(World world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
                            SettlementGrudges.Offence offence) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return false;
        }
        return switch (offence) {
            case BREAK -> (state.isIn(BlockTags.LOGS) || state.getBlock() instanceof LeavesBlock) && inTree(serverWorld, pos);
            case OPEN -> {
                RacialHomes.Visit visit = blockEntity instanceof ChestBlockEntity ? enclave(serverWorld, pos) : null;
                yield visit != null && visit.inHearth(pos);
            }
            case HARM, FIRE -> inEnclave(serverWorld, pos);
        };
    }

    /** 16 blocks, adjusted for the player's standing with the Sylvan Court (as for the dwarves). */
    @Override
    public double witnessRange(PlayerEntity player) {
        return TierEffects.witnessRange(TierEffects.tierWith(player, ModEntityTypes.ELF_WARDEN));
    }

    @Override
    public List<ElfEntity> witnesses(PlayerEntity player, BlockPos pos) {
        return player.world.getEntitiesByClass(ElfEntity.class, new Box(pos).expand(this.witnessRange(player)),
                elf -> elf.isAlive() && elf.canSee(player));
    }

    @Override
    public void provoke(MobEntity guard, PlayerEntity player) {
        if (guard instanceof ElfWardenEntity warden) {
            warden.provoke(player);
        } else {
            guard.getLookControl().lookAt(player);
            guard.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, guard.getSoundPitch());
        }
    }

    @Override
    public void warn(MobEntity guard, PlayerEntity player) {
        guard.getLookControl().lookAt(player);
        guard.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0F, guard.getSoundPitch() * 0.9F);
        player.sendMessage(Text.translatable("message.dndclasses.home.elf_warning").formatted(Formatting.GOLD), true);
    }

    @Override
    public SettlementGrudges.Verdict judge(ServerPlayerEntity player, BlockPos pos, SettlementGrudges.Offence offence) {
        if (offence == SettlementGrudges.Offence.BREAK && RacialHomes.ELVEN_ENCLAVE.isHomeOf(RaceLifecycle.activeRaceOf(player))) {
            return SettlementGrudges.dailyWarning(player, this);
        }
        return SettlementGrudges.Verdict.GRUDGE;
    }
}

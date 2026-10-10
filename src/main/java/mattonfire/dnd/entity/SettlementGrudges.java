package mattonfire.dnd.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import mattonfire.dnd.classes.IEntityDataSaver;
import mattonfire.dnd.classes.Race.RaceLifecycle;
import mattonfire.dnd.faction.FactionEvents;
import mattonfire.dnd.faction.TierEffects;
import mattonfire.dnd.world.gen.RacialHomes;
import mattonfire.dnd.world.gen.fortress.DwarvenFortressStructures;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Settlements that defend their things. Each {@link Rules} says what a settlement protects, who guards
 * it and how far they see: meddle with a protected block where a guard can see you and the guards turn on
 * you, and the settlement's faction loses standing ({@link FactionEvents#theftWitnessed}).
 *
 * <p>Kin trust: each rule set can let its home race off. Dwarves let a Dwarf open the chests in a
 * fortress's forge, barracks, mead hall and mine, and the first other offence each in-game day only gets
 * a warning growl.
 *
 * <p>{@code D20.register()} must run before {@link #register}: a failed lockpick stops the chest opening,
 * so the guards see nothing.
 */
public final class SettlementGrudges {
    /** What a player did to a protected block. */
    public enum Offence {
        OPEN, BREAK
    }

    /** How the guards take it. */
    public enum Verdict {
        /** The guards turn on the player. */
        GRUDGE,
        /** Forgiven this time, with a warning. */
        WARNING,
        /** Kin may do this. */
        TRUSTED
    }

    /** One settlement's grudge rules. */
    public interface Rules {
        /** The faction id whose kin-trust bookkeeping this is (one warning a day per faction). */
        String id();

        /** Whether doing {@code offence} to this block is meddling. */
        boolean protects(World world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, Offence offence);

        /** Guards within this range of the block who can see {@code player} witness it. */
        double witnessRange(PlayerEntity player);

        /** The guards of this settlement near {@code pos} who can see {@code player}. */
        List<? extends MobEntity> witnesses(PlayerEntity player, BlockPos pos);

        /** A guard who saw it turns on the player. */
        void provoke(MobEntity guard, PlayerEntity player);

        /** A guard who saw it lets the player off with a warning. */
        void warn(MobEntity guard, PlayerEntity player);

        /** Kin trust: {@link Verdict#GRUDGE} unless the player's race earns them some slack. */
        Verdict judge(ServerPlayerEntity player, BlockPos pos, Offence offence);
    }

    private static final List<Rules> RULES = new ArrayList<>();
    private static final String KIN_TRUST_KEY = "DndKinTrust";

    /** The mountain dwarves guarding their fortress's chests, barrels and gold. */
    public static final Rules MOUNTAIN_DWARVES = add(new DwarfRules());

    private SettlementGrudges() {
    }

    public static Rules add(Rules rules) {
        RULES.add(rules);
        return rules;
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            // Sneaking with an item in hand places it against the chest instead of opening it
            boolean opens = !(player.shouldCancelInteraction() && !player.getStackInHand(hand).isEmpty());
            if (!world.isClient && opens) {
                BlockPos pos = hit.getBlockPos();
                offence(player, world, pos, world.getBlockState(pos), world.getBlockEntity(pos), Offence.OPEN);
            }
            return ActionResult.PASS;
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) ->
                offence(player, world, pos, state, blockEntity, Offence.BREAK));
    }

    /** {@code player} opened the container at {@code pos} some other way (a picked lock). */
    public static void opened(PlayerEntity player, BlockPos pos) {
        World world = player.getWorld();
        offence(player, world, pos, world.getBlockState(pos), world.getBlockEntity(pos), Offence.OPEN);
    }

    private static void offence(PlayerEntity player, World world, BlockPos pos, BlockState state,
                                @Nullable BlockEntity blockEntity, Offence offence) {
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        for (Rules rules : RULES) {
            if (rules.protects(world, pos, state, blockEntity, offence)) {
                witness(rules, player, pos, offence);
            }
        }
    }

    /** Guards of {@code rules} who can see {@code player} meddling at {@code pos} react to it. */
    public static void witness(Rules rules, PlayerEntity player, BlockPos pos, Offence offence) {
        List<? extends MobEntity> witnesses = rules.witnesses(player, pos);
        if (witnesses.isEmpty()) {
            return;
        }
        Verdict verdict = player instanceof ServerPlayerEntity serverPlayer
                ? rules.judge(serverPlayer, pos, offence) : Verdict.GRUDGE;
        switch (verdict) {
            case TRUSTED -> {
            }
            case WARNING -> witnesses.forEach(guard -> rules.warn(guard, player));
            case GRUDGE -> {
                witnesses.forEach(guard -> rules.provoke(guard, player));
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    FactionEvents.theftWitnessed(serverPlayer, witnesses.get(0));
                }
            }
        }
    }

    /**
     * The first offence of each in-game day against {@code rules} is a warning; after that it's a grudge.
     * Stored in the player's persistent data ({@code DndKinTrust}: rule id to the day of the last warning).
     */
    public static Verdict dailyWarning(ServerPlayerEntity player, Rules rules) {
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        NbtCompound trust = data.getCompound(KIN_TRUST_KEY);
        long today = player.getServer().getOverworld().getTimeOfDay() / 24000L;
        if (trust.contains(rules.id()) && trust.getLong(rules.id()) == today) {
            return Verdict.GRUDGE;
        }
        trust.putLong(rules.id(), today);
        data.put(KIN_TRUST_KEY, trust);
        return Verdict.WARNING;
    }

    /** Every dwarf within {@code range} of {@code pos} drops its grudge against {@code player}. */
    public static void forgive(PlayerEntity player, BlockPos pos, double range) {
        for (MountainDwarfEntity dwarf : player.world.getEntitiesByClass(MountainDwarfEntity.class,
                new Box(pos).expand(range), dwarf -> dwarf.isAlive())) {
            if (dwarf.shouldAngerAt(player) || dwarf.getTarget() == player) {
                dwarf.stopAnger();
            }
        }
    }

    /**
     * Dwarves defend their fortress the way piglins guard their gold: open or break a chest or barrel, or
     * break a gold block, where a dwarf can see you and it and its kin turn on you. This applies anywhere a
     * dwarf stands, not only inside a fortress. Help them drive off a goblin raid and every dwarf of the
     * fortress forgives you ({@link #forgive}).
     */
    private static final class DwarfRules implements Rules {
        /** The rooms whose chests a Dwarf may open. The treasury, hall and throne room stay off limits. */
        private static final Set<StructurePieceType> KIN_ROOMS = Set.of(DwarvenFortressStructures.FORGE,
                DwarvenFortressStructures.BARRACKS, DwarvenFortressStructures.BREWHALL, DwarvenFortressStructures.MINE);

        @Override
        public String id() {
            return "dndclasses:mountain_dwarves";
        }

        @Override
        public boolean protects(World world, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity,
                                Offence offence) {
            return isHoard(blockEntity) || offence == Offence.BREAK && state.isOf(Blocks.GOLD_BLOCK);
        }

        /** Chests (trapped ones too) and barrels; not hoppers, dispensers, shulker boxes and the like. */
        private static boolean isHoard(@Nullable BlockEntity blockEntity) {
            return blockEntity instanceof ChestBlockEntity || blockEntity instanceof BarrelBlockEntity;
        }

        /** 16 blocks, 24 for players the dwarves already distrust (Unfriendly or worse). */
        @Override
        public double witnessRange(PlayerEntity player) {
            return TierEffects.witnessRange(TierEffects.tierWith(player, ModEntityTypes.MOUNTAIN_DWARF));
        }

        @Override
        public List<MountainDwarfEntity> witnesses(PlayerEntity player, BlockPos pos) {
            return player.world.getEntitiesByClass(MountainDwarfEntity.class,
                    new Box(pos).expand(this.witnessRange(player)), dwarf -> dwarf.canSee(player));
        }

        @Override
        public void provoke(MobEntity guard, PlayerEntity player) {
            ((MountainDwarfEntity) guard).provoke(player);
        }

        @Override
        public void warn(MobEntity guard, PlayerEntity player) {
            guard.getLookControl().lookAt(player);
            guard.playSound(SoundEvents.ENTITY_VINDICATOR_AMBIENT, 1.0F, 0.5F);
            player.sendMessage(Text.translatable("message.dndclasses.home.dwarf_warning").formatted(Formatting.GOLD), true);
        }

        @Override
        public Verdict judge(ServerPlayerEntity player, BlockPos pos, Offence offence) {
            if (!RacialHomes.DWARVEN_FORTRESS.isHomeOf(RaceLifecycle.activeRaceOf(player))) {
                return Verdict.GRUDGE;
            }
            if (offence == Offence.OPEN) {
                RacialHomes.Visit visit = RacialHomes.homeAt(player.getWorld(), pos);
                if (visit != null && visit.home() == RacialHomes.DWARVEN_FORTRESS) {
                    for (StructurePieceType room : KIN_ROOMS) {
                        if (visit.pieceAt(pos, room) != null) {
                            return Verdict.TRUSTED;
                        }
                    }
                }
            }
            return dailyWarning(player, this);
        }
    }
}

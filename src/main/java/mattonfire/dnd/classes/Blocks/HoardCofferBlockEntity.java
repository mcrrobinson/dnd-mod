package mattonfire.dnd.classes.Blocks;

import java.util.List;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Registry.ModBlocks;
import mattonfire.dnd.dungeon.DungeonLoot;
import mattonfire.dnd.dungeon.DungeonRegistry;
import mattonfire.dnd.dungeon.DungeonState;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Which dungeon a Hoard Coffer belongs to (its start key and, from worldgen, the whole plan, like a
 * ward's). The claims themselves live in the dungeon's {@link DungeonState} (one roll per player per
 * clear), so they survive the coffer's chunk unloading and a {@code /dungeon reset}.
 *
 * A coffer placed by hand ({@code /setblock}) finds its dungeon from where it stands.
 */
public class HoardCofferBlockEntity extends BlockEntity {
    private long startKey;
    private boolean hasStartKey;
    @Nullable
    private NbtCompound summary;

    public HoardCofferBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.HOARD_COFFER_ENTITY, pos, state);
    }

    public void setup(long startKey, @Nullable NbtCompound summary) {
        this.startKey = startKey;
        this.hasStartKey = true;
        this.summary = summary;
        this.markDirty();
    }

    @Nullable
    public DungeonState dungeon(ServerWorld world) {
        DungeonRegistry registry = DungeonRegistry.get(world);
        DungeonState dungeon = this.hasStartKey ? registry.get(this.startKey) : null;
        if (dungeon == null && this.summary != null) {
            dungeon = registry.getOrCreate(this.summary);
        }
        if (dungeon == null) {
            dungeon = DungeonRegistry.find(world, this.pos).orElse(null);
            if (dungeon != null) {
                this.setup(dungeon.startKey(), null);
            }
        }
        return dungeon;
    }

    /**
     * A player opens the coffer: shut while the dungeon stands; otherwise the player's first roll
     * ({@code vault_t<tier>}), or after a repopulation and fresh clear the salvage roll, straight
     * into their inventory (what doesn't fit drops at their feet).
     */
    public void open(ServerWorld world, ServerPlayerEntity player) {
        DungeonState dungeon = this.dungeon(world);
        if (dungeon == null) {
            player.sendMessage(Text.translatable("dungeon.dndclasses.coffer.none").formatted(Formatting.GRAY), true);
            return;
        }
        DungeonState.CofferClaim claim = dungeon.claimCoffer(player.getUuid());
        BlockPos pos = this.pos;
        switch (claim) {
            case LOCKED -> {
                player.sendMessage(Text.translatable("dungeon.dndclasses.coffer.locked").formatted(Formatting.RED), true);
                world.playSound(null, pos, SoundEvents.BLOCK_CHEST_LOCKED, SoundCategory.BLOCKS, 1.0F, 0.8F);
            }
            case NONE -> {
                player.sendMessage(Text.translatable("dungeon.dndclasses.coffer.claimed").formatted(Formatting.GRAY), true);
                world.playSound(null, pos, SoundEvents.BLOCK_CHEST_LOCKED, SoundCategory.BLOCKS, 0.6F, 1.2F);
            }
            case FULL, SALVAGE -> {
                boolean full = claim == DungeonState.CofferClaim.FULL;
                Identifier table = full ? DungeonLoot.vaultTable(dungeon.tier()) : DungeonLoot.salvageTable();
                List<ItemStack> loot = DungeonLoot.roll(world, table, Vec3d.ofCenter(pos), player);
                int items = 0;
                for (ItemStack stack : loot) {
                    items += stack.getCount();
                    player.getInventory().offerOrDrop(stack);
                }
                DnDClasses.LOGGER.info("[DungeonLoot] {} took a {} roll from the Hoard Coffer of {} ({}): {} stack(s)",
                        player.getName().getString(), claim, dungeon.startKey(), table, loot.size());
                player.sendMessage(Text.translatable(full ? "dungeon.dndclasses.coffer.full" : "dungeon.dndclasses.coffer.salvage",
                        items).formatted(full ? Formatting.GOLD : Formatting.YELLOW), true);
                world.playSound(null, pos, SoundEvents.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 1.0F, 0.9F);
                if (full) {
                    world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 0.7F, 1.2F);
                }
                world.spawnParticles(full ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.WAX_OFF, pos.getX() + 0.5,
                        pos.getY() + 1.0, pos.getZ() + 0.5, full ? 30 : 10, 0.3, 0.3, 0.3, 0.2);
            }
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (this.hasStartKey) {
            nbt.putLong("StartKey", this.startKey);
        }
        if (this.summary != null) {
            nbt.put("Dungeon", this.summary);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.hasStartKey = nbt.contains("StartKey");
        this.startKey = nbt.getLong("StartKey");
        this.summary = nbt.contains("Dungeon") ? nbt.getCompound("Dungeon") : null;
    }
}

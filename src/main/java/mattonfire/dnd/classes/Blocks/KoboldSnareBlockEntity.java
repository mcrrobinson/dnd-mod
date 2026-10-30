package mattonfire.dnd.classes.Blocks;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.classes.Party.Party;
import mattonfire.dnd.classes.Party.PartyManager;
import mattonfire.dnd.classes.SkillChecks.D20;
import mattonfire.dnd.classes.SkillChecks.Perceivable;
import mattonfire.dnd.classes.SkillChecks.PerceptionService;
import mattonfire.dnd.entity.kobold.KoboldSaves;
import mattonfire.dnd.entity.kobold.Kobolds;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Who set a Kobold Snare and when it falls apart, and its spotting: like a dungeon trap, Rogues and Rangers always
 * see it, anyone else when their passive Perception meets DC {@value KoboldSaves#SNARE_DC}, or after a Search (V)
 * finds it. Spotters get its outline drawn in particles, only for them, every half second within
 * {@link #SPOT_RANGE} blocks.
 */
public class KoboldSnareBlockEntity extends BlockEntity implements Perceivable {
    public static final double SPOT_RANGE = 6.0D;
    private static final DustParticleEffect OUTLINE = new DustParticleEffect(new Vector3f(1.0F, 0.45F, 0.1F), 0.9F);

    /** The kobold or player who set it; null for one placed by a structure or a command (a kobold snare). */
    @Nullable
    private UUID owner;
    /** Set by a player: spares them and their party, never expires. Otherwise it's a kobold snare. */
    private boolean playerSnare;
    /** World time it falls apart, 0 = never. */
    private long expiresAt;
    private final Set<UUID> searched = new HashSet<>();
    private final Set<UUID> spotted = new HashSet<>();

    public KoboldSnareBlockEntity(BlockPos pos, BlockState state) {
        super(Kobolds.KOBOLD_SNARE_ENTITY, pos, state);
    }

    public void setPlayerOwner(UUID player) {
        this.owner = player;
        this.playerSnare = true;
        this.expiresAt = 0;
        this.markDirty();
    }

    public void setKoboldOwner(UUID kobold, long expiresAt) {
        this.owner = kobold;
        this.playerSnare = false;
        this.expiresAt = expiresAt;
        this.markDirty();
    }

    public boolean isKoboldSnare() {
        return !this.playerSnare;
    }

    @Nullable
    public UUID owner() {
        return this.owner;
    }

    /** Whether stepping in it does nothing to {@code entity}: kobolds in a kobold's snare, the owner's party in a player's. */
    public boolean spares(Entity entity) {
        if (entity instanceof PlayerEntity player && (player.isCreative() || player.isSpectator())) {
            return true;
        }
        if (!this.playerSnare) {
            return Kobolds.isKobold(entity);
        }
        if (this.owner == null) {
            return false;
        }
        if (this.owner.equals(entity.getUuid())) {
            return true;
        }
        if (entity instanceof ServerPlayerEntity player && player.getServer() != null) {
            Party party = PartyManager.get(player.getServer()).getParty(player.getUuid());
            return party != null && party.contains(this.owner);
        }
        return false;
    }

    public static void tick(World world, BlockPos pos, BlockState state, KoboldSnareBlockEntity snare) {
        if (!(world instanceof ServerWorld server) || world.getTime() % 10 != 0) {
            return;
        }
        if (snare.expiresAt > 0 && world.getTime() >= snare.expiresAt) {
            DnDClasses.LOGGER.info("[Kobold] snare at {} falls apart", pos.toShortString());
            world.breakBlock(pos, false);
            return;
        }
        snare.showToSpotters(server);
    }

    private void showToSpotters(ServerWorld world) {
        Vec3d centre = Vec3d.ofCenter(this.pos);
        for (ServerPlayerEntity player : world.getPlayers(p -> p.isAlive() && !p.isSpectator()
                && p.squaredDistanceTo(centre) <= SPOT_RANGE * SPOT_RANGE)) {
            if (!this.spottedBy(player)) {
                continue;
            }
            double y = this.pos.getY() + 0.1D;
            for (int i = 0; i < 4; i++) {
                double t = i / 4.0D;
                outline(world, player, this.pos.getX() + t, y, this.pos.getZ());
                outline(world, player, this.pos.getX() + 1, y, this.pos.getZ() + t);
                outline(world, player, this.pos.getX() + 1 - t, y, this.pos.getZ() + 1);
                outline(world, player, this.pos.getX(), y, this.pos.getZ() + 1 - t);
            }
            if (this.spotted.add(player.getUuid())) {
                player.sendMessage(Text.translatable("block.dndclasses.kobold_snare.spotted"), true);
                DnDClasses.LOGGER.info("[Kobold] {} spots the snare at {}", player.getEntityName(), this.pos.toShortString());
            }
        }
    }

    private static void outline(ServerWorld world, ServerPlayerEntity player, double x, double y, double z) {
        world.spawnParticles(player, OUTLINE, true, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    /**
     * Rogues and Rangers, passive Perception {@value KoboldSaves#SNARE_DC}+, a Search that found it, or the player
     * (and party) who set it.
     */
    public boolean spottedBy(ServerPlayerEntity player) {
        if (this.searched.contains(player.getUuid()) || this.playerSnare && this.spares(player)) {
            return true;
        }
        DndCharacter c = D20.classOf(player);
        return c == DndCharacter.ROGUE || c == DndCharacter.RANGER
                || PerceptionService.noticesPassively(player, Skill.PERCEPTION, KoboldSaves.SNARE_DC);
    }

    // ---- Perceivable: a Search (V) ----

    @Override
    public int perceptionDc() {
        return KoboldSaves.SNARE_DC;
    }

    @Override
    public Vec3d perceptionPos() {
        return Vec3d.ofCenter(this.pos);
    }

    @Override
    public boolean hiddenFrom(ServerPlayerEntity player) {
        return !this.spottedBy(player);
    }

    @Override
    public void perceive(ServerPlayerEntity player, boolean searched) {
        if (searched && this.searched.add(player.getUuid()) && this.world instanceof ServerWorld server) {
            this.showToSpotters(server);
        }
    }

    // ---- Saving ----

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (this.owner != null) {
            nbt.putUuid("Owner", this.owner);
        }
        nbt.putBoolean("PlayerSnare", this.playerSnare);
        nbt.putLong("ExpiresAt", this.expiresAt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.owner = nbt.containsUuid("Owner") ? nbt.getUuid("Owner") : null;
        this.playerSnare = nbt.getBoolean("PlayerSnare");
        this.expiresAt = nbt.getLong("ExpiresAt");
    }
}

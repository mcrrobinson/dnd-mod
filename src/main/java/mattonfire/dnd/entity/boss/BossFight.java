package mattonfire.dnd.entity.boss;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import mattonfire.dnd.classes.mixin.MobEntityAccessor;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * Shared boss fight logic, owned by a {@link Boss} mob (see there for the calls to forward).
 *
 * <ul>
 *   <li>Boss bar: shown to players within range while the boss is fighting a player.</li>
 *   <li>Music: the fight track plays for players who can see the bar (EventMusic on the client).</li>
 *   <li>Phases: entered once each, in order, as health drops below their thresholds; each can
 *       recolour the bar and run a callback. The current phase is saved with the boss.</li>
 *   <li>Loot: optional XP, a loot table other than the entity type's, and an advancement for the
 *       player credited with the kill.</li>
 * </ul>
 *
 * <pre>{@code
 * this.bossFight = new BossFight(this, BossBar.Color.YELLOW, BossBar.Style.NOTCHED_10)
 *         .range(48.0)
 *         .music(ModSounds.MUSIC_DRAGON_FIGHT)
 *         .phase(0.5F, BossBar.Color.RED, this::enrage)
 *         .xp(60);
 * }</pre>
 */
public class BossFight {
    private static final String PHASE_NBT = "BossPhase";

    private record Phase(float healthFraction, BossBar.Color color, Runnable onEnter) {
    }

    private final MobEntity boss;
    private final ServerBossBar bar;
    private final BossBar.Color baseColor;
    private final List<Phase> phases = new ArrayList<>();
    private double range = 64.0;
    private BooleanSupplier canFight = () -> true;
    @Nullable
    private SoundEvent music;
    @Nullable
    private Identifier lootTable;
    @Nullable
    private Identifier advancement;
    private int phase;
    private boolean killed;

    public BossFight(MobEntity boss, BossBar.Color color, BossBar.Style style) {
        this.boss = boss;
        this.baseColor = color;
        this.bar = new ServerBossBar(boss.getDisplayName(), color, style);
    }

    /** Players within this many blocks see the bar (default 64). */
    public BossFight range(double range) {
        this.range = range;
        return this;
    }

    /** Extra condition for the fight to count, e.g. a tamed dragon isn't a boss fight. */
    public BossFight activeWhen(BooleanSupplier condition) {
        this.canFight = condition;
        return this;
    }

    /** Track played (looping, over other music) to players who can see the bar. */
    public BossFight music(SoundEvent track) {
        this.music = track;
        return this;
    }

    /**
     * Adds a phase entered when health drops below {@code healthFraction} of max health.
     * Add phases from highest threshold to lowest; the first is phase 1 (phase 0 is the start).
     */
    public BossFight phase(float healthFraction, BossBar.Color color, Runnable onEnter) {
        this.phases.add(new Phase(healthFraction, color, onEnter));
        return this;
    }

    /** XP dropped on death, when a player killed it (like any mob's XP). */
    public BossFight xp(int xp) {
        ((MobEntityAccessor) this.boss).setExperiencePoints(xp);
        return this;
    }

    /** Loot table to drop instead of the entity type's own (data/<ns>/loot_tables/entities/<type>.json). */
    public BossFight lootTable(Identifier lootTable) {
        this.lootTable = lootTable;
        return this;
    }

    /** Advancement granted to the player credited with the kill; use a "minecraft:impossible" criterion. */
    public BossFight advancement(Identifier advancement) {
        this.advancement = advancement;
        return this;
    }

    /** Current phase: 0 at the start, then 1, 2... as each {@link #phase} is entered. */
    public int getPhase() {
        return this.phase;
    }

    public ServerBossBar getBar() {
        return this.bar;
    }

    public Identifier getLootTable(Identifier typeLootTable) {
        return this.lootTable != null ? this.lootTable : typeLootTable;
    }

    public boolean isFightingPlayer() {
        return this.boss.isAlive() && this.canFight.getAsBoolean()
                && (this.boss.getTarget() instanceof PlayerEntity || this.boss.getAttacker() instanceof PlayerEntity);
    }

    /** Call from the boss's tick(); does nothing on the client. */
    public void tick() {
        if (this.boss.world.isClient) {
            return;
        }
        this.updatePhase();
        this.updateBar();
    }

    private void updatePhase() {
        if (!this.boss.isAlive()) {
            return;
        }
        float health = this.boss.getHealth() / this.boss.getMaxHealth();
        while (this.phase < this.phases.size() && health < this.phases.get(this.phase).healthFraction()) {
            this.phase++;
            this.phases.get(this.phase - 1).onEnter().run();
        }
    }

    private BossBar.Color currentColor() {
        return this.phase == 0 ? this.baseColor : this.phases.get(this.phase - 1).color();
    }

    private void updateBar() {
        this.bar.setPercent(this.boss.getHealth() / this.boss.getMaxHealth());
        this.bar.setName(this.boss.getDisplayName());
        this.bar.setColor(this.currentColor());

        boolean fighting = this.isFightingPlayer();
        for (ServerPlayerEntity player : List.copyOf(this.bar.getPlayers())) {
            if (!fighting || !this.inRange(player)) {
                this.removePlayer(player);
            }
        }
        if (fighting) {
            for (ServerPlayerEntity player : ((ServerWorld) this.boss.world).getPlayers(this::inRange)) {
                this.addPlayer(player);
            }
        }
    }

    private boolean inRange(ServerPlayerEntity player) {
        return player.world == this.boss.world && player.isAlive() && !player.isSpectator()
                && player.squaredDistanceTo(this.boss) < this.range * this.range;
    }

    private void addPlayer(ServerPlayerEntity player) {
        if (this.bar.getPlayers().contains(player)) {
            return;
        }
        this.bar.addPlayer(player);
        if (this.music != null) {
            BossMusic.send(player, this.bar.getUuid(), this.music);
        }
    }

    private void removePlayer(ServerPlayerEntity player) {
        if (!this.bar.getPlayers().contains(player)) {
            return;
        }
        this.bar.removePlayer(player);
        if (this.music != null) {
            BossMusic.send(player, this.bar.getUuid(), null);
        }
    }

    /** Call from the boss's onStoppedTrackingBy(player). */
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        this.removePlayer(player);
    }

    /** Call from the boss's remove(reason). */
    public void onRemoved() {
        for (ServerPlayerEntity player : List.copyOf(this.bar.getPlayers())) {
            this.removePlayer(player);
        }
    }

    /** Call from the boss's onDeath(source); grants the kill advancement. */
    public void onDeath() {
        if (this.killed || this.boss.world.isClient) {
            return;
        }
        this.killed = true;
        // Same player vanilla credits with the kill (player_killed_entity)
        LivingEntity killer = this.boss.getPrimeAdversary();
        if (this.advancement != null && killer instanceof ServerPlayerEntity player) {
            grantAdvancement(player, this.advancement);
        }
    }

    /** Grants every remaining criterion of an advancement, e.g. a kill reward given some other way. */
    public static void grantAdvancement(ServerPlayerEntity player, Identifier id) {
        Advancement advancement = player.server.getAdvancementLoader().get(id);
        if (advancement != null) {
            PlayerAdvancementTracker tracker = player.getAdvancementTracker();
            List<String> criteria = new ArrayList<>();
            tracker.getProgress(advancement).getUnobtainedCriteria().forEach(criteria::add);
            for (String criterion : criteria) {
                tracker.grantCriterion(advancement, criterion);
            }
        }
    }

    public void writeNbt(NbtCompound nbt) {
        nbt.putInt(PHASE_NBT, this.phase);
    }

    /** Restores the phase without running its callback again (its effects were saved with the boss). */
    public void readNbt(NbtCompound nbt) {
        this.phase = Math.min(nbt.getInt(PHASE_NBT), this.phases.size());
        this.bar.setName(this.boss.getDisplayName());
        this.bar.setColor(this.currentColor());
    }
}

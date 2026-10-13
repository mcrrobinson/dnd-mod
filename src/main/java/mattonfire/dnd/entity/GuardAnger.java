package mattonfire.dnd.entity;

import java.util.UUID;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.TimeHelper;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link SettlementGuard} that keeps its own anger (a mob that isn't an iron golem): the {@link net.minecraft.entity.mob.Angerable}
 * accessors read and write its {@link State}, and a grudge lasts {@link #ANGER_TIME} (30-50 s) after the last
 * provocation. The guard still ticks it ({@code tickAngerLogic}) and saves it ({@code writeAngerToNbt} /
 * {@code readAngerFromNbt}) itself.
 */
public interface GuardAnger extends SettlementGuard {
    UniformIntProvider ANGER_TIME = TimeHelper.betweenSeconds(30, 50);

    State angerState();

    @Override
    default int getAngerTime() {
        return this.angerState().angerTime;
    }

    @Override
    default void setAngerTime(int angerTime) {
        this.angerState().angerTime = angerTime;
    }

    @Nullable
    @Override
    default UUID getAngryAt() {
        return this.angerState().angryAt;
    }

    @Override
    default void setAngryAt(@Nullable UUID angryAt) {
        this.angerState().angryAt = angryAt;
    }

    @Override
    default void chooseRandomAngerTime() {
        this.setAngerTime(ANGER_TIME.get(((LivingEntity) this).getRandom()));
    }

    /** Ticks left of the grudge, and whom it's against. */
    final class State {
        private int angerTime;
        @Nullable
        private UUID angryAt;
    }
}

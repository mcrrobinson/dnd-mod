package mattonfire.dnd.classes.Race;

import org.jetbrains.annotations.Nullable;

import mattonfire.dnd.classes.PlayerEntityExt;
import mattonfire.dnd.classes.Misc.BloodHunterControl;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Racial body sizes. Minecraft 1.19.4 has no scale attribute, so three hooks apply the race's
 * {@link RaceInfo} {@code scale}, {@code hitboxWidth} and {@code modelWidth}:
 * <ul>
 * <li>{@code mixin/PlayerDimensionsMixin}: the hitbox ({@code getDimensions}) and eye height
 * ({@code getActiveEyeHeight}) of every pose, recalculated when the synced body race changes.</li>
 * <li>{@code mixin/PlayerEntityRendererMixin}: the model ({@code scale}), so every client draws every
 * player at their size. The first-person hand isn't scaled.</li>
 * </ul>
 * The size follows {@link PlayerEntityExt#getBodyRace()}, which the server keeps in step with the active
 * race (NONE while {@code dndRaces} is off), so client and server always agree. While Identity gives the
 * player a shape (Druid Wild Shape, Blood Hunter control) the form's own size and model are used.
 */
public final class RaceSize {
    private RaceSize() {
    }

    /** The player's body, or null for a normal-sized player (no race, Medium, or in an Identity form). */
    @Nullable
    public static RaceInfo body(PlayerEntity player) {
        if (!(player instanceof PlayerEntityExt ext)) {
            return null;
        }
        RaceInfo info = RaceInfo.get(ext.getBodyRace());
        if (info == null || (info.scale() == 1.0 && info.hitboxWidth() == 1.0 && info.modelWidth() == 1.0)) {
            return null;
        }
        return hasForm(player) ? null : info;
    }

    /** Whether Identity gives the player a shape (Druid Wild Shape, Blood Hunter control). */
    public static boolean hasForm(PlayerEntity player) {
        return BloodHunterControl.hasIdentityForm(player);
    }

    /** The hitbox for a pose, scaled to the race. */
    public static EntityDimensions dimensions(PlayerEntity player, EntityDimensions vanilla) {
        RaceInfo body = body(player);
        if (body == null || (body.scale() == 1.0 && body.hitboxWidth() == 1.0)) {
            return vanilla;
        }
        return vanilla.scaled((float) body.hitboxWidth(), (float) body.scale());
    }

    /** The eye height for a pose, scaled with the hitbox height. */
    public static float eyeHeight(PlayerEntity player, float vanilla) {
        RaceInfo body = body(player);
        return body == null ? vanilla : vanilla * (float) body.scale();
    }
}

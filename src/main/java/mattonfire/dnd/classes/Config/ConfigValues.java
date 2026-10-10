package mattonfire.dnd.classes.Config;
import com.google.gson.annotations.SerializedName;

public record ConfigValues(
        boolean applyArmorEffects,

        // For backwards compatibility
        @SerializedName("applyModificators")
        boolean applyModifiers,

        boolean showDescriptions,

        int descrtiptionsLength,

        /**
         * Client: how saving throws show on the d20 HUD. "compact" (default): small rows beside the crosshair;
         * "full": the big roll panel, like a lockpick; "off": nothing (natural 20s and 1s still sound).
         * See {@link SaveRollsMode}.
         */
        String saveRolls

//        boolean useRecipes
) {
}
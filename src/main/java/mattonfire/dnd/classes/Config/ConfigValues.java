package mattonfire.dnd.classes.Config;
import com.google.gson.annotations.SerializedName;

public record ConfigValues(
        boolean applyArmorEffects,

        // For backwards compatibility
        @SerializedName("applyModificators")
        boolean applyModifiers,

        boolean showDescriptions,

        int descrtiptionsLength

//        boolean useRecipes
) {
}
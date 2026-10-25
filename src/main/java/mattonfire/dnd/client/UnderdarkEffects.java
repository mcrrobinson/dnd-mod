package mattonfire.dnd.client;

import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * How the Underdark looks from inside (its dimension type's {@code effects}, {@code dndclasses:underdark}): no sky,
 * no clouds, no sun dimming the fog, and the darkened lightmap of a sunless world. The fog is each biome's own colour.
 * (Thick mist over the Darklake comes with the flora ticket, T3.)
 */
@Environment(EnvType.CLIENT)
public class UnderdarkEffects extends DimensionEffects {
    public static final Identifier ID = new Identifier(DnDClasses.MOD_ID, "underdark");

    public UnderdarkEffects() {
        super(Float.NaN, false, SkyType.NONE, false, true);
    }

    public static void register() {
        DimensionRenderingRegistry.registerDimensionEffects(ID, new UnderdarkEffects());
    }

    @Override
    public Vec3d adjustFogColor(Vec3d color, float sunHeight) {
        return color;
    }

    @Override
    public boolean useThickFog(int camX, int camY) {
        return false;
    }
}

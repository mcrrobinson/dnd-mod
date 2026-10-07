package mattonfire.dnd.client.model;

import mattonfire.dnd.entity.GelatinousCubeEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class GelatinousCubeModel extends DefaultedEntityGeoModel<GelatinousCubeEntity> {
    private RenderLayer jellyLayer;

    public GelatinousCubeModel() {
        super(new Identifier("dndclasses", "gelatinous_cube"));
    }

    @Override
    public RenderLayer getRenderType(GelatinousCubeEntity animatable, Identifier texture) {
        if (this.jellyLayer == null) {
            this.jellyLayer = createJellyLayer(texture);
        }
        return this.jellyLayer;
    }

    /**
     * Entity-translucent, but without culling (it also tints the view of anyone stuck inside) and without
     * writing depth, so a mob it has engulfed still shows through the jelly whichever is drawn first.
     */
    private static RenderLayer createJellyLayer(Identifier texture) {
        return RenderLayer.of("dndclasses_gelatinous_cube", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                VertexFormat.DrawMode.QUADS, 256, true, true,
                RenderLayer.MultiPhaseParameters.builder()
                        .program(RenderPhase.ENTITY_TRANSLUCENT_PROGRAM)
                        .texture(new RenderPhase.Texture(texture, false, false))
                        .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                        .cull(RenderPhase.DISABLE_CULLING)
                        .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                        .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                        .writeMaskState(RenderPhase.COLOR_MASK)
                        .build(false));
    }
}

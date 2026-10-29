package mattonfire.dnd.client.kobold;

import mattonfire.dnd.entity.kobold.Kobolds;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;

/** The kobolds' client side: renderers and block render layers. Called once from {@code DndClassesClient}. */
public final class KoboldClient {
    private KoboldClient() {
    }

    public static void register() {
        EntityRendererRegistry.register(Kobolds.KOBOLD, KoboldRenderer::small);
        EntityRendererRegistry.register(Kobolds.KOBOLD_SLINGER, KoboldRenderer::small);
        EntityRendererRegistry.register(Kobolds.KOBOLD_TRAPMAKER, KoboldRenderer::small);
        EntityRendererRegistry.register(Kobolds.KOBOLD_DRAGONSHIELD, KoboldRenderer::dragonshield);
        BlockRenderLayerMap.INSTANCE.putBlock(Kobolds.KOBOLD_SNARE_BLOCK, RenderLayer.getCutout());
        // Kobold warrens (K2)
        BlockRenderLayerMap.INSTANCE.putBlock(mattonfire.dnd.classes.Blocks.TrapBlocks.KOBOLD_TRIPLINE, RenderLayer.getCutout());
    }
}

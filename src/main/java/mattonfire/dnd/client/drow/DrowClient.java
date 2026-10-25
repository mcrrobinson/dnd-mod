package mattonfire.dnd.client.drow;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.client.renderer.RacialHumanoidRenderer;
import mattonfire.dnd.entity.drow.Drow;
import mattonfire.dnd.entity.drow.DrowBoltEntity;
import mattonfire.dnd.entity.drow.DrowEntity;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * The drow on the client: the racial humanoid model with elf ears and dark skins ({@code tools/drow_skins.py}), the
 * bolt (an arrow), and Drow Poison's darkened screen edge.
 */
public final class DrowClient {
    /** Drow: slim arms, elf ears (coloured from the skin, so they're dark too), human size. */
    public static final RacialHumanoidRenderer.Look DROW = new RacialHumanoidRenderer.Look(true, DndRace.ELF, 1.0F);
    /** How dark the poison's screen edge gets (vanilla vignette, 40%). */
    public static final float VIGNETTE_STRENGTH = 0.4F;

    private static final Identifier[] WARRIOR_SKINS = new Identifier[DrowEntity.VARIANTS];
    private static final Identifier[] MAGE_SKINS = new Identifier[2];
    private static final Identifier[] PRIESTESS_SKINS = new Identifier[2];
    private static final Identifier ELITE_SKIN = skin("elite");
    private static final Identifier MATRON_SKIN = skin("matron");
    /** The Matron Mother: as the drow, a head taller. */
    public static final RacialHumanoidRenderer.Look MATRON = new RacialHumanoidRenderer.Look(true, DndRace.ELF, 1.13F);
    private static final Identifier BOLT = new Identifier("textures/entity/projectiles/arrow.png");
    private static final Identifier VIGNETTE = new Identifier("textures/misc/vignette.png");

    static {
        for (int i = 0; i < WARRIOR_SKINS.length; i++) {
            WARRIOR_SKINS[i] = skin("drow_" + i);
        }
        for (int i = 0; i < MAGE_SKINS.length; i++) {
            MAGE_SKINS[i] = skin("mage_" + i);
            PRIESTESS_SKINS[i] = skin("priestess_" + i);
        }
    }

    private DrowClient() {
    }

    public static void register() {
        EntityRendererRegistry.register(Drow.DROW_WARRIOR, context -> drow(context, WARRIOR_SKINS));
        EntityRendererRegistry.register(Drow.DROW_MAGE, context -> drow(context, MAGE_SKINS));
        EntityRendererRegistry.register(Drow.DROW_PRIESTESS, context -> drow(context, PRIESTESS_SKINS));
        EntityRendererRegistry.register(Drow.DROW_ELITE, context -> new RacialHumanoidRenderer<>(context, DROW, drow -> ELITE_SKIN));
        EntityRendererRegistry.register(Drow.DROW_MATRON, context -> new RacialHumanoidRenderer<>(context, MATRON, drow -> MATRON_SKIN));
        EntityRendererRegistry.register(Drow.DROW_BOLT, BoltRenderer::new);
        PiwafwiRenderer.register();
        HudRenderCallback.EVENT.register(DrowClient::poisonVignette);
    }

    private static <E extends DrowEntity> RacialHumanoidRenderer<E> drow(EntityRendererFactory.Context context, Identifier[] skins) {
        return new RacialHumanoidRenderer<>(context, DROW, drow -> skins[Math.floorMod(drow.getVariant(), skins.length)]);
    }

    private static Identifier skin(String name) {
        return new Identifier(mattonfire.dnd.classes.DnDClasses.MOD_ID, "textures/entity/drow/" + name + ".png");
    }

    /** Drow Poison darkens the screen edge (vanilla's vignette texture, all channels). */
    private static void poisonVignette(MatrixStack matrices, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || Drow.DROW_POISON == null
                || !client.player.hasStatusEffect(Drow.DROW_POISON)) {
            return;
        }
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.ZERO, GlStateManager.DstFactor.ONE_MINUS_SRC_COLOR,
                GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
        RenderSystem.setShaderColor(VIGNETTE_STRENGTH, VIGNETTE_STRENGTH * 0.85F, VIGNETTE_STRENGTH, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, VIGNETTE);
        DrawableHelper.drawTexture(matrices, 0, 0, 0.0F, 0.0F, width, height, width, height);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** The bolt: drawn as an arrow. */
    private static final class BoltRenderer extends ProjectileEntityRenderer<DrowBoltEntity> {
        BoltRenderer(EntityRendererFactory.Context context) {
            super(context);
        }

        @Override
        public Identifier getTexture(DrowBoltEntity entity) {
            return BOLT;
        }
    }
}

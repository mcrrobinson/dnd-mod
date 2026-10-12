package mattonfire.dnd.classes.Client.Render;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Obstacles.ObstacleBlock;
import mattonfire.dnd.classes.Obstacles.types.CraftElementObstacles;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.ModelHelper;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.Baker;
import net.minecraft.client.render.model.ModelBakeSettings;
import net.minecraft.client.render.model.UnbakedModel;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;

/**
 * The sealed Fire Curtain's look: every block draws a flat sheet of soul fire, and picks its texture from its
 * neighbours so the blocks of a curtain join into one sheet of flame, with tongues licking only along the top
 * of the whole curtain, ragged flames down its two ends and a white-hot base along the bottom.
 * <ul>
 * <li>A curtain that runs along X (curtain to the east or west, none north or south) draws its sheet through
 * the middle of the block, facing north and south; one along Z faces east and west.</li>
 * <li>A lone block or column (no curtain beside it) draws two crossed sheets, like a vanilla fire.</li>
 * <li>A thick curtain (curtain along both axes, e.g. the Nether fortress's 2x2x2) draws a sheet just inside
 * each outer face, closed at the corners, so it reads as one block of flame; a block with no outer face draws
 * the cross.</li>
 * </ul>
 * The textures ({@code textures/block/fire_curtain/}, from {@code tools/fire_curtain_textures.py}) are cut
 * from one flame field that tiles across blocks, so the joins line up in every animation frame. Connections
 * are read from the world as the chunk is drawn, so there's no extra block state and every placement path
 * (command, structure, template) looks the same. Fully lit and unshaded, like a light source should be.
 */
public final class FireCurtainModel implements UnbakedModel, BakedModel {
    /** The model id the sealed curtain's blockstate points at; {@link #register()} supplies it. */
    public static final Identifier MODEL_ID = new Identifier(DnDClasses.MOD_ID, "block/fire_curtain_sheet");
    /** Blocks along a sheet cycle through this many texture columns (world position mod 3). */
    private static final int COLUMNS = 3;
    /** How far inside an outer face a thick curtain's sheet sits. */
    private static final float INSET = 1f / 16f;

    /** By {@link #index}, then column. */
    private final Sprite[][] sprites = new Sprite[16][COLUMNS];
    private RenderMaterial material;

    /** Hooks the model into model loading. Client init only. */
    public static void register() {
        ModelLoadingPlugin.register(context -> context.resolveModel()
                .register(resolver -> MODEL_ID.equals(resolver.id()) ? new FireCurtainModel() : null));
    }

    private static int index(boolean top, boolean base, boolean left, boolean right) {
        return (top ? 8 : 0) | (base ? 4 : 0) | (left ? 2 : 0) | (right ? 1 : 0);
    }

    private static String textureName(int index, int column) {
        return "block/fire_curtain/sheet_" + ((index & 8) != 0 ? "t" : "x") + ((index & 4) != 0 ? "b" : "x")
                + ((index & 2) != 0 ? "l" : "x") + ((index & 1) != 0 ? "r" : "x") + "_" + column;
    }

    // ---- UnbakedModel

    @Override
    public Collection<Identifier> getModelDependencies() {
        return List.of();
    }

    @Override
    public void setParents(Function<Identifier, UnbakedModel> modelLoader) {
    }

    @Override
    public BakedModel bake(Baker baker, Function<SpriteIdentifier, Sprite> textureGetter,
            ModelBakeSettings rotationContainer, Identifier modelId) {
        for (int i = 0; i < 16; i++) {
            for (int c = 0; c < COLUMNS; c++) {
                sprites[i][c] = textureGetter.apply(new SpriteIdentifier(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE,
                        new Identifier(DnDClasses.MOD_ID, textureName(i, c))));
            }
        }
        Renderer renderer = RendererAccess.INSTANCE.getRenderer();
        if (renderer != null) {
            material = renderer.materialFinder().blendMode(BlendMode.CUTOUT).emissive(true).disableDiffuse(true)
                    .ambientOcclusion(TriState.FALSE).find();
        }
        return this;
    }

    // ---- Drawing

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(BlockRenderView world, BlockState state, BlockPos pos,
            Supplier<Random> randomSupplier, RenderContext context) {
        if (material == null) {
            return;
        }
        boolean north = curtain(world, pos.north());
        boolean south = curtain(world, pos.south());
        boolean west = curtain(world, pos.west());
        boolean east = curtain(world, pos.east());
        boolean top = !curtain(world, pos.up());
        boolean base = !curtain(world, pos.down());
        boolean alongX = west || east;
        boolean alongZ = north || south;
        QuadEmitter emitter = context.getEmitter();
        int columnX = MathHelper.floorMod(pos.getX(), COLUMNS);
        int columnZ = MathHelper.floorMod(pos.getZ(), COLUMNS);
        Sprite xSheet = sprites[index(top, base, !west, !east)][columnX];
        Sprite zSheet = sprites[index(top, base, !north, !south)][columnZ];
        if (alongX && !alongZ) {
            sheet(emitter, true, 0.5f, xSheet);
        } else if (alongZ && !alongX) {
            sheet(emitter, false, 0.5f, zSheet);
        } else if (!alongX || north && south && west && east) {
            // A lone block or column, or the inside of a thick curtain: crossed sheets
            sheet(emitter, true, 0.5f, xSheet);
            sheet(emitter, false, 0.5f, zSheet);
        } else {
            // Thick: a sheet just inside each outer face. A face's open end is a corner, where the next face's
            // sheet carries on, so the ends aren't cut and the corners stay closed.
            Sprite xFace = sprites[index(top, base, false, false)][columnX];
            Sprite zFace = sprites[index(top, base, false, false)][columnZ];
            if (!north) {
                sheet(emitter, true, INSET, xFace);
            }
            if (!south) {
                sheet(emitter, true, 1 - INSET, xFace);
            }
            if (!west) {
                sheet(emitter, false, INSET, zFace);
            }
            if (!east) {
                sheet(emitter, false, 1 - INSET, zFace);
            }
        }
    }

    private static boolean curtain(BlockRenderView world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.isOf(CraftElementObstacles.FIRE_CURTAIN_BLOCK) && ObstacleBlock.isSealed(state);
    }

    /**
     * Both faces of one sheet: along X it spans the block's width at z = {@code depth}, along Z its depth at
     * x = {@code depth}. The texture runs west to east (north to south) seen from either side, so a sheet's
     * left edge is always the west (north) end and neighbouring blocks' textures meet.
     */
    private void sheet(QuadEmitter emitter, boolean alongX, float depth, Sprite sprite) {
        if (alongX) {
            quad(emitter, Direction.SOUTH, sprite, true, new float[][] {
                    { 0, 1, depth }, { 0, 0, depth }, { 1, 0, depth }, { 1, 1, depth } });
            quad(emitter, Direction.NORTH, sprite, true, new float[][] {
                    { 1, 1, depth }, { 1, 0, depth }, { 0, 0, depth }, { 0, 1, depth } });
        } else {
            quad(emitter, Direction.EAST, sprite, false, new float[][] {
                    { depth, 1, 1 }, { depth, 0, 1 }, { depth, 0, 0 }, { depth, 1, 0 } });
            quad(emitter, Direction.WEST, sprite, false, new float[][] {
                    { depth, 1, 0 }, { depth, 0, 0 }, { depth, 0, 1 }, { depth, 1, 1 } });
        }
    }

    /** One quad, corners counter-clockwise as seen from {@code face}, UVs from the position along the sheet. */
    private void quad(QuadEmitter emitter, Direction face, Sprite sprite, boolean alongX, float[][] corners) {
        for (int i = 0; i < 4; i++) {
            float[] v = corners[i];
            emitter.pos(i, v[0], v[1], v[2]);
            emitter.uv(i, alongX ? v[0] : v[2], 1 - v[1]);
        }
        emitter.nominalFace(face);
        emitter.cullFace(null);
        emitter.material(material);
        emitter.color(-1, -1, -1, -1);
        emitter.spriteBake(sprite, MutableQuadView.BAKE_NORMALIZED);
        emitter.emit();
    }

    // ---- BakedModel (vanilla path: unused, the item has its own model)

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction face, Random random) {
        return List.of();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean hasDepth() {
        return false;
    }

    @Override
    public boolean isSideLit() {
        return false;
    }

    @Override
    public boolean isBuiltin() {
        return false;
    }

    @Override
    public Sprite getParticleSprite() {
        return sprites[index(true, true, true, true)][0];
    }

    @Override
    public ModelTransformation getTransformation() {
        return ModelHelper.MODEL_TRANSFORM_BLOCK;
    }

    @Override
    public ModelOverrideList getOverrides() {
        return ModelOverrideList.EMPTY;
    }
}

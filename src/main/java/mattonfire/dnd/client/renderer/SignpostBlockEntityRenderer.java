package mattonfire.dnd.client.renderer;

import java.util.ArrayList;
import java.util.List;

import mattonfire.dnd.classes.Blocks.adventuring.SignpostBlock;
import mattonfire.dnd.classes.Blocks.adventuring.SignpostBlockEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

/**
 * Draws an Adventurer's Signpost's text on both faces of its arrow board, like sign text: each line wrapped, then the
 * whole block scaled down (never above vanilla sign size) to fit the board between the arrow's tip and the post.
 */
public class SignpostBlockEntityRenderer implements BlockEntityRenderer<SignpostBlockEntity> {
    /** Text colour where a line has none of its own: dark brown, like burnt-in letters. */
    private static final int INK = 0x2E1E0F;
    /** The board's faces are 1 px apart; the text sits just off each. */
    private static final float FACE = 0.5F / 16.0F + 0.002F;
    /** The text area on the board, in the model's north-pointing frame (block pixels). */
    private static final float CENTRE_Y = 11.5F / 16.0F;
    private static final float CENTRE_Z = (7.5F - 8.0F) / 16.0F;
    private static final float AREA_W = 8.0F / 16.0F;
    private static final float AREA_H = 6.0F / 16.0F;
    /** Vanilla sign text scale: the most a line is ever drawn at. */
    private static final float MAX_SCALE = 1.0F / 96.0F;
    private static final int WRAP = 90;
    private static final int LINE = 10;

    private final TextRenderer text;

    public SignpostBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.text = context.getTextRenderer();
    }

    @Override
    public void render(SignpostBlockEntity post, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertices,
                       int light, int overlay) {
        List<Text> lines = post.lines();
        if (lines.isEmpty()) {
            return;
        }
        List<OrderedText> wrapped = new ArrayList<>();
        int widest = 1;
        for (Text line : lines) {
            for (OrderedText part : this.text.wrapLines(line, WRAP)) {
                wrapped.add(part);
                widest = Math.max(widest, this.text.getWidth(part));
            }
        }
        float height = wrapped.size() * LINE - 2;
        float scale = Math.min(MAX_SCALE, Math.min(AREA_W / widest, AREA_H / height));

        Direction facing = post.getCachedState().get(SignpostBlock.FACING);
        matrices.push();
        matrices.translate(0.5F, 0.0F, 0.5F);
        // The model turns clockwise (seen from above) from north: north 0, east 90, south 180, west 270.
        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((facing.asRotation() + 180.0F) % 360.0F));
        for (int side = 0; side < 2; side++) {
            matrices.push();
            boolean east = side == 0;
            matrices.translate(east ? FACE : -FACE, CENTRE_Y, CENTRE_Z);
            // Text faces +z: turn it to face out of this side, reading towards the tip on the east face.
            matrices.multiply((east ? RotationAxis.POSITIVE_Y : RotationAxis.NEGATIVE_Y).rotationDegrees(90.0F));
            matrices.scale(scale, -scale, scale);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float y = -height / 2.0F;
            for (OrderedText line : wrapped) {
                float x = -this.text.getWidth(line) / 2.0F;
                this.text.draw(line, x, y, INK, false, matrix, vertices, TextRenderer.TextLayerType.POLYGON_OFFSET, 0, light);
                y += LINE;
            }
            matrices.pop();
        }
        matrices.pop();
    }
}

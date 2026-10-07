package mattonfire.dnd.client;

import java.io.IOException;
import java.io.InputStream;
import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The mimic's skin: the real chest texture (from whatever resource pack is active), with the
 * mimic's mouth, teeth and tongue painted over the parts a closed chest never shows. Built on
 * first use and rebuilt after every resource reload, so a disguised mimic always matches the
 * chests around it.
 */
public final class MimicTexture {
    private static final Logger LOGGER = LoggerFactory.getLogger("dndclasses/mimic");
    public static final Identifier ID = new Identifier(DnDClasses.MOD_ID, "dynamic/mimic");
    private static final Identifier CHEST = new Identifier("minecraft", "textures/entity/chest/normal.png");
    /** 64x64, laid out like the chest texture; transparent wherever the chest should show through. */
    private static final Identifier MOUTH = new Identifier(DnDClasses.MOD_ID, "textures/entity/mimic/mouth.png");

    private static boolean built;
    private static Identifier current = CHEST;

    private MimicTexture() {
    }

    public static void register() {
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return new Identifier(DnDClasses.MOD_ID, "mimic_texture");
            }

            @Override
            public void reload(ResourceManager manager) {
                built = false;
            }
        });
    }

    /** The texture to draw a mimic with. Call on the render thread. */
    public static Identifier get() {
        if (!built) {
            built = true;
            current = build();
        }
        return current;
    }

    private static Identifier build() {
        MinecraftClient client = MinecraftClient.getInstance();
        ResourceManager resources = client.getResourceManager();
        try (InputStream chestIn = resources.open(CHEST); InputStream mouthIn = resources.open(MOUTH);
             NativeImage chest = NativeImage.read(chestIn); NativeImage mouth = NativeImage.read(mouthIn)) {
            int scale = Math.max(1, chest.getWidth() / mouth.getWidth());
            NativeImage out = new NativeImage(chest.getWidth(), chest.getHeight(), true);
            out.copyFrom(chest);
            for (int y = 0; y < mouth.getHeight(); y++) {
                for (int x = 0; x < mouth.getWidth(); x++) {
                    int color = mouth.getColor(x, y);
                    if ((color >>> 24) == 0) {
                        continue;
                    }
                    for (int dy = 0; dy < scale; dy++) {
                        for (int dx = 0; dx < scale; dx++) {
                            int px = x * scale + dx;
                            int py = y * scale + dy;
                            if (px < out.getWidth() && py < out.getHeight()) {
                                out.setColor(px, py, color);
                            }
                        }
                    }
                }
            }
            client.getTextureManager().registerTexture(ID, new NativeImageBackedTexture(out));
            return ID;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Couldn't build the mimic texture, drawing it as a plain chest", e);
            return CHEST;
        }
    }
}

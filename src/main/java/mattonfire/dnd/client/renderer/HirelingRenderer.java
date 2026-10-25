package mattonfire.dnd.client.renderer;

import java.util.EnumMap;
import java.util.Map;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.entity.HirelingEntity;
import mattonfire.dnd.entity.hireling.HirelingRace;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Hirelings: one {@link RacialHumanoidRenderer} per race, so each wears its race's model and head features.
 * Humans use the hireling skins ({@code tools/hireling_skins.py}); the other races borrow their settlement
 * folk's skins (elves, orcs, Tieflings, Dragonborn, gnomes).
 */
public class HirelingRenderer extends EntityRenderer<HirelingEntity> {
    private final Map<HirelingRace, RacialHumanoidRenderer<HirelingEntity>> renderers = new EnumMap<>(HirelingRace.class);

    public HirelingRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.renderers.put(HirelingRace.HUMAN, new RacialHumanoidRenderer<>(context, RacialHumanoidRenderer.HUMAN,
                skins("hireling/human", HirelingRace.HUMAN)));
        this.renderers.put(HirelingRace.ELF, new RacialHumanoidRenderer<>(context, RacialHumanoidRenderer.ELF,
                skins("elf/elf", HirelingRace.ELF)));
        this.renderers.put(HirelingRace.HALF_ORC, new RacialHumanoidRenderer<>(context, RacialHumanoidRenderer.HALF_ORC,
                skins("orc/orc", HirelingRace.HALF_ORC)));
        this.renderers.put(HirelingRace.TIEFLING, new RacialHumanoidRenderer<>(context, RacialHumanoidRenderer.TIEFLING,
                skins("tiefling/tiefling", HirelingRace.TIEFLING)));
        this.renderers.put(HirelingRace.DRAGONBORN, new RacialHumanoidRenderer<>(context, RacialHumanoidRenderer.DRAGONBORN,
                skins("dragonborn/dragonborn", HirelingRace.DRAGONBORN)));
        this.renderers.put(HirelingRace.GNOME, new RacialHumanoidRenderer<>(context, RacialHumanoidRenderer.GNOME,
                skins("gnome/gnome", HirelingRace.GNOME)));
    }

    private static java.util.function.Function<HirelingEntity, Identifier> skins(String prefix, HirelingRace race) {
        Identifier[] textures = new Identifier[race.skins];
        for (int i = 0; i < textures.length; i++) {
            textures[i] = new Identifier(DnDClasses.MOD_ID, "textures/entity/" + prefix + "_" + i + ".png");
        }
        return hireling -> textures[Math.floorMod(hireling.variant(), textures.length)];
    }

    private RacialHumanoidRenderer<HirelingEntity> rendererFor(HirelingEntity entity) {
        return this.renderers.get(entity.race());
    }

    @Override
    public void render(HirelingEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light) {
        this.rendererFor(entity).render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(HirelingEntity entity) {
        return this.rendererFor(entity).getTexture(entity);
    }
}

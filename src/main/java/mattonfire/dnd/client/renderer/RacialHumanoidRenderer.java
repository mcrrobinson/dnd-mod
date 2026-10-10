package mattonfire.dnd.client.renderer;

import java.util.function.Function;
import mattonfire.dnd.classes.Client.Render.RaceFeatures;
import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.Race.DndRace;
import mattonfire.dnd.classes.Race.DragonAncestry;
import mattonfire.dnd.entity.DragonbornElderEntity;
import mattonfire.dnd.entity.DragonbornEntity;
import mattonfire.dnd.entity.DragonbornTemplarEntity;
import mattonfire.dnd.entity.ElfEntity;
import mattonfire.dnd.entity.ElfMerchantEntity;
import mattonfire.dnd.entity.ModEntityTypes;
import mattonfire.dnd.entity.OrcChampionEntity;
import mattonfire.dnd.entity.OrcEntity;
import mattonfire.dnd.entity.OrcQuartermasterEntity;
import mattonfire.dnd.entity.OrcWarChiefEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * The racial NPC model kit: the player model (slim Alex arms or classic Steve ones) with the race's head
 * features, a per-entity scale, armour, and arm poses for held items and drawn bows. The settlement NPCs of
 * each race use it with their own skins: the elves, the Dragon Temple's Dragonborn and the Orc Strongholds' orcs. Head features (elf ears, and later orc
 * tusks, tiefling horns, a dragonborn snout) come from {@link RaceFeatures#renderHead}, the same code that
 * draws them on players, so NPCs and players of a race match.
 */
public class RacialHumanoidRenderer<T extends MobEntity> extends BipedEntityRenderer<T, RacialHumanoidRenderer.HumanoidModel<T>> {
    /** What one race's NPCs look like: arms, the race whose head features they wear, and scale. */
    public record Look(boolean slim, DndRace race, float scale, float width) {
        public Look(boolean slim, DndRace race, float scale) {
            this(slim, race, scale, scale);
        }
    }

    public static final Look ELF = new Look(true, DndRace.ELF, 1.0F);
    /** Dragonborn: classic arms, snout and frill, 1.05x. */
    public static final Look DRAGONBORN = new Look(false, DndRace.DRAGONBORN, 1.05F);

    private final Function<T, Identifier> texture;
    private final Look look;

    public RacialHumanoidRenderer(EntityRendererFactory.Context context, Look look, Function<T, Identifier> texture) {
        super(context, new HumanoidModel<>(context.getPart(look.slim() ? EntityModelLayers.PLAYER_SLIM : EntityModelLayers.PLAYER),
                look.slim()), 0.5F * look.width());
        this.texture = texture;
        this.look = look;
        this.addFeature(new ArmorFeatureRenderer<>(this,
                new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER_INNER_ARMOR)),
                new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        if (look.race() != DndRace.NONE) {
            this.addFeature(new HeadFeature<>(this, look.race()));
        }
    }

    // ---- The elves ----

    private static final Identifier[] ELF_SKINS = new Identifier[ElfEntity.VARIANTS];
    private static final Identifier SPEAKER_SKIN = new Identifier(DnDClasses.MOD_ID, "textures/entity/elf/speaker.png");
    private static final Identifier FLETCHER_SKIN = new Identifier(DnDClasses.MOD_ID, "textures/entity/elf/fletcher.png");

    static {
        for (int i = 0; i < ELF_SKINS.length; i++) {
            ELF_SKINS[i] = new Identifier(DnDClasses.MOD_ID, "textures/entity/elf/elf_" + i + ".png");
        }
    }

    /** Wood elves and wardens: one of the six skins (wardens wear their leathers over it). */
    public static <E extends ElfEntity> RacialHumanoidRenderer<E> elf(EntityRendererFactory.Context context) {
        return new RacialHumanoidRenderer<>(context, ELF, elf -> ELF_SKINS[Math.floorMod(elf.getVariant(), ELF_SKINS.length)]);
    }

    public static RacialHumanoidRenderer<ElfMerchantEntity> elfMerchant(EntityRendererFactory.Context context) {
        return new RacialHumanoidRenderer<>(context, ELF,
                elf -> elf.getType() == ModEntityTypes.ELF_SPEAKER ? SPEAKER_SKIN : FLETCHER_SKIN);
    }

    // ---- The Dragonborn ----

    private static final Identifier[] DRAGONBORN_SKINS = new Identifier[DragonbornEntity.VARIANTS];
    private static final Identifier ELDER_SKIN = new Identifier(DnDClasses.MOD_ID, "textures/entity/dragonborn/elder.png");

    static {
        for (int i = 0; i < DRAGONBORN_SKINS.length; i++) {
            DRAGONBORN_SKINS[i] = new Identifier(DnDClasses.MOD_ID, "textures/entity/dragonborn/dragonborn_" + i + ".png");
        }
    }

    /** Templars: scales in red, white, blue, bronze or gold (their variant), under a chainmail shirt. */
    public static RacialHumanoidRenderer<DragonbornTemplarEntity> dragonborn(EntityRendererFactory.Context context) {
        return new RacialHumanoidRenderer<>(context, DRAGONBORN,
                npc -> DRAGONBORN_SKINS[Math.floorMod(npc.getVariant(), DRAGONBORN_SKINS.length)]);
    }

    /** The Elder: gold-trimmed robes over scales. */
    public static RacialHumanoidRenderer<DragonbornElderEntity> dragonbornElder(EntityRendererFactory.Context context) {
        return new RacialHumanoidRenderer<>(context, DRAGONBORN, npc -> ELDER_SKIN);
    }

    // ---- The orcs ----

    /** Orcs: classic arms, tusks, 1.1x tall and 1.15x wide. */
    public static final Look ORC = new Look(false, DndRace.HALFORC, 1.1F, 1.15F);
    public static final Look ORC_CHAMPION = new Look(false, DndRace.HALFORC, 1.15F, 1.2F);
    public static final Look ORC_WAR_CHIEF = new Look(false, DndRace.HALFORC, 1.25F, 1.3F);
    private static final Identifier[] ORC_SKINS = new Identifier[OrcEntity.VARIANTS];
    private static final Identifier QUARTERMASTER_SKIN = new Identifier(DnDClasses.MOD_ID, "textures/entity/orc/quartermaster.png");
    private static final Identifier CHAMPION_SKIN = new Identifier(DnDClasses.MOD_ID, "textures/entity/orc/champion.png");
    private static final Identifier WAR_CHIEF_SKIN = new Identifier(DnDClasses.MOD_ID, "textures/entity/orc/war_chief.png");

    static {
        for (int i = 0; i < ORC_SKINS.length; i++) {
            ORC_SKINS[i] = new Identifier(DnDClasses.MOD_ID, "textures/entity/orc/orc_" + i + ".png");
        }
    }

    /** Warriors and archers: one of the six skins (green to grey-green, in hide and fur). */
    public static <E extends OrcEntity> RacialHumanoidRenderer<E> orc(EntityRendererFactory.Context context) {
        return new RacialHumanoidRenderer<>(context, ORC, orc -> ORC_SKINS[Math.floorMod(orc.getVariant(), ORC_SKINS.length)]);
    }

    public static RacialHumanoidRenderer<OrcQuartermasterEntity> orcQuartermaster(EntityRendererFactory.Context context) {
        return new RacialHumanoidRenderer<>(context, ORC, orc -> QUARTERMASTER_SKIN);
    }

    public static RacialHumanoidRenderer<OrcChampionEntity> orcChampion(EntityRendererFactory.Context context) {
        return new RacialHumanoidRenderer<>(context, ORC_CHAMPION, orc -> CHAMPION_SKIN);
    }

    public static RacialHumanoidRenderer<OrcWarChiefEntity> orcWarChief(EntityRendererFactory.Context context) {
        return new RacialHumanoidRenderer<>(context, ORC_WAR_CHIEF, orc -> WAR_CHIEF_SKIN);
    }

    @Override
    public Identifier getTexture(T entity) {
        return this.texture.apply(entity);
    }

    @Override
    protected void scale(T entity, MatrixStack matrices, float amount) {
        if (this.look.scale() != 1.0F || this.look.width() != 1.0F) {
            matrices.scale(this.look.width(), this.look.scale(), this.look.width());
        }
    }

    /** The player model, with arm poses from what the mob holds (a bow drawn while it's being used). */
    public static class HumanoidModel<T extends MobEntity> extends PlayerEntityModel<T> {
        HumanoidModel(ModelPart root, boolean slim) {
            super(root, slim);
        }

        @Override
        public void setAngles(T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
            ItemStack held = entity.getMainHandStack();
            if (held.getItem() instanceof net.minecraft.item.CrossbowItem) {
                this.rightArmPose = entity.isUsingItem() ? ArmPose.CROSSBOW_CHARGE
                        : net.minecraft.item.CrossbowItem.isCharged(held) ? ArmPose.CROSSBOW_HOLD : ArmPose.ITEM;
            } else {
                this.rightArmPose = held.isEmpty() ? ArmPose.EMPTY
                        : entity.isUsingItem() && held.getItem() instanceof BowItem ? ArmPose.BOW_AND_ARROW : ArmPose.ITEM;
            }
            this.leftArmPose = ArmPose.EMPTY;
            super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
        }
    }

    /** Draws the race's head features on the posed head (none while something is worn on the head). */
    private static final class HeadFeature<T extends MobEntity> extends FeatureRenderer<T, HumanoidModel<T>> {
        private final DndRace race;

        HeadFeature(FeatureRendererContext<T, HumanoidModel<T>> context, DndRace race) {
            super(context);
            this.race = race;
        }

        @Override
        public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, T entity, float limbAngle,
                           float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
            if (entity.isInvisible() || !entity.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
                return;
            }
            DragonAncestry ancestry = entity instanceof DragonbornEntity dragonborn ? dragonborn.getAncestry() : DragonAncestry.EMBER;
            RaceFeatures.renderHead(matrices, vertexConsumers, light, this.getContextModel().head, this.race,
                    ancestry, this.getTexture(entity));
        }
    }
}

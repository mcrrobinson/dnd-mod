package mattonfire.dnd.entity.dragon;

import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.SkillChecks.MobSaveInfo;
import mattonfire.dnd.classes.SkillChecks.Study;
import mattonfire.dnd.entity.ModEntityTypes;
import net.minecraft.text.Text;

/**
 * The Ancient Red Dragon's saves (design 10, 5.5-5.9), registered with {@link MobSaveInfo} so its Study card lists
 * them; each call site rolls from its entry so the two can't drift apart.
 */
public final class AncientSaves {
    public static final int FIRE_BREATH_DC = 18;
    public static final int FRIGHTFUL_PRESENCE_DC = 17;
    public static final int WING_ATTACK_DC = 18;
    public static final int MAGMA_VENTS_DC = 15;
    public static final int TREMOR_DC = 15;
    public static final int VOLCANIC_GAS_DC = 13;

    public static final MobSaveInfo.Entry FIRE_BREATH = MobSaveInfo.register(ModEntityTypes.ANCIENT_RED_DRAGON,
            "save.dndclasses.ancient_fire_breath", Ability.DEX, FIRE_BREATH_DC, true,
            "save.dndclasses.effect.fire_breath");
    public static final MobSaveInfo.Entry FRIGHTFUL_PRESENCE = MobSaveInfo.register(ModEntityTypes.ANCIENT_RED_DRAGON,
            "save.dndclasses.frightful_presence", Ability.WIS, FRIGHTFUL_PRESENCE_DC, false,
            "save.dndclasses.effect.frightful_presence");
    public static final MobSaveInfo.Entry WING_ATTACK = MobSaveInfo.register(ModEntityTypes.ANCIENT_RED_DRAGON,
            "save.dndclasses.wing_attack", Ability.DEX, WING_ATTACK_DC, true, "save.dndclasses.effect.wing_attack");
    public static final MobSaveInfo.Entry MAGMA_VENTS = MobSaveInfo.register(ModEntityTypes.ANCIENT_RED_DRAGON,
            "save.dndclasses.magma_vents", Ability.DEX, MAGMA_VENTS_DC, true, "save.dndclasses.effect.half_damage");
    public static final MobSaveInfo.Entry TREMOR = MobSaveInfo.register(ModEntityTypes.ANCIENT_RED_DRAGON,
            "save.dndclasses.tremor", Ability.DEX, TREMOR_DC, false, "save.dndclasses.effect.tremor");
    public static final MobSaveInfo.Entry VOLCANIC_GAS = MobSaveInfo.register(ModEntityTypes.ANCIENT_RED_DRAGON,
            "save.dndclasses.volcanic_gas", Ability.CON, VOLCANIC_GAS_DC, false,
            "save.dndclasses.effect.volcanic_gas");

    private AncientSaves() {
    }

    /** Registers the entries above and the Study weakness line; call once after the entity types. */
    public static void register() {
        Study.registerWeakness(ModEntityTypes.ANCIENT_RED_DRAGON, Text.translatable("study.dndclasses.weakness.ancient"));
    }
}

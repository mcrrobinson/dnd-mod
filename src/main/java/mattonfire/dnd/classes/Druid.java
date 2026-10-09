package mattonfire.dnd.classes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.SkillNode;
import mattonfire.dnd.classes.Progression.Classes.DruidSkills;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;

/**
 * Druid mechanics: extra hearts from tamed animals, light based regen /
 * darkness hunger, and Wild Shape into animals the druid has killed and
 * unlocked in the bestiary.
 */
public class Druid {
    // Extra hearts from tamed animals.
    public static final UUID ANIMAL_HEARTS_UUID = UUID.fromString("5f6c1f0e-3a2b-4d8e-9a71-0d4b3c2e1a77");
    public static final int MAX_ANIMAL_HEARTS = 5;
    public static final int BEAST_BOND_MAX_ANIMAL_HEARTS = 10;

    // Light: regen at or above this light level, hunger at or below the dark one.
    public static final int REGEN_LIGHT_LEVEL = 10;
    public static final int DARK_LIGHT_LEVEL = 4;
    public static final float DARK_EXHAUSTION_PER_SECOND = 0.1F; // 4.0 = half a drumstick, so ~1 per 40s

    // Animal form. Forms come from the bestiary (learned by killing, unlocked at an
    // Attunement Table); the duration and tiers are DruidSkills.WILD_SHAPE's ranks.
    private static final String FORM_EXPIRY_KEY = "druidFormExpiry";
    /** The form picked with sneak + power-up; missing means a random unlocked form. */
    private static final String CHOSEN_FORM_KEY = "druidChosenForm";
    private static final String WILD_SHAPE = "druid.wild_shape";
    /** Creatures besides animals whose form a druid learns by killing one (the Owlbear). */
    public static final TagKey<EntityType<?>> DRUID_FORMS = TagKey.of(RegistryKeys.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "druid_forms"));

    public static boolean isDruid(PlayerEntity player) {
        return player instanceof PlayerEntityExt ext && ext.getDndClass() == DndCharacter.DRUID;
    }

    /** Called once a second for every player on the server. */
    public static void serverTick(ServerPlayerEntity player) {
        if (isDruid(player)) {
            updateAnimalHearts(player);
        } else {
            removeAnimalHearts(player);
        }

        // Done regardless of class so a form never outlives a relog or class change.
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        if (data.contains(FORM_EXPIRY_KEY) && player.getWorld().getTime() >= data.getLong(FORM_EXPIRY_KEY)) {
            revertForm(player);
        }
    }

    /** Clears everything the druid class applies; called on class change. */
    public static void onClassReset(PlayerEntity player) {
        removeAnimalHearts(player);
        if (player instanceof ServerPlayerEntity serverPlayer
                && ((IEntityDataSaver) serverPlayer).getPersistentData().contains(FORM_EXPIRY_KEY)) {
            revertForm(serverPlayer);
        }
    }

    // ---- Extra hearts ----

    /**
     * Counts the tamed animals (wolves, cats, parrots, horses...) owned by the
     * player that are currently loaded in the player's world.
     */
    public static int countTamedAnimals(ServerPlayerEntity player) {
        UUID owner = player.getUuid();
        return player.getWorld().getEntitiesByType(TypeFilter.instanceOf(LivingEntity.class),
                e -> e.isAlive() && e instanceof Tameable t && owner.equals(t.getOwnerUuid())).size();
    }

    public static void updateAnimalHearts(ServerPlayerEntity player) {
        int maxHearts = Progression.hasPassive(player, "druid.beast_bond") ? BEAST_BOND_MAX_ANIMAL_HEARTS
                : MAX_ANIMAL_HEARTS;
        int hearts = Math.min(countTamedAnimals(player), maxHearts);
        EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (attribute == null)
            return;

        double bonus = hearts * 2.0D;
        EntityAttributeModifier current = attribute.getModifier(ANIMAL_HEARTS_UUID);
        if (current != null && current.getValue() == bonus)
            return;

        // removeModifier, not tryRemoveModifier: the latter only removes persistent modifiers.
        attribute.removeModifier(ANIMAL_HEARTS_UUID);
        if (bonus > 0) {
            // Temporary so it isn't saved; it is recomputed every second instead.
            attribute.addTemporaryModifier(new EntityAttributeModifier(ANIMAL_HEARTS_UUID,
                    "Druid tamed animal hearts", bonus, EntityAttributeModifier.Operation.ADDITION));
        }
        clampHealth(player);
    }

    public static void removeAnimalHearts(PlayerEntity player) {
        EntityAttributeInstance attribute = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (attribute != null && attribute.getModifier(ANIMAL_HEARTS_UUID) != null) {
            attribute.removeModifier(ANIMAL_HEARTS_UUID);
            clampHealth(player);
        }
    }

    private static void clampHealth(PlayerEntity player) {
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // ---- Light ----

    /** Called once a second for druids on the server. */
    public static void lightTick(PlayerEntity player) {
        int light = player.getWorld().getLightLevel(player.getBlockPos());
        if (light >= REGEN_LIGHT_LEVEL) {
            if (player.getHealth() < player.getMaxHealth()) {
                player.heal(0.5F); // Heal even without food requirement
            }
            // Photosynthesis: half a drumstick every 3 seconds.
            if (player.age % 60 < 20 && Progression.hasPassive(player, "druid.photosynthesis")) {
                player.getHungerManager().add(1, 0.5F);
            }
        } else if (light <= DARK_LIGHT_LEVEL) {
            player.addExhaustion(DARK_EXHAUSTION_PER_SECOND);
        }
    }

    // ---- Animal form ----

    public static boolean isTransformed(PlayerEntity player) {
        return ((IEntityDataSaver) player).getPersistentData().contains(FORM_EXPIRY_KEY);
    }

    /**
     * The forms Wild Shape can take now: unlocked in the bestiary, still a living
     * entity type, and within the tier Wild Shape's rank allows.
     */
    public static List<String> availableForms(ClassProgress progress) {
        int rank = DruidSkills.WILD_SHAPE.rank(progress);
        List<String> forms = new ArrayList<>();
        for (String id : progress.bestiary) {
            if (progress.learned.contains(id) && progress.bestiaryRank(id) <= rank) {
                forms.add(id);
            }
        }
        return forms;
    }

    /**
     * Sneak + power-up with Wild Shape equipped picks the next unlocked form
     * (then "random"), without mana. Checked before the power-up's mana check.
     *
     * @return whether the key press was used up
     */
    public static boolean cycleForm(ServerPlayerEntity player) {
        if (!isDruid(player) || !player.isSneaking())
            return false;
        ClassProgress progress = Progression.current(player);
        SkillNode active = progress.activeNode();
        if (active == null || !active.id().equals(WILD_SHAPE))
            return false;

        List<String> forms = availableForms(progress);
        if (forms.isEmpty()) {
            player.sendMessage(Text.literal(progress.learned.isEmpty()
                    ? "Kill animals to learn their forms."
                    : "Unlock a learned form at an Attunement Table first.").formatted(Formatting.RED), true);
            return true;
        }
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        // Cycle: form 1, form 2, ..., random, form 1...
        int next = forms.indexOf(data.getString(CHOSEN_FORM_KEY)) + 1;
        if (!data.contains(CHOSEN_FORM_KEY)) {
            next = 0;
        }
        if (next == forms.size()) {
            data.remove(CHOSEN_FORM_KEY);
            player.sendMessage(Text.literal("Wild Shape form: random").formatted(Formatting.DARK_GREEN), true);
        } else {
            data.putString(CHOSEN_FORM_KEY, forms.get(next));
            player.sendMessage(Text.literal("Wild Shape form: " + Progression.entityName(forms.get(next)) + " ("
                    + (next + 1) + "/" + forms.size() + ")").formatted(Formatting.DARK_GREEN), true);
        }
        return true;
    }

    /**
     * Transforms the druid into their chosen form, or a random unlocked one.
     *
     * @return false if nothing happened, so no mana is spent.
     */
    public static boolean transform(ServerPlayerEntity player) {
        if (!FabricLoader.getInstance().isModLoaded("identity")) {
            player.sendMessage(Text.literal("Animal forms need the Identity mod.").formatted(Formatting.RED), true);
            return false;
        }

        ClassProgress progress = Progression.current(player);
        List<String> forms = availableForms(progress);
        if (forms.isEmpty()) {
            player.sendMessage(Text.literal(progress.learned.isEmpty()
                    ? "You haven't slain any animals to take the form of yet."
                    : "Unlock a learned form at an Attunement Table first.").formatted(Formatting.RED), true);
            return false;
        }

        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        String id = data.getString(CHOSEN_FORM_KEY);
        if (!forms.contains(id)) {
            id = forms.get(player.getRandom().nextInt(forms.size()));
        }
        EntityType<?> type = Registries.ENTITY_TYPE.get(new Identifier(id));
        if (!(type.create(player.getWorld()) instanceof LivingEntity form)) {
            player.sendMessage(Text.literal("You can't recall that animal's form.").formatted(Formatting.RED), true);
            return false;
        }

        if (!draylar.identity.api.PlayerIdentity.updateIdentity(player, null, form))
            return false;

        data.putLong(FORM_EXPIRY_KEY,
                player.getWorld().getTime() + DruidSkills.WILD_SHAPE.ticks(progress, "Duration"));
        player.sendMessage(Text.literal("You take the form of a ").append(form.getName()).append(".")
                .formatted(Formatting.DARK_GREEN), true);
        return true;
    }

    private static void revertForm(ServerPlayerEntity player) {
        ((IEntityDataSaver) player).getPersistentData().remove(FORM_EXPIRY_KEY);
        if (FabricLoader.getInstance().isModLoaded("identity")) {
            draylar.identity.api.PlayerIdentity.updateIdentity(player, null, null);
        }
    }
}

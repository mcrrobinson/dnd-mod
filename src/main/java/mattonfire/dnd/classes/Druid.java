package mattonfire.dnd.classes;

import java.util.UUID;

import mattonfire.dnd.classes.Progression.Progression;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
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
 * darkness hunger, and transforming into animals the druid has killed.
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

    // Animal form.
    public static final int FORM_DURATION_TICKS = 30 * 20;
    private static final int MAX_RECORDED_ANIMALS = 16;
    private static final String KILLED_ANIMALS_KEY = "druidKilledAnimals";
    private static final String FORM_EXPIRY_KEY = "druidFormExpiry";
    /** Creatures besides animals whose form a druid learns by killing one (the Owlbear). */
    public static final TagKey<EntityType<?>> DRUID_FORMS = TagKey.of(RegistryKeys.ENTITY_TYPE,
            new Identifier(DnDClasses.MOD_ID, "druid_forms"));

    public static void register() {
        // Remember every animal (and wild beast like the Owlbear) a druid kills.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof AnimalEntity) && !entity.getType().isIn(DRUID_FORMS))
                return;
            Entity attacker = damageSource.getAttacker();
            if (attacker instanceof ServerPlayerEntity player && isDruid(player)) {
                recordKill(player, entity.getType());
            }
        });
    }

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

    private static void recordKill(ServerPlayerEntity player, EntityType<?> type) {
        String id = Registries.ENTITY_TYPE.getId(type).toString();
        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        NbtList list = data.getList(KILLED_ANIMALS_KEY, NbtElement.STRING_TYPE);

        // Keep each animal once, most recent last.
        list.removeIf(e -> e.asString().equals(id));
        list.add(NbtString.of(id));
        while (list.size() > MAX_RECORDED_ANIMALS) {
            list.remove(0);
        }
        data.put(KILLED_ANIMALS_KEY, list);
    }

    /**
     * Transforms the druid into a random animal they've killed.
     *
     * @return false if nothing happened, so no mana is spent.
     */
    public static boolean transform(ServerPlayerEntity player) {
        if (!FabricLoader.getInstance().isModLoaded("identity")) {
            player.sendMessage(Text.literal("Animal forms need the Identity mod.").formatted(Formatting.RED), true);
            return false;
        }

        NbtCompound data = ((IEntityDataSaver) player).getPersistentData();
        NbtList list = data.getList(KILLED_ANIMALS_KEY, NbtElement.STRING_TYPE);
        if (list.isEmpty()) {
            player.sendMessage(Text.literal("You haven't slain any animals to take the form of yet.")
                    .formatted(Formatting.RED), true);
            return false;
        }

        String id = list.getString(player.getRandom().nextInt(list.size()));
        EntityType<?> type = Registries.ENTITY_TYPE.get(new Identifier(id));
        if (!(type.create(player.getWorld()) instanceof LivingEntity form)) {
            player.sendMessage(Text.literal("You can't recall that animal's form.").formatted(Formatting.RED), true);
            return false;
        }

        if (!draylar.identity.api.PlayerIdentity.updateIdentity(player, null, form))
            return false;

        data.putLong(FORM_EXPIRY_KEY, player.getWorld().getTime() + FORM_DURATION_TICKS);
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

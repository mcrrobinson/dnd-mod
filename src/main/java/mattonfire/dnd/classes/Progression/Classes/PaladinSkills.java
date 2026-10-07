package mattonfire.dnd.classes.Progression.Classes;

import static mattonfire.dnd.classes.Progression.SkillHelpers.effects;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Progression.ClassProgress;
import mattonfire.dnd.classes.Progression.ClassSkills;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.Progression.SkillNode;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;

/**
 * Paladin skills. Hellforged is checked in {@code PaladinNetherWeakness}, and
 * Aura of Protection in {@code PaladinAuraMixin} (it also protects other
 * players, whose damage hooks belong to their own class).
 *
 * Paladins only ignore potion effects, so the effects given here still apply.
 */
public class PaladinSkills extends ClassSkills {
    public static final String HELLFORGED = "paladin.hellforged";
    private static final String AURA_OF_PROTECTION = "paladin.aura_of_protection";

    private static final int UNDEAD_KILL_BONUS_XP = 3;
    private static final int GROUP_KILL_BONUS_XP = 1;
    private static final double GROUP_RADIUS = 16;

    private static final float SMITE_BONUS = 4.0F;
    private static final int SACRED_WEAPON_TICKS = 15 * 20;
    private static final int SACRED_FIRE_SECONDS = 4;
    private static final double AURA_RADIUS = 8;
    private static final float AURA_PROTECTION = 0.85F;
    private static final int SHIELD_TICKS = 15 * 20;
    private static final int ANGEL_TICKS = 20 * 20;
    private static final double ANGEL_RADIUS = 10;
    private static final int ANGEL_FIRE_SECONDS = 3;

    private static final UUID SHIELD_KNOCKBACK_UUID = UUID.fromString("8d0e6f43-2b7a-4c11-9e55-3f7a1c2d9b01");

    /** World time each player's timed buff ends at. */
    private static final Map<UUID, Long> SACRED_WEAPON = new HashMap<>();
    private static final Map<UUID, Long> DIVINE_SHIELD = new HashMap<>();
    private static final Map<UUID, Long> AVENGING_ANGEL = new HashMap<>();

    @Override
    public DndCharacter dndClass() {
        return DndCharacter.PALADIN;
    }

    @Override
    public List<SkillNode> nodes() {
        return List.of(
                active("paladin.lay_on_hands", "Lay on Hands", "Fully heal every player within 10 blocks.",
                        "minecraft:glistering_melon_slice", 9, 0, 1, 3),
                // Devotion
                passive("paladin.divine_smite", "Divine Smite", "Melee hits deal +4 damage to undead.",
                        "minecraft:golden_sword", 1, 2, 3, "paladin.lay_on_hands"),
                active("paladin.sacred_weapon", "Sacred Weapon",
                        "Strength for 15 seconds, and your melee hits set targets alight.", "minecraft:blaze_rod", 4,
                        1, 2, 2, "paladin.divine_smite"),
                passive(AURA_OF_PROTECTION, "Aura of Protection",
                        "You and players within 8 blocks take 15% less damage.", "minecraft:beacon", 1, 2, 1,
                        "paladin.sacred_weapon"),
                // Conquest
                passive(HELLFORGED, "Hellforged", "The Nether no longer weakens you.", "minecraft:netherite_ingot",
                        1, 0, 3, "paladin.lay_on_hands"),
                active("paladin.divine_shield", "Divine Shield",
                        "Absorption III and no knockback for 15 seconds.", "minecraft:shield", 5, 1, 0, 2,
                        HELLFORGED),
                passive("paladin.aura_of_courage", "Aura of Courage",
                        "You and players within 8 blocks can't be weakened or slowed.", "minecraft:golden_helmet", 1,
                        0, 1, "paladin.divine_shield"),
                active("paladin.avenging_angel", "Avenging Angel",
                        "Strength II, Regeneration II and Resistance II for 20 seconds; undead within 10 blocks burn.",
                        "minecraft:elytra", 9, 2, 1, 0, AURA_OF_PROTECTION, "paladin.aura_of_courage"));
    }

    @Override
    public void register() {
        // Divine Shield's knockback resistance is an attribute, so it's dropped
        // here even if the player changed class meanwhile (no secondTick then).
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (DIVINE_SHIELD.isEmpty() || server.getTicks() % 20 != 0)
                return;
            long now = server.getOverworld().getTime();
            DIVINE_SHIELD.entrySet().removeIf(entry -> {
                if (now < entry.getValue())
                    return false;
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
                if (player != null) {
                    EntityAttributeInstance knockback = player
                            .getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
                    if (knockback != null)
                        knockback.removeModifier(SHIELD_KNOCKBACK_UUID);
                }
                return true;
            });
        });
    }

    @Override
    public boolean activate(ServerPlayerEntity player, SkillNode node) {
        long now = ((ServerWorld) player.getWorld()).getTime();
        switch (node.id()) {
            case "paladin.sacred_weapon" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, SACRED_WEAPON_TICKS, 0));
                SACRED_WEAPON.put(player.getUuid(), now + SACRED_WEAPON_TICKS);
                effects(player, SoundEvents.ITEM_FIRECHARGE_USE, ParticleTypes.FLAME, 30);
            }
            case "paladin.divine_shield" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, SHIELD_TICKS, 2));
                EntityAttributeInstance knockback = player
                        .getAttributeInstance(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE);
                if (knockback != null && knockback.getModifier(SHIELD_KNOCKBACK_UUID) == null) {
                    knockback.addTemporaryModifier(new EntityAttributeModifier(SHIELD_KNOCKBACK_UUID,
                            "Divine Shield", 1.0, EntityAttributeModifier.Operation.ADDITION));
                }
                DIVINE_SHIELD.put(player.getUuid(), now + SHIELD_TICKS);
                effects(player, SoundEvents.ITEM_SHIELD_BLOCK, ParticleTypes.ENCHANTED_HIT, 30);
            }
            case "paladin.avenging_angel" -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, ANGEL_TICKS, 1));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, ANGEL_TICKS, 1));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, ANGEL_TICKS, 1));
                AVENGING_ANGEL.put(player.getUuid(), now + ANGEL_TICKS);
                burnUndead(player);
                effects(player, SoundEvents.BLOCK_BEACON_ACTIVATE, ParticleTypes.END_ROD, 40);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public int killXp(ServerPlayerEntity player, LivingEntity killed, DamageSource source) {
        int xp = isUndead(killed) ? UNDEAD_KILL_BONUS_XP : 0;
        if (killed instanceof Monster && playersNear(player, GROUP_RADIUS).size() > 1) {
            xp += GROUP_KILL_BONUS_XP; // Someone besides us is nearby
        }
        return xp;
    }

    @Override
    public float modifyDealtDamage(PlayerEntity player, ClassProgress progress, LivingEntity target,
            DamageSource source, float amount) {
        if (source.getSource() != player)
            return amount; // Melee only
        if (progress.hasPassive("paladin.divine_smite") && isUndead(target)) {
            amount += SMITE_BONUS;
        }
        if (running(SACRED_WEAPON, player)) {
            target.setOnFireFor(SACRED_FIRE_SECONDS);
        }
        return amount;
    }

    @Override
    public void secondTick(ServerPlayerEntity player, ClassProgress progress) {
        if (progress.hasPassive("paladin.aura_of_courage")) {
            for (PlayerEntity ally : playersNear(player, AURA_RADIUS)) {
                ally.removeStatusEffect(StatusEffects.WEAKNESS);
                ally.removeStatusEffect(StatusEffects.SLOWNESS);
            }
        }
        if (running(AVENGING_ANGEL, player)) {
            burnUndead(player);
        }
    }

    /**
     * Aura of Protection, called from {@code PaladinAuraMixin}: damage to a
     * player near a Paladin with the aura (or to that Paladin) is cut by 15%.
     * Several auras don't stack.
     */
    public static float applyAuraOfProtection(LivingEntity target, float amount) {
        if (!(target instanceof PlayerEntity player) || target.getWorld().isClient)
            return amount;
        for (PlayerEntity paladin : playersNear(player, AURA_RADIUS)) {
            if (Progression.classOf(paladin) == DndCharacter.PALADIN
                    && Progression.hasPassive(paladin, AURA_OF_PROTECTION)) {
                return amount * AURA_PROTECTION;
            }
        }
        return amount;
    }

    private static boolean isUndead(LivingEntity entity) {
        return entity.getGroup() == EntityGroup.UNDEAD;
    }

    /** Players within the radius, including this one. */
    private static List<PlayerEntity> playersNear(PlayerEntity player, double radius) {
        return player.getWorld().getEntitiesByClass(PlayerEntity.class, player.getBoundingBox().expand(radius),
                p -> p.isAlive() && !p.isSpectator());
    }

    private static void burnUndead(ServerPlayerEntity player) {
        for (LivingEntity mob : player.getWorld().getEntitiesByClass(LivingEntity.class,
                player.getBoundingBox().expand(ANGEL_RADIUS), e -> e.isAlive() && isUndead(e))) {
            mob.setOnFireFor(ANGEL_FIRE_SECONDS);
        }
    }

    /** Whether the player's timed buff is still going; forgets it once over. */
    private static boolean running(Map<UUID, Long> buffs, PlayerEntity player) {
        Long until = buffs.get(player.getUuid());
        if (until == null)
            return false;
        if (player.getWorld().getTime() < until)
            return true;
        buffs.remove(player.getUuid());
        return false;
    }
}

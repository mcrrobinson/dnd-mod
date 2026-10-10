package mattonfire.dnd.classes.SkillChecks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.Abilities.Skill;
import mattonfire.dnd.faction.TierEffects;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.village.VillageGossipType;
import net.minecraft.village.VillagerProfession;

/**
 * A Bard can sneak + right-click a villager with an empty hand to persuade it (a Persuasion check from
 * the character sheet against DC 12),
 * once per villager per day. Success earns reputation with that villager, which lowers its prices
 * the same way curing a zombie villager does: a natural 20 earns twice as much. A natural 1 offends
 * it and its prices go up. Gossip spreads, so the villager's neighbours hear about it too.
 */
public final class Persuasion {
    public static final int DC = 12;
    private static final int SUCCESS_REPUTATION = 40;
    private static final int CRITICAL_REPUTATION = 80;
    private static final int FUMBLE_REPUTATION = 25;

    /** "villager uuid/player uuid" to the day the player last tried to persuade that villager. */
    private static final Map<String, Long> LAST_TRIED = new HashMap<>();

    /** Called on disconnect: only today's attempts still matter. */
    public static void pruneOldDays(long today) {
        LAST_TRIED.values().removeIf(day -> day != today);
    }

    private Persuasion() {
    }

    /**
     * The DC to persuade {@code target}: {@link #DC} shifted by the player's standing with its faction
     * (Hostile +5, Unfriendly +3, Friendly -2, Honored -4, Exalted -6). Dialogue checks use the same shift.
     */
    public static int dc(PlayerEntity player, Entity target) {
        return dc(player, target, DC);
    }

    public static int dc(PlayerEntity player, Entity target, int base) {
        return Math.max(1, base + TierEffects.dcShift(player, target));
    }

    static void register() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // The client sends interact-at first (hitResult set), then plain interact: handle only the latter
            if (world.isClient || hitResult != null || hand != Hand.MAIN_HAND
                    || !(entity instanceof VillagerEntity villager)
                    || !player.isSneaking() || !player.getMainHandStack().isEmpty()
                    || D20.classOf(player) != DndCharacter.BARD) {
                return ActionResult.PASS;
            }
            VillagerProfession profession = villager.getVillagerData().getProfession();
            if (villager.isBaby() || villager.isSleeping() || villager.hasCustomer()
                    || profession == VillagerProfession.NONE || profession == VillagerProfession.NITWIT) {
                return ActionResult.PASS;
            }

            long day = world.getTimeOfDay() / 24000L;
            String key = villager.getUuidAsString() + "/" + player.getUuidAsString();
            Long lastDay = LAST_TRIED.get(key);
            if (lastDay != null && lastDay == day) {
                player.sendMessage(Text.translatable("skill.dndclasses.persuasion.already"), true);
                villager.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, villager.getSoundPitch());
                return ActionResult.SUCCESS;
            }
            LAST_TRIED.put(key, day);

            // Standing with the target's faction shifts the DC (no faction: no shift).
            D20.Roll roll = SkillCheck.builder(player, Skill.PERSUASION, dc(player, villager), "villager")
                    .label(D20.PERSUASION).roll();
            switch (roll.outcome()) {
                case CRITICAL, SUCCESS -> {
                    boolean critical = roll.outcome() == D20.Outcome.CRITICAL;
                    villager.getGossip().startGossip(player.getUuid(), VillageGossipType.MINOR_POSITIVE,
                            critical ? CRITICAL_REPUTATION : SUCCESS_REPUTATION);
                    world.sendEntityStatus(villager, EntityStatuses.ADD_VILLAGER_HEART_PARTICLES);
                    villager.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1.0f, villager.getSoundPitch());
                    D20.show(player, roll, Text.translatable(critical
                            ? "skill.dndclasses.persuasion.critical"
                            : "skill.dndclasses.persuasion.success"));
                }
                case FAILURE -> {
                    villager.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, villager.getSoundPitch());
                    D20.show(player, roll, Text.translatable("skill.dndclasses.persuasion.failure"));
                }
                case FUMBLE -> {
                    villager.getGossip().startGossip(player.getUuid(), VillageGossipType.MINOR_NEGATIVE,
                            FUMBLE_REPUTATION);
                    world.sendEntityStatus(villager, EntityStatuses.ADD_VILLAGER_ANGRY_PARTICLES);
                    villager.playSound(SoundEvents.ENTITY_VILLAGER_NO, 1.0f, villager.getSoundPitch() * 0.8f);
                    D20.show(player, roll, Text.translatable("skill.dndclasses.persuasion.fumble"));
                }
            }
            return ActionResult.SUCCESS;
        });
    }
}

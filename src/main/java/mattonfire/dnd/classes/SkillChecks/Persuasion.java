package mattonfire.dnd.classes.SkillChecks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DndCharacter;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.village.VillageGossipType;
import net.minecraft.village.VillagerProfession;

/**
 * A Bard can sneak + right-click a villager with an empty hand to persuade it (d20 + 5 against DC 12),
 * once per villager per day. Success earns reputation with that villager, which lowers its prices
 * the same way curing a zombie villager does: a natural 20 earns twice as much. A natural 1 offends
 * it and its prices go up. Gossip spreads, so the villager's neighbours hear about it too.
 */
public final class Persuasion {
    /** Bard: Charisma +3 and expertise in Persuasion. */
    public static final int BARD_MODIFIER = 5;
    public static final int DC = 12;
    private static final int SUCCESS_REPUTATION = 40;
    private static final int CRITICAL_REPUTATION = 80;
    private static final int FUMBLE_REPUTATION = 25;

    /** "villager uuid/player uuid" to the day the player last tried to persuade that villager. */
    private static final Map<String, Long> LAST_TRIED = new HashMap<>();

    private Persuasion() {
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

            D20.Roll roll = D20.check(player, D20.Skill.PERSUASION, BARD_MODIFIER, DC);
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

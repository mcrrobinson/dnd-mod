package mattonfire.dnd.magic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import mattonfire.dnd.classes.DnDClasses;
import mattonfire.dnd.classes.DndCharacter;
import mattonfire.dnd.classes.ManaManager;
import mattonfire.dnd.classes.Abilities.Ability;
import mattonfire.dnd.classes.Abilities.RollKind;
import mattonfire.dnd.classes.Progression.Progression;
import mattonfire.dnd.classes.SkillChecks.D20;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Remove Curse (design section 3.6): breaks a cursed bond. The item stays cursed and identified, but it's no
 * longer attuned and can be put down.
 * <ul>
 * <li>A Cleric sneak + right-clicks a player with an empty hand, or sneak + uses an empty hand on the air to
 * target themselves: {@value #MANA_COST} mana pips, d20 + {@value #CLERIC_BONUS} against the item's DC. A failure
 * spends the mana and the Cleric can retry after {@value #RETRY_SECONDS} s.</li>
 * <li>A Scroll of Remove Curse, read on yourself: succeeds on its own up to Rare, d20 + {@value #SCROLL_BONUS}
 * above that.</li>
 * </ul>
 */
public final class RemoveCurse {
    /** Sent by the client for a Cleric's sneak + use on the air with an empty hand (self-target). */
    public static final Identifier C2S_SELF = new Identifier(DnDClasses.MOD_ID, "remove_curse_self");

    public static final int MANA_COST = 6;
    /** The Cleric's spellcasting bonus (Wisdom). */
    public static final int CLERIC_BONUS = 5;
    public static final int SCROLL_BONUS = 3;
    public static final int RETRY_SECONDS = 60;

    /** Cleric UUID to the server tick they can try again after a failure. */
    private static final Map<UUID, Integer> RETRY_AT = new HashMap<>();

    private RemoveCurse() {
    }

    static void register() {
        ServerPlayNetworking.registerGlobalReceiver(C2S_SELF, (server, player, handler, buf, sender) -> server
                .execute(() -> {
                    if (player.isSneaking() && player.getMainHandStack().isEmpty())
                        cleric(player, player);
                }));
    }

    /** The DC to break a curse on an item of this tier. */
    public static int dc(MagicTier tier) {
        return switch (tier) {
            case COMMON, UNCOMMON -> 10;
            case RARE -> 13;
            case VERY_RARE -> 16;
            case LEGENDARY -> 19;
        };
    }

    private static MagicTier tierOf(AttunementSnapshot.Bond bond) {
        MagicTier tier = MagicTier.byId(bond.tier());
        return tier != null ? tier : MagicTier.UNCOMMON;
    }

    public static boolean isCleric(ServerPlayerEntity player) {
        return Progression.classOf(player) == DndCharacter.CLERIC;
    }

    /**
     * A Cleric casts Remove Curse on {@code target} (themselves or another player).
     *
     * @return whether the cast happened (false: not a Cleric, so the click is someone else's to handle)
     */
    public static boolean cleric(ServerPlayerEntity cleric, ServerPlayerEntity target) {
        if (!isCleric(cleric))
            return false;
        AttunementSnapshot.Bond bond = Attunement.cursedBond(target);
        if (bond == null) {
            cleric.sendMessage(Text.translatable(cleric == target ? "magic.dndclasses.remove_curse.none_self"
                    : "magic.dndclasses.remove_curse.none", target.getDisplayName()).formatted(Formatting.GRAY), true);
            return true;
        }
        int now = cleric.getServer().getTicks();
        Integer retryAt = RETRY_AT.get(cleric.getUuid());
        if (retryAt != null && now < retryAt) {
            cleric.sendMessage(Text.translatable("magic.dndclasses.remove_curse.retry", (retryAt - now + 19) / 20)
                    .formatted(Formatting.RED), true);
            return true;
        }
        if (ManaManager.getMana(cleric) < MANA_COST && !cleric.isCreative()) {
            cleric.sendMessage(Text.translatable("magic.dndclasses.remove_curse.mana", MANA_COST)
                    .formatted(Formatting.RED), true);
            return true;
        }
        if (!cleric.isCreative()) {
            ManaManager.setMana(cleric, ManaManager.getMana(cleric) - MANA_COST);
            ManaManager.sync(cleric);
        }
        int dc = dc(tierOf(bond));
        D20.Roll roll = D20.roll(cleric).label(D20.REMOVE_CURSE).ability(Ability.WIS).kind(RollKind.CHECK)
                .modifier(CLERIC_BONUS).dc(dc).roll();
        boolean success = roll.outcome().succeeded();
        Text detail = success ? Text.translatable("magic.dndclasses.remove_curse.success_hud", bond.name())
                : Text.translatable("magic.dndclasses.remove_curse.failure_hud");
        D20.show(cleric, roll, detail);
        if (target != cleric)
            D20.show(target, roll, detail);
        if (success) {
            RETRY_AT.remove(cleric.getUuid());
            free(target, bond);
            if (target != cleric)
                cleric.sendMessage(Text.translatable("magic.dndclasses.remove_curse.freed_other",
                        target.getDisplayName(), bond.name()).formatted(Formatting.AQUA), false);
        } else {
            RETRY_AT.put(cleric.getUuid(), now + RETRY_SECONDS * 20);
            cleric.sendMessage(Text.translatable("magic.dndclasses.remove_curse.failed", RETRY_SECONDS)
                    .formatted(Formatting.RED), false);
        }
        return true;
    }

    /**
     * Reads a Scroll of Remove Curse on yourself.
     *
     * @return whether the scroll was used (false: no curse to break, the scroll is kept)
     */
    public static boolean scroll(ServerPlayerEntity player) {
        AttunementSnapshot.Bond bond = Attunement.cursedBond(player);
        if (bond == null) {
            player.sendMessage(Text.translatable("magic.dndclasses.remove_curse.none_self")
                    .formatted(Formatting.GRAY), true);
            return false;
        }
        MagicTier tier = tierOf(bond);
        boolean success;
        if (tier.ordinal() <= MagicTier.RARE.ordinal()) {
            success = true;
        } else {
            D20.Roll roll = D20.roll(player).label(D20.REMOVE_CURSE).ability(Ability.WIS).kind(RollKind.CHECK)
                    .modifier(SCROLL_BONUS).dc(dc(tier)).roll();
            success = roll.outcome().succeeded();
            D20.show(player, roll, success ? Text.translatable("magic.dndclasses.remove_curse.success_hud", bond.name())
                    : Text.translatable("magic.dndclasses.remove_curse.failure_hud"));
        }
        if (success) {
            free(player, bond);
        } else {
            player.sendMessage(Text.translatable("magic.dndclasses.remove_curse.scroll_failed")
                    .formatted(Formatting.RED), false);
        }
        return true;
    }

    private static void free(ServerPlayerEntity target, AttunementSnapshot.Bond bond) {
        Attunement.breakCurse(target, bond.uuid());
        target.sendMessage(Text.translatable("magic.dndclasses.remove_curse.freed", bond.name())
                .formatted(Formatting.AQUA), false);
        target.getWorld().playSound(null, target.getBlockPos(), SoundEvents.BLOCK_BEACON_DEACTIVATE,
                SoundCategory.PLAYERS, 1.0F, 1.2F);
        target.getWorld().spawnParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 1.0, target.getZ(), 20,
                0.4, 0.6, 0.4, 0.05);
        DnDClasses.LOGGER.info("[Curse] {} freed from {}", target.getEntityName(), bond.name().getString());
    }
}

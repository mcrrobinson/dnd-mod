package mattonfire.dnd.classes.Misc;

import draylar.identity.api.PlayerHostility;
import draylar.identity.api.PlayerIdentity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * The only class that touches the optional Identity mod (mixins included). Callers must check
 * {@link BloodHunterControl#isIdentityLoaded()} first so this class is never
 * loaded when Identity is absent.
 */
final class BloodHunterIdentityCompat {
    private BloodHunterIdentityCompat() {
    }

    /**
     * Turns the player into the given entity. Unlike setting the identity field
     * directly, updateIdentity fires Identity's swap event, refreshes the
     * player's hitbox and syncs the new shape to the player and everyone
     * tracking them.
     */
    static boolean morph(ServerPlayerEntity player, LivingEntity identity) {
        return PlayerIdentity.updateIdentity(player, null, identity);
    }

    static void unmorph(ServerPlayerEntity player) {
        PlayerIdentity.updateIdentity(player, null, null);
    }

    static boolean hasForm(PlayerEntity player) {
        return PlayerIdentity.getIdentity(player) != null;
    }

    static boolean hasHostility(PlayerEntity player) {
        return PlayerHostility.hasHostility(player);
    }
}

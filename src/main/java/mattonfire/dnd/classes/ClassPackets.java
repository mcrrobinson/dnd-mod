package mattonfire.dnd.classes;

import net.minecraft.util.Identifier;

/**
 * The class system's packet ids: specials' effects, mana sync and the class picker. The ids (and their
 * namespaces) are part of the network protocol, so they stay as they are.
 */
public final class ClassPackets {
        private ClassPackets() {
        }

        public static final Identifier C2S_DOUBLEJUMP_EFFECTS_REQUEST_PACKET_ID = Identifier.of("doublejump",
                        "request_doublejump_effects");
        public static final Identifier S2C_DOUBLEJUMP_EFFECTS_PACKET_ID = Identifier.of("doublejump",
                        "play_doublejump_effects");

        public static final Identifier C2S_POWERUP_EFFECTS_REQUEST_PACKET_ID = Identifier.of("powerup",
                        "request_powerup_effects");
        public static final Identifier S2C_POWERUP_EFFECTS_PACKET_ID = Identifier.of("powerup",
                        "play_powerup_effects");

        public static final Identifier S2C_LUNGE_EFFECTS_PACKET_ID = Identifier.of("lunge", "play_lunge_effects");

        public static final Identifier S2C_SYNC_MANA = Identifier.of("mana",
                        "sync_mana");

        public static final Identifier S2C_WARLOCK_FIREBREATH = Identifier.of(DnDClasses.MOD_ID, "warlock_firebreath");
        public static final Identifier S2C_WIZARD_EFFECTS_PACKET_ID = Identifier.of(DnDClasses.MOD_ID, "wizard_powerup");

        public static final Identifier C2S_CLASS_PICK_PACKET_ID = Identifier.of("classpick", "class_pick");
        public static final Identifier S2C_CLASS_QUERY_PACKET_ID = Identifier.of("classpick", "class_query");
        public static final Identifier S2C_APPROVE_CLASS_PICK_PACKET_ID = Identifier.of("classpick",
                        "approve_class_pick");
}

package mattonfire.dnd.particle;

import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModParticles {
    public static final DefaultParticleType TRANSLUCENT_FLAME = FabricParticleTypes.simple();
    public static final DefaultParticleType DRAGON_FLAME = FabricParticleTypes.simple();
    /** The Frost Drake's breath: flies like DRAGON_FLAME (same drag and age) */
    public static final DefaultParticleType DRAGON_FROST = FabricParticleTypes.simple();
    /** Over an NPC the DM speaks through ({@code /dm say}). */
    public static final DefaultParticleType SPEECH_BUBBLE = FabricParticleTypes.simple(true);
    // Dragon flame puffs keep this much of their velocity each tick and last about this many ticks;
    // FireBreath works out launch speeds from them
    public static final float DRAGON_FLAME_DRAG = 0.9F;
    public static final int DRAGON_FLAME_AGE = 12;

    public static void registerParticles() {
        Registry.register(Registries.PARTICLE_TYPE, new Identifier(DnDClasses.MOD_ID, "translucent_flame"),
                TRANSLUCENT_FLAME);
        Registry.register(Registries.PARTICLE_TYPE, new Identifier(DnDClasses.MOD_ID, "dragon_flame"),
                DRAGON_FLAME);
        Registry.register(Registries.PARTICLE_TYPE, new Identifier(DnDClasses.MOD_ID, "dragon_frost"),
                DRAGON_FROST);
        Registry.register(Registries.PARTICLE_TYPE, new Identifier(DnDClasses.MOD_ID, "speech_bubble"),
                SPEECH_BUBBLE);
    }
}
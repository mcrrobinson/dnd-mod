package mattonfire.dnd.particle;

import mattonfire.dnd.classes.DnDClasses;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModParticles {
    public static final DefaultParticleType TRANSLUCENT_FLAME = FabricParticleTypes.simple();

    public static void registerParticles() {
        Registry.register(Registries.PARTICLE_TYPE, new Identifier(DnDClasses.MOD_ID, "translucent_flame"),
                TRANSLUCENT_FLAME);
    }
}
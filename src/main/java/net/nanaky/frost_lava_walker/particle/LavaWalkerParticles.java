// net.nanaky.frost_lava_walker.particle.LavaWalkerParticles.java
package net.nanaky.frost_lava_walker.particle;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.core.Registry;

public class LavaWalkerParticles {
    public static final SimpleParticleType LAVA_POP =
        FabricParticleTypes.simple();

    public static void register() {
        Registry.register(
            BuiltInRegistries.PARTICLE_TYPE,
            Identifier.fromNamespaceAndPath("frost_lava_walker", "lava_pop"),
            LAVA_POP
        );
    }
}
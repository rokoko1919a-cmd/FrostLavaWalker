package net.nanaky.frost_lava_walker.util;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class Particle_Compat {
    private static ParticleOptions LAVA_POP_CACHE = null;

    public static ParticleOptions lavaPop() {
        if (LAVA_POP_CACHE != null) {
            return LAVA_POP_CACHE;
        } else {
            Identifier id = Identifier.fromNamespaceAndPath("eg_particle_interactions", "lava_pop");
            Optional<Holder.Reference<ParticleType<?>>> type = BuiltInRegistries.PARTICLE_TYPE.get(id);
            if (type.isPresent()) {
                Object var3 = ((Holder.Reference)type.get()).value();
                if (var3 instanceof SimpleParticleType) {
                    SimpleParticleType simple = (SimpleParticleType)var3;
                    LAVA_POP_CACHE = simple;
                    return LAVA_POP_CACHE;
                }
            }

            LAVA_POP_CACHE = ParticleTypes.SMALL_FLAME;
            return LAVA_POP_CACHE;
        }
    }
}

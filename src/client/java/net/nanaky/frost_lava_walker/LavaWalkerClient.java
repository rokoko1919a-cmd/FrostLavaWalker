package net.nanaky.frost_lava_walker;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.nanaky.frost_lava_walker.config.ConfigManager;
import net.nanaky.frost_lava_walker.network.SpawnParticlePacket;
import net.nanaky.frost_lava_walker.particle.LavaPopParticle;
import net.nanaky.frost_lava_walker.particle.LavaWalkerParticles;

public class LavaWalkerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ParticleProviderRegistry.getInstance().register(
            LavaWalkerParticles.LAVA_POP, LavaPopParticle.Provider::new
        );

        ClientPlayNetworking.registerGlobalReceiver(SpawnParticlePacket.TYPE, (packet, context) -> {
            context.client().execute(() -> {
                ClientLevel level = Minecraft.getInstance().level;
                if (level != null) {
                    level.addParticle(LavaWalkerParticles.LAVA_POP,
                        packet.x(), packet.y(), packet.z(),
                        0, 0.1, 0);
                }
            });
        });
    }
}
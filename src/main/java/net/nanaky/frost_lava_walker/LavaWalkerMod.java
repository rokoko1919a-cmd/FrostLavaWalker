package net.nanaky.frost_lava_walker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.nanaky.frost_lava_walker.enchantment.LavaWalkerEnchantmentLogic;
import net.nanaky.frost_lava_walker.network.SpawnParticlePacket;
import net.nanaky.frost_lava_walker.particle.LavaWalkerParticles;
import net.nanaky.frost_lava_walker.util.BlockConversionScheduler;

public class LavaWalkerMod implements ModInitializer {
    public static final String MOD_ID = "frost_lava_walker";

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            BlockConversionScheduler.tick(server.overworld());
        });
        LavaWalkerParticles.register();
        SpawnParticlePacket.register();

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerLevel level)) return;
            if (!BlockConversionScheduler.isTracked(pos)) return;

            BlockConversionScheduler.cancel(pos);
            level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
            LavaWalkerEnchantmentLogic.killDroppedItems(level, pos);
        });
    }
}
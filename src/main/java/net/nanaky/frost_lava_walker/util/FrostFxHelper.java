package net.nanaky.frost_lava_walker.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class FrostFxHelper {

    private static final int FREEZE_DETECT_WINDOW = 2;

    // entityId -> (BlockPos -> gametime when it was last seen as water)
    private static final Map<Integer, Map<BlockPos, Long>> waterTimestamps = new HashMap<>();

    public static void onEntityStep(ServerLevel level, LivingEntity entity) {
        int enchantLevel;
        try {
            enchantLevel = EnchantmentHelper.getEnchantmentLevel(
                level.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.FROST_WALKER),
                entity
            );
        } catch (Exception e) {
            System.err.println("[LavaWalker] enchant lookup failed: " + e);
            return;
        }
        if (enchantLevel <= 0) {
            waterTimestamps.remove(entity.getId());
            return;
        }

        long now = level.getGameTime();
        BlockPos center = entity.blockPosition();
        int radius = 2 + enchantLevel;

        Map<BlockPos, Long> timestamps = waterTimestamps.computeIfAbsent(
            entity.getId(), k -> new HashMap<>()
        );

        // Scan radius for water and ice
        for (BlockPos candidate : BlockPos.betweenClosed(
                center.offset(-radius, -1, -radius),
                center.offset(radius, -1, radius))) {

            if (center.distSqr(candidate) > (radius + 0.5) * (radius + 0.5)) continue;

            BlockPos immutable = candidate.immutable();

            if (level.getFluidState(immutable).is(Fluids.WATER)
                    && level.getFluidState(immutable).isSource()) {
                // Record or refresh the last-seen-as-water timestamp
                timestamps.put(immutable, now);

            } else if (level.getBlockState(immutable).is(Blocks.FROSTED_ICE)
                    || level.getBlockState(immutable).is(Blocks.ICE)) {
                Long seenAt = timestamps.get(immutable);
                if (seenAt != null && (now - seenAt) <= FREEZE_DETECT_WINDOW) {
                    // Was water recently, is now ice — confirmed freeze
                    spawnIceFx(level, immutable);
                    timestamps.remove(immutable); // don't fire again for this block
                }
            }
        }

        // Evict stale entries (older than window, no longer water or ice)
        Iterator<Map.Entry<BlockPos, Long>> it = timestamps.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Long> entry = it.next();
            if (now - entry.getValue() > FREEZE_DETECT_WINDOW) {
                it.remove();
            }
        }
    }

    public static void onEntityRemoved(LivingEntity entity) {
        waterTimestamps.remove(entity.getId());
    }

    private static void spawnIceFx(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        level.sendParticles(
            ParticleTypes.SNOWFLAKE,
            center.x, center.y + 0.4, center.z,
            3, 0.3, 0.05, 0.3, 0.05
        );
        level.playSound(
            null, pos,
            SoundEvents.SNOW_GOLEM_HURT,
            SoundSource.BLOCKS,
            0.15f, 1.0f
        );
    }
}
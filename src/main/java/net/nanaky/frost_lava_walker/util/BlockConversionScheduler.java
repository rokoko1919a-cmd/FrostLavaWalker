package net.nanaky.frost_lava_walker.util;

import net.nanaky.frost_lava_walker.enchantment.LavaWalkerEnchantmentLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlockConversionScheduler {

    private record ScheduledRevert(BlockState revertTo, long revertAtTick, LivingEntity entity) {}

    // LinkedHashMap preserves insertion order so tick() can break early
    private static final LinkedHashMap<BlockPos, ScheduledRevert> QUEUE = new LinkedHashMap<>();

    public static void schedule(BlockPos pos, BlockState revertTo, int delayTicks, long currentGameTime, LivingEntity entity) {
        // Overwrite any existing entry for this pos (e.g. grace period renewal)
        QUEUE.put(pos, new ScheduledRevert(revertTo, currentGameTime + delayTicks, entity));
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        LavaWalkerEnchantmentLogic.pruneConversionCooldown(now);
        List<Map.Entry<BlockPos, ScheduledRevert>> due = new ArrayList<>();
        var iterator = QUEUE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, ScheduledRevert> entry = iterator.next();
            if (entry.getValue().revertAtTick() > now) continue;
            due.add(entry);
            iterator.remove();
        }
        for (Map.Entry<BlockPos, ScheduledRevert> entry : due) {
            LavaWalkerEnchantmentLogic.revertBlock(
                level,
                entry.getKey(),
                entry.getValue().revertTo(),
                entry.getValue().entity()
            );
        }
        if (!QUEUE.isEmpty()) {
            LavaWalkerPersistentState state = LavaWalkerPersistentState.get(level);
            for (BlockPos pos : QUEUE.keySet()) {
                state.startTimes.putIfAbsent(pos, System.currentTimeMillis());
            }
            state.setDirty();
        }
    }

    public static boolean isTracked(BlockPos pos) {
        return QUEUE.containsKey(pos);
    }

    public static void cancel(BlockPos pos) {
        QUEUE.remove(pos);
    }
}
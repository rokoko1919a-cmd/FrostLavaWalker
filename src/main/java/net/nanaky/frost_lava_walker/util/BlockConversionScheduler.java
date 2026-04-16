package net.nanaky.frost_lava_walker.util;

import net.nanaky.frost_lava_walker.enchantment.LavaWalkerEnchantmentLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BlockConversionScheduler {

    private record DimPos(ResourceKey<Level> dim, BlockPos pos) {}  // ← was missing

    private record ScheduledRevert(BlockState revertTo, long revertAtTick, LivingEntity entity) {}

    private static final LinkedHashMap<DimPos, ScheduledRevert> QUEUE = new LinkedHashMap<>();

    public static void schedule(BlockPos pos, BlockState revertTo, int delayTicks,
                                long currentGameTime, LivingEntity entity,
                                ServerLevel level) {
        DimPos key = new DimPos(level.dimension(), pos.immutable());
        QUEUE.put(key, new ScheduledRevert(revertTo, currentGameTime + delayTicks, entity));
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        ResourceKey<Level> dim = level.dimension();
        LavaWalkerEnchantmentLogic.pruneConversionCooldown(now);

        List<Map.Entry<DimPos, ScheduledRevert>> due = new ArrayList<>();
        var iterator = QUEUE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<DimPos, ScheduledRevert> entry = iterator.next();
            if (!entry.getKey().dim().equals(dim)) continue;
            if (entry.getValue().revertAtTick() > now) continue;
            due.add(entry);
            iterator.remove();
        }

        for (Map.Entry<DimPos, ScheduledRevert> entry : due) {
            LavaWalkerEnchantmentLogic.revertBlock(
                level,
                entry.getKey().pos(),
                entry.getValue().revertTo(),
                entry.getValue().entity()
            );
        }

        if (!QUEUE.isEmpty()) {
            LavaWalkerPersistentState state = LavaWalkerPersistentState.get(level);
            QUEUE.entrySet().stream()
                .filter(e -> e.getKey().dim().equals(dim))
                .forEach(e -> state.startTimes.putIfAbsent(e.getKey().pos(), System.currentTimeMillis()));
            state.setDirty();
        }
    }

    public static boolean isTracked(BlockPos pos, ServerLevel level) {
        return QUEUE.containsKey(new DimPos(level.dimension(), pos));
    }

    public static void cancel(BlockPos pos, ServerLevel level) {
        QUEUE.remove(new DimPos(level.dimension(), pos));
    }
}
package net.nanaky.frost_lava_walker.enchantment;

import net.nanaky.frost_lava_walker.util.Particle_Compat;
import net.nanaky.frost_lava_walker.particle.LavaWalkerParticles;
import net.nanaky.frost_lava_walker.util.BlockConversionScheduler;
import net.nanaky.frost_lava_walker.util.LavaWalkerPersistentState;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

public class LavaWalkerEnchantmentLogic {

    private static final int GILDED_INITIAL_TICKS  = 3;      // Quick Gilded Flash Before Settling
    private static final int BLACKSTONE_MAIN_TICKS  = 40;    // Main Safe State
    private static final int GILDED_WARNING_TICKS   = 15;    // First Warning + FX
    private static final int MAGMA_SHORT_TICKS      = 10;    // Second Warning
    
    private static final int TOTAL_LIFECYCLE_TICKS  =        // Allows each block to revert independently
        GILDED_INITIAL_TICKS + BLACKSTONE_MAIN_TICKS + GILDED_WARNING_TICKS + MAGMA_SHORT_TICKS;

    private static final int CONVERSION_COOLDOWN_TICKS =     // Allows a short cooldown window for LAVA
        TOTAL_LIFECYCLE_TICKS + 20;

    static final java.util.Map<BlockPos, Long> CONVERSION_COOLDOWN = new java.util.HashMap<>();
    static final java.util.Map<BlockPos, Long> CONVERSION_START = new java.util.HashMap<>();

    private static BlockState getLavaBlockState(BlockPos pos, ServerLevel level) {
        return Blocks.LAVA.defaultBlockState();
    }

    public static void pruneConversionCooldown(long now) {
        CONVERSION_COOLDOWN.entrySet().removeIf(e -> now - e.getValue() >= CONVERSION_COOLDOWN_TICKS);
        CONVERSION_START.entrySet().removeIf(e -> now - e.getValue() >= TOTAL_LIFECYCLE_TICKS + 20);
    }

    // On server start / player login, call this for the level:
    public static void resumePendingBlocks(ServerLevel level) {
        LavaWalkerPersistentState state = LavaWalkerPersistentState.get(level);
        long now = level.getGameTime();
        long nowMs = System.currentTimeMillis();

        state.startTimes.entrySet().removeIf(entry -> {
            BlockPos pos = entry.getKey();
            long startMs = entry.getValue();
            long elapsedTicks = (nowMs - startMs) / 50L; // 50ms per tick

            if (elapsedTicks >= TOTAL_LIFECYCLE_TICKS) {
                // Lifecycle already over, just restore lava
                level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
                LavaWalkerEnchantmentLogic.killDroppedItems(level, pos);
                return true; // remove from map
            }

            // Re-anchor start in game ticks for the in-memory map
            long fakestart = now - elapsedTicks;
            LavaWalkerEnchantmentLogic.CONVERSION_START.put(pos, fakestart);

            // Figure out current phase and re-queue from here
            BlockState current = level.getBlockState(pos);
            reQueueFromCurrentState(level, pos, current, fakestart, now);
            return false;
        });

        state.setDirty();
    }

    private static void reQueueFromCurrentState(ServerLevel level, BlockPos pos, BlockState current, long start, long now) {
        // work out which phase it's in and schedule the next transition
        if (current.is(Blocks.GILDED_BLACKSTONE)) {
            long target = start + GILDED_INITIAL_TICKS;
            BlockConversionScheduler.schedule(pos, Blocks.BLACKSTONE.defaultBlockState(),
                (int) Math.max(1, target - now), now, null);
        } else if (current.is(Blocks.BLACKSTONE)) {
            long target = start + GILDED_INITIAL_TICKS + BLACKSTONE_MAIN_TICKS;
            BlockConversionScheduler.schedule(pos, Blocks.GILDED_BLACKSTONE.defaultBlockState(),
                (int) Math.max(1, target - now), now, null);
        } else if (current.is(Blocks.MAGMA_BLOCK)) {
            long target = start + GILDED_INITIAL_TICKS + BLACKSTONE_MAIN_TICKS + GILDED_WARNING_TICKS;
            BlockConversionScheduler.schedule(pos, getLavaBlockState(pos, level),
                (int) Math.max(1, target - now), now, null);
        }
        // if it's already lava or air, nothing to do
    }

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
        if (enchantLevel <= 0) return;

        // Only act when entity is walking ON lava, not swimming IN it
        if (entity.isInLava()) return;

        BlockPos center = entity.blockPosition();
        int radius = enchantLevel;
        long now = level.getGameTime();

        for (BlockPos candidate : BlockPos.betweenClosed(
                center.offset(-radius, -1, -radius),
                center.offset(radius, -1, radius))) {

            if (center.distSqr(candidate) > (radius + 0.5) * (radius + 0.5)) continue;

            int absDx = Math.abs(center.getX() - candidate.getX());
            int absDz = Math.abs(center.getZ() - candidate.getZ());
            if (absDx == radius && absDz == radius) continue;

            if (!level.getFluidState(candidate).is(Fluids.LAVA)) continue;
            if (!level.getFluidState(candidate).isSource()) continue;

            BlockPos immutable = candidate.immutable();

            // Skip if this block was converted recently
            Long lastConverted = CONVERSION_COOLDOWN.get(immutable);
            if (lastConverted != null && now - lastConverted < CONVERSION_COOLDOWN_TICKS) continue;
            CONVERSION_COOLDOWN.put(immutable, now);
            CONVERSION_START.putIfAbsent(immutable, now);
            LavaWalkerPersistentState.get(level).startTimes.putIfAbsent(immutable, System.currentTimeMillis());
            LavaWalkerPersistentState.get(level).setDirty();

            // Phase 1: lava -> GILDED_BLACKSTONE (quick flash)
            level.setBlock(immutable, Blocks.GILDED_BLACKSTONE.defaultBlockState(), 3);
            spawnConversionEffects(level, immutable);

            // Schedule phase 2: GILDED_BLACKSTONE -> BLACKSTONE
            BlockConversionScheduler.schedule(
                immutable,
                Blocks.BLACKSTONE.defaultBlockState(),
                GILDED_INITIAL_TICKS,
                level.getGameTime(),
                entity
            );
        }
    }

    public static void revertBlock(ServerLevel level, BlockPos pos, BlockState revertTo, LivingEntity entity) {
        BlockState current = level.getBlockState(pos);
        long now = level.getGameTime();
        long START = CONVERSION_START.getOrDefault(pos, now);  // fallback to now if missing

        // Phase 1: GILDED_BLACKSTONE (flash) → BLACKSTONE
        if (current.is(Blocks.GILDED_BLACKSTONE) && revertTo.is(Blocks.BLACKSTONE)) {
            level.setBlock(pos, Blocks.BLACKSTONE.defaultBlockState(), 3);
            spawnSettleEffects(level, pos);
            long warnAt  = START + GILDED_INITIAL_TICKS + BLACKSTONE_MAIN_TICKS;
            long delayTo = Math.max(1, warnAt - now);
            BlockConversionScheduler.schedule(pos, Blocks.GILDED_BLACKSTONE.defaultBlockState(),
                (int) delayTo, now, entity);
            return;
        }

        // Phase 2: BLACKSTONE → GILDED_BLACKSTONE (first warning)
        if (current.is(Blocks.BLACKSTONE)) {
            level.setBlock(pos, Blocks.GILDED_BLACKSTONE.defaultBlockState(), 3);
            spawnFirstWarningEffects(level, pos);
            long magmaAt = START + GILDED_INITIAL_TICKS + BLACKSTONE_MAIN_TICKS + GILDED_WARNING_TICKS;
            long delayTo = Math.max(1, magmaAt - now);
            BlockConversionScheduler.schedule(pos, Blocks.MAGMA_BLOCK.defaultBlockState(),
                (int) delayTo, now, entity);
            return;
        }

        // Phase 3: GILDED_BLACKSTONE (warning) → MAGMA
        if (current.is(Blocks.GILDED_BLACKSTONE) && revertTo.is(Blocks.MAGMA_BLOCK)) {
            level.setBlock(pos, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
            spawnRevertEffects(level, pos);
            long lavaAt  = START + GILDED_INITIAL_TICKS + BLACKSTONE_MAIN_TICKS + GILDED_WARNING_TICKS + MAGMA_SHORT_TICKS;
            long delayTo = Math.max(1, lavaAt - now);
            BlockConversionScheduler.schedule(pos, getLavaBlockState(pos, level),
                (int) delayTo, now, entity);
            return;
        }

        // Phase 4: MAGMA → lava
        if (current.is(Blocks.MAGMA_BLOCK)) {
            level.setBlock(pos, revertTo, 3);
            CONVERSION_START.remove(pos);  // lifecycle complete, clean up
            LavaWalkerPersistentState.get(level).startTimes.remove(pos);
            LavaWalkerPersistentState.get(level).setDirty();
            return;
        }

        // Block broken externally
        if (current.isAir()) {
            level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
            killDroppedItems(level, pos);
            CONVERSION_START.remove(pos);
            LavaWalkerPersistentState.get(level).startTimes.remove(pos);
            LavaWalkerPersistentState.get(level).setDirty();
        }
    }

    // ── Effects ───────────────────────────────────────────────────────────────

    // lava → GILDED_BLACKSTONE (initial conversion)
    private static void spawnConversionEffects(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        level.sendParticles(ParticleTypes.WHITE_SMOKE,
            center.x, center.y + 0.5, center.z, 3, 0.2, 0.1, 0.2, 0.01);
        level.sendParticles(ParticleTypes.SMOKE,
            center.x, center.y + 0.25, center.z, 1, 0.1, 0.05, 0.1, 0.05);
        level.playSound(null, pos, SoundEvents.MAGMA_CUBE_DEATH,
            SoundSource.BLOCKS, 0.5f, 0.5f);
    }

    // GILDED_BLACKSTONE → BLACKSTONE (settling)
    private static void spawnSettleEffects(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        level.sendParticles(ParticleTypes.SMOKE,
            center.x, center.y + 1, center.z, 4, 0.2, 0.05, 0.2, 0.01);
    }

    // BLACKSTONE → GILDED_BLACKSTONE (first warning)
    private static void spawnFirstWarningEffects(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        level.sendParticles(LavaWalkerParticles.LAVA_POP,
            center.x, center.y + 0.5, center.z, 4, 0.2, 0.1, 0.2, 0.02);
        level.sendParticles(ParticleTypes.SMALL_FLAME,
            center.x, center.y + 0.25, center.z, 1, 0.1, 0.05, 0.1, 0.01);
    }

    // MAGMA → lava (final revert)
    private static void spawnRevertEffects(ServerLevel level, BlockPos pos) {
        Vec3 center = Vec3.atCenterOf(pos);
        level.sendParticles(ParticleTypes.SMALL_FLAME,
            center.x, center.y + 0.5, center.z, 2, 0.1, 0.05, 0.1, 0.01);
        level.sendParticles(ParticleTypes.SMOKE,
            center.x, center.y + 0.5, center.z, 15, 0.2, 0.1, 0.2, 0.05);
        level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH,
            SoundSource.BLOCKS, 0.2f, 0.9f);
    }

    public static void killDroppedItems(ServerLevel level, BlockPos pos) {
        net.minecraft.world.phys.AABB searchBox =
            new net.minecraft.world.phys.AABB(pos).inflate(1.0);
        level.getEntitiesOfClass(
            net.minecraft.world.entity.item.ItemEntity.class,
            searchBox,
            item -> item.getItem().is(net.minecraft.world.item.Items.GILDED_BLACKSTONE)
                 || item.getItem().is(net.minecraft.world.item.Items.BLACKSTONE)
                 || item.getItem().is(net.minecraft.world.item.Items.MAGMA_BLOCK)
        ).forEach(net.minecraft.world.entity.Entity::discard);
    }
}
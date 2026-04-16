package net.nanaky.frost_lava_walker.mixin;

import net.nanaky.frost_lava_walker.enchantment.LavaWalkerEnchantmentLogic;
import net.nanaky.frost_lava_walker.util.FrostFxHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class FrostWalkerEnchantmentMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void lavawalker_tick(CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        Level level = self.level();
        if (level.isClientSide()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!self.onGround()) return;

        FrostFxHelper.onEntityStep(serverLevel, self);
        
        LavaWalkerEnchantmentLogic.onEntityStep(serverLevel, self);
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void lavawalker_die(net.minecraft.world.damagesource.DamageSource src, CallbackInfo ci) {
        FrostFxHelper.onEntityRemoved((LivingEntity)(Object)this);
    }
}
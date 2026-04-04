package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.events.ElytraUpdateEvent;
import com.dev.leavesHack.modules.FireworkElytraFly;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(LivingEntity.class)
public class MixinLivingEntity {
    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isFallFlying()Z")
    )
    private boolean wrapIsFallFlying(LivingEntity instance, Operation<Boolean> original) {
        if (instance == mc.player) {
            ElytraUpdateEvent elytraTransformEvent = new ElytraUpdateEvent(instance);
            MeteorClient.EVENT_BUS.post(elytraTransformEvent);
            FireworkElytraFly.INSTANCE.isFallFlying = original.call(instance);
            if (elytraTransformEvent.isCancelled()) {
                return false;
            }
        }
        return original.call(instance);
    }
}
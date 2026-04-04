package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.events.ElytraUpdateEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(PlayerEntityRenderer.class)
public class MixinPlayerEntityRenderer {
    @WrapOperation(
            method = "setupTransforms(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;FFFF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;isFallFlying()Z")
    )
    private boolean wrapSetupTransforms(AbstractClientPlayerEntity instance, Operation<Boolean> original) {
        if (instance == mc.player) {
            ElytraUpdateEvent elytraTransformEvent = new ElytraUpdateEvent(instance);
            MeteorClient.EVENT_BUS.post(elytraTransformEvent);
            if (elytraTransformEvent.isCancelled()) {
                return false;
            }
        }
        return original.call(instance);
    }
}
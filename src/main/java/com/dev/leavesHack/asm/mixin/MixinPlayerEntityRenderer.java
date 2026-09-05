package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.events.ElytraUpdateEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(AvatarRenderer.class)
public class MixinPlayerEntityRenderer {
    @WrapOperation(
        method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;isFallFlying:Z")
    )
    private boolean wrapIsGliding(AvatarRenderState instance, Operation<Boolean> original) {
        if (mc.player != null && instance.scoreText == mc.player.getName()) {
            ElytraUpdateEvent event = new ElytraUpdateEvent(mc.player);
            MeteorClient.EVENT_BUS.post(event);
            if (event.isCancelled()) {
                return false;
            }
        }
        return original.call(instance);
    }
}

package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.events.ElytraUpdateEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(PlayerEntityRenderer.class)
public class MixinPlayerEntityRenderer {
    @WrapOperation(
        method = "setupTransforms(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;FF)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;isGliding:Z")
    )
    private boolean wrapIsGliding(PlayerEntityRenderState instance, Operation<Boolean> original) {
        if (mc.player != null && instance.playerName == mc.player.getName()) {
            ElytraUpdateEvent event = new ElytraUpdateEvent(mc.player);
            MeteorClient.EVENT_BUS.post(event);
            if (event.isCancelled()) {
                return false;
            }
        }
        return original.call(instance);
    }
}

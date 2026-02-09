package com.dev.leavesHack.mixin;

import com.dev.leavesHack.utils.rotation.Rotation;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity {
    @Inject(method = "sendMovementPackets", at = {@At("HEAD")}, cancellable = true)
    private void sendMovementPacketsHook(CallbackInfo ci) {
        if (mc.player != null) {
            Rotation.rotationYaw = mc.player.getYaw();
            Rotation.rotationPitch = mc.player.getPitch();
        }
    }
}

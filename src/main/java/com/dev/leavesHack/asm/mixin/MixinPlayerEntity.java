package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.events.ElytraUpdateEvent;
import com.dev.leavesHack.events.TravelEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravelPre(Vec3d movementInput, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if(player != mc.player)
            return;
        TravelEvent event = new TravelEvent(player);
        MeteorClient.EVENT_BUS.post(event);
        if (event.isCancelled()) {
            ci.cancel();
            event = new TravelEvent(player);
            MeteorClient.EVENT_BUS.post(event);
        }
    }
    @Inject(method = "travel", at = @At("RETURN"))
    private void onTravelPost(Vec3d movementInput, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if(player != mc.player)
            return;
        TravelEvent event = new TravelEvent(player);
        MeteorClient.EVENT_BUS.post(event);
    }
    @WrapOperation(method = "updatePose", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;isFallFlying()Z"))
    private boolean hookUpdatePose(PlayerEntity instance, Operation<Boolean> original) {
        if (instance == mc.player) {
            ElytraUpdateEvent elytraTransformEvent = new ElytraUpdateEvent(instance);
            MeteorClient.EVENT_BUS.post(elytraTransformEvent);
            if (elytraTransformEvent.isCancelled()) {
                return false;
            }
        }
        return instance.isFallFlying();
    }
}

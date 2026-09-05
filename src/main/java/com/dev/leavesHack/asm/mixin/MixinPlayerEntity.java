package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.events.ElytraUpdateEvent;
import com.dev.leavesHack.events.TravelEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(Player.class)
public abstract class MixinPlayerEntity {
    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravelPre(Vec3 movementInput, CallbackInfo ci) {
        Player player = (Player) (Object) this;
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
    private void onTravelPost(Vec3 movementInput, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if(player != mc.player)
            return;
        TravelEvent event = new TravelEvent(player);
        MeteorClient.EVENT_BUS.post(event);
    }
    @WrapOperation(method = "getDesiredPose", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isFallFlying()Z"))
    private boolean hookUpdatePose(Player instance, Operation<Boolean> original) {
        if (instance == mc.player) {
            ElytraUpdateEvent elytraTransformEvent = new ElytraUpdateEvent(instance);
            MeteorClient.EVENT_BUS.post(elytraTransformEvent);
            if (elytraTransformEvent.isCancelled()) {
                return false;
            }
        }
        return instance.isFallFlying();
    }
//    @Inject(method = "attack", at = @At(value = "RETURN"))
//    private void onAfterAttack(Entity target, CallbackInfo ci) {
//        if (SprintPlus.INSTANCE.isActive() && !mc.player.isSprinting()) {
//            if (SprintPlus.INSTANCE.isLoyisa()) mc.player.setSprinting(true);
//        }
//    }
//    @Inject(method = "attack", at = @At(value = "HEAD"))
//    private void onBeforeAttack(Entity target, CallbackInfo ci) {
//        if (SprintPlus.INSTANCE.isActive() && mc.player.isSprinting()) {
//            if (!SprintPlus.INSTANCE.isLoyisa()) mc.player.setSprinting(false);
//        }
//    }
}

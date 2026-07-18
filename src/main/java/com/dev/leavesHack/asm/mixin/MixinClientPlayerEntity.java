package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.events.MoveEvent;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayerEntity {
    @Final
    @Shadow
    public ClientPlayNetworkHandler networkHandler;
    public MixinClientPlayerEntity(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }
    @Inject(method = "sendMovementPackets", at = {@At("HEAD")}, cancellable = true)
    private void sendMovementPacketsHook(CallbackInfo ci) {
        Rotation.rotationYaw = this.getYaw();
        Rotation.rotationPitch = this.getPitch();
    }
    @Inject(method = "sendMovementPackets", at = {@At("TAIL")}, cancellable = true)
    private void sendMovementPacketsHook2(CallbackInfo ci) {
        Rotation.rotation = false;
    }
    @ModifyExpressionValue(
            method = "sendMovementPackets",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F")
    )
    private float hookYaw(float original) {
        if (Rotation.rotation) {
            return Rotation.targetYaw;
        }
        return original;
    }

    @ModifyExpressionValue(
            method = "sendMovementPackets",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F")
    )
    private float hookPitch(float original) {
        if (Rotation.rotation) {
            return Rotation.targetPitch;
        }
        return original;
    }
    @Inject(method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;hasVehicle()Z",
                    shift = At.Shift.AFTER
            ),
            cancellable = true)
    private void tickHook(CallbackInfo ci) {
        try {
            if (this.hasVehicle()) {
                Rotation.rotationYaw = this.getYaw();
                Rotation.rotationPitch = this.getPitch();
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(this.getYaw(), this.getPitch(), this.isOnGround(), this.horizontalCollision));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"), cancellable = true)
    public void onMoveHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        MoveEvent event = new MoveEvent(movement.x, movement.y, movement.z);
        MeteorClient.EVENT_BUS.post(event);
        ci.cancel();
        if (!event.isCancelled()) {
            super.move(movementType, new Vec3d(event.getX(), event.getY(), event.getZ()));
        }
    }
}

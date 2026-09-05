package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.events.MoveEvent;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayer {
    @Final
    @Shadow
    public ClientPacketListener connection;
    public MixinClientPlayerEntity(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }
    @Inject(method = "sendPosition", at = {@At("HEAD")}, cancellable = true)
    private void sendMovementPacketsHook(CallbackInfo ci) {
        Rotation.rotationYaw = this.getYRot();
        Rotation.rotationPitch = this.getXRot();
    }
    @Inject(method = "sendPosition", at = {@At("TAIL")}, cancellable = true)
    private void sendMovementPacketsHook2(CallbackInfo ci) {
        Rotation.rotation = false;
    }
    @ModifyExpressionValue(
            method = "sendPosition",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F")
    )
    private float hookYaw(float original) {
        if (Rotation.rotation) {
            return Rotation.targetYaw;
        }
        return original;
    }

    @ModifyExpressionValue(
            method = "sendPosition",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F")
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
                    target = "Lnet/minecraft/client/player/LocalPlayer;isPassenger()Z",
                    shift = At.Shift.AFTER
            ),
            cancellable = true)
    private void tickHook(CallbackInfo ci) {
        try {
            if (this.isPassenger()) {
                Rotation.rotationYaw = this.getYRot();
                Rotation.rotationPitch = this.getXRot();
                this.connection.send(new ServerboundMovePlayerPacket.Rot(this.getYRot(), this.getXRot(), this.onGround(), this.horizontalCollision));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"), cancellable = true)
    public void onMoveHook(MoverType movementType, Vec3 movement, CallbackInfo ci) {
        MoveEvent event = new MoveEvent(movement.x, movement.y, movement.z);
        MeteorClient.EVENT_BUS.post(event);
        ci.cancel();
        if (!event.isCancelled()) {
            super.move(movementType, new Vec3(event.getX(), event.getY(), event.getZ()));
        }
    }
}

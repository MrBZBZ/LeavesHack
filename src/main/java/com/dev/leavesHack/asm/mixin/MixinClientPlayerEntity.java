package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.utils.rotation.Rotation;
import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
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
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(this.getYaw(), this.getPitch(), this.isOnGround()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

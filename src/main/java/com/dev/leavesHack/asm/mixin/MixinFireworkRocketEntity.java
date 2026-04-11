package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.modules.FireworkElytraFly;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(FireworkRocketEntity.class)
public abstract class MixinFireworkRocketEntity {
    @WrapOperation(
            method = {"tick"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;")
    )
    private Vec3d wrapGetRotationVector(LivingEntity instance, Operation<Vec3d> original) {
        if (instance == mc.player) {
            if (FireworkElytraFly.INSTANCE.isActive() && FireworkElytraFly.INSTANCE.mode.get() == FireworkElytraFly.Mode.GrimDurability && FireworkElytraFly.INSTANCE.control.get()) {
                float yaw = FireworkElytraFly.INSTANCE.yaw;
                float pitch = FireworkElytraFly.INSTANCE.pitch;
                return instance.getRotationVector(pitch, yaw);
            }
        }
        return original.call(instance);
    }
}
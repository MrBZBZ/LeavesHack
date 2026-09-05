package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.modules.FireworkElytraFly;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(FireworkRocketEntity.class)
public abstract class MixinFireworkRocketEntity {
    @WrapOperation(
            method = {"tick"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;")
    )
    private Vec3 wrapGetRotationVector(LivingEntity instance, Operation<Vec3> original) {
        if (instance == mc.player) {
            if (FireworkElytraFly.INSTANCE.isActive() && FireworkElytraFly.INSTANCE.mode.get() == FireworkElytraFly.Mode.GrimDurability && FireworkElytraFly.INSTANCE.control.get()) {
                float yaw = FireworkElytraFly.INSTANCE.yaw;
                float pitch = FireworkElytraFly.INSTANCE.pitch;
                return instance.calculateViewVector(pitch, yaw);
            }
        }
        return original.call(instance);
    }
}

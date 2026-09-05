package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.modules.GlobalSetting;
import com.dev.leavesHack.utils.rotation.Rotation;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow
    public abstract void setDeltaMovement(Vec3 velocity);

    @Shadow
    public abstract Vec3 getDeltaMovement();

    @Inject(
            method = "moveRelative",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hookUpdateVelocity(float speed, Vec3 movementInput, CallbackInfo ci) {
        if (GlobalSetting.INSTANCE.moveFix.get()) {
            Entity entity = (Entity) (Object) this;
            if (entity != Minecraft.getInstance().player)
                return;
            if (Rotation.rotation) {
                Vec3 vec3d = movementInputToVelocity(
                        movementInput,
                        speed,
                        Rotation.targetYaw
                );
                this.setDeltaMovement(this.getDeltaMovement().add(vec3d));
                ci.cancel();
            }
        }
    }

    @Unique
    private static Vec3 movementInputToVelocity(Vec3 movementInput, float speed, float yaw) {
        double d = movementInput.lengthSqr();

        if (d < 1.0E-7) {
            return Vec3.ZERO;
        }

        Vec3 vec3d = (d > 1.0
                ? movementInput.normalize()
                : movementInput).scale(speed);

        float sin = Mth.sin(yaw * ((float)Math.PI / 180F));
        float cos = Mth.cos(yaw * ((float)Math.PI / 180F));

        return new Vec3(
                vec3d.x * cos - vec3d.z * sin,
                vec3d.y,
                vec3d.z * cos + vec3d.x * sin
        );
    }
}

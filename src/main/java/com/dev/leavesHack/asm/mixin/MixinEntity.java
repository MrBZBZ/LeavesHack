package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.modules.GlobalSetting;
import com.dev.leavesHack.utils.rotation.Rotation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Shadow
    public abstract void setVelocity(Vec3d velocity);

    @Shadow
    public abstract Vec3d getVelocity();

    @Inject(
            method = "updateVelocity",
            at = @At("HEAD"),
            cancellable = true
    )
    private void hookUpdateVelocity(float speed, Vec3d movementInput, CallbackInfo ci) {
        if (GlobalSetting.INSTANCE.moveFix.get()) {
            Entity entity = (Entity) (Object) this;
            if (entity != MinecraftClient.getInstance().player)
                return;
            if (Rotation.rotation) {
                Vec3d vec3d = movementInputToVelocity(
                        movementInput,
                        speed,
                        Rotation.targetYaw
                );
                this.setVelocity(this.getVelocity().add(vec3d));
                ci.cancel();
            }
        }
    }

    @Unique
    private static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
        double d = movementInput.lengthSquared();

        if (d < 1.0E-7) {
            return Vec3d.ZERO;
        }

        Vec3d vec3d = (d > 1.0
                ? movementInput.normalize()
                : movementInput).multiply(speed);

        float sin = MathHelper.sin(yaw * ((float)Math.PI / 180F));
        float cos = MathHelper.cos(yaw * ((float)Math.PI / 180F));

        return new Vec3d(
                vec3d.x * cos - vec3d.z * sin,
                vec3d.y,
                vec3d.z * cos + vec3d.x * sin
        );
    }
}

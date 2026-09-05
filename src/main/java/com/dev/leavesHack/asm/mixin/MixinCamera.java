package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.modules.GlobalSetting;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static meteordevelopment.meteorclient.MeteorClient.mc;

// 26.1.2 把 fov 计算从 GameRenderer#getFov 搬到了 Camera#calculateFov
@Mixin(Camera.class)
public class MixinCamera {

    @Redirect(method = "calculateFov",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/Mth;lerp(FFF)F"))
    private float removeSprintFov(float delta, float start, float end, float tickDelta) {
        if (mc.player != null && !GlobalSetting.INSTANCE.changeFov.get()) {
            return 1.0F;
        }
        return Mth.lerp(delta, start, end);  // 其它情况维持原样
    }
}

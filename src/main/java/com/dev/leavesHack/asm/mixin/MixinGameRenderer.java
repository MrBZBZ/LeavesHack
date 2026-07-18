package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.modules.GlobalSetting;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Redirect(method = "getFov",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F"))
    private float removeSprintFov(float delta, float start, float end,
                                  Camera camera, float tickDelta, boolean changingFov) {
        if (mc.player != null && !GlobalSetting.INSTANCE.changeFov.get()) {
            return 1.0F;
        }
        return MathHelper.lerp(delta, start, end);  // 其它情况维持原样
    }
}

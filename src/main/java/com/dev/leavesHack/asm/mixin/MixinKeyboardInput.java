package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.events.KeyboardInputEvent;
import com.dev.leavesHack.modules.GlobalSetting;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput {

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
        if (GlobalSetting.INSTANCE.moveFix.get()) {
            KeyboardInput self = (KeyboardInput) (Object) this;

            KeyboardInputEvent event = new KeyboardInputEvent(
                    self.pressingForward,
                    self.pressingBack,
                    self.pressingLeft,
                    self.pressingRight,
                    self.jumping,
                    self.sneaking
            );

            MeteorClient.EVENT_BUS.post(event);

            self.movementForward = event.getForward();
            self.movementSideways = event.getStrafe();

            self.pressingForward = event.getForward() > 0;
            self.pressingBack = event.getForward() < 0;
            self.pressingLeft = event.getStrafe() < 0;
            self.pressingRight = event.getStrafe() > 0;

            self.jumping = event.jump;
            self.sneaking = event.sneak;
            if (slowDown) {
                self.movementForward *= slowDownFactor;
                self.movementSideways *= slowDownFactor;
            }
        }
    }
}

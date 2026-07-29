package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.events.ModuleActiveEvent;
import com.dev.leavesHack.events.ModuleDeactiveEvent;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.modules.Module;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Module.class)
public class MixinMeteorModule {
    @Inject(method = "toggle", at = @At("RETURN"))
    private void onToggleReturn(CallbackInfo ci) {
        Module self = (Module) (Object) this;
        if (self.isActive()) {
            MeteorClient.EVENT_BUS.post(new ModuleActiveEvent());
        } else {
            MeteorClient.EVENT_BUS.post(new ModuleDeactiveEvent());
        }
    }
}

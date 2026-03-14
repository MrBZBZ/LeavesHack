package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.asm.accessors.SystemsAccessor;
import com.dev.leavesHack.modules.autoLogin.AutoLoginAccounts;
import meteordevelopment.meteorclient.systems.Systems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Systems.class)
public class SystemsMixin {
    @Inject(method = "init", at = @At("TAIL"))
    private static void onInit(CallbackInfo ci) {

        SystemsAccessor.invokeAdd(new AutoLoginAccounts());

    }
}
package com.dev.leavesHack.asm.accessors;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientLevel.class)
public interface IClientWorld {
    @Invoker("getBlockStatePredictionHandler")
    BlockStatePredictionHandler invokeGetPendingUpdateManager();
}

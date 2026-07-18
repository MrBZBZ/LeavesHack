package com.dev.leavesHack.asm.accessors;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.network.PendingUpdateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientWorld.class)
public interface IClientWorld {
    @Invoker("getPendingUpdateManager")
    PendingUpdateManager invokeGetPendingUpdateManager();
}

package com.dev.leavesHack.asm.accessors;

import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Systems.class)
public interface SystemsAccessor {

    @Invoker("add")
    static System<?> invokeAdd(System<?> system) {
        throw new AssertionError();
    }
}
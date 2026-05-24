package com.dev.leavesHack.asm.accessors;

import meteordevelopment.meteorclient.systems.modules.Module;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Module.class,remap = false)
public interface IModule {


    @Mutable
    @Accessor("title")
    public void setTitle(String title);
    @Accessor("title")
    public String getTitle();
    @Mutable
    @Accessor("description")
    public void setDescription(String description);
    @Accessor("description")
    public String getDescription();
}

package com.dev.leavesHack.asm.accessors;

import meteordevelopment.meteorclient.settings.Setting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = Setting.class,remap = false)
public interface SettingAccessor {
    @Accessor("title")
    @Mutable
    public void setTitle(String title);
    @Accessor( "description")
    @Mutable
    public void setDescription(String description);
}

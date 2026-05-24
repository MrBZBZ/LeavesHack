package com.dev.leavesHack.asm.accessors;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = SettingGroup.class, remap = false)
public interface SettingGroupAccessor {
    @Accessor("settings")
    public List<Setting<?>> getSettings();
}

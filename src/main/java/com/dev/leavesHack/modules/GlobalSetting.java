package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class GlobalSetting extends Module {
    public static GlobalSetting INSTANCE;
    public GlobalSetting() {
        super(LeavesHack.CATEGORY, "GlobalSetting", "全局设置");
        INSTANCE = this;
    }
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgRotation = this.settings.createGroup("Rotation");
    public final Setting<Boolean> packetPlace = sgGeneral.add(new BoolSetting.Builder()
            .name("PacketPlace")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> grimRotation = sgRotation.add(new BoolSetting.Builder()
            .name("GrimRotation")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> snapBack = sgRotation.add(new BoolSetting.Builder()
            .name("SnapBack")
            .defaultValue(true)
            .build()
    );
}


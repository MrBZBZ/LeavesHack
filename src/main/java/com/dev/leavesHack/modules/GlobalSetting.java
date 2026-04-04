package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
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
    private final SettingGroup sgElytra = this.settings.createGroup("Elytra");
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
    public final Setting<Boolean> baritone = sgElytra.add(new BoolSetting.Builder()
            .name("Baritone")
            .defaultValue(true)
            .build()
    );
    public final Setting<Integer> elytraMinDamage = sgElytra.add(new IntSetting.Builder()
            .name("ElytraMinDamage")
            .defaultValue(10)
            .min(0)
            .max(100)
            .build()
    );
    public final Setting<Integer> minFireworks = sgElytra.add(new IntSetting.Builder()
            .name("MinFireworks")
            .defaultValue(10)
            .min(0)
            .max(64)
            .build()
    );
}


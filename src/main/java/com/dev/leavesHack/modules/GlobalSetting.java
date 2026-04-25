package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.modules.render.HandView;

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
    public final Setting<Boolean> optimizedCalc = sgGeneral.add(new BoolSetting.Builder()
            .name("OptimizedCalc")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> inventorySync = sgGeneral.add(new BoolSetting.Builder()
            .name("InventorySync")
            .defaultValue(true)
            .build()
    );
    public final Setting<SwingMode> placeSwing = sgGeneral.add(new EnumSetting.Builder<SwingMode>()
            .name("PlaceSwing")
            .defaultValue(SwingMode.Packet)
            .build()
    );
    public final Setting<SwingMode> attackSwing = sgGeneral.add(new EnumSetting.Builder<SwingMode>()
            .name("AttackSwing")
            .defaultValue(SwingMode.Packet)
            .build()
    );
    public final Setting<HandMode> handMode = sgGeneral.add(new EnumSetting.Builder<HandMode>()
            .name("HandMode")
            .defaultValue(HandMode.MainHand)
            .build()
    );
    public final Setting<Boolean> interactRotation = sgRotation.add(new BoolSetting.Builder()
            .name("1.21+")
            .defaultValue(true)
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
    public enum SwingMode {
        Both,
        Packet,
        Client,
        None
    }
    public enum HandMode {
        MainHand,
        OffHand
    }
}


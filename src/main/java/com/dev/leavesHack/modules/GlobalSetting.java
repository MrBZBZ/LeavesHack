package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import meteordevelopment.meteorclient.settings.*;
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
    public final Setting<Boolean> chinese = sgGeneral.add(new BoolSetting.Builder()
            .name("Chinese")
            .description("汉化")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> packetPlace = sgGeneral.add(new BoolSetting.Builder()
            .name("PacketPlace")
            .description("发包放置")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> optimizedCalc = sgGeneral.add(new BoolSetting.Builder()
            .name("OptimizedCalc")
            .description("优化计算")
            .defaultValue(true)
            .build()
    );
    public final Setting<SwingMode> placeSwing = sgGeneral.add(new EnumSetting.Builder<SwingMode>()
            .name("PlaceSwing")
            .description("放置挥手模式")
            .defaultValue(SwingMode.Packet)
            .build()
    );
    public final Setting<SwingMode> attackSwing = sgGeneral.add(new EnumSetting.Builder<SwingMode>()
            .name("AttackSwing")
            .description("攻击挥手模式")
            .defaultValue(SwingMode.Packet)
            .build()
    );
    public final Setting<HandMode> handMode = sgGeneral.add(new EnumSetting.Builder<HandMode>()
            .name("HandMode")
            .description("手部模式")
            .defaultValue(HandMode.MainHand)
            .build()
    );
    public final Setting<Boolean> noBadPackets = sgGeneral.add(new BoolSetting.Builder()
            .name("NoBadPackets")
            .description("屏蔽错误数据包")
            .defaultValue(false)
            .build()
    );
    public final Setting<Boolean> packetKickFix = sgGeneral.add(new BoolSetting.Builder()
            .name("PacketKickFix")
            .description("数据包踢出修复")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> clientSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("ClientSwitch")
            .description("客户端切换")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> moveFix = sgRotation.add(new BoolSetting.Builder()
            .name("1.21+")
            .description("1.21+移动修复")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> grimRotation = sgRotation.add(new BoolSetting.Builder()
            .name("GrimRotation")
            .description("Grim旋转修复")
            .defaultValue(true)
            .visible(() -> !moveFix.get())
            .build()
    );
    public final Setting<Boolean> snapBack = sgRotation.add(new BoolSetting.Builder()
            .name("SnapBack")
            .description("转头自动回正")
            .defaultValue(true)
            .visible(() -> !moveFix.get())
            .build()
    );
    public final Setting<Boolean> baritone = sgElytra.add(new BoolSetting.Builder()
            .name("Baritone")
            .description("Baritone兼容")
            .defaultValue(true)
            .build()
    );
    public final Setting<Integer> elytraMinDamage = sgElytra.add(new IntSetting.Builder()
            .name("ElytraMinDamage")
            .description("鞘翅最小耐久")
            .defaultValue(10)
            .min(0)
            .max(100)
            .build()
    );
    public final Setting<Integer> minFireworks = sgElytra.add(new IntSetting.Builder()
            .name("MinFireworks")
            .description("最少烟花数量")
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


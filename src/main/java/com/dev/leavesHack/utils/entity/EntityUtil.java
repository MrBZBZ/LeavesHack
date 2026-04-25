package com.dev.leavesHack.utils.entity;

import com.dev.leavesHack.modules.GlobalSetting;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.util.Hand;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EntityUtil {
    public static void attackSwingHand() {
        Hand hand = GlobalSetting.INSTANCE.handMode.get() == GlobalSetting.HandMode.MainHand ? Hand.MAIN_HAND : Hand.OFF_HAND;
        if (GlobalSetting.INSTANCE.attackSwing.get() != GlobalSetting.SwingMode.Packet && GlobalSetting.INSTANCE.attackSwing.get() != GlobalSetting.SwingMode.None) mc.player.swingHand(hand);
        if (GlobalSetting.INSTANCE.attackSwing.get() != GlobalSetting.SwingMode.Client && GlobalSetting.INSTANCE.attackSwing.get() != GlobalSetting.SwingMode.None) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
    }
    public static void placeSwingHand() {
        Hand hand = GlobalSetting.INSTANCE.handMode.get() == GlobalSetting.HandMode.MainHand ? Hand.MAIN_HAND : Hand.OFF_HAND;
        if (GlobalSetting.INSTANCE.placeSwing.get() != GlobalSetting.SwingMode.Packet && GlobalSetting.INSTANCE.placeSwing.get() != GlobalSetting.SwingMode.None) mc.player.swingHand(hand);
        if (GlobalSetting.INSTANCE.placeSwing.get() != GlobalSetting.SwingMode.Client && GlobalSetting.INSTANCE.placeSwing.get() != GlobalSetting.SwingMode.None) mc.getNetworkHandler().sendPacket(new HandSwingC2SPacket(hand));
    }
}

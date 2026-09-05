package com.dev.leavesHack.utils.entity;

import com.dev.leavesHack.modules.GlobalSetting;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class EntityUtil {
    public static void attackSwingHand() {
        InteractionHand hand = GlobalSetting.INSTANCE.handMode.get() == GlobalSetting.HandMode.MainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (GlobalSetting.INSTANCE.attackSwing.get() != GlobalSetting.SwingMode.Packet && GlobalSetting.INSTANCE.attackSwing.get() != GlobalSetting.SwingMode.None) mc.player.swing(hand);
        if (GlobalSetting.INSTANCE.attackSwing.get() != GlobalSetting.SwingMode.Client && GlobalSetting.INSTANCE.attackSwing.get() != GlobalSetting.SwingMode.None) mc.getConnection().send(new ServerboundSwingPacket(hand));
    }
    public static void placeSwingHand() {
        InteractionHand hand = GlobalSetting.INSTANCE.handMode.get() == GlobalSetting.HandMode.MainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (GlobalSetting.INSTANCE.placeSwing.get() != GlobalSetting.SwingMode.Packet && GlobalSetting.INSTANCE.placeSwing.get() != GlobalSetting.SwingMode.None) mc.player.swing(hand);
        if (GlobalSetting.INSTANCE.placeSwing.get() != GlobalSetting.SwingMode.Client && GlobalSetting.INSTANCE.placeSwing.get() != GlobalSetting.SwingMode.None) mc.getConnection().send(new ServerboundSwingPacket(hand));
    }
}

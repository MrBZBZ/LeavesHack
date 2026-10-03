package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/**
 * 一键珍珠（移植自 meteor-miku OneKeyPearl）
 * 激活即投掷珍珠：主手直接投；否则静默换手投掷后还原，随后模块自动关闭。
 */
public class OneKeyPearl extends Module {

    public OneKeyPearl() {
        super(LeavesHack.LEAVES_COMBAT, "OneKeyPearl", "一键珍珠");
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }

        FindItemResult pearl = InvUtils.find(Items.ENDER_PEARL);
        if (!pearl.found()) {
            ChatUtils.error("OneKeyPearl: 背包里没有珍珠");
            toggle();
            return;
        }

        if (pearl.isOffhand()) {
            // 副手珍珠直接投，无需换手
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
        } else if (!pearl.isMainHand()) {
            // 静默换手 → 投掷 → 还原
            InvUtils.swap(pearl.slot(), false);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            InvUtils.swapBack();
        } else {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        }

        toggle();
    }
}

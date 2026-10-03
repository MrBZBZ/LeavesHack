package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.asm.accessors.IClientWorld;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

/**
 * 一键烟花（移植自 meteor-miku OnekeyFireWork）
 * 仅在滑行状态释放。
 * <p>
 * 对照 Grim v2.3.73 取证重写：
 * - PacketOrderE: HELD_ITEM_CHANGE 不得与 USE_ITEM 同 tick → 切槽与使用分 tick
 * - Post: 交互包必须在移动包之前 → 全部状态机推进放在 TickEvent.Pre
 * - CLOSE_WINDOW 不再发送（无窗口操作，不需要重同步）
 * 时序: t0 HELD(烟花槽) -> t1 USE(烟花) -> closeDelay 后 t2 HELD(还原槽位) 并自动关闭
 */
public class OneKeyFirework extends Module {
    private static final int PH_IDLE = 0;
    private static final int PH_SWAPPED = 1;   // 已切到烟花槽，待使用
    private static final int PH_FIRED = 2;     // 已使用，待还原

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> closeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("还原延迟")
        .description("使用烟花后还原选中槽位的延迟（游戏刻）")
        .defaultValue(4)
        .sliderRange(1, 40)
        .build()
    );

    private int phase = PH_IDLE;
    private int delay = 0;
    private int slotBefore = 0;
    private int fireworkHotbar = -1;

    public OneKeyFirework() {
        super(LeavesHack.LEAVES_COMBAT, "OneKeyFirework", "一键烟花");
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }
        // 需求：仅在滑行状态释放
        if (!mc.player.isGliding()) {
            ChatUtils.error("OneKeyFirework: 未在滑行状态，取消");
            toggle();
            return;
        }

        slotBefore = mc.player.getInventory().getSelectedSlot();

        // 主手/副手烟花：直接使用，零切槽零还原
        if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
            firework(Hand.MAIN_HAND);
            toggle();
            return;
        }
        if (mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET) {
            firework(Hand.OFF_HAND);
            toggle();
            return;
        }

        // 快捷栏烟花：t0 切槽（本 tick 独占），下一 tick 使用，closeDelay 后还原
        int fw = InventoryUtil.findItemInventorySlot(Items.FIREWORK_ROCKET);
        if (fw >= 36 && fw <= 44) {
            fireworkHotbar = fw;
            InventoryUtil.switchToSlot(fw - 36);
            phase = PH_SWAPPED;
            delay = 0;
            return;
        }

        ChatUtils.error("OneKeyFirework: 烟花需在主手/副手/快捷栏");
        toggle();
    }

    @Override
    public void onDeactivate() {
        phase = PH_IDLE;
        delay = 0;
        fireworkHotbar = -1;
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (phase == PH_IDLE) return;

        if (phase == PH_SWAPPED) {
            // t1: 使用烟花（与切槽分 tick，规避 PacketOrderE）
            firework(Hand.MAIN_HAND);
            phase = PH_FIRED;
            delay = closeDelay.get();
            return;
        }

        if (phase == PH_FIRED) {
            if (--delay > 0) return;
            // t2: 还原选中槽位（HELD_ITEM_CHANGE，独占本 tick 交互）
            if (fireworkHotbar != -1 && mc.player != null) {
                InventoryUtil.switchToSlot(slotBefore);
            }
            phase = PH_IDLE;
            toggle();
        }
    }

    /** 序列化右键包（PendingUpdateManager 分配 sequence） */
    private void firework(Hand hand) {
        if (mc.getNetworkHandler() == null || mc.world == null) return;
        try (PendingUpdateManager pendingUpdateManager = ((IClientWorld) mc.world).invokeGetPendingUpdateManager().incrementSequence()) {
            int sequence = pendingUpdateManager.getSequence();
            mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(hand, sequence, mc.player.getYaw(), mc.player.getPitch()));
        }
    }
}

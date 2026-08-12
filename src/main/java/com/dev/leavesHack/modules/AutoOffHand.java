package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.SlotActionType;

public class AutoOffHand extends Module {
    public static AutoOffHand INSTANCE;
    public AutoOffHand() {
        super(LeavesHack.LEAVES_COMBAT, "AutoOffHand", "自动副手");
    }
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgGap = this.settings.createGroup("UseGap");
    private final Setting<Boolean> useGap = sgGap.add(new BoolSetting.Builder()
        .name("UseGap")
        .description("右键金苹果")
        .defaultValue(true)
        .build()
    );
    private final Setting<GapMode> gapMode = sgGap.add(new EnumSetting.Builder<GapMode>()
        .name("GapMode")
        .description("金苹果模式")
        .defaultValue(GapMode.MainHand)
        .build()
    );
    private final Setting<Boolean> pickaxe = sgGap.add(new BoolSetting.Builder()
        .name("WhenPickAxe")
        .description("镐子")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> sword = sgGap.add(new BoolSetting.Builder()
        .name("WhenSword")
        .description("剑")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> whenTotem = sgGap.add(new BoolSetting.Builder()
        .name("WhenTotem")
        .description("图腾")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> autoMain = sgGeneral.add(new BoolSetting.Builder()
        .name("AutoMain")
        .description("自动主手")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> mainSlot = sgGeneral.add(new IntSetting.Builder()
        .name("MainSlot")
        .description("主手槽位")
        .defaultValue(0)
        .sliderRange(0,8)
        .build()
    );
    private final Setting<Integer> checkHealth = sgGeneral.add(new IntSetting.Builder()
        .name("CheckHealth")
        .description("检查血量")
        .defaultValue(10)
        .sliderRange(0,36)
        .build()
    );
    private int oldSlot = -1;
    private int swapSlot = -1;
    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (isHandledScreenOpen()) return;
        int totem = InventoryUtil.findItemInventorySlot(Items.TOTEM_OF_UNDYING);
        boolean hasTotem = mc.player.getInventory().getStack(mainSlot.get()).getItem() == Items.TOTEM_OF_UNDYING;
        int checkSlot = gapMode.get() == GapMode.MainHand ? 40 : mc.player.getInventory().getSelectedSlot();
        boolean hasGap = mc.player.getInventory().getStack(checkSlot).getItem() == Items.ENCHANTED_GOLDEN_APPLE || mc.player.getInventory().getStack(checkSlot).getItem() == Items.GOLDEN_APPLE;
        int gap = InventoryUtil.findItemInventorySlot(Items.ENCHANTED_GOLDEN_APPLE);
        if (gap == -1) {
            gap = InventoryUtil.findItemInventorySlot(Items.GOLDEN_APPLE);
        }
        if (useGap.get() && gap != -1 && gapMode.get() == GapMode.OffHand) {
            if (mc.options.useKey.isPressed() && oldSlot != -1) {
                if (!hasTotem && totem != -1) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, totem, mainSlot.get(), SlotActionType.SWAP, mc.player);
                }
            }
            if (mc.options.useKey.isPressed() && checkItem() && !hasGap) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, gap, 40, SlotActionType.SWAP, mc.player);
                InventoryUtil.switchToSlot(mc.player.getInventory().getSelectedSlot());
                totem = InventoryUtil.findItemInventorySlot(Items.TOTEM_OF_UNDYING);
                if (autoMain.get() && EntityUtils.getTotalHealth(mc.player) <= checkHealth.get()) {
                    if (!hasTotem && totem != -1) {
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, totem, mainSlot.get(), SlotActionType.SWAP, mc.player);
                    }
                    if (oldSlot == -1) {
                        oldSlot = mc.player.getInventory().getSelectedSlot();
                        mc.player.getInventory().setSelectedSlot(mainSlot.get());
                    }
                }
            } else {
                if (autoMain.get() && oldSlot != -1 && !mc.options.useKey.isPressed()) {
                    mc.player.getInventory().setSelectedSlot(oldSlot);
                    oldSlot = -1;
                }
                if (!mc.options.useKey.isPressed() && totem != -1) {
                    if (mc.player.getInventory().getStack(40).getItem() != Items.TOTEM_OF_UNDYING) {
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, totem, 40, SlotActionType.SWAP, mc.player);
                        oldSlot = -1;
                    }
                }
            }
        } else if (useGap.get() && gap != -1 && gapMode.get() == GapMode.MainHand) {
            if (totem != -1) {
                if (mc.player.getInventory().getStack(40).getItem() != Items.TOTEM_OF_UNDYING) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, totem, 40, SlotActionType.SWAP, mc.player);
                }
            }
            if (oldSlot == -1 && mc.options.useKey.isPressed() && checkItem() && !hasGap) {
                oldSlot = mc.player.getInventory().getSelectedSlot();
                swapSlot = gap;
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, gap, oldSlot, SlotActionType.SWAP, mc.player);
                InventoryUtil.switchToSlot(mc.player.getInventory().getSelectedSlot());
            }
            if (oldSlot != -1 && !mc.options.useKey.isPressed()) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, swapSlot, oldSlot, SlotActionType.SWAP, mc.player);
                InventoryUtil.switchToSlot(mc.player.getInventory().getSelectedSlot());
                oldSlot = -1;
                swapSlot = -1;
            }
        } else {
            if (totem != -1) {
                if (mc.player.getInventory().getStack(40).getItem() != Items.TOTEM_OF_UNDYING) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, totem, 40, SlotActionType.SWAP, mc.player);
                }
            }
        }
    }
    private boolean checkItem() {
        if (pickaxe.get() && mc.player.getInventory().getStack(mc.player.getInventory().getSelectedSlot()).isIn(ItemTags.PICKAXES)) {
            return true;
        }
        if (sword.get() && mc.player.getInventory().getStack(mc.player.getInventory().getSelectedSlot()).isIn(ItemTags.SWORDS)) {
            return true;
        }
        if (whenTotem.get() && mc.player.getInventory().getStack(mc.player.getInventory().getSelectedSlot()).getItem() == Items.TOTEM_OF_UNDYING) {
            return true;
        }
        return false;
    }
    private boolean isHandledScreenOpen() {
        return mc.currentScreen instanceof HandledScreen<?>;
    }
    private enum GapMode {
        OffHand,
        MainHand
    }
}

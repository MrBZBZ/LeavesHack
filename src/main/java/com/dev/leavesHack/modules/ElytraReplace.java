package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/**
 * 鞘翅替换（移植自 MeteorPlusPlusAddon ElytraReplace）
 * 自动替换耐久度过低的鞘翅：优先从快捷栏，其次从背包。
 */
public class ElytraReplace extends Module {
    private final SettingGroup sg = settings.getDefaultGroup();

    private final Setting<Integer> durability = sg.add(new IntSetting.Builder()
        .name("耐久阈值")
        .description("耐久度百分比阈值，低于此值时替换鞘翅。")
        .defaultValue(5)
        .min(0)
        .max(100)
        .build()
    );

    public ElytraReplace() {
        super(LeavesHack.LEAVES_MISC, "ElytraSwap", "自动替换耐久度过低的鞘翅");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);

        // 穿着鞘翅且耐久度过低时触发
        if (chestStack.isOf(Items.ELYTRA) && getDurabilityPercent(chestStack) <= durability.get()) {
            // 1. 先在快捷栏中寻找
            int hotbarSlot = findBetterElytraInHotbar();
            if (hotbarSlot != -1) {
                swapElytra(hotbarSlot);
                ChatUtils.info("§a从快捷栏替换了鞘翅!");
                return;
            }

            // 2. 快捷栏没有，再从背包中寻找
            int inventorySlot = findBetterElytraInInventory();
            if (inventorySlot != -1) {
                swapElytra(inventorySlot);
                ChatUtils.info("§a从背包替换了鞘翅!");
            }
        }
    }

    /** 获取物品剩余耐久度百分比（空物品按 100 处理，避免除零） */
    private float getDurabilityPercent(ItemStack stack) {
        float maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) return 100f;
        float currentDamage = stack.getDamage();
        return 100f - (currentDamage / maxDamage * 100f);
    }

    /** 在快捷栏（界面槽位 36-44）中查找耐久度更好的鞘翅 */
    private int findBetterElytraInHotbar() {
        for (int i = 36; i <= 44; i++) {
            ItemStack stack = mc.player.currentScreenHandler.getSlot(i).getStack();
            if (stack.isOf(Items.ELYTRA) && getDurabilityPercent(stack) > durability.get()) {
                return i;
            }
        }
        return -1;
    }

    /** 在背包（界面槽位 9-35）中查找耐久度更好的鞘翅 */
    private int findBetterElytraInInventory() {
        for (int i = 9; i <= 35; i++) {
            ItemStack stack = mc.player.currentScreenHandler.getSlot(i).getStack();
            if (stack.isOf(Items.ELYTRA) && getDurabilityPercent(stack) > durability.get()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 执行鞘翅替换（三次 PICKUP 点击与胸甲槽交换）
     * 1.21.11 取证：PlayerScreenHandler 槽位 5-8 为盔甲（HEAD=5, CHEST=6, LEGS=7, FEET=8）
     */
    private void swapElytra(int screenSlot) {
        int syncId = mc.player.currentScreenHandler.syncId;

        // 1. 拾取新鞘翅
        mc.interactionManager.clickSlot(syncId, screenSlot, 0, SlotActionType.PICKUP, mc.player);
        // 2. 与胸甲槽交换
        mc.interactionManager.clickSlot(syncId, 6, 0, SlotActionType.PICKUP, mc.player);
        // 3. 放回旧鞘翅
        mc.interactionManager.clickSlot(syncId, screenSlot, 0, SlotActionType.PICKUP, mc.player);
    }
}

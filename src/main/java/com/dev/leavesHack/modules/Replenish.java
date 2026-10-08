package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.math.Timer;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.SlotActionType;

import java.util.List;

public class Replenish extends Module {
    public static Replenish INSTANCE;
    private static final int HOTBAR_SIZE = 9;
    private final ItemStack[] previousHotbar = new ItemStack[HOTBAR_SIZE];
    private boolean hasSnapshot;

    public Replenish() {
        super(LeavesHack.LEAVES_MISC, "Replenish", "自动补充");
        INSTANCE = this;
    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> pauseWhileUsing = sgGeneral.add(new BoolSetting.Builder()
        .name("PauseWhileUsing")
        .description("使用物品时暂停")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> pauseTime = sgGeneral.add(new IntSetting.Builder()
        .name("PauseTime")
        .description("暂停时间")
        .defaultValue(200)
        .min(0)
        .sliderMax(500)
        .build()
    );
    private final Setting<Boolean> noPotion = sgGeneral.add(new BoolSetting.Builder()
        .name("NoPotion")
        .description("不补充药水")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> noTools = sgGeneral.add(new BoolSetting.Builder()
        .name("NoTools")
        .description("不替换工具")
        .defaultValue(true)
        .build()
    );
    private final Setting<List<Item>> blackList = sgGeneral.add(new ItemListSetting.Builder()
        .name("BlackList")
        .description("黑名单")
        .defaultValue(Items.SHULKER_BOX)
        .build()
    );
    private final Timer pauseTimer = new Timer();

    @Override
    public void onActivate() {
        pauseTimer.setMs(999999);
        clearSnapshot();
    }

    @Override
    public void onDeactivate() {
        clearSnapshot();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) {
            clearSnapshot();
            return;
        }
        if (mc.player.isCreative() || mc.currentScreen != null) {
            clearSnapshot();
            return;
        }
        if (pauseWhileUsing.get() && mc.options.useKey.isPressed()) {
            pauseTimer.reset();
            return;
        }
        if (!pauseTimer.passedMs(pauseTime.get())) return;
        if (hasSnapshot) {
            for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
                if (shouldSkip(previousHotbar[slot])) continue;
                ItemStack currentStack = mc.player.getInventory().getStack(slot);
                if (previousHotbar[slot].getItem() != currentStack.getItem()) {
                    doReplenish(slot);
                }
            }
        }

        saveSnapshot();
    }

    private boolean shouldSkip(ItemStack stack) {
        if (noTools.get()) {
            if (stack.getItem() instanceof PotionItem) return noPotion.get();
            if (stack.getItem() instanceof AxeItem) return true;
            if (stack.isIn(ItemTags.SWORDS)) return true;
            if (stack.getItem() instanceof MaceItem) return true;
            if (stack.getItem() instanceof TridentItem) return true;
            if (stack.isIn(ItemTags.PICKAXES)) return true;
        }
        return blackList.get().contains(stack.getItem());
    }

    private void saveSnapshot() {
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            previousHotbar[slot] = mc.player.getInventory().getStack(slot).copy();
        }
        hasSnapshot = true;
    }

    private void clearSnapshot() {
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            previousHotbar[slot] = ItemStack.EMPTY;
        }
        hasSnapshot = false;
    }

    protected void doReplenish(int slot) {
        int syncId = mc.player.currentScreenHandler.syncId;
        int targetSlot = -1;
        Item item = previousHotbar[slot].getItem();
        if (item instanceof PotionItem) {
            targetSlot = findPotionSlot(previousHotbar[slot]);
            if (targetSlot == -1) return;
        } else {
            targetSlot = findItemInventorySlot(item);
            if (targetSlot == -1) return;
        }
        if (mc.player.getInventory().getStack(slot).isEmpty()) {
            mc.interactionManager.clickSlot(syncId, targetSlot, slot, SlotActionType.SWAP, mc.player);
        } else {
            int currentCount = mc.player.getInventory().getStack(slot).getCount();
            int invCount = mc.player.getInventory().getStack(targetSlot).getCount();
            if (currentCount + invCount > 64 || !previousHotbar[slot].isStackable()) {
                mc.interactionManager.clickSlot(syncId, targetSlot, slot, SlotActionType.SWAP, mc.player);
            } else {
                mc.interactionManager.clickSlot(syncId, targetSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
            }
        }
    }

    private int findPotionSlot(ItemStack stack) {
        for (int i = 9; i < 35; ++i) {
            ItemStack currentStack = mc.player.getInventory().getStack(i);
            if (getPotionId(currentStack).equals(getPotionId(stack))) return i;
        }
        return -1;
    }

    public int findItemInventorySlot(Item item) {
        for (int i = 9; i < 35; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == item) return i;
        }
        return -1;
    }
    private String getPotionId(ItemStack stack) {
        if (stack.getItem() instanceof PotionItem) {
            PotionContentsComponent potionContents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (potionContents != null) {
                StringBuilder sb = new StringBuilder();
                for (StatusEffectInstance effect : potionContents.getEffects()) {
                    RegistryEntry<StatusEffect> effectType = effect.getEffectType();
                    effectType.getKey().ifPresent(key -> {
                        if (!sb.isEmpty()) sb.append("+");
                        sb.append(key.getValue().toString());
                    });
                }
                return sb.toString();
            }
        }
        return "";
    }
}

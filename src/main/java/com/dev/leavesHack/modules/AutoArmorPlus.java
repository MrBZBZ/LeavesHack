package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.asm.accessors.IClientWorld;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

import java.util.HashMap;
import java.util.Map;

import static com.dev.leavesHack.utils.rotation.Rotation.sendPacket;

public class AutoArmorPlus extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
            private final Setting<Boolean> ignoreBinding = sgGeneral.add(new BoolSetting.Builder()
        .name("IgnoreBinding")
        .description("忽略绑定诅咒")
        .defaultValue(false)
        .build()
    );
        private final Setting<Boolean> doubleTapTakeoff = sgGeneral.add(new BoolSetting.Builder()
        .name("DoubleTapTakeoff")
        .description("双击空格切鞘翅起飞（装备同步+下落段自动起滑）")
        .defaultValue(true)
        .build()
    );
    private final Setting<Double> jumpDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("DoubleTapWindow")
        .description("双击空格的有效时间窗口（秒）")
        .defaultValue(0.317)
        .min(0.0)
        .sliderMax(1.0)
        .visible(doubleTapTakeoff::get)
        .build()
    );
    private long lastPressTime = 0L;
    private boolean wasJumpPressed = false;
    private int elytraSwapPhase = 0;
    private int elytraSwapSlot = -1;
    private int elytraSwapTicks = 0;
    public AutoArmorPlus() {
        super(LeavesHack.LEAVES_COMBAT, "AutoArmorPlus", "双击空格起飞");
    }
    @Override
    public void onActivate() {
    }
    @EventHandler
    public void onTick(TickEvent.Pre event){
        if (mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen) && !(mc.currentScreen instanceof InventoryScreen) && !(mc.currentScreen instanceof WidgetScreen)) {
            return;
        }
        if (mc.player.playerScreenHandler != mc.player.currentScreenHandler) return;
        // 双击空格起飞切鞘翅（每 tick 检测）
        if (doubleTapTakeoff.get()) handleDoubleTapTakeoff();
    }
    /** 双击空格：鞘翅换入胸甲槽起飞（原版 MeteorPlusPlus ElytraAndArmor 逻辑 1:1 移植） */
    private void handleDoubleTapTakeoff() {
        // 右键装备状态机: 1=已静默切槽，本 tick 右键装备
        if (elytraSwapPhase == 1) {
            if (++elytraSwapTicks > 5) { elytraSwapPhase = 0; return; }
            if (mc.player != null && mc.world != null) {
                try (PendingUpdateManager pm = ((IClientWorld) mc.world).invokeGetPendingUpdateManager().incrementSequence()) {
                    int sequence = pm.getSequence();
                    mc.getNetworkHandler().sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, mc.player.getYaw(), mc.player.getPitch()));
                }
            }
            elytraSwapPhase = 2; // 确认态: 等胸部同步+离地+下落段才起滑(服务端canGlide确定性成立)
            elytraSwapTicks = 0;
            return;
        }
        if (elytraSwapPhase == 2) {
            if (++elytraSwapTicks > 15) { elytraSwapPhase = 0; return; }
            // 起滑条件全部确定性成立后才发START: 胸部鞘翅已同步 + 已离地 + 下落段(vel.y<0)
            // 双击过快时的地面/上升段绝不启滑(服务端canGlide的!onGround为假会拒旗->客户端旗分裂=卡地+Simulation)
            if (mc.player.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST).isOf(net.minecraft.item.Items.ELYTRA)
                && !mc.player.isOnGround() && mc.player.getVelocity().y < 0 && !mc.player.isGliding()) {
                mc.player.startGliding();
            }
            if (mc.player.isGliding()) elytraSwapPhase = 0;
            return;
        }
        boolean isJumpPressed = mc.options.jumpKey.isPressed();

        // 上升沿检测：松开后再按下才算一次有效点击，避免长按触发
        if (isJumpPressed && !wasJumpPressed) {
            long now = System.currentTimeMillis();
            double threshold = jumpDelay.get() * 1000.0;

            if (lastPressTime != 0L && now - lastPressTime <= threshold) {
                // 双击判定成立；已穿鞘翅则只重置计时，不切换
                ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
                if (!chest.isOf(Items.ELYTRA)) {
                    // 胸甲槽为空或为普通胸甲时，切换为鞘翅
                    if (chest.isEmpty() || isChestplateOrEmpty(chest)) {
                        // 与 FireworkElytraFly 的 GrimDurability/AutoSpear 模式互斥（它们自行管理鞘翅换装）
                        FireworkElytraFly fly = Modules.get().get(FireworkElytraFly.class);
                        boolean flyManaging = fly != null && fly.isActive()
                            && (FireworkElytraFly.INSTANCE.mode.get() == FireworkElytraFly.Mode.GrimDurability
                                || FireworkElytraFly.INSTANCE.mode.get() == FireworkElytraFly.Mode.AutoSpear);
                        if (!flyManaging) {
                            if (mc.currentScreen != null) mc.currentScreen.close();
                            FindItemResult elytraSlot = InvUtils.find(Items.ELYTRA);
                            if (elytraSlot.found() && elytraSlot.slot() < 9) {
                                // 快捷栏鞘翅：t0 静默切槽 → t1 右键装备（USE_ITEM 不受 MultiActionsC/PacketOrderE 管辖，零 CLICK_WINDOW）
                                elytraSwapSlot = elytraSlot.slot();
                                if (mc.player.getInventory().getSelectedSlot() != elytraSwapSlot) {
                                    InventoryUtil.switchToSlot(elytraSwapSlot);
                                }
                                elytraSwapPhase = 1;
                                elytraSwapTicks = 0;
                            } else if (elytraSlot.found()) {
                                // 背包鞘翅：原版 clickSlot 切换（建议放快捷栏走右键）
                                InvUtils.move().from(elytraSlot.slot()).toArmor(2);
                            }
                        }
                    }
                }
                lastPressTime = 0L; // 双击触发后重置时间，防止连续多次点击被误判为多次双击
            } else {
                // 第一次点击，或者超时后的点击，记录时间
                lastPressTime = now;
            }
        }

        // 更新上一 Tick 的按键状态
        wasJumpPressed = isJumpPressed;
    }

    private boolean isChestplateOrEmpty(ItemStack stack) {
        if (stack.isEmpty()) return true;
        return stack.contains(DataComponentTypes.EQUIPPABLE)
            && stack.get(DataComponentTypes.EQUIPPABLE).slot() == EquipmentSlot.CHEST;
    }

    private int getBaseArmorScore(ItemStack itemStack) {
        if (!itemStack.contains(DataComponentTypes.ATTRIBUTE_MODIFIERS)) return 0;
        int score = 0;
        AttributeModifiersComponent component = itemStack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        for (AttributeModifiersComponent.Entry modifier : component.modifiers()) {
            if (modifier.attribute() == EntityAttributes.ARMOR || modifier.attribute() == EntityAttributes.ARMOR_TOUGHNESS) {
                double e = modifier.modifier().value();
                score += switch (modifier.modifier().operation()) {
                    case ADD_VALUE -> (int) e;
                    case ADD_MULTIPLIED_BASE -> (int) (e * mc.player.getAttributeBaseValue(modifier.attribute())); // 乘基础值
                    case ADD_MULTIPLIED_TOTAL -> 0;
                };
            }
        }
        return score;
    }
}

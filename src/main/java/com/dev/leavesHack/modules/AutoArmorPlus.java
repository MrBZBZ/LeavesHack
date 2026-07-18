package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
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
    private Timer timer = new Timer();
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("Delay")
        .description("操作延迟(毫秒MS)")
        .defaultValue(10)
        .min(0)
        .sliderMax(1000)
        .build()
    );
    private final Setting<Boolean> autoElytra = sgGeneral.add(new BoolSetting.Builder()
        .name("AutoElytra")
        .description("自动切换鞘翅")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> ignoreBinding = sgGeneral.add(new BoolSetting.Builder()
        .name("IgnoreBinding")
        .description("忽略绑定诅咒")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> snowBug = sgGeneral.add(new BoolSetting.Builder()
        .name("SnowBug")
        .description("")
        .defaultValue(false)
        .build()
    );
    public AutoArmorPlus() {
        super(LeavesHack.CATEGORY, "AutoArmorPlus", "自动穿甲与鞘翅切换");
    }
    @Override
    public void onActivate() {
        timer.setMs(999999);
    }
    @EventHandler
    public void onTick(TickEvent.Pre event){
        if (mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen) && !(mc.currentScreen instanceof InventoryScreen) && !(mc.currentScreen instanceof WidgetScreen)) {
            return;
        }
        if (mc.player.playerScreenHandler != mc.player.currentScreenHandler) return;
        if (!timer.passedMs(delay.get())) return;
        timer.reset();
        Map<EquipmentSlot, int[]> armorMap = new HashMap<>(4);
        armorMap.put(EquipmentSlot.FEET, new int[]{36, getProtection(mc.player.getInventory().getStack(36)), -1, -1});
        armorMap.put(EquipmentSlot.LEGS, new int[]{37, getProtection(mc.player.getInventory().getStack(37)), -1, -1});
        armorMap.put(EquipmentSlot.CHEST, new int[]{38, getProtection(mc.player.getInventory().getStack(38)), -1, -1});
        armorMap.put(EquipmentSlot.HEAD, new int[]{39, getProtection(mc.player.getInventory().getStack(39)), -1, -1});
        for (int s = 0; s < 36; s++) {
            if (!(mc.player.getInventory().getStack(s).contains(DataComponentTypes.EQUIPPABLE)) && mc.player.getInventory().getStack(s).getItem() != Items.ELYTRA)
                continue;
            int protection = getProtection(mc.player.getInventory().getStack(s));
            EquipmentSlot slot = (mc.player.getInventory().getStack(s).getItem() == Items.ELYTRA ? EquipmentSlot.CHEST : mc.player.getInventory().getStack(s).get(DataComponentTypes.EQUIPPABLE).slot());
            for (Map.Entry<EquipmentSlot, int[]> e : armorMap.entrySet()) {
                if (e.getKey() == EquipmentSlot.FEET) {
                    if (mc.player.hurtTime > 1 && snowBug.get()) {
                        if (!mc.player.getInventory().getStack(36).isEmpty() && mc.player.getInventory().getStack(36).getItem() == Items.LEATHER_BOOTS) {
                            continue;
                        }
                        if (!mc.player.getInventory().getStack(s).isEmpty() && mc.player.getInventory().getStack(s).getItem() == Items.LEATHER_BOOTS) {
                            e.getValue()[2] = s;
                            continue;
                        }
                    }
                }
                FireworkElytraFly fireworkElytraFly = Modules.get().get(FireworkElytraFly.class);
                if (autoElytra.get() && fireworkElytraFly.isActive() && e.getKey() == EquipmentSlot.CHEST) {
                    if (FireworkElytraFly.INSTANCE.mode.get() == FireworkElytraFly.Mode.GrimDurability || FireworkElytraFly.INSTANCE.mode.get() == FireworkElytraFly.Mode.AutoSpear) continue;
                    if (!mc.player.getInventory().getStack(38).isEmpty() && mc.player.getInventory().getStack(38).getItem() == Items.ELYTRA && mc.player.getInventory().getStack(38).isDamageable() && mc.player.getInventory().getStack(38).getDamage() < mc.player.getInventory().getStack(38).getMaxDamage()) {
                        continue;
                    }
                    if (e.getValue()[2] != -1 && !mc.player.getInventory().getStack(e.getValue()[2]).isEmpty() && mc.player.getInventory().getStack(e.getValue()[2]).getItem() == Items.ELYTRA && mc.player.getInventory().getStack(e.getValue()[2]).isDamageable() && mc.player.getInventory().getStack(e.getValue()[2]).getDamage() < mc.player.getInventory().getStack(e.getValue()[2]).getMaxDamage()) {
                        continue;
                    }
                    if (!mc.player.getInventory().getStack(s).isEmpty() && mc.player.getInventory().getStack(s).getItem() == Items.ELYTRA && mc.player.getInventory().getStack(s).isDamageable() && mc.player.getInventory().getStack(s).getDamage() < mc.player.getInventory().getStack(s).getMaxDamage()) {
                        e.getValue()[2] = s;
                    }
                    continue;
                }
                if (protection > 0) {
                    if (e.getKey() == slot) {
                        if (protection > e.getValue()[1] && protection > e.getValue()[3]) {
                            e.getValue()[2] = s;
                            e.getValue()[3] = protection;
                        }
                    }
                }
            }
        }
        for (Map.Entry<EquipmentSlot, int[]> equipmentSlotEntry : armorMap.entrySet()) {
            if (equipmentSlotEntry.getValue()[2] != -1) {
                if (equipmentSlotEntry.getValue()[1] == -1 && equipmentSlotEntry.getValue()[2] < 9) {
/*					if (equipmentSlotEntry.getValue()[2] != mc.player.getInventory().selectedSlot) {
						mc.player.getInventory().selectedSlot = equipmentSlotEntry.getValue()[2];
						sendPacket(new UpdateSelectedSlotC2SPacket(equipmentSlotEntry.getValue()[2]));
					}*/
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 36 + equipmentSlotEntry.getValue()[2], 1, SlotActionType.QUICK_MOVE, mc.player);
                    sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                } else if (mc.player.playerScreenHandler == mc.player.currentScreenHandler) {
                    int armorSlot = (equipmentSlotEntry.getValue()[0] - 34) + (39 - equipmentSlotEntry.getValue()[0]) * 2;
                    int newArmorSlot = equipmentSlotEntry.getValue()[2] < 9 ? 36 + equipmentSlotEntry.getValue()[2] : equipmentSlotEntry.getValue()[2];
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, newArmorSlot, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, armorSlot, 0, SlotActionType.PICKUP, mc.player);
                    if (equipmentSlotEntry.getValue()[1] != -1)
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, newArmorSlot, 0, SlotActionType.PICKUP, mc.player);
                    sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                }
                return;
            }
        }
    }
    private int getProtection(ItemStack is) {
        if (is.contains(DataComponentTypes.EQUIPPABLE) || is.getItem() == Items.ELYTRA) {
            int prot = 0;
            if (is.getItem() == Items.ELYTRA) {
                if (!(is.isDamageable() && is.getDamage() < is.getMaxDamage())) return 0;
                prot = 1;
            }
            if (is.hasEnchantments()) {
                if (ignoreBinding.get() && InventoryUtil.hasEnchantment(is, Enchantments.BINDING_CURSE)) return -1;
                prot += InventoryUtil.getEnchantmentLevel(is, Enchantments.PROTECTION);
            }
            return (is.contains(DataComponentTypes.EQUIPPABLE) ? getBaseArmorScore(is) : 0) + prot;
        } else if (!is.isEmpty()) {
            return 0;
        }
        return -1;
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

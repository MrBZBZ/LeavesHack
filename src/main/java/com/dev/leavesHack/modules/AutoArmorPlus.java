package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import java.util.HashMap;
import java.util.Map;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

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
        super(LeavesHack.LEAVES_COMBAT, "AutoArmorPlus", "自动穿甲与鞘翅切换");
    }
    @Override
    public void onActivate() {
        timer.setMs(999999);
    }
    @EventHandler
    public void onTick(TickEvent.Pre event){
        if (mc.screen != null && !(mc.screen instanceof ChatScreen) && !(mc.screen instanceof InventoryScreen) && !(mc.screen instanceof WidgetScreen)) {
            return;
        }
        if (mc.player.inventoryMenu != mc.player.containerMenu) return;
        if (!timer.passedMs(delay.get())) return;
        timer.reset();
        Map<EquipmentSlot, int[]> armorMap = new HashMap<>(4);
        armorMap.put(EquipmentSlot.FEET, new int[]{36, getProtection(mc.player.getInventory().getItem(36)), -1, -1});
        armorMap.put(EquipmentSlot.LEGS, new int[]{37, getProtection(mc.player.getInventory().getItem(37)), -1, -1});
        armorMap.put(EquipmentSlot.CHEST, new int[]{38, getProtection(mc.player.getInventory().getItem(38)), -1, -1});
        armorMap.put(EquipmentSlot.HEAD, new int[]{39, getProtection(mc.player.getInventory().getItem(39)), -1, -1});
        for (int s = 0; s < 36; s++) {
            if (!(mc.player.getInventory().getItem(s).has(DataComponents.EQUIPPABLE)) && mc.player.getInventory().getItem(s).getItem() != Items.ELYTRA)
                continue;
            int protection = getProtection(mc.player.getInventory().getItem(s));
            EquipmentSlot slot = (mc.player.getInventory().getItem(s).getItem() == Items.ELYTRA ? EquipmentSlot.CHEST : mc.player.getInventory().getItem(s).get(DataComponents.EQUIPPABLE).slot());
            for (Map.Entry<EquipmentSlot, int[]> e : armorMap.entrySet()) {
                if (e.getKey() == EquipmentSlot.FEET) {
                    if (mc.player.hurtTime > 1 && snowBug.get()) {
                        if (!mc.player.getInventory().getItem(36).isEmpty() && mc.player.getInventory().getItem(36).getItem() == Items.LEATHER_BOOTS) {
                            continue;
                        }
                        if (!mc.player.getInventory().getItem(s).isEmpty() && mc.player.getInventory().getItem(s).getItem() == Items.LEATHER_BOOTS) {
                            e.getValue()[2] = s;
                            continue;
                        }
                    }
                }
                FireworkElytraFly fireworkElytraFly = Modules.get().get(FireworkElytraFly.class);
                if (autoElytra.get() && fireworkElytraFly.isActive() && e.getKey() == EquipmentSlot.CHEST) {
                    if (FireworkElytraFly.INSTANCE.mode.get() == FireworkElytraFly.Mode.GrimDurability || FireworkElytraFly.INSTANCE.mode.get() == FireworkElytraFly.Mode.AutoSpear) continue;
                    if (!mc.player.getInventory().getItem(38).isEmpty() && mc.player.getInventory().getItem(38).getItem() == Items.ELYTRA && mc.player.getInventory().getItem(38).isDamageableItem() && mc.player.getInventory().getItem(38).getDamageValue() < mc.player.getInventory().getItem(38).getMaxDamage()) {
                        continue;
                    }
                    if (e.getValue()[2] != -1 && !mc.player.getInventory().getItem(e.getValue()[2]).isEmpty() && mc.player.getInventory().getItem(e.getValue()[2]).getItem() == Items.ELYTRA && mc.player.getInventory().getItem(e.getValue()[2]).isDamageableItem() && mc.player.getInventory().getItem(e.getValue()[2]).getDamageValue() < mc.player.getInventory().getItem(e.getValue()[2]).getMaxDamage()) {
                        continue;
                    }
                    if (!mc.player.getInventory().getItem(s).isEmpty() && mc.player.getInventory().getItem(s).getItem() == Items.ELYTRA && mc.player.getInventory().getItem(s).isDamageableItem() && mc.player.getInventory().getItem(s).getDamageValue() < mc.player.getInventory().getItem(s).getMaxDamage()) {
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
                    mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, 36 + equipmentSlotEntry.getValue()[2], 1, ContainerInput.QUICK_MOVE, mc.player);
                    sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                } else if (mc.player.inventoryMenu == mc.player.containerMenu) {
                    int armorSlot = (equipmentSlotEntry.getValue()[0] - 34) + (39 - equipmentSlotEntry.getValue()[0]) * 2;
                    int newArmorSlot = equipmentSlotEntry.getValue()[2] < 9 ? 36 + equipmentSlotEntry.getValue()[2] : equipmentSlotEntry.getValue()[2];
                    mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, newArmorSlot, 0, ContainerInput.PICKUP, mc.player);
                    mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, armorSlot, 0, ContainerInput.PICKUP, mc.player);
                    if (equipmentSlotEntry.getValue()[1] != -1)
                        mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, newArmorSlot, 0, ContainerInput.PICKUP, mc.player);
                    sendPacket(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
                }
                return;
            }
        }
    }
    private int getProtection(ItemStack is) {
        if (is.has(DataComponents.EQUIPPABLE) || is.getItem() == Items.ELYTRA) {
            int prot = 0;
            if (is.getItem() == Items.ELYTRA) {
                if (!(is.isDamageableItem() && is.getDamageValue() < is.getMaxDamage())) return 0;
                prot = 1;
            }
            if (is.isEnchanted()) {
                if (ignoreBinding.get() && InventoryUtil.hasEnchantment(is, Enchantments.BINDING_CURSE)) return -1;
                prot += InventoryUtil.getEnchantmentLevel(is, Enchantments.PROTECTION);
            }
            return (is.has(DataComponents.EQUIPPABLE) ? getBaseArmorScore(is) : 0) + prot;
        } else if (!is.isEmpty()) {
            return 0;
        }
        return -1;
    }
    private int getBaseArmorScore(ItemStack itemStack) {
        if (!itemStack.has(DataComponents.ATTRIBUTE_MODIFIERS)) return 0;
        int score = 0;
        ItemAttributeModifiers component = itemStack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        for (ItemAttributeModifiers.Entry modifier : component.modifiers()) {
            if (modifier.attribute() == Attributes.ARMOR || modifier.attribute() == Attributes.ARMOR_TOUGHNESS) {
                double e = modifier.modifier().amount();
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

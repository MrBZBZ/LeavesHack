package com.dev.leavesHack.utils.entity;

import com.dev.leavesHack.modules.GlobalSetting;
import com.dev.leavesHack.utils.world.BlockUtil;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import java.util.Set;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class InventoryUtil {
    public static final InventoryUtil INSTANCE = new InventoryUtil();
    private InventoryUtil() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }
    static int lastSlot = -1;
    public static int serverSlot = 0;
    static int lastSelect = -1;
    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof ServerboundSetCarriedItemPacket packet) {
            if (GlobalSetting.INSTANCE.noBadPackets.get() && packet.getSlot() == serverSlot) {
                event.cancel();
            }
            serverSlot = packet.getSlot();
        }
    }
//    public static int getEquipmentLevel(Player player, ResourceKey<Enchantment> enchantmentKey) {
//        int maxLevel = 0;
//        for (ItemStack stack : player.getArmorItems()) {
//            if (!stack.isEmpty()) {
//                int level = getEnchantmentLevel(stack, enchantmentKey);
//                if (level > maxLevel) {
//                    maxLevel = level;
//                }
//            }
//        }
//        return maxLevel;
//    }
    public static boolean hasEnchantment(ItemStack itemStack, ResourceKey<Enchantment> enchantment) {
        return getEnchantmentLevel(itemStack, enchantment) > 0;
    }
    public static int getEnchantmentLevel(ItemStack itemStack, ResourceKey<Enchantment> enchantment) {
        if (itemStack.isEmpty()) return 0;
        Object2IntMap<Holder<Enchantment>> itemEnchantments = new Object2IntArrayMap<>();
        getEnchantments(itemStack, itemEnchantments);
        return getEnchantmentLevel(itemEnchantments, enchantment);
    }
    public static int getEnchantmentLevel(Object2IntMap<Holder<Enchantment>> itemEnchantments, ResourceKey<Enchantment> enchantment) {
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : Object2IntMaps.fastIterable(itemEnchantments)) {
            if (entry.getKey().is(enchantment)) return entry.getIntValue();
        }
        return 0;
    }
    public static void getEnchantments(ItemStack itemStack, Object2IntMap<Holder<Enchantment>> enchantments) {
        enchantments.clear();

        if (!itemStack.isEmpty()) {
            Set<Object2IntMap.Entry<Holder<Enchantment>>> itemEnchantments = itemStack.getItem() == Items.ENCHANTED_BOOK
                ? itemStack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet()
                : itemStack.getEnchantments().entrySet();

            for (Object2IntMap.Entry<Holder<Enchantment>> entry : itemEnchantments) {
                enchantments.put(entry.getKey(), entry.getIntValue());
            }
        }
    }
    public static void inventorySwap(int slot, int selectedSlot) {
        if (slot == lastSlot) {
            switchToSlot(lastSelect);
            lastSlot = -1;
            lastSelect = -1;
            return;
        }
        if (slot - 36 == selectedSlot) return;
        if (slot - 36 >= 0) {
            lastSlot = slot;
            lastSelect = selectedSlot;
            switchToSlot(slot - 36);
            return;
        }
        mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, slot, selectedSlot, ContainerInput.SWAP, mc.player);
    }
    public static int findItemInventorySlot(Item item) {
        for (int i = 0; i < 45; ++i) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() == item) return i < 9 ? i + 36 : i;
        }
        return -1;
    }
    public static int findBlock() {
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = getStackInSlot(i);
            if (stack.getItem() instanceof BlockItem && !BlockUtil.shiftBlocks.contains(Block.byItem(stack.getItem())) && ((BlockItem) stack.getItem()).getBlock() != Blocks.COBWEB)
                return i;
        }
        return -1;
    }
    public static int findSlabBlock() {
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = getStackInSlot(i);
            if (stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                if (block instanceof SlabBlock) {
                    return i;
                }
            }
        }
        return -1;
    }
    public static ItemStack getStackInSlot(int i) {
        return mc.player.getInventory().getItem(i);
    }
    public static void switchToSlot(int slot) {
        if (GlobalSetting.INSTANCE.clientSwitch.get()) mc.player.getInventory().setSelectedSlot(slot);
        sendPacket(new ServerboundSetCarriedItemPacket(slot));
    }
    public enum MineSwitchMode {
        Delay,
        Silent,
        None
    }
    public static int findItem(Item input) {
        for (int i = 0; i < 9; ++i) {
            Item item = getStackInSlot(i).getItem();
            if (Item.getId(item) != Item.getId(input)) continue;
            return i;
        }
        return -1;
    }
    public static int findClass(Class clazz) {
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = getStackInSlot(i);
            if (stack == ItemStack.EMPTY) continue;
            if (clazz.isInstance(stack.getItem())) {
                return i;
            }
            if (!(stack.getItem() instanceof BlockItem) || !clazz.isInstance(((BlockItem) stack.getItem()).getBlock()))
                continue;
            return i;
        }
        return -1;
    }
    public static int findClassInventory(Class clazz) {
        for (int i = 0; i < 45; ++i) {
            ItemStack stack = getStackInSlot(i);
            if (stack == ItemStack.EMPTY) continue;
            if (clazz.isInstance(stack.getItem())) {
                return i < 9 ? i + 36 : i;
            }
            if (!(stack.getItem() instanceof BlockItem) || !clazz.isInstance(((BlockItem) stack.getItem()).getBlock()))
                continue;
            return i < 9 ? i + 36 : i;
        }
        return -1;
    }
    public static void sendPacket(Packet<?> packet) {
        mc.getConnection().send(packet);
    }

    public static int findBlock(Block block) {
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = getStackInSlot(i);
            if (stack.getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() == block) {
                    return i;
                }
            }
        }
        return -1;
    }
    public static int findBlockInventory(Block block) {
        for (int i = 0; i < 45; ++i) {
            ItemStack stack = getStackInSlot(i);
            if (stack.getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() == block) {
                    return i < 9 ? i + 36 : i;
                }
            }
        }
        return -1;
    }
}

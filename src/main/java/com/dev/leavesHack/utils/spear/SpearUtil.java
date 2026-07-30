package com.dev.leavesHack.utils.spear;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;

import java.util.Set;

/**
 * 长矛（Spear）识别工具类。
 * <p>
 * ViaBackwards 在服务端做协议降级（1.21.11 → 1.21.1）时，将矛翻译成剑，
 * 同时在 minecraft:custom_data 中写入原始物品的协议 ID：
 * <pre>
 *   "minecraft:custom_data": {
 *     "VB|Protocol1_21_11To1_21_9|id": 1302  // 下界合金矛
 *   }
 * </pre>
 * <p>
 * 本类通过正则匹配 custom_data 中的 VB|Protocol*|id 键来识别长矛，
 * 并按协议 ID 区分材质。同时保留 Lore "lunge" 和 custom_name 作为兜底。
 */
public class SpearUtil {

    // ===== 1.21.11 矛的协议物品 ID =====

    private static final Set<Integer> SPEAR_PROTOCOL_IDS = Set.of(
        1296, // wooden_spear
        1297, // stone_spear
        1298, // copper_spear
        1299, // iron_spear
        1300, // golden_spear
        1301, // diamond_spear
        1302  // netherite_spear
    );

    /**
     * 从 custom_data 中提取 VB|Protocol*|id 的值。
     * 返回 null 表示未找到。
     */
    private static Integer getProtocolId(ItemStack stack) {
        NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (customData == null) return null;

        NbtCompound nbt = customData.copyNbt();
        for (String key : nbt.getKeys()) {
            if (key.matches("^VB\\|Protocol.+id$")) {
                NbtElement element = nbt.get(key);
                if (element instanceof AbstractNbtNumber num) {
                    return num.intValue();
                }
            }
        }
        return null;
    }

    /**
     * 判断物品是否为长矛。
     * 检测顺序：enchantments 中的 lunge → Lore → custom_name。
     */
    public static boolean isSpear(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        // 1) 附魔包含 "lunge" — 1.21.11 长矛专属附魔
        if (hasLungeInEnchantments(stack)) {
            return true;
        }

        // 2) Lore 包含 "lunge" — ViaVersion 将未知附魔转为 Lore 文本
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                if (line.getString().toLowerCase().contains("lunge")) {
                    return true;
                }
            }
        }

        // 3) custom_name 包含关键词（兜底）
        Text customName = stack.get(DataComponentTypes.CUSTOM_NAME);
        if (customName != null) {
            String name = customName.getString().toLowerCase();
            if (name.contains("spear") || name.contains("长矛") || name.contains("突进")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 获取长矛的协议 ID（用于区分材质）。返回 -1 表示无法识别。
     */
    public static int getSpearProtocolId(ItemStack stack) {
        Integer id = getProtocolId(stack);
        return id != null ? id : -1;
    }

    /**
     * 检查 enchantments 组件中是否包含 "lunge" 附魔。
     * <p>
     * ViaVersion 在 1.21.11→1.21.1 翻译时，可能将 lunge（未知附魔）保留在
     * enchantments NBT 中而不是转为 Lore。本方法遍历 enchantments 的 registry entry
     * 检查其 ID 路径是否以 "lunge" 结尾。
     */
    private static boolean hasLungeInEnchantments(ItemStack stack) {
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) return false;

        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments.getEnchantmentEntries()) {
            String id = entry.getKey().getIdAsString();
            if (id.endsWith("lunge")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断长矛是否带有"突进"（Lunge）附魔。
     * <p>
     * 同时检查 enchantments 组件和 Lore 文本。ViaVersion 在不同配置下
     * 可能将 lunge 附魔保留在 enchantments 中，也可能转为 Lore 文本。
     *
     * @return true 表示物品带有 Lunge 附魔
     */
    public static boolean hasLunge(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        // 1) enchantments 组件中包含 lunge（直接来自 1.21.11 协议）
        if (hasLungeInEnchantments(stack)) {
            return true;
        }

        // 2) Lore 文本中包含 lunge（ViaVersion 将未知附魔转为 Lore）
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                if (line.getString().toLowerCase().contains("lunge")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取长矛材质名称（基于协议 ID）。
     */
    public static String getSpearMaterial(ItemStack stack) {
        int id = getSpearProtocolId(stack);
        return switch (id) {
            case 1296 -> "wooden";
            case 1297 -> "stone";
            case 1298 -> "copper";
            case 1299 -> "iron";
            case 1300 -> "golden";
            case 1301 -> "diamond";
            case 1302 -> "netherite";
            default -> "unknown";
        };
    }
}

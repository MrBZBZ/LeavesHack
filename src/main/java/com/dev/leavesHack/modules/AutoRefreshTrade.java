package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.dev.leavesHack.utils.world.BlockUtil;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.EnchantmentListSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import static meteordevelopment.meteorclient.utils.Utils.getEnchantments;

public class AutoRefreshTrade extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("Range")
        .description("操作距离")
        .defaultValue(5)
        .min(0)
        .sliderMax(12)
        .build()
    );
    private final Setting<Integer> wallRange = sgGeneral.add(new IntSetting.Builder()
        .name("WallRange")
        .description("穿墙操作距离")
        .defaultValue(5)
        .min(0)
        .sliderMax(12)
        .build()
    );
    private final Setting<Integer> waitMine = sgGeneral.add(new IntSetting.Builder()
        .name("WaitMineDelay")
        .description("等待挖掘延迟(毫秒MS)")
        .defaultValue(5000)
        .min(0)
        .sliderMax(10000)
        .build()
    );
    private final Setting<Set<ResourceKey<Enchantment>>> enchantmentList = sgGeneral.add(new EnchantmentListSetting.Builder()
        .name("EnchantmentsList")
        .description("目标附魔列表")
        .build()
    );
    private final Setting<Integer> enchantmentLevel = sgGeneral.add(new IntSetting.Builder()
        .name("Level")
        .description("目标附魔等级")
        .defaultValue(3)
        .min(0)
        .sliderMax(5)
        .build()
    );
    public AutoRefreshTrade() {
        super(LeavesHack.LEAVES_MISC, "AutoRefreshTrade", "自动刷交易附魔书");
    }
    public BlockPos pos = null;
    public Timer timer = new Timer();
    @Override
    public void onActivate() {
        pos = null;
        timer.setMs(999999);
    }
    @EventHandler
    public void onTick(TickEvent.Pre event){
        if (!timer.passedMs(waitMine.get())) return;
        if (mc.options.keyDown.isDown()) {
            toggle();
            return;
        }
        if (pos != null && mc.level.isEmptyBlock(pos)) {
            int slot = findItem(Items.LECTERN);
            int old = mc.player.getInventory().getSelectedSlot();
            if (slot != -1) {
                InventoryUtil.switchToSlot(slot);
                Direction side = BlockUtil.getPlaceSide(pos, null);
                if (side != null) {
                    BlockUtil.placeBlock(pos, side, true);
                    InventoryUtil.switchToSlot(old);
                    Rotation.snapBack();
                }
            }
            return;
        }
        Villager target = getTarget();
        if (target == null) return;
        Rotation.snapAt(target.getEyePosition());
        Vec3 playerPos = mc.player.getEyePosition();
        Vec3 villagerPos = target.getEyePosition();
        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
                mc.player,
                playerPos,
                villagerPos,
                target.getBoundingBox(),
                Entity::isPickable,
                playerPos.distanceToSqr(villagerPos)
        );
        if (hitResult == null) {
            mc.gameMode.interact(
                    mc.player,
                    target,
                    new EntityHitResult(target, target.getBoundingBox().getCenter()),
                    InteractionHand.MAIN_HAND
            );
        } else {
            InteractionResult result = mc.gameMode.interact(
                    mc.player,
                    target,
                    hitResult,
                    InteractionHand.MAIN_HAND
            );
            if (!result.consumesAction()) {
                mc.gameMode.interact(
                        mc.player,
                        target,
                        new EntityHitResult(target, target.getBoundingBox().getCenter()),
                        InteractionHand.MAIN_HAND
                );
            }
        }
        if (mc.player.containerMenu instanceof MerchantMenu handler) {
            MerchantOffers list = handler.getOffers();
            AtomicBoolean find = new AtomicBoolean(false);
            boolean findBook = false;
            for (int size = 0; size < list.size(); ++size) {
                MerchantOffer tradeOffer = list.get(size);
                Item item = tradeOffer.getResult().getItem();
                ItemStack sellStack = tradeOffer.getResult();
                if (item == Items.ENCHANTED_BOOK) {
                    findBook = true;
                    ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(sellStack);
                    enchantments.keySet().forEach(entry -> {
                        int level = enchantments.getLevel(entry);
                        int maxLevel = entry.value().getMaxLevel();
                        String name = Enchantment.getFullname(entry, level).getString();
                        info("[LeavesHack]本次结果 " + name);
                        for (ResourceKey<Enchantment> enchantmentKey : enchantmentList.get()){
                            if (hasEnchantments(sellStack, enchantmentKey) && (level >= enchantmentLevel.get() || level == maxLevel)) {
                                find.set(true);
                                mc.player.sendOverlayMessage(Component.nullToEmpty("[LeavesHack]:已找到所需附魔"));
                                return;
                            }
                        }
                    });
                }
            }
            if (!findBook) info("[LeavesHack]:本次未找到附魔书");
            mc.getConnection().send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
            mc.screen.onClose();
            if (find.get()) {
                toggle();
                return;
            }
            Direction facing1 = mc.player.getDirection();
            switch (facing1) {
                case NORTH -> pos = mc.player.blockPosition().north();
                case SOUTH -> pos = mc.player.blockPosition().south();
                case EAST -> pos = mc.player.blockPosition().east();
                case WEST -> pos = mc.player.blockPosition().west();
                default -> pos = mc.player.blockPosition();
            }
            Rotation.snapAt(pos.getCenter());
            mc.gameMode.startDestroyBlock(pos, BlockUtils.getClosestPlaceSide(pos));
            timer.reset();
        }
    }
    public int findItem(Item input) {
        for (int i = 0; i < 9; ++i) {
            Item item = getStackInSlot(i).getItem();
            if (Item.getId(item) != Item.getId(input)) continue;
            return i;
        }
        return -1;
    }
    public ItemStack getStackInSlot(int i) {
        return mc.player.getInventory().getItem(i);
    }
    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (pos == null) return;
        Color color = new Color(50, 232, 252, 80);
        event.renderer.box(pos,color,color, ShapeMode.Both,0);
    }
    private Villager getTarget() {
        Entity target = null;
        double distance = range.get();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Villager)) continue;
            if (!mc.player.hasLineOfSight(entity) && mc.player.distanceTo(entity) > wallRange.get()) {
                continue;
            }
            if (target == null) {
                target = entity;
                distance = mc.player.distanceTo(entity);
            } else {
                if (mc.player.distanceTo(entity) < distance) {
                    target = entity;
                    distance = mc.player.distanceTo(entity);
                }
            }
        }
        return (Villager)(target);
    }
    public static boolean hasEnchantments(ItemStack itemStack, ResourceKey<Enchantment>... enchantments) {
        if (itemStack.isEmpty()) return false;
        Object2IntMap<Holder<Enchantment>> itemEnchantments = new Object2IntArrayMap<>();
        getEnchantments(itemStack, itemEnchantments);

        for (ResourceKey<Enchantment> enchantment : enchantments) {
            if (!hasEnchantment(itemEnchantments, enchantment)) return false;
        }
        return true;
    }
    private static boolean hasEnchantment(Object2IntMap<Holder<Enchantment>> itemEnchantments, ResourceKey<Enchantment> enchantmentKey) {
        for (Holder<Enchantment> enchantment : itemEnchantments.keySet()) {
            if (enchantment.is(enchantmentKey)) return true;
        }
        return false;
    }
}

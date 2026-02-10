package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.dev.leavesHack.utils.world.BlockUtil;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.*;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static meteordevelopment.meteorclient.utils.Utils.getEnchantments;

public class AutoRefreshTrade extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("Range")
            .description("range")
            .defaultValue(5)
            .min(0)
            .sliderMax(12)
            .build()
    );
    private final Setting<Integer> wallRange = sgGeneral.add(new IntSetting.Builder()
            .name("WallRange")
            .description("wallRange")
            .defaultValue(5)
            .min(0)
            .sliderMax(12)
            .build()
    );
    private final Setting<Integer> waitMine = sgGeneral.add(new IntSetting.Builder()
            .name("WaitMineDelay")
            .description("MS")
            .defaultValue(5000)
            .min(0)
            .sliderMax(10000)
            .build()
    );
    private final Setting<Set<RegistryKey<Enchantment>>> enchantmentList = sgGeneral.add(new EnchantmentListSetting.Builder()
            .name("EnchantmentsList")
            .description("")
            .build()
    );
    private final Setting<Integer> enchantmentLevel = sgGeneral.add(new IntSetting.Builder()
            .name("Level")
            .description("Enchantment level")
            .defaultValue(3)
            .min(0)
            .sliderMax(5)
            .build()
    );
    public AutoRefreshTrade() {
        super(LeavesHack.CATEGORY, "AutoRefreshTrade", "dev Leaves_awa");
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
        if (mc.options.backKey.isPressed()) {
            toggle();
            return;
        }
        if (pos != null && mc.world.isAir(pos)) {
            int slot = findItem(Items.LECTERN);
            int old = mc.player.getInventory().selectedSlot;
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
        VillagerEntity target = getTarget();
        if (target == null) return;
        Rotation.snapAt(target.getEyePos());
        Vec3d playerPos = mc.player.getEyePos();
        Vec3d villagerPos = target.getEyePos();
        EntityHitResult hitResult = ProjectileUtil.raycast(
                mc.player,
                playerPos,
                villagerPos,
                target.getBoundingBox(),
                Entity::canHit,
                playerPos.squaredDistanceTo(villagerPos)
        );
        if (hitResult == null) {
            mc.interactionManager.interactEntity(
                    mc.player,
                    target,
                    Hand.MAIN_HAND
            );
        } else {
            ActionResult result = mc.interactionManager.interactEntityAtLocation(
                    mc.player,
                    target,
                    hitResult,
                    Hand.MAIN_HAND
            );
            if (!result.isAccepted()) {
                mc.interactionManager.interactEntity(
                        mc.player,
                        target,
                        Hand.MAIN_HAND
                );
            }
        }
        if (mc.player.currentScreenHandler instanceof MerchantScreenHandler handler) {
            TradeOfferList list = handler.getRecipes();
            AtomicBoolean find = new AtomicBoolean(false);
            boolean findBook = false;
            for (int size = 0; size < list.size(); ++size) {
                TradeOffer tradeOffer = list.get(size);
                Item item = tradeOffer.getSellItem().getItem();
                ItemStack sellStack = tradeOffer.getSellItem();
                if (item instanceof EnchantedBookItem) {
                    findBook = true;
                    ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(sellStack);
                    enchantments.getEnchantments().forEach(entry -> {
                        int level = enchantments.getLevel(entry);
                        String name = Enchantment.getName(entry, level).getString();
                        mc.player.sendMessage(Text.of("[LeavesHack]本次结果 " + name));
                        for (RegistryKey<Enchantment> enchantmentKey : enchantmentList.get()){
                            if (hasEnchantments(sellStack, enchantmentKey) && level >= enchantmentLevel.get()) {
                                find.set(true);
                                mc.player.sendMessage(Text.of("[LeavesHack]:已找到所需附魔"));
                                return;
                            }
                        }
                    });
                }
            }
            if (!findBook) mc.player.sendMessage(Text.of("[LeavesHack]:本次未找到附魔书"));
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
            mc.currentScreen.close();
            if (find.get()) {
                toggle();
                return;
            }
            Direction facing1 = mc.player.getHorizontalFacing();
            switch (facing1) {
                case NORTH -> pos = mc.player.getBlockPos().north();
                case SOUTH -> pos = mc.player.getBlockPos().south();
                case EAST -> pos = mc.player.getBlockPos().east();
                case WEST -> pos = mc.player.getBlockPos().west();
                default -> pos = mc.player.getBlockPos();
            }
            Rotation.snapAt(pos.toCenterPos());
            mc.interactionManager.attackBlock(pos, BlockUtils.getClosestPlaceSide(pos));
            timer.reset();
        }
    }
    public int findItem(Item input) {
        for (int i = 0; i < 9; ++i) {
            Item item = getStackInSlot(i).getItem();
            if (Item.getRawId(item) != Item.getRawId(input)) continue;
            return i;
        }
        return -1;
    }
    public ItemStack getStackInSlot(int i) {
        return mc.player.getInventory().getStack(i);
    }
    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (pos == null) return;
        Color color = new Color(50, 232, 252, 80);
        event.renderer.box(pos,color,color, ShapeMode.Both,0);
    }
    private VillagerEntity getTarget() {
        Entity target = null;
        double distance = range.get();
        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof VillagerEntity)) continue;
            if (!mc.player.canSee(entity) && mc.player.distanceTo(entity) > wallRange.get()) {
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
        return (VillagerEntity)(target);
    }
    public static boolean hasEnchantments(ItemStack itemStack, RegistryKey<Enchantment>... enchantments) {
        if (itemStack.isEmpty()) return false;
        Object2IntMap<RegistryEntry<Enchantment>> itemEnchantments = new Object2IntArrayMap<>();
        getEnchantments(itemStack, itemEnchantments);

        for (RegistryKey<Enchantment> enchantment : enchantments) {
            if (!hasEnchantment(itemEnchantments, enchantment)) return false;
        }
        return true;
    }
    private static boolean hasEnchantment(Object2IntMap<RegistryEntry<Enchantment>> itemEnchantments, RegistryKey<Enchantment> enchantmentKey) {
        for (RegistryEntry<Enchantment> enchantment : itemEnchantments.keySet()) {
            if (enchantment.matchesKey(enchantmentKey)) return true;
        }
        return false;
    }
}

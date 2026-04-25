package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.combat.CombatUtil;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public class AutoAnchor extends Module {
    public static AutoAnchor INSTANCE;
    public AutoAnchor() {
        super(LeavesHack.CATEGORY, "AutoAnchor", "自动重生锚");
        INSTANCE = this;
    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("TargetRange")
            .defaultValue(6.0)
            .sliderRange(1, 6)
            .build()
    );
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .defaultValue(4.5)
            .sliderRange(1, 6)
            .build()
    );
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay-ms")
            .defaultValue(50)
            .sliderRange(0, 500)
            .build()
    );
    private final Setting<Double> minDamage = sgGeneral.add(new DoubleSetting.Builder()
            .name("MinDamage")
            .defaultValue(4.0)
            .sliderRange(1, 36)
            .build()
    );
    private final Setting<Double> maxSelfDmg = sgGeneral.add(new DoubleSetting.Builder()
            .name("MaxSelfDmg")
            .defaultValue(12)
            .sliderRange(1, 36)
            .build()
    );
    private final Setting<Boolean> usingPause = sgGeneral.add(
            new BoolSetting.Builder()
                    .name("UsingPause")
                    .defaultValue(true)
                    .build()
    );
    private final Setting<Boolean> onlyMain = sgGeneral.add(
            new BoolSetting.Builder()
                    .name("OnlyMain")
                    .defaultValue(true)
                    .visible(usingPause::get)
                    .build()
    );
    private final Setting<Boolean> preferHead = sgGeneral.add(new BoolSetting.Builder()
            .name("PreferHead")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> placeHelper = sgGeneral.add(new BoolSetting.Builder()
            .name("PlaceHelper")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> noSuicide = sgGeneral.add(new BoolSetting.Builder()
            .name("NoSuicide")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> inventory = sgGeneral.add(new BoolSetting.Builder()
            .name("InventorySwap")
            .defaultValue(true)
            .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(
            new EnumSetting.Builder<ShapeMode>()
                    .name("Shape Mode")
                    .defaultValue(ShapeMode.Both)
                    .build()
    );
    private final Setting<SettingColor> lineColor = sgRender.add(
            new ColorSetting.Builder()
                    .name("Line")
                    .defaultValue(new SettingColor(255, 255, 255, 255))
                    .build()
    );
    private final Setting<SettingColor> sideColor = sgRender.add(
            new ColorSetting.Builder()
                    .name("Side")
                    .defaultValue(new SettingColor(255, 255, 255, 10))
                    .build()
    );
    private final Setting<Double> renderSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("RenderSpeed")
            .defaultValue(0.1)
            .sliderRange(0, 1)
            .build()
    );
    public PlayerEntity target;
    public BlockPos currentPos;
    public final Timer placeTimer = new Timer();
    public PosEntry renderPosEntry = new PosEntry();
    @Override
    public void onDeactivate() {
        currentPos = null;
    }
    @Override
    public void onActivate() {
        placeTimer.setMs(9999999);
        renderPosEntry = new PosEntry();
    }
    public String getInfoString() {
        return target == null ? null : "§f[" + target.getName().getString() + "]";
    }
    @EventHandler
    public void onRender3D(Render3DEvent event) {
        if (currentPos != null) {
            if (renderPosEntry.x == 0 && renderPosEntry.y == 0 && renderPosEntry.z == 0) {
                renderPosEntry.x = mc.player.getX();
                renderPosEntry.y = mc.player.getY();
                renderPosEntry.z = mc.player.getZ();
            }
            renderPosEntry.x += (currentPos.getX() - renderPosEntry.x) * renderSpeed.get();
            renderPosEntry.y += (currentPos.getY() - renderPosEntry.y) * renderSpeed.get();
            renderPosEntry.z += (currentPos.getZ() - renderPosEntry.z) * renderSpeed.get();

            Box renderBox = new Box(
                    renderPosEntry.x, renderPosEntry.y, renderPosEntry.z,
                    renderPosEntry.x + 1.0, renderPosEntry.y + 1.0, renderPosEntry.z + 1.0
            );
            event.renderer.box(renderBox, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        } else {
            renderPosEntry = new PosEntry();
        }
    }
    @EventHandler
    public void onTick(TickEvent.Pre event) {
        target = CombatUtil.getClosestEnemy(targetRange.get());
        if (target == null) {
            currentPos = null;
            return;
        }
        if (shouldPause()) {
            return;
        }
        int anchor = inventory.get() ? InventoryUtil.findItemInventorySlot(Items.RESPAWN_ANCHOR) : InventoryUtil.findItem(Items.RESPAWN_ANCHOR);
        int glow = inventory.get() ? InventoryUtil.findItemInventorySlot(Items.GLOWSTONE) : InventoryUtil.findItem(Items.GLOWSTONE);
        if (anchor == -1 || glow == -1) {
            return;
        }
        updatePos(target);
        if (placeTimer.passedMs(delay.get())) doAnchor(anchor, glow);
    }

    private void doAnchor(int anchor, int glow) {
        if (currentPos != null) {
            if (noSuicide.get() && DamageUtils.anchorDamage(mc.player, currentPos.toCenterPos()) > EntityUtils.getTotalHealth(mc.player)) return;
            if (mc.player.getEyePos().distanceTo(currentPos.toCenterPos()) > range.get() || (!BlockUtil.canPlace(currentPos) && !(BlockUtil.getBlock(currentPos) instanceof RespawnAnchorBlock))) {
                updatePos(target);
            }
            if (!(BlockUtil.getBlock(currentPos) instanceof RespawnAnchorBlock)) {
                Direction side = BlockUtil.getPlaceSide(currentPos, null);
                if (side != null) {
                    int old = mc.player.getInventory().selectedSlot;
                    doSwap(anchor);
                    BlockUtil.placeBlock(currentPos, side, rotate.get());
                    if (inventory.get()) {
                        doSwap(anchor);
                    } else {
                        doSwap(old);
                    }
                    placeTimer.reset();
                }
            } else if (BlockUtil.getBlock(currentPos) instanceof RespawnAnchorBlock){
                int old = mc.player.getInventory().selectedSlot;
                Direction side2 = BlockUtil.getClickSide(currentPos);
                if (mc.world.getBlockState(currentPos).get(RespawnAnchorBlock.CHARGES) > 0) {
                    BlockUtil.clickBlock(currentPos, side2, rotate.get());
                    placeTimer.reset();
                    return;
                }
                if (side2 != null) {
                    doSwap(glow);
                    BlockUtil.clickBlock(currentPos, side2, rotate.get());
                    mc.world.playSound(null, mc.player.getX(), mc.player.getY(), mc.player.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.AMBIENT, 10.0f, 1.0f);
                    if (inventory.get()) {
                        doSwap(glow);
                    } else {
                        doSwap(old);
                    }
                    placeTimer.reset();
                }
            }
        }
    }

    private void updatePos(PlayerEntity target) {
        if (preferHead.get()) {
            BlockPos head = target.getBlockPos().up(2);
            if (DamageUtils.anchorDamage(target, head.toCenterPos()) > minDamage.get()) {
                if (BlockUtil.canPlace(head)) {
                    currentPos = head;
                    return;
                } else {
                    if (placeHelper.get()) {
                        for (Direction dir : Direction.HORIZONTAL) {
                            BlockPos temp = head.offset(dir);
                            if (BlockUtil.canPlace(temp) && BlockUtil.isGrimDirection(temp.offset(dir), dir.getOpposite())) {
                                placeHelper(temp);
                                return;
                            }
                        }
                    }
                }
            }
        }
        float bestDmg = Float.MIN_VALUE;
        BlockPos bestPos = null;
        for (BlockPos pos : BlockUtil.getSphere(range.get())){
            if (!BlockUtil.canPlace(pos) && !(BlockUtil.getBlock(pos) instanceof RespawnAnchorBlock)) continue;
            if (DamageUtils.anchorDamage(target, pos.toCenterPos()) > bestDmg && DamageUtils.anchorDamage(target, pos.toCenterPos()) > minDamage.get() && DamageUtils.anchorDamage(mc.player, pos.toCenterPos()) < maxSelfDmg.get()){
                bestDmg = DamageUtils.anchorDamage(target, pos.toCenterPos());
                bestPos = pos;
            }
        }
        if (bestPos != null) {
            currentPos = bestPos;
        }
    }
    private void placeHelper(BlockPos pos){
        Direction dir = BlockUtil.getPlaceSide(pos, null);
        if (dir == null) return;
        int old = mc.player.getInventory().selectedSlot;
        int anchor = inventory.get() ? InventoryUtil.findItemInventorySlot(Items.RESPAWN_ANCHOR) : InventoryUtil.findItem(Items.RESPAWN_ANCHOR);
        doSwap(anchor);
        BlockUtil.placeBlock(pos, dir, rotate.get());
        if (inventory.get()) {
            doSwap(anchor);
        } else {
            doSwap(old);
        }
    }
    private boolean shouldPause() {
        if (AutoCrystal.INSTANCE.isActive() && AutoCrystal.INSTANCE.preferMode.get() == AutoCrystal.PreferMode.PreferCrystal) {
            return AutoCrystal.INSTANCE.crystalPos != null;
        }
        return !usingPause.get() || checkPause(onlyMain.get());
    }
    public boolean checkPause(boolean onlyMain) {
        return (mc.options.useKey.isPressed() || mc.player.isUsingItem()) && (!onlyMain || mc.player.getActiveHand() == Hand.MAIN_HAND);
    }
    private void doSwap(int slot) {
        if (!inventory.get()) {
            InventoryUtil.switchToSlot(slot);
        } else {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        }
    }
    public static class PosEntry {
        double x = 0;
        double y = 0;
        double z = 0;
        PosEntry() {}
    }
}

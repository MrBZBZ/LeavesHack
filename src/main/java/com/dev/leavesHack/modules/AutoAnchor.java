package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.manager.LeavesModule;
import com.dev.leavesHack.utils.combat.CombatUtil;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AutoAnchor extends LeavesModule {
    public static AutoAnchor INSTANCE;
    public AutoAnchor() {
        super(LeavesHack.LEAVES_COMBAT, "AutoAnchor", "自动重生锚");
        INSTANCE = this;
    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("TargetRange")
        .description("目标距离")
        .defaultValue(12.0)
        .sliderRange(1, 20)
        .build()
    );
    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("操作距离")
        .defaultValue(4.0)
        .sliderRange(1, 6)
        .build()
    );
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay-ms")
        .description("放置延迟")
        .defaultValue(50)
        .sliderRange(0, 500)
        .build()
    );
    private final Setting<Integer> explodeDelay = sgGeneral.add(new IntSetting.Builder()
        .name("explode-delay-ms")
        .description("引爆延迟")
        .defaultValue(50)
        .sliderRange(0, 500)
        .build()
    );
    private final Setting<Double> minDamage = sgGeneral.add(new DoubleSetting.Builder()
        .name("MinDamage")
        .description("最小伤害")
        .defaultValue(4.0)
        .sliderRange(1, 36)
        .build()
    );
    private final Setting<Double> maxSelfDmg = sgGeneral.add(new DoubleSetting.Builder()
        .name("MaxSelfDmg")
        .description("最大自伤")
        .defaultValue(12)
        .sliderRange(1, 36)
        .build()
    );
    private final Setting<Double> minRatio = sgGeneral.add(new DoubleSetting.Builder()
        .name("MinRatio")
        .description("最小伤害比(敌伤/自伤)")
        .defaultValue(2.0)
        .sliderRange(0.1, 10)
        .build()
    );
    private final Setting<Integer> maxTargets = sgGeneral.add(new IntSetting.Builder()
        .name("MaxTargets")
        .description("最多同时计算的目标数")
        .defaultValue(4)
        .sliderRange(1, 8)
        .build()
    );
    private final Setting<Double> targetRadius = sgGeneral.add(new DoubleSetting.Builder()
        .name("TargetRadius")
        .description("目标邻域半径,方块需在此范围内才会被选中")
        .defaultValue(3.5)
        .sliderRange(1, 6)
        .build()
    );
    private final Setting<Boolean> thread = sgGeneral.add(new BoolSetting.Builder()
        .name("Thread")
        .description("多线程")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> usingPause = sgGeneral.add(new BoolSetting.Builder()
        .name("UsingPause")
        .description("使用暂停")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> onlyMain = sgGeneral.add(new BoolSetting.Builder()
        .name("OnlyMain")
        .description("仅主手")
        .defaultValue(true)
        .visible(usingPause::get)
        .build()
    );
    private final Setting<Boolean> preferHead = sgGeneral.add(new BoolSetting.Builder()
        .name("PreferHead")
        .description("优先炸头")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> placeHelper = sgGeneral.add(new BoolSetting.Builder()
        .name("PlaceHelper")
        .description("放置辅助方块")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> noSuicide = sgGeneral.add(new BoolSetting.Builder()
        .name("NoSuicide")
        .description("防自杀")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("rotate")
        .description("转头")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> inventory = sgGeneral.add(new BoolSetting.Builder()
        .name("InventorySwap")
        .description("静默背包物品切换")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> renderDmg = sgRender.add(new BoolSetting.Builder()
        .name("RenderDmg")
        .description("渲染伤害")
        .defaultValue(true)
        .build()
    );
    private final Setting<SettingColor> dmgColor = sgRender.add(new ColorSetting.Builder()
        .name("DamageColor")
        .description("伤害文本颜色")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("Shape Mode")
        .description("方块渲染模式")
        .defaultValue(ShapeMode.Both)
        .build()
    );
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("Line")
        .description("方块边框颜色")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("Side")
        .description("方块填充颜色")
        .defaultValue(new SettingColor(255, 255, 255, 10))
        .build()
    );
    private final Setting<Double> renderSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("RenderSpeed")
        .description("方块渲染速度")
        .defaultValue(0.1)
        .sliderRange(0, 1)
        .build()
    );
    private static final long ANCHOR_TTL = 600L;
    public PlayerEntity target;
    private final CopyOnWriteArrayList<PlayerEntity> targets = new CopyOnWriteArrayList<>();
    private final List<Anchor> anchors = new ArrayList<>();
    public BlockPos currentPos;
    public int dmg;
    public final Timer placeTimer = new Timer();
    private final Timer explodeTimer = new Timer();
    private final Timer threadTimer = new Timer();
    public PosEntry renderPosEntry = new PosEntry();
    @Override
    public void onDeactivate() {
        currentPos = null;
        anchors.clear();
        targets.clear();
    }
    @Override
    public void onActivate() {
        placeTimer.setMs(9999999);
        explodeTimer.setMs(9999999);
        anchors.clear();
        targets.clear();
        renderPosEntry = new PosEntry();
    }
    @Override
    public void onThread() {
        if (!thread.get()) return;
        if (!threadTimer.passedMs(50)) return;
        threadTimer.reset();
        if (shouldPause()) return;
        if (!hasItems()) {
            currentPos = null;
            return;
        }
        updatePos();
    }
    public String getInfoString() {
        return target == null ? null : "[" + target.getName().getString() + "]";
    }
    //    @EventHandler
//    private void onMyRender3D(RenderLeaves3DEvent event) {
//        if (renderDmg.get() && currentPos != null) {
//            Render3DUtil.renderText3D(dmg + "f", currentPos.toCenterPos(), dmgColor.get().getPacked());
//        }
//    }
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
        updateTargets();
        if (target == null) {
            currentPos = null;
            return;
        }
        if (shouldPause()) return;
        int anchor = getSlot(Items.RESPAWN_ANCHOR);
        int glow = getSlot(Items.GLOWSTONE);
        if (anchor == -1 || glow == -1) {
            currentPos = null;
            return;
        }
        anchors.removeIf(a -> System.currentTimeMillis() - a.time > ANCHOR_TTL * 2);
        if (!thread.get()) updatePos();
        if (placeTimer.passedMs(delay.get())) updatePlacing(anchor, glow);
        if (explodeTimer.passedMs(explodeDelay.get())) updateExploding(anchor, glow);
    }

    private void updatePlacing(int anchor, int glow) {
        BlockPos pos = currentPos;
        if (pos == null) return;
        if (getAnchor(pos).state != AnchorState.Air) return;
        if (mc.player.getEyePos().distanceTo(pos.toCenterPos()) > range.get() || !BlockUtil.canPlace(pos)) {
            updatePos();
            return;
        }
        if (!placeDmgCheck(pos)) return;
        if (BlockUtil.hasEntity(pos, false)) return;
        Direction side = BlockUtil.getPlaceSide(pos, null);
        if (side == null) return;
        int old = mc.player.getInventory().getSelectedSlot();
        doSwap(anchor);
        BlockUtil.placeBlock(pos, side, rotate.get());
        if (inventory.get()) {
            doSwap(anchor);
        } else {
            doSwap(old);
        }
        anchors.removeIf(a -> a.pos.equals(pos));
        anchors.add(new Anchor(pos, AnchorState.Anchor, 0));
        placeTimer.reset();
    }

    private void updateExploding(int anchor, int glow) {
        BlockPos best = null;
        double bestDmg = -1;
        long now = System.currentTimeMillis();
        List<BlockPos> candidates = new ArrayList<>();
        if (currentPos != null && getAnchor(currentPos).state != AnchorState.Air) candidates.add(currentPos);
        for (Anchor a : anchors) {
            if (a.state == AnchorState.Air || now - a.time > ANCHOR_TTL) continue;
            if (!candidates.contains(a.pos)) candidates.add(a.pos);
        }
        for (BlockPos pos : candidates) {
            if (!inRangeToTargets(pos)) continue;
            if (!(BlockUtil.getBlock(pos) instanceof RespawnAnchorBlock)) continue;
            double d = maxTargetDamage(pos);
            if (!placeDmgCheck(pos, d)) continue;
            if (d > bestDmg) {
                bestDmg = d;
                best = pos;
            }
        }
        if (best == null) return;
        Anchor a = getAnchor(best);
        int old = mc.player.getInventory().getSelectedSlot();
        Direction side = BlockUtil.getClickSide(best);
        if (a.state == AnchorState.Anchor) {
            doSwap(glow);
            BlockUtil.clickBlock(best, side, rotate.get());
            mc.world.playSound(null, mc.player.getX(), mc.player.getY(), mc.player.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.AMBIENT, 5.0f, 1.0f);
            if (inventory.get()) {
                doSwap(glow);
            } else {
                doSwap(old);
            }
            BlockPos finalBest1 = best;
            anchors.removeIf(x -> x.pos.equals(finalBest1));
            anchors.add(new Anchor(best, AnchorState.Loaded, a.charges + 1));
        } else {
            doSwap(anchor);
            BlockUtil.clickBlock(best, side, rotate.get());
            if (inventory.get()) {
                doSwap(anchor);
            } else {
                doSwap(old);
            }
            BlockPos finalBest = best;
            anchors.removeIf(x -> x.pos.equals(finalBest));
            anchors.add(new Anchor(best, AnchorState.Air, 0));
        }
        dmg = (int) bestDmg;
        explodeTimer.reset();
    }

    private void updatePos() {
        if (target == null) return;
        if (preferHead.get()) {
            BlockPos head = target.getBlockPos().up(2);
            if (placeDmgCheck(head)) {
                if (BlockUtil.canPlace(head) || BlockUtil.getBlock(head) instanceof RespawnAnchorBlock) {
                    currentPos = head;
                    dmg = (int) maxTargetDamage(head);
                    return;
                } else {
                    if (placeHelper.get()) {
                        for (Direction dir : Direction.HORIZONTAL) {
                            BlockPos temp = head.offset(dir);
                            if (BlockUtil.canPlace(temp) && BlockUtil.isGrimDirection(temp.offset(dir), dir.getOpposite())) {
                                placeHelper(temp);
                                currentPos = head;
                                dmg = (int) maxTargetDamage(head);
                                return;
                            }
                        }
                    }
                }
            }
        }
        float bestDmg = Float.MIN_VALUE;
        BlockPos bestPos = null;
        for (BlockPos pos : BlockUtil.getSphere(range.get())) {
            if (!BlockUtil.canPlace(pos) && !(BlockUtil.getBlock(pos) instanceof RespawnAnchorBlock)) continue;
            if (!inRangeToTargets(pos)) continue;
            if (BlockUtil.hasEntity(pos, false)) continue;
            double d = maxTargetDamage(pos);
            if (!placeDmgCheck(pos, d)) continue;
            if (d > bestDmg) {
                bestDmg = (float) d;
                bestPos = pos;
            }
        }
        if (bestPos != null) {
            currentPos = bestPos;
            dmg = (int) bestDmg;
        }
    }
    private void placeHelper(BlockPos pos){
        Direction dir = BlockUtil.getPlaceSide(pos, null);
        if (dir == null) return;
        int old = mc.player.getInventory().getSelectedSlot();
        int anchor = getSlot(Items.RESPAWN_ANCHOR);
        doSwap(anchor);
        BlockUtil.placeBlock(pos, dir, rotate.get());
        if (inventory.get()) {
            doSwap(anchor);
        } else {
            doSwap(old);
        }
    }
    private void updateTargets() {
        targets.clear();
        List<PlayerEntity> enemies = CombatUtil.getEnemies(targetRange.get());
        enemies.sort(Comparator.comparingDouble(p -> mc.player.squaredDistanceTo(p.getEntityPos())));
        int count = Math.min(enemies.size(), maxTargets.get());
        for (int i = 0; i < count; i++) targets.add(enemies.get(i));
        target = targets.isEmpty() ? null : targets.get(0);
    }
    private boolean hasItems() {
        return getSlot(Items.RESPAWN_ANCHOR) != -1 && getSlot(Items.GLOWSTONE) != -1;
    }
    private int getSlot(Item item) {
        return inventory.get() ? InventoryUtil.findItemInventorySlot(item) : InventoryUtil.findItem(item);
    }
    private boolean inRangeToTargets(BlockPos pos) {
        Vec3d center = pos.toCenterPos();
        for (PlayerEntity p : targets) {
            if (p.getEntityPos().add(0, 1, 0).distanceTo(center) < targetRadius.get()) return true;
        }
        return false;
    }
    private double maxTargetDamage(BlockPos pos) {
        double max = -1;
        for (PlayerEntity p : targets) {
            double d = DamageUtils.anchorDamage(p, pos.toCenterPos());
            if (d > max) max = d;
        }
        return max;
    }
    private boolean placeDmgCheck(BlockPos pos, double enemyDmg) {
        if (enemyDmg < minDamage.get()) return false;
        double self = DamageUtils.anchorDamage(mc.player, pos.toCenterPos());
        if (self > maxSelfDmg.get()) return false;
        if (self > 0 && enemyDmg / self < minRatio.get()) return false;
        return !noSuicide.get() || self <= EntityUtils.getTotalHealth(mc.player);
    }
    private boolean placeDmgCheck(BlockPos pos) {
        return placeDmgCheck(pos, maxTargetDamage(pos));
    }
    private Anchor getAnchor(BlockPos pos) {
        long now = System.currentTimeMillis();
        for (Anchor a : anchors) {
            if (a.pos.equals(pos) && now - a.time < ANCHOR_TTL) return a;
        }
        BlockState state = mc.world.getBlockState(pos);
        if (state.getBlock() == Blocks.RESPAWN_ANCHOR) {
            int c = state.get(RespawnAnchorBlock.CHARGES);
            return new Anchor(pos, c < 1 ? AnchorState.Anchor : AnchorState.Loaded, c);
        }
        return new Anchor(pos, AnchorState.Air, 0);
    }
    private boolean shouldPause() {
        if (AutoCrystal.INSTANCE.isActive() && AutoCrystal.INSTANCE.preferMode.get() == AutoCrystal.PreferMode.PreferCrystal) {
            return AutoCrystal.INSTANCE.crystalPos != null;
        }
        return usingPause.get() && checkPause(onlyMain.get());
    }
    public boolean checkPause(boolean onlyMain) {
        return (mc.options.useKey.isPressed() || mc.player.isUsingItem()) && (!onlyMain || mc.player.getActiveHand() == Hand.MAIN_HAND);
    }
    private void doSwap(int slot) {
        if (!inventory.get()) {
            InventoryUtil.switchToSlot(slot);
        } else {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().getSelectedSlot());
        }
    }
    private enum AnchorState {
        Air,
        Anchor,
        Loaded
    }
    private static class Anchor {
        BlockPos pos;
        AnchorState state;
        int charges;
        long time;
        Anchor(BlockPos pos, AnchorState state, int charges) {
            this.pos = pos;
            this.state = state;
            this.charges = charges;
            this.time = System.currentTimeMillis();
        }
    }
    public static class PosEntry {
        double x = 0;
        double y = 0;
        double z = 0;
        PosEntry() {}
    }
}

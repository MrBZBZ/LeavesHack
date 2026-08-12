package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.manager.LeavesModule;
import com.dev.leavesHack.utils.combat.CombatUtil;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.dev.leavesHack.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PistonHeadBlock;
import net.minecraft.block.RedstoneBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class PistonCrystal extends LeavesModule {
    public static PistonCrystal INSTANCE;
    public PistonCrystal() {
        super(LeavesHack.LEAVES_COMBAT, "PistonCrystal", "活塞水晶");
        INSTANCE = this;
    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
        .name("TargetRange")
        .description("目标距离")
        .defaultValue(6.0)
        .sliderRange(1, 6)
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
    private final Setting<Integer> breakDelay = sgGeneral.add(new IntSetting.Builder()
        .name("breakDelay-ms")
        .description("破坏延迟")
        .defaultValue(200)
        .sliderRange(0, 500)
        .build()
    );
    private final Setting<Double> minDamage = sgGeneral.add(new DoubleSetting.Builder()
        .name("MinDamage")
        .description("最小伤害")
        .defaultValue(6.0)
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
    private final Setting<Boolean> usingPause = sgGeneral.add(new BoolSetting.Builder()
        .name("UsingPause")
        .description("使用暂停")
        .defaultValue(true)
        .build()
    );
    public final Setting<Boolean> packetPlace = sgGeneral.add(new BoolSetting.Builder()
        .name("PacketPlace")
        .description("发包放置")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> onlyMain = sgGeneral.add(new BoolSetting.Builder()
        .name("OnlyMain")
        .description("仅主手暂停")
        .defaultValue(true)
        .visible(usingPause::get)
        .build()
    );
    private final Setting<Boolean> thread = sgGeneral.add(new BoolSetting.Builder()
        .name("Thread")
        .description("多线程计算")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> preferCrystal = sgGeneral.add(new BoolSetting.Builder()
        .name("PreferCrystal")
        .description("优先水晶光环")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> noSuicide = sgGeneral.add(new BoolSetting.Builder()
        .name("NoSuicide")
        .description("防止自杀")
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
        .description("静默背包切换")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> mine = sgGeneral.add(new BoolSetting.Builder()
        .name("Mine")
        .description("自动挖活塞")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> yawDeceive = sgGeneral.add(new BoolSetting.Builder()
        .name("YawDeceive")
        .description("Yaw欺骗")
        .defaultValue(true)
        .build()
    );
    private final Setting<RedstoneMode> redStoneMode = sgGeneral.add(new EnumSetting.Builder<RedstoneMode>()
        .name("redStone")
        .description("红石模式")
        .defaultValue(RedstoneMode.Block)
        .build()
    );
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
        .name("render")
        .description("渲染")
        .defaultValue(true)
        .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .description("渲染模式")
        .defaultValue(ShapeMode.Both)
        .build()
    );
    private final Setting<SettingColor> crystalColor = sgRender.add(new ColorSetting.Builder()
        .name("Crystal")
        .description("水晶颜色")
        .defaultValue(new SettingColor(255, 0, 0, 80))
        .build()
    );
    private final Setting<SettingColor> pistonColor = sgRender.add(new ColorSetting.Builder()
        .name("Piston")
        .description("活塞颜色")
        .defaultValue(new SettingColor(255, 255, 255, 80))
        .build()
    );
    private final Setting<SettingColor> redstoneColor = sgRender.add(new ColorSetting.Builder()
        .name("RedStone")
        .description("红石颜色")
        .defaultValue(new SettingColor(255, 100, 0, 80))
        .build()
    );
    private long lastAction = 0;
    private BlockPos crystalPos;
    private BlockPos pistonPos;
    private BlockPos redstonePos;
    private BlockPos lastPiston;
    private BlockPos lastRedstone;
    private BlockPos lastCrystal;
    private Direction face;
    private BlockPos lastBestPos; // 缓存上次最优爆炸中心，容差防止震荡
    private final Timer breakTimer = new Timer();
    private PlayerEntity target;
    private PlayerEntity lastTarget;
    @Override
    public void onActivate() {
        breakTimer.setMs(99999999);
        lastBestPos = null;
        lastTarget = null;
    }
    @Override
    public String getInfoString() {
        return target == null ? null : "[" + target.getName().getString() + "]";
    }
    @Override
    public void onThread() {
        if (!thread.get()) return;
        PlayerEntity tTarget = target;
        if (tTarget == null) return;
        if (pistonPos == null && crystalPos == null && redstonePos == null) {
            doPistonCrystal(tTarget);
        }
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        if (System.currentTimeMillis() - lastAction < delay.get()) return;
        if (preferCrystal.get() && AutoCrystal.INSTANCE.crystalPos != null) return;
        target = CombatUtil.getClosestEnemy(targetRange.get());
        // 目标切换时清除缓存，重新计算最优位置
        if (lastBestPos != null && target != null && lastTarget != null && target != lastTarget) {
            lastBestPos = null;
        }
        lastTarget = target;
        int redStone = findRedstone();
        int crystal = inventory.get() ? InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL) : InventoryUtil.findItem(Items.END_CRYSTAL);
        int piston = inventory.get() ? InventoryUtil.findClassInventory(PistonBlock.class) : InventoryUtil.findClass(PistonBlock.class);
        if (shouldPause()) {
            return;
        }
        if (redStone == -1 || crystal == -1 || piston == -1) {
            pistonPos = null;
            crystalPos = null;
            redstonePos = null;
            return;
        }
        if (crystalPos != null && !BlockUtil.hasCrystal(crystalPos) && !BlockUtil.canPlaceCrystal(crystalPos)) {
            lastAction = System.currentTimeMillis();
            pistonPos = null;
            crystalPos = null;
            redstonePos = null;
            lastBestPos = null;
        }
        if (crystalPos != null && BlockUtil.hasCrystal(crystalPos) && breakTimer.passedMs(breakDelay.get())) {
            CombatUtil.attackCrystal(crystalPos, true, false);
            lastAction = System.currentTimeMillis();
            pistonPos = null;
            crystalPos = null;
            redstonePos = null;
            breakTimer.reset();
            return;
        }
        if (target == null) return;
        if (!thread.get()) {
            if (pistonPos == null && crystalPos == null && redstonePos == null) {
                doPistonCrystal(target);
                return;
            }
        }
        //检查有没有活塞堵着导致放不了水晶，attack触发包挖挖活塞
        if (lastPiston != null) {
            if (BlockUtil.getBlock(lastPiston.offset(face.getOpposite())) instanceof PistonHeadBlock && BlockUtil.getBlock(lastPiston) instanceof PistonBlock) {
                if (mine.get()) {
                    Direction side = BlockUtil.getClickSide(lastPiston);
                    mc.interactionManager.attackBlock(lastPiston, side);
                    lastPiston = null;
                    lastCrystal = null;
                    lastRedstone = null;
                }
            }
        }
        //这三个都是1tick内一起放完，但是水晶还是要放到最后放
        //放活塞
        if (pistonPos != null && BlockUtil.canPlace(pistonPos)) {
            place(Items.PISTON, piston, pistonPos, face);
            lastPiston = pistonPos;
        }
        //放红石
        if (redstonePos != null && BlockUtil.canPlace(redstonePos)) {
            if (redStoneMode.get() == RedstoneMode.Torch) {
                placeTorch(redstonePos, redStone);
            } else {
                place(Items.REDSTONE_BLOCK, redStone, redstonePos, null);
            }
            lastRedstone = redstonePos;
        }
        //放水晶
        if (crystalPos != null && BlockUtil.canPlaceCrystal(crystalPos)) {
            placeCrystal(crystalPos, crystal);
            lastCrystal = crystalPos;
            lastAction = System.currentTimeMillis();
        }
    }

    private int findRedstone() {
        if (redStoneMode.get() == RedstoneMode.Torch) {
            return inventory.get() ? InventoryUtil.findItemInventorySlot(Items.REDSTONE_TORCH) : InventoryUtil.findItem(Items.REDSTONE_TORCH);
        } else {
            return inventory.get() ? InventoryUtil.findItemInventorySlot(Items.REDSTONE_BLOCK) : InventoryUtil.findItem(Items.REDSTONE_BLOCK);
        }
    }

    @EventHandler
    private void onRender(Render3DEvent e) {
        if (!render.get()) return;

        if (crystalPos != null) {
            e.renderer.box(crystalPos, crystalColor.get(), crystalColor.get(), shapeMode.get(), 0);
        }

        if (pistonPos != null) {
            e.renderer.box(pistonPos, pistonColor.get(), pistonColor.get(), shapeMode.get(), 0);
        }

        if (redstonePos != null) {
            e.renderer.box(redstonePos, redstoneColor.get(), redstoneColor.get(), shapeMode.get(), 0);
        }
    }

    private void doPistonCrystal(PlayerEntity target) {
        // 第一阶段：收集所有可行的爆炸中心点，计算伤害并排序
        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos pos : BlockUtil.getSphere(range.get())) {
            boolean canPlace = false;
            for (Direction dir : Direction.Type.HORIZONTAL) {
                if (BlockUtil.canPlaceCrystal(pos.offset(dir)) || BlockUtil.hasCrystalPlace(pos.offset(dir))) canPlace = true;
            }
            if (!canPlace) continue;
            Vec3d vec2 = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            float targetDmg = DamageUtils.crystalDamage(target, vec2);
            if (targetDmg < minDamage.get()) continue;
            float selfDmg = DamageUtils.crystalDamage(mc.player, vec2);
            if (selfDmg > maxSelfDmg.get()) continue;
            if (selfDmg > EntityUtils.getTotalHealth(mc.player) && noSuicide.get()) continue;
            candidates.add(pos);
        }

        if (candidates.isEmpty()) {
            lastBestPos = null;
            return;
        }

        // 按对敌人伤害从高到低排序，伤害相同则按距离玩家从近到远
        candidates.sort((a, b) -> {
            Vec3d va = new Vec3d(a.getX() + 0.5, a.getY(), a.getZ() + 0.5);
            Vec3d vb = new Vec3d(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
            float dmgA = DamageUtils.crystalDamage(target, va);
            float dmgB = DamageUtils.crystalDamage(target, vb);
            int cmp = Float.compare(dmgB, dmgA);
            if (cmp != 0) return cmp;
            return Float.compare(
                (float) mc.player.getEyePos().distanceTo(va),
                (float) mc.player.getEyePos().distanceTo(vb)
            );
        });

        // 第二阶段：选择爆炸中心（借鉴 AutoCrystal lastBestPos 容差机制）
        BlockPos best = candidates.get(0);
        Vec3d bestVec = new Vec3d(best.getX() + 0.5, best.getY(), best.getZ() + 0.5);
        float bestDamage = DamageUtils.crystalDamage(target, bestVec);

        if (lastBestPos != null) {
            Vec3d lastVec = new Vec3d(lastBestPos.getX() + 0.5, lastBestPos.getY(), lastBestPos.getZ() + 0.5);
            float lastDmg = DamageUtils.crystalDamage(target, lastVec);
            float lastSelfDmg = DamageUtils.crystalDamage(mc.player, lastVec);

            // 旧位置伤害仍在新最优的95%以内，且自伤不超限→保持旧位置（防震荡）
            if (lastDmg >= bestDamage * 0.95
                && lastSelfDmg < maxSelfDmg.get()
                && (!noSuicide.get() || lastSelfDmg < EntityUtils.getTotalHealth(mc.player))) {
                best = lastBestPos;
            } else {
                lastBestPos = best;
            }
        } else {
            lastBestPos = best;
        }

        // 第三阶段：检查是否需要先炸掉已有水晶
        if (breakTimer.passedMs(breakDelay.get())) {
            if (BlockUtil.hasCrystalPlace(best)) {
                CombatUtil.attackCrystal(best, true, false);
                lastAction = System.currentTimeMillis();
                pistonPos = null;
                crystalPos = null;
                redstonePos = null;
                breakTimer.reset();
                return;
            }
        }

        // 第四阶段：为爆炸中心寻找活塞+红石+水晶的放置方案
        for (Direction dir : Direction.Type.HORIZONTAL) {
            if (!yawDeceive.get() && dir != mc.player.getHorizontalFacing()) continue;
            BlockPos temp1 = best.offset(dir);
            if (!BlockUtil.canPlaceCrystal(temp1)) continue;
            BlockPos tempCrystalPos = temp1;
            BlockPos temp = temp1.offset(dir);
            if (!BlockUtil.canPlace(temp) && !(BlockUtil.getBlock(temp) instanceof PistonBlock)) {
                if (!mc.world.getBlockState(temp).isReplaceable()) continue;
                for (Direction help : Direction.values()) {
                    if (help == dir.getOpposite()) continue;
                    BlockPos helpPos = temp.offset(help);
                    if (helpPos.equals(tempCrystalPos)) continue;
                    if (!mc.world.getBlockState(helpPos).isReplaceable()) continue;
                    if (!BlockUtil.isGrimDirection(helpPos, help.getOpposite())) continue;
                    if (!BlockUtil.canPlace(helpPos)) continue;
                    int old = mc.player.getInventory().getSelectedSlot();
                    Direction side = BlockUtil.getPlaceSide(helpPos, null);
                    doSwap(findRedstone());
                    BlockUtil.placeBlock(helpPos, side, rotate.get(), packetPlace.get());
                    if (inventory.get()) {
                        doSwap(findRedstone());
                    } else {
                        doSwap(old);
                    }
                    lastAction = System.currentTimeMillis();
                    return;
                }
            }
            BlockPos tempPistonPos = temp;
            BlockPos tempRedstonePos = null;
            for (Direction dir3 : Direction.values()) {
                if (dir3 == dir.getOpposite()) continue;
                BlockPos temp3 = tempPistonPos.offset(dir3);
                if ((BlockUtil.getBlock(temp3) instanceof RedstoneBlock && redStoneMode.get() == RedstoneMode.Block) || (BlockUtil.getBlock(temp3) instanceof RedstoneTorchBlock && redStoneMode.get() == RedstoneMode.Torch)) {
                    tempRedstonePos = tempPistonPos.offset(dir3);
                    break;
                }
                if (!BlockUtil.canPlace(temp3)) {
                    continue;
                }
                tempRedstonePos = tempPistonPos.offset(dir3);
                break;
            }
            if (tempRedstonePos == null) continue;
            face = dir;
            crystalPos = tempCrystalPos;
            pistonPos = tempPistonPos;
            redstonePos = tempRedstonePos;
            return;
        }
        // 当前best找不到活塞布局方案→清除锁定，下次重选
        lastBestPos = null;
    }

    private void place(net.minecraft.item.Item item, int slot, BlockPos pos, Direction dir) {
        Direction side = BlockUtil.getPlaceSide(pos, null);
        if (side == null) {
            return;
        }
        if (slot == -1) {
            return;
        }
        int old = mc.player.getInventory().getSelectedSlot();
        doSwap(slot);
        if (rotate.get()) {
            Rotation.snapAt(pos.toCenterPos().add(new Vec3d(side.getVector().getX() * 0.5, side.getVector().getY() * 0.5, side.getVector().getZ() * 0.5)));
        }
        if (item == Items.PISTON) {
            if (yawDeceive.get() && rotate.get()) {
                pistonFacing(dir);
            }
        }
        BlockUtil.placeBlock(pos, side, false, packetPlace.get());
        if (rotate.get()) {
            Rotation.snapBack();
        }
        if (inventory.get()) {
            doSwap(slot);
        } else {
            doSwap(old);
        }
    }
    private boolean shouldPause() {
        return usingPause.get() && checkPause(onlyMain.get());
    }
    public boolean checkPause(boolean onlyMain) {
        return (mc.options.useKey.isPressed() || mc.player.isUsingItem()) && (!onlyMain || mc.player.getActiveHand() == Hand.MAIN_HAND);
    }
    public static void pistonFacing(Direction i) {
        if (i == Direction.EAST) {
            Rotation.snapAt(-90.0f, 5.0f);
        } else if (i == Direction.WEST) {
            Rotation.snapAt(90.0f, 5.0f);
        } else if (i == Direction.NORTH) {
            Rotation.snapAt(180.0f, 5.0f);
        } else if (i == Direction.SOUTH) {
            Rotation.snapAt(0.0f, 5.0f);
        }
    }
    private void placeCrystal(BlockPos pos, int slot) {
        BlockPos base = pos.down();

        Direction side = BlockUtil.getClickSide(base);
        if (side == null) return;
        if (slot == -1) return;

        int old = mc.player.getInventory().getSelectedSlot();
        doSwap(slot);
        BlockUtil.clickBlock(base, side, rotate.get());
        if (inventory.get()) {
            doSwap(slot);
        } else {
            doSwap(old);
        }
    }

    private void placeTorch(BlockPos pos, int slot) {
        if (!BlockUtil.canPlace(pos)) return;
        ArrayList<Direction> sides = BlockUtil.getPlaceSides(pos, null);
        if (sides.isEmpty()) return;
        for (Direction side : sides) {
            if (BlockUtil.getBlock(pos.offset(side)) instanceof PistonBlock) continue;
            if (side == Direction.UP) continue;
            if (slot == -1) return;
            int old = mc.player.getInventory().getSelectedSlot();
            doSwap(slot);
            BlockUtil.placeBlock(pos, side, rotate.get(), packetPlace.get());
            if (inventory.get()) {
                doSwap(slot);
            } else {
                doSwap(old);
            }
            return;
        }
    }
    private void doSwap(int slot) {
        if (!inventory.get()) {
            InventoryUtil.switchToSlot(slot);
        } else {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().getSelectedSlot());
        }
    }

    public enum RedstoneMode {
        Torch,
        Block
    }
}

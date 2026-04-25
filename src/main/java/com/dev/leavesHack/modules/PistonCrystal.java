package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.combat.CombatUtil;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.rotation.Rotation;
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

public class PistonCrystal extends Module {
    public static PistonCrystal INSTANCE;
    public PistonCrystal() {
        super(LeavesHack.CATEGORY, "PistonCrystal", "活塞水晶");
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
    private final Setting<Integer> breakDelay = sgGeneral.add(new IntSetting.Builder()
            .name("breakDelay-ms")
            .defaultValue(300)
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
    private final Setting<Boolean> mine = sgGeneral.add(new BoolSetting.Builder()
            .name("Mine")
            .defaultValue(true)
            .build()
    );
//    private final Setting<Integer> checkMineTime = sgGeneral.add(new IntSetting.Builder()
//            .name("CheckMineTime")
//            .defaultValue(300)
//            .sliderRange(0, 1000)
//            .build()
//    );
    private final Setting<Boolean> yawDeceive = sgGeneral.add(new BoolSetting.Builder()
            .name("YawDeceive")
            .defaultValue(true)
            .build()
    );
    private final Setting<RedstoneMode> redStoneMode = sgGeneral.add(new EnumSetting.Builder<RedstoneMode>()
            .name("redStone")
            .defaultValue(RedstoneMode.Block)
            .build()
    );
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .defaultValue(true)
            .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .defaultValue(ShapeMode.Both)
            .build()
    );
    private final Setting<SettingColor> crystalColor = sgRender.add(new ColorSetting.Builder()
            .name("crystal")
            .defaultValue(new SettingColor(255, 0, 0, 80))
            .build()
    );
    private final Setting<SettingColor> pistonColor = sgRender.add(new ColorSetting.Builder()
            .name("piston")
            .defaultValue(new SettingColor(255, 255, 255, 80))
            .build()
    );
    private final Setting<SettingColor> redstoneColor = sgRender.add(new ColorSetting.Builder()
            .name("redstone")
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
    private final Timer breakTimer = new Timer();
    private PlayerEntity target;
//    private int checkMine = 9999999;
    @Override
    public void onActivate() {
//        checkMine = 999999999;
        breakTimer.setMs(99999999);
    }
    @Override
    public String getInfoString() {
        return target == null ? null : "§f[" + target.getName().getString() + "]";
    }
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        if (System.currentTimeMillis() - lastAction < delay.get()) return;
        target = CombatUtil.getClosestEnemy(targetRange.get());
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
        if (crystalPos != null && redstonePos != null && pistonPos != null) {
            if (!BlockUtil.canPlaceCrystal(crystalPos) || !BlockUtil.canPlace(redstonePos) || !BlockUtil.canPlace(pistonPos)) {
                pistonPos = null;
                crystalPos = null;
                redstonePos = null;
            }
        }
        if (target == null) return;
        if (pistonPos == null && crystalPos == null && redstonePos == null) doPistonCrystal(target);
        if (lastPiston != null) {
            if (BlockUtil.getBlock(lastPiston.offset(face.getOpposite())) instanceof PistonHeadBlock && BlockUtil.getBlock(lastPiston) instanceof PistonBlock) {
                if (mine.get()) {
                    Direction side = BlockUtil.getClickSide(lastPiston);
                    mc.interactionManager.attackBlock(lastPiston, side);
                    lastPiston = null;
                    lastCrystal = null;
                    lastRedstone = null;
                    return;
                }
            }
        }
        if (pistonPos != null && BlockUtil.canPlace(pistonPos)) {
            place(Items.PISTON, piston, pistonPos, face);
            lastPiston = pistonPos;
        }
        if (redstonePos != null && BlockUtil.canPlace(redstonePos)) {
            if (redStoneMode.get() == RedstoneMode.Torch) {
                placeTorch(redstonePos, redStone);
            } else {
                place(Items.REDSTONE_BLOCK, redStone, redstonePos, null);
            }
            lastRedstone = redstonePos;
        }
        if (crystalPos != null && BlockUtil.canPlaceCrystal(crystalPos)) {
            placeCrystal(crystalPos, crystal);
            lastCrystal = crystalPos;
            lastAction = System.currentTimeMillis();
            return;
        }
        if (breakTimer.passedMs(breakDelay.get())) {
            if (BlockUtil.hasCrystal(target.getBlockPos().up())) {
                CombatUtil.attackCrystal(target.getBlockPos().up(), true, false);
                lastAction = System.currentTimeMillis();
                pistonPos = null;
                crystalPos = null;
                redstonePos = null;
                breakTimer.reset();
            }
            if (BlockUtil.hasCrystal(target.getBlockPos().up(2))) {
                CombatUtil.attackCrystal(target.getBlockPos().up(2), true, false);
                lastAction = System.currentTimeMillis();
                pistonPos = null;
                crystalPos = null;
                redstonePos = null;
                breakTimer.reset();
            }
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
        BlockPos base = target.getBlockPos();
        BlockPos tempCrystalPos = null;
        BlockPos tempPistonPos = null;
        BlockPos tempRedstonePos = null;
        Vec3d vec = new Vec3d(base.up().getX() + 0.5, base.up().getY(), base.up().getZ() + 0.5);
        float damage1 = DamageUtils.crystalDamage(target, vec);
        float selfDmg1 = DamageUtils.crystalDamage(mc.player, vec);
        if (damage1 > minDamage.get() && selfDmg1 <= maxSelfDmg.get()) {
            for (Direction dir : Direction.Type.HORIZONTAL) {
                if (!yawDeceive.get() && dir != mc.player.getHorizontalFacing()) continue;
                BlockPos temp1 = base.offset(dir).up();
                if (!BlockUtil.canPlaceCrystal(temp1)) continue;
                if (mc.player.getEyePos().distanceTo(temp1.toCenterPos()) > range.get()) {
                    continue;
                }
                tempCrystalPos = temp1;
//                BlockPos temp = temp1.offset(dir);
//                if (!BlockUtil.canPlace(temp) && !(BlockUtil.getBlock(temp) instanceof PistonBlock)) {
//                    for (Direction help : Direction.values()) {
//                        if (help == dir.getOpposite()) continue;
//                        if (!BlockUtil.isGrimDirection(temp.offset(help), help.getOpposite())) continue;
//                        if (!BlockUtil.canPlace(temp.offset(help)) || mc.player.getEyePos().distanceTo(temp.offset(help).toCenterPos()) > range.get()) continue;
//                        BlockPos helpPos = temp.offset(help);
//                        int old = mc.player.getInventory().selectedSlot;
//                        Direction side = BlockUtil.getPlaceSide(helpPos, null);
//                        doSwap(findRedstone());
//                        BlockUtil.placeBlock(helpPos, side, rotate.get());
//                        if (inventory.get()){
//                            doSwap(findRedstone());
//                        } else {
//                            doSwap(old);
//                        }
//                        return;
//                    }
//                } else {
//                    if (mc.player.getEyePos().distanceTo(temp.toCenterPos()) <= range.get()) {
//                        tempPistonPos = temp;
//                    }
//                }
                for (Direction dir2 : Direction.Type.HORIZONTAL) {
                    if (dir2 == dir.getOpposite()) continue;
                    BlockPos temp2 = temp1.offset(dir).offset(dir2);
                    if (!mc.world.isAir(temp2.offset(dir.getOpposite())) && !mc.world.getBlockState(temp2.offset(dir.getOpposite())).isReplaceable()) {
                        continue;
                    }
                    if (!BlockUtil.canPlace(temp2) && !(BlockUtil.getBlock(temp2) instanceof PistonBlock)) {
                        for (Direction help : Direction.values()) {
                            if (help == dir.getOpposite()) continue;
                            if (!BlockUtil.isGrimDirection(temp2.offset(help), help.getOpposite())) continue;
                            if (!BlockUtil.canPlace(temp2.offset(help)) || mc.player.getEyePos().distanceTo(temp2.offset(help).toCenterPos()) > range.get())
                                continue;
                            BlockPos helpPos = temp2.offset(help);
                            int old = mc.player.getInventory().selectedSlot;
                            Direction side = BlockUtil.getPlaceSide(helpPos, null);
                            doSwap(findRedstone());
                            BlockUtil.placeBlock(helpPos, side, rotate.get());
                            if (inventory.get()) {
                                doSwap(findRedstone());
                            } else {
                                doSwap(old);
                            }
                            return;
                        }
                        continue;
                    }
                    if (mc.player.getEyePos().distanceTo(temp2.toCenterPos()) > range.get()) {
                        continue;
                    }
                    tempPistonPos = temp1.offset(dir).offset(dir2);
                    //                mc.player.sendMessage(Text.of("dir2" + dir2.name()));
                    for (Direction dir3 : Direction.values()) {
                        if (dir3 == dir.getOpposite()) continue;
                        BlockPos temp3 = temp2.offset(dir3);
                        if ((BlockUtil.getBlock(temp3) instanceof RedstoneBlock && redStoneMode.get() == RedstoneMode.Block) || (BlockUtil.getBlock(temp3) instanceof RedstoneTorchBlock && redStoneMode.get() == RedstoneMode.Torch)) {
                            tempRedstonePos = tempPistonPos.offset(dir3);
                            break;
                        }
                        if (!BlockUtil.canPlace(temp3)) {
                            continue;
                        }
                        if (mc.player.getEyePos().distanceTo(temp3.toCenterPos()) > range.get()) {
                            continue;
                        }
                        tempRedstonePos = tempPistonPos.offset(dir3);
//                    mc.player.sendMessage(Text.of("dir3" + dir3.name()));
//                    mc.player.sendMessage(Text.of("------------结束"));
                        break;
                    }
                    break;
                }
                if (tempPistonPos == null) {
                    tempCrystalPos = null;
                    tempRedstonePos = null;
                    continue;
                }
                if (tempRedstonePos == null) {
                    tempCrystalPos = null;
                    tempPistonPos = null;
                    continue;
                }
                if (tempCrystalPos == null) {
                    tempPistonPos = null;
                    tempRedstonePos = null;
                    continue;
                }
                if (selfDmg1 > EntityUtils.getTotalHealth(mc.player) && noSuicide.get()) {
                    return;
                }
                face = dir;
                crystalPos = tempCrystalPos;
                pistonPos = tempPistonPos;
                redstonePos = tempRedstonePos;
                return;
            }
        }
        Vec3d vec2 = new Vec3d(base.up(2).getX() + 0.5, base.up(2).getY(), base.up(2).getZ() + 0.5);
        float damage2 = DamageUtils.crystalDamage(target, vec2);
        float selfDmg2 = DamageUtils.crystalDamage(mc.player, vec2);
        if (selfDmg2 > EntityUtils.getTotalHealth(mc.player) && noSuicide.get()) return;
        if (damage2 > minDamage.get() && selfDmg2 <= maxSelfDmg.get()) {
            if (crystalPos == null && pistonPos == null && redstonePos == null) {
                for (Direction dir : Direction.Type.HORIZONTAL) {
                    if (!yawDeceive.get() && dir != mc.player.getHorizontalFacing()) continue;
                    BlockPos temp1 = base.offset(dir).up(2);
                    if (!BlockUtil.canPlaceCrystal(temp1)) continue;
                    if (mc.player.getEyePos().distanceTo(temp1.toCenterPos()) > range.get()) {
                        continue;
                    }
                    tempCrystalPos = temp1;
//                    BlockPos temp = temp1.offset(dir);
//                    if (!BlockUtil.canPlace(temp) && !(BlockUtil.getBlock(temp) instanceof PistonBlock)) {
//                        for (Direction help : Direction.values()) {
//                            if (help == dir.getOpposite()) continue;
//                            if (!BlockUtil.isGrimDirection(temp.offset(help), help.getOpposite())) continue;
//                            if (!BlockUtil.canPlace(temp.offset(help)) || mc.player.getEyePos().distanceTo(temp.offset(help).toCenterPos()) > range.get())
//                                continue;
//                            BlockPos helpPos = temp.offset(help);
//                            int old = mc.player.getInventory().selectedSlot;
//                            Direction side = BlockUtil.getPlaceSide(helpPos, null);
//                            doSwap(findRedstone());
//                            BlockUtil.placeBlock(helpPos, side, rotate.get());
//                            if (inventory.get()) {
//                                doSwap(findRedstone());
//                            } else {
//                                doSwap(old);
//                            }
//                            return;
//                        }
//                    } else {
//                        if (mc.player.getEyePos().distanceTo(temp.toCenterPos()) <= range.get()) {
//                            tempPistonPos = temp;
//                        }
//                    }
                    for (Direction dir2 : Direction.Type.HORIZONTAL) {
                        if (dir2 == dir.getOpposite()) continue;
                        BlockPos temp2 = temp1.offset(dir).offset(dir2);
                        if (!mc.world.isAir(temp2.offset(dir.getOpposite())) && !mc.world.getBlockState(temp2.offset(dir.getOpposite())).isReplaceable()) {
                            continue;
                        }
                        if (!BlockUtil.canPlace(temp2) && !(BlockUtil.getBlock(temp2) instanceof PistonBlock)) {
                            for (Direction help : Direction.values()) {
                                if (help == dir.getOpposite()) continue;
                                if (!BlockUtil.isGrimDirection(temp2.offset(help), help.getOpposite())) continue;
                                if (!BlockUtil.canPlace(temp2.offset(help)) || mc.player.getEyePos().distanceTo(temp2.offset(help).toCenterPos()) > range.get())
                                    continue;
                                BlockPos helpPos = temp2.offset(help);
                                int old = mc.player.getInventory().selectedSlot;
                                Direction side = BlockUtil.getPlaceSide(helpPos, null);
                                doSwap(findRedstone());
                                BlockUtil.placeBlock(helpPos, side, rotate.get());
                                if (inventory.get()) {
                                    doSwap(findRedstone());
                                } else {
                                    doSwap(old);
                                }
                                return;
                            }
                            continue;
                        }
                        if (mc.player.getEyePos().distanceTo(temp2.toCenterPos()) > range.get()) {
                            continue;
                        }
                        tempPistonPos = temp1.offset(dir).offset(dir2);
                        //                mc.player.sendMessage(Text.of("dir2" + dir2.name()));
                        for (Direction dir3 : Direction.values()) {
                            if (dir3 == dir.getOpposite()) continue;
                            BlockPos temp3 = temp2.offset(dir3);
                            if ((BlockUtil.getBlock(temp3) instanceof RedstoneBlock && redStoneMode.get() == RedstoneMode.Block) || (BlockUtil.getBlock(temp3) instanceof RedstoneTorchBlock && redStoneMode.get() == RedstoneMode.Torch)) {
                                tempRedstonePos = tempPistonPos.offset(dir3);
                                break;
                            }
                            if (!BlockUtil.canPlace(temp3)) {
                                continue;
                            }
                            if (mc.player.getEyePos().distanceTo(temp3.toCenterPos()) > range.get()) {
                                continue;
                            }
                            tempRedstonePos = tempPistonPos.offset(dir3);
//                    mc.player.sendMessage(Text.of("dir3" + dir3.name()));
//                    mc.player.sendMessage(Text.of("------------结束"));
                            break;
                        }
                        break;
                    }
                    if (tempPistonPos == null) {
                        tempCrystalPos = null;
                        tempRedstonePos = null;
                        continue;
                    }
                    if (tempRedstonePos == null) {
                        tempCrystalPos = null;
                        tempPistonPos = null;
                        continue;
                    }
                    if (tempCrystalPos == null) {
                        tempPistonPos = null;
                        tempRedstonePos = null;
                        continue;
                    }
                    if (selfDmg1 > EntityUtils.getTotalHealth(mc.player) && noSuicide.get()) {
                        return;
                    }
                    face = dir;
                    crystalPos = tempCrystalPos;
                    pistonPos = tempPistonPos;
                    redstonePos = tempRedstonePos;
                    return;
                }
            }
        }
    }

    private void place(net.minecraft.item.Item item, int slot, BlockPos pos, Direction dir) {
        Direction side = BlockUtil.getPlaceSide(pos, d -> true);
        if (side == null) return;

        if (slot == -1) return;

        int old = mc.player.getInventory().selectedSlot;
        doSwap(slot);
        if (rotate.get()) {
            Rotation.snapAt(pos.toCenterPos().add(new Vec3d(side.getVector().getX() * 0.5, side.getVector().getY() * 0.5, side.getVector().getZ() * 0.5)));
        }
        if (item == Items.PISTON) {
            if (yawDeceive.get() && rotate.get()) {
                pistonFacing(dir);
            }
        }
        BlockUtil.placeBlock(pos, side, false);
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
        return !usingPause.get() || checkPause(onlyMain.get());
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

        int old = mc.player.getInventory().selectedSlot;
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
            int old = mc.player.getInventory().selectedSlot;
            doSwap(slot);
            BlockUtil.placeBlock(pos, side, rotate.get());
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
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        }
    }

    public enum RedstoneMode {
        Torch,
        Block
    }
}
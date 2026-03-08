package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

public class ScaffoldPlus extends Module {
    public ScaffoldPlus() {
        super(LeavesHack.CATEGORY, "scaffold+", "Automatically places blocks under you.");
    }
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("Rotate")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> usingPause = sgGeneral.add(new BoolSetting.Builder()
            .name("UsingPause")
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
                    .defaultValue(new SettingColor(255, 255, 255, 50))
                    .build()
    );
    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;
        if (mc.player.isUsingItem() && usingPause.get()) return;
        ItemStack stack = mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot);
        BlockPos pos = mc.player.getBlockPos();
        boolean slabMode = BlockUtil.getBlock(pos) instanceof SlabBlock;
        for (Direction i : Direction.values()) {
            if (i == Direction.UP || i == Direction.DOWN) continue;
            if (BlockUtil.getBlock(pos.offset(i)) instanceof SlabBlock) {
                slabMode = true;
                break;
            }
        }
        int block;
        block = slabMode ? InventoryUtil.findSlabBlock() : InventoryUtil.findBlock();
        if (slabMode){
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SlabBlock && !BlockUtil.shiftBlocks.contains(Block.getBlockFromItem(stack.getItem())) && ((BlockItem) stack.getItem()).getBlock() != Blocks.COBWEB) {
                block = mc.player.getInventory().selectedSlot;
            }
        }else {
            if (stack.getItem() instanceof BlockItem && !BlockUtil.shiftBlocks.contains(Block.getBlockFromItem(stack.getItem())) && ((BlockItem) stack.getItem()).getBlock() != Blocks.COBWEB) {
                block = mc.player.getInventory().selectedSlot;
            }
        }
        if (block == -1) return;
        BlockPos placePos = mc.player.getBlockPos().down();
        if (!slabMode) {
            if (BlockUtil.clientCanPlace(placePos, false)) {
                int old = mc.player.getInventory().selectedSlot;
                if (BlockUtil.getPlaceSide(placePos, null) == null) {
                    double distance = 1000;
                    BlockPos bestPos = null;
                    for (Direction i : Direction.values()) {
                        if (i == Direction.UP) continue;
                        if (BlockUtil.canPlace(placePos.offset(i))) {
                            if (bestPos == null || mc.player.squaredDistanceTo(placePos.offset(i).toCenterPos()) < distance) {
                                bestPos = placePos.offset(i);
                                distance = mc.player.squaredDistanceTo(placePos.offset(i).toCenterPos());
                            }
                        }
                    }
                    if (bestPos != null) {
                        placePos = bestPos;
                    } else {
                        return;
                    }
                }
                Direction side = BlockUtil.getPlaceSide(placePos, null);
                Direction slabSide = null;
                if (mc.player.getInventory().getStack(block).getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SlabBlock) {
                    slabSide = Direction.UP;
                }
                if (side != null) {
                    event.renderer.box(new Box(placePos), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    InventoryUtil.switchToSlot(block);
                    BlockUtil.placeSlabBlock(placePos, side, slabSide, rotate.get());
                    InventoryUtil.switchToSlot(old);
                }
            }
        } else {
            placePos = pos;
            if (BlockUtil.getBlock(pos) instanceof SlabBlock) {
                Direction face = mc.player.getHorizontalFacing();
                placePos = pos.offset(face);
            } else {
                if (BlockUtil.clientCanPlace(placePos, false)) {
                    if (BlockUtil.getPlaceSide(placePos, null) == null) {
                        double distance = 1000;
                        BlockPos bestPos = null;
                        for (Direction i : Direction.values()) {
                            if (i == Direction.UP) continue;
                            if (BlockUtil.canPlace(placePos.offset(i))) {
                                if (bestPos == null || mc.player.squaredDistanceTo(placePos.offset(i).toCenterPos()) < distance) {
                                    bestPos = placePos.offset(i);
                                    distance = mc.player.squaredDistanceTo(placePos.offset(i).toCenterPos());
                                }
                            }
                        }
                        if (bestPos != null) {
                            placePos = bestPos;
                        } else {
                            return;
                        }
                    }
                }
            }
            int old = mc.player.getInventory().selectedSlot;
            Direction side = BlockUtil.getPlaceSide(placePos, null);
            if (side != null && !(BlockUtil.getBlock(placePos) instanceof SlabBlock)) {
                event.renderer.box(new Box(placePos), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                InventoryUtil.switchToSlot(block);
                BlockUtil.placeSlabBlock(placePos, side, Direction.DOWN, rotate.get());
                InventoryUtil.switchToSlot(old);
            }
        }
    }
}

package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.dev.leavesHack.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.phys.AABB;

public class ScaffoldPlus extends Module {
    public ScaffoldPlus() {
        super(LeavesHack.LEAVES_MISC, "Scaffold+", "自动搭路");
    }
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("Rotate")
        .description("转头")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> usingPause = sgGeneral.add(new BoolSetting.Builder()
        .name("UsingPause")
        .description("使用暂停")
        .defaultValue(true)
        .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(
        new EnumSetting.Builder<ShapeMode>()
            .name("Shape Mode")
            .description("渲染模式")
            .defaultValue(ShapeMode.Both)
            .build()
    );
    private final Setting<SettingColor> lineColor = sgRender.add(
        new ColorSetting.Builder()
            .name("Line")
            .description("边框颜色")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
    );
    private final Setting<SettingColor> sideColor = sgRender.add(
        new ColorSetting.Builder()
            .name("Side")
            .description("填充颜色")
            .defaultValue(new SettingColor(255, 255, 255, 50))
            .build()
    );
    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (mc.player == null || mc.level == null) return;
        if (mc.player.isUsingItem() && usingPause.get()) return;
        ItemStack stack = mc.player.getInventory().getItem(mc.player.getInventory().getSelectedSlot());
        BlockPos pos = mc.player.blockPosition();
        if (GlobalSetting.INSTANCE.moveFix.get() && rotate.get()) Rotation.snapAt(pos.getCenter());
        boolean slabMode = BlockUtil.getBlock(pos) instanceof SlabBlock;
        for (Direction i : Direction.values()) {
            if (i == Direction.UP || i == Direction.DOWN) continue;
            if (BlockUtil.getBlock(pos.relative(i)) instanceof SlabBlock) {
                slabMode = true;
                break;
            }
        }
        int block;
        block = slabMode ? InventoryUtil.findSlabBlock() : InventoryUtil.findBlock();
        if (slabMode){
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SlabBlock && !BlockUtil.shiftBlocks.contains(Block.byItem(stack.getItem())) && ((BlockItem) stack.getItem()).getBlock() != Blocks.COBWEB) {
                block = mc.player.getInventory().getSelectedSlot();
            }
        }else {
            if (stack.getItem() instanceof BlockItem && !BlockUtil.shiftBlocks.contains(Block.byItem(stack.getItem())) && ((BlockItem) stack.getItem()).getBlock() != Blocks.COBWEB) {
                block = mc.player.getInventory().getSelectedSlot();
            }
        }
        if (block == -1) return;
        BlockPos placePos = mc.player.blockPosition().below();
        if (!slabMode) {
            if (BlockUtil.clientCanPlace(placePos, false)) {
                int old = mc.player.getInventory().getSelectedSlot();
                if (BlockUtil.getPlaceSide(placePos, null) == null) {
                    double distance = 1000;
                    BlockPos bestPos = null;
                    for (Direction i : Direction.values()) {
                        if (i == Direction.UP) continue;
                        if (BlockUtil.canPlace(placePos.relative(i))) {
                            if (bestPos == null || mc.player.distanceToSqr(placePos.relative(i).getCenter()) < distance) {
                                bestPos = placePos.relative(i);
                                distance = mc.player.distanceToSqr(placePos.relative(i).getCenter());
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
                if (mc.player.getInventory().getItem(block).getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SlabBlock) {
                    slabSide = Direction.UP;
                }
                if (side != null) {
                    event.renderer.box(new AABB(placePos), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                    InventoryUtil.switchToSlot(block);
                    BlockUtil.placeSlabBlock(placePos, side, slabSide, rotate.get());
                    InventoryUtil.switchToSlot(old);
                }
            }
        } else {
            placePos = pos;
            if (BlockUtil.getBlock(pos) instanceof SlabBlock) {
                Direction face = mc.player.getDirection();
                placePos = pos.relative(face);
            } else {
                if (BlockUtil.clientCanPlace(placePos, false)) {
                    if (BlockUtil.getPlaceSide(placePos, null) == null) {
                        double distance = 1000;
                        BlockPos bestPos = null;
                        for (Direction i : Direction.values()) {
                            if (i == Direction.UP) continue;
                            if (BlockUtil.canPlace(placePos.relative(i))) {
                                if (bestPos == null || mc.player.distanceToSqr(placePos.relative(i).getCenter()) < distance) {
                                    bestPos = placePos.relative(i);
                                    distance = mc.player.distanceToSqr(placePos.relative(i).getCenter());
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
            int old = mc.player.getInventory().getSelectedSlot();
            Direction side = BlockUtil.getPlaceSide(placePos, null);
            if (side != null && !(BlockUtil.getBlock(placePos) instanceof SlabBlock)) {
                event.renderer.box(new AABB(placePos), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                InventoryUtil.switchToSlot(block);
                BlockUtil.placeSlabBlock(placePos, side, Direction.DOWN, rotate.get());
                InventoryUtil.switchToSlot(old);
            }
        }
    }
}

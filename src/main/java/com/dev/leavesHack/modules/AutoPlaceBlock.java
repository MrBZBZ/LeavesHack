package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;

public class AutoPlaceBlock extends Module {
    private final Timer placeTimer = new Timer();
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgDirection = this.settings.createGroup("Direction");
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("Delay")
            .description("MS")
            .defaultValue(50)
            .min(0)
            .sliderMax(10000)
            .build()
    );
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("Range")
            .defaultValue(3)
            .min(0)
            .sliderMax(6)
            .build()
    );
    private final Setting<Block> selectBlock = sgGeneral.add(new BlockSetting.Builder()
            .name("Block")
            .defaultValue(Blocks.STONE)
            .build()
    );
    private final Setting<Directions> direction = sgDirection.add(new EnumSetting.Builder<Directions>()
            .name("Direction")
            .defaultValue(Directions.UP)
            .build()
    );
    private final Setting<SlabDirection> slabDirection = sgDirection.add(new EnumSetting.Builder<SlabDirection>()
            .name("SlabDirection")
            .defaultValue(SlabDirection.UP)
            .build()
    );
    private final Setting<Integer> blocksPer = sgGeneral.add(new IntSetting.Builder()
            .name("BlocksPer")
            .defaultValue(1)
            .min(0)
            .sliderMax(4)
            .build()
    );
    public AutoPlaceBlock() {
        super(LeavesHack.CATEGORY, "AutoPlaceBlock", "Automatically place block");
    }
    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (!placeTimer.passedMs(delay.get())) return;
        int oldSlot = mc.player.getInventory().selectedSlot;
        Block block = selectBlock.get();
        int slot = InventoryUtil.findBlock(block);
        if (slot == -1) return;
        InventoryUtil.switchToSlot(slot);
        int counts = 0;
        for (BlockPos pos : BlockUtil.getSphere(range.get())) {
            if (counts >= blocksPer.get()) break;
            if ((mc.world.isAir(pos) || mc.world.getBlockState(pos).isReplaceable()) && !BlockUtil.hasPlayerEntity(pos) && !BlockUtil.hasEntity(pos,false)) {
                ArrayList<Direction> side = BlockUtil.getPlaceSides(pos, null);
                if (!side.isEmpty()) {
                    for (Direction dir : side) {
                        if (checkDirection(dir)) {
                            if (block instanceof SlabBlock && dir.getAxis().isHorizontal()) {
                                switch (slabDirection.get()){
                                    case UP -> BlockUtil.placeSlabBlock(pos, dir, Direction.UP, true);
                                    case DOWN -> BlockUtil.placeSlabBlock(pos, dir, Direction.DOWN, true);
                                }
                            } else {
                                BlockUtil.placeBlock(pos, dir, true);
                            }
                            Color color = new Color(255, 255, 255, 80);
                            event.renderer.box(pos, color, color, ShapeMode.Both, 0);
                            counts++;
                        }
                    }
                }
            }
            placeTimer.reset();
        }
        InventoryUtil.switchToSlot(oldSlot);
    }

    private boolean checkDirection(Direction dir) {
        switch (direction.get()) {
            case UP -> {
                return dir == Direction.UP;
            }
            case DOWN -> {
                return dir == Direction.DOWN;
            }
            case HORIZONTAL -> {
                return dir.getAxis().isHorizontal();
            }
        }
        return false;
    }

    private enum Directions {
        UP,
        DOWN,
        HORIZONTAL
    }
    private enum SlabDirection {
        UP,
        DOWN,
    }
}

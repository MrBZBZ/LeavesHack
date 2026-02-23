package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.SlabBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class AutoPlaceSlab extends Module {
    private Timer placeTimer = new Timer();
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
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
            .defaultValue(5)
            .min(0)
            .sliderMax(6)
            .build()
    );
    private final Setting<Integer> blocksPer = sgGeneral.add(new IntSetting.Builder()
            .name("BlocksPer")
            .defaultValue(1)
            .min(0)
            .sliderMax(4)
            .build()
    );
    public AutoPlaceSlab() {
        super(LeavesHack.CATEGORY, "AutoPlaceSlab", "Automatically place slab block");
    }
    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (!placeTimer.passedMs(delay.get())) return;
        int oldSlot = mc.player.getInventory().selectedSlot;
        int slabSlot = InventoryUtil.findClass(SlabBlock.class);
        if (slabSlot == -1) return;
        InventoryUtil.switchToSlot(slabSlot);
        int counts = 0;
        for (BlockPos pos : BlockUtil.getSphere(range.get())) {
            if (counts >= blocksPer.get()) return;
            if (!(BlockUtil.getBlock(pos) instanceof SlabBlock) && !(BlockUtil.getBlock(pos.down()) instanceof SlabBlock) && (mc.world.isAir(pos) || mc.world.getBlockState(pos).isReplaceable()) && !mc.world.isAir(pos.down()) && !mc.world.getBlockState(pos.down()).isReplaceable() && !BlockUtil.hasPlayerEntity(pos) && !BlockUtil.hasEntity(pos,false)) {
                Direction side = BlockUtil.getPlaceSide(pos, null);
                if (side != null) {
                    BlockUtil.placeBlock(pos, side, true);
                    Color color = new Color(255, 255, 255, 80);
                    event.renderer.box(pos,color,color, ShapeMode.Both,0);
                    counts++;
                }
            }
            placeTimer.reset();
        }
        InventoryUtil.switchToSlot(oldSlot);
    }
}

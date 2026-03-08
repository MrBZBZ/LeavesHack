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
import net.minecraft.block.TorchBlock;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;

public class AutoTorch extends Module {
    public static AutoTorch INSTANCE;
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
    private final Setting<Integer> renderRange = sgGeneral.add(new IntSetting.Builder()
            .name("RenderRange")
            .defaultValue(10)
            .min(0)
            .sliderMax(10)
            .build()
    );
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("Range")
            .defaultValue(5)
            .min(0)
            .sliderMax(6)
            .build()
    );
    private final Setting<Integer> checkLightLevel = sgGeneral.add(new IntSetting.Builder()
            .name("CheckLightLevel")
            .defaultValue(7)
            .min(0)
            .sliderMax(15)
            .build()
    );
    public AutoTorch() {
        super(LeavesHack.CATEGORY, "AutoTorch", "Automatically place torch");
    }
    @EventHandler
    private void onRender3d(Render3DEvent event) {
        for (BlockPos pos : BlockUtil.getSphere(renderRange.get())) {
            if (mc.world.getLightLevel(LightType.BLOCK, pos) <= checkLightLevel.get() && BlockUtil.canPlace(pos)) {
                Color color = new Color(255, 0, 0, 255);
                event.renderer.line(
                        pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 1, pos.getY(), pos.getZ() + 1,
                        color
                );
                event.renderer.line(
                        pos.getX() + 1, pos.getY(), pos.getZ(),
                        pos.getX(), pos.getY(), pos.getZ() + 1,
                        color
                );
            }
        }
        if (!placeTimer.passedMs(delay.get())) return;
        int oldSlot = mc.player.getInventory().selectedSlot;
        int slot = InventoryUtil.findClass(TorchBlock.class);
        if (slot == -1) return;
        int counts = 0;
        for (BlockPos pos : BlockUtil.getSphere(range.get())) {
            if (counts >= 1) break;
            if (!(BlockUtil.getBlock(pos) instanceof TorchBlock) && !(BlockUtil.getBlock(pos.down()) instanceof TorchBlock) && (mc.world.isAir(pos) || mc.world.getBlockState(pos).isReplaceable()) && !mc.world.isAir(pos.down()) && !mc.world.getBlockState(pos.down()).isReplaceable() && !BlockUtil.hasPlayerEntity(pos) && !BlockUtil.hasEntity(pos,false)) {
                if (mc.world.getLightLevel(LightType.BLOCK, pos)  > checkLightLevel.get()) continue;
                Direction side = BlockUtil.getPlaceSide(pos, null);
                if (side != null && side != Direction.UP) {
                    InventoryUtil.switchToSlot(slot);
                    BlockUtil.placeBlock(pos, side, true);
                    Color color = new Color(255, 255, 255, 80);
                    event.renderer.box(pos,color,color, ShapeMode.Both,0);
                    counts++;
                    InventoryUtil.switchToSlot(oldSlot);
                }
            }
        }
        placeTimer.reset();
    }
}

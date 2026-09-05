package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class AutoTorch extends Module {
    public static AutoTorch INSTANCE;
    private Timer placeTimer = new Timer();
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("Delay")
        .description("放置延迟(毫秒MS)")
        .defaultValue(50)
        .min(0)
        .sliderMax(10000)
        .build()
    );
    private final Setting<Integer> renderRange = sgGeneral.add(new IntSetting.Builder()
        .name("RenderRange")
        .description("渲染距离")
        .defaultValue(10)
        .min(0)
        .sliderMax(10)
        .build()
    );
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("Range")
        .description("操作距离")
        .defaultValue(5)
        .min(0)
        .sliderMax(6)
        .build()
    );
    private final Setting<Boolean> onlyRender = sgGeneral.add(new BoolSetting.Builder()
        .name("OnlyRender")
        .description("仅渲染而不操作")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> throughWall = sgGeneral.add(new BoolSetting.Builder()
        .name("ThroughWall")
        .description("穿墙放置")
        .defaultValue(false)
        .build()
    );
    private final Setting<Integer> checkLightLevel = sgGeneral.add(new IntSetting.Builder()
        .name("CheckLightLevel")
        .description("光照等级")
        .defaultValue(7)
        .min(0)
        .sliderMax(15)
        .build()
    );
    public AutoTorch() {
        super(LeavesHack.LEAVES_MISC, "AutoTorch", "自动放火把");
        INSTANCE = this;
    }
    @EventHandler
    private void onRender3d(Render3DEvent event) {
        for (BlockPos pos : BlockUtil.getSphere(renderRange.get())) {
            if (mc.level.getBrightness(LightLayer.BLOCK, pos) <= checkLightLevel.get() && BlockUtil.canPlace(pos)) {
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
        if (onlyRender.get()) return;
        if (!placeTimer.passedMs(delay.get())) return;
        int oldSlot = mc.player.getInventory().getSelectedSlot();
        int slot = InventoryUtil.findClass(TorchBlock.class);
        if (slot == -1) return;
        int counts = 0;
        for (BlockPos pos : BlockUtil.getSphere(range.get())) {
            if (counts >= 1) break;
            if (throughWall.get() && behindWall(pos)) continue;
            if (!(BlockUtil.getBlock(pos) instanceof TorchBlock) && !(BlockUtil.getBlock(pos.below()) instanceof TorchBlock) && mc.level.isEmptyBlock(pos) && !mc.level.isEmptyBlock(pos.below()) && !mc.level.getBlockState(pos.below()).canBeReplaced() && !BlockUtil.hasPlayerEntity(pos) && !BlockUtil.hasEntity(pos,false)) {
                if (mc.level.getBrightness(LightLayer.BLOCK, pos)  > checkLightLevel.get()) continue;
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
    public boolean behindWall(BlockPos pos) {
        Vec3 testVec = new Vec3(pos.getX() + 0.5, pos.getY() + 2 * 0.85, pos.getZ() + 0.5);
        HitResult result = mc.level.clip(new ClipContext(mc.player.getEyePosition(), testVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return result != null && result.getType() != HitResult.Type.MISS;
    }
}

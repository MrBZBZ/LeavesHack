package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.dev.leavesHack.utils.world.BlockUtil;
import java.util.ArrayList;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class AutoTree extends Module {
    public static AutoTree INSTANCE;
    public AutoTree() {
        super(LeavesHack.LEAVES_MISC, "AutoTree", "自动树场");
        INSTANCE = this;
    }
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Integer> useDelay = sgGeneral.add(new IntSetting.Builder()
        .name("UseDelay")
        .description("使用延迟(毫秒MS)")
        .defaultValue(50)
        .min(0)
        .sliderMax(1000)
        .build()
    );
    private final Setting<Integer> BlocksPer = sgGeneral.add(new IntSetting.Builder()
        .name("BlocksPer")
        .description("每tick操作方块数量")
        .defaultValue(1)
        .min(0)
        .sliderMax(4)
        .build()
    );
    private final Setting<Boolean> useBoneMeal = sgGeneral.add(new BoolSetting.Builder()
        .name("UseBoneMeal")
        .description("使用骨粉")
        .defaultValue(true)
        .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("Shape Mode")
        .description("渲染模式")
        .defaultValue(ShapeMode.Both)
        .build()
    );
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("Line Color")
        .description("外框颜色")
        .defaultValue(new SettingColor(new java.awt.Color(255, 255, 255, 255)))
        .build()
    );
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("Side Color")
        .description("填充颜色")
        .defaultValue(new SettingColor(new java.awt.Color(255, 255, 255, 50)))
        .build()
    );
    public ArrayList<BlockPos> treePos = new ArrayList<>();
    public Timer timer = new Timer();
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!treePos.isEmpty()) {
            for (BlockPos pos : treePos) {
                event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
            if (!timer.passedMs(useDelay.get())) return;
            int i = 0;
            int old = mc.player.getInventory().getSelectedSlot();
            int tree = InventoryUtil.findClass(SaplingBlock.class);
            int boneMeal = InventoryUtil.findItem(Items.BONE_MEAL);
            if (mc.player.getInventory().getItem(mc.player.getInventory().getSelectedSlot()).getItem() instanceof BlockItem treeItem && treeItem.getBlock() instanceof SaplingBlock) {
                for (BlockPos pos : treePos) {
                    if (i >= BlocksPer.get()) break;
                    if (tree != -1) {
                        if (useBoneMeal.get() && boneMeal == -1) return;
                        if (BlockUtil.getBlock(pos.above()) instanceof SaplingBlock && useBoneMeal.get()) {
                            Direction side = BlockUtil.getClickSide(pos.above());
                            InventoryUtil.switchToSlot(boneMeal);
                            mc.player.swing(InteractionHand.MAIN_HAND);
                            clickBlock(pos.above(), side, true);
                            InventoryUtil.switchToSlot(old);
                            i++;
                        } else if (mc.level.isEmptyBlock(pos.above()) || mc.level.getBlockState(pos.above()).canBeReplaced()) {
                            BlockUtil.placeBlock(pos.above(), Direction.DOWN, true);
                            i++;
                        }
                        timer.reset();
                    }
                }
            }
        }
    }
    @Override
    public void onActivate() {
        treePos.clear();
        timer.setMs(999999);
    }
    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (!BlockUtils.canBreak(event.blockPos)) return;
        event.cancel();
        if (!treePos.contains(event.blockPos)) {
            treePos.add(event.blockPos);
        } else {
            treePos.remove(event.blockPos);
        }
    }
    public void clickBlock(BlockPos pos, Direction side, boolean rotate) {
        Vec3 directionVec = new Vec3(pos.getX() + 0.5 + side.getUnitVec3i().getX() * 0.5, pos.getY() + 0.5 + side.getUnitVec3i().getY() * 0.5, pos.getZ() + 0.5 + side.getUnitVec3i().getZ() * 0.5);
        if (rotate) Rotation.snapAt(directionVec);
        mc.player.swing(InteractionHand.MAIN_HAND);
        BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, result);
        if (rotate) Rotation.snapBack();
    }
}

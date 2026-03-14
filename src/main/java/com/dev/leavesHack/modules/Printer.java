package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.dev.leavesHack.utils.world.BlockUtil;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class Printer extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgShift = this.settings.createGroup("IgnoreSneak");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("Rotate")
            .description("Rotate towards blocks when placing.")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> printingRange = sgGeneral.add(new IntSetting.Builder()
            .name("PrintingRange")
            .description("How far to place blocks around the player.")
            .defaultValue(5)
            .min(1)
            .sliderMax(6)
            .build()
    );
    private final Setting<Boolean> inventorySwap = sgGeneral.add(new BoolSetting.Builder()
            .name("InventorySwap")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> ignoreSneak = sgShift.add(new BoolSetting.Builder()
            .name("IgnoreSneak")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> shiftTime = sgShift.add(new IntSetting.Builder()
            .name("ShiftTime")
            .defaultValue(100)
            .min(0)
            .sliderMax(1000)
            .build()
    );
    // 渲染设置
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("Shape Mode")
            .defaultValue(ShapeMode.Both)
            .build()
    );
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("Line Color")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
    );
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("Side Color")
            .defaultValue(new SettingColor(255, 255, 255, 50))
            .build()
    );
    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
            .name("DeBug")
            .description("Dev用来测试的，iq低的不要开")
            .defaultValue(false)
            .build()
    );
    public Printer() {
        super(LeavesHack.CATEGORY, "printer", "Places blocks based on a Litematica schematic.");
    }
    boolean hasSneak = false;
    private Timer shiftTimer = new Timer();
    @Override
    public void onActivate() {
        hasSneak = false;
        shiftTimer.setMs(99999);
    }
    @Override
    public void onDeactivate() {
        if (hasSneak) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
            hasSneak = false;
        }
    }
    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;
        WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
        if (schematic == null) return;
        if (!shiftTimer.passedMs(shiftTime.get()) && hasSneak) return;
        List<BlockPos> sphere = BlockUtil.getSphere(printingRange.get());
        int placed = 0;
        for (BlockPos pos : sphere) {
            BlockState required = schematic.getBlockState(pos);
            if (!required.isAir() && !required.isLiquid() && (mc.world.isAir(pos) || BlockUtil.canReplace(pos)) && !BlockUtil.hasEntity(pos, false)) {
                if (placed >= 1) {
                    if (debug.get()) mc.player.sendMessage(Text.of("已超过最大数量，当前placed:" + placed));
                    return;
                }
                int slot = inventorySwap.get() ? InventoryUtil.findBlockInventory(required.getBlock()) : InventoryUtil.findBlock(required.getBlock());
                if (slot == -1) continue;
                int old = mc.player.getInventory().selectedSlot;
                ArrayList<Direction> sides = BlockUtil.getPlaceSides(pos, null, ignoreSneak.get());
                if (sides.isEmpty()) continue;
                event.renderer.box(new Box(pos), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                Direction target = sides.getFirst();
                Direction facing = getBlockFacing(required);
                if (facing != null && !isRedstoneComponent(required)) {
                    if (debug.get()) mc.player.sendMessage(Text.of("方块包含方向"));
                    boolean find = false;
                    for (Direction i : sides) {
                        if (debug.get()) mc.player.sendMessage(Text.of("side列表: " + i));
                        if (checkState(pos.offset(i), required, i.getOpposite())) {
                            find = true;
                            target = i;
                        }
                    }
                    if (!find) {
                        if (debug.get()) mc.player.sendMessage(Text.of("未找到目标方向"));
                        continue;
                    }
                }
                if (required.getBlock() instanceof RedstoneWireBlock && (mc.world.isAir(pos.down()) || mc.world.getBlockState(pos.down()).isReplaceable())) continue;
                if (BlockUtil.needSneak(BlockUtil.getBlock(pos.offset(target))) && !hasSneak) {
                    mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
                    hasSneak = true;
                    shiftTimer.reset();
                    return;
                }
                placed++;
                doSwap(slot);
                if (rotate.get()) {
                    Vec3d directionVec = new Vec3d(pos.getX() + 0.5 + target.getVector().getX() * 0.5, pos.getY() + 0.5 + target.getVector().getY() * 0.5, pos.getZ() + 0.5 + target.getVector().getZ() * 0.5);
                    Rotation.snapAt(directionVec);
                }
                if (facing != null && isRedstoneComponent(required)) blockFacing(facing.getOpposite());
                SlabType type = getSlabType(required);
                if (type != null) {
                    switch (type) {
                        case SlabType.TOP -> {
                            if (!(BlockUtil.getBlock(pos) instanceof SlabBlock)) BlockUtil.placeSlabBlock(pos, target, Direction.UP, false);
                        }
                        case SlabType.BOTTOM -> {
                            if (!(BlockUtil.getBlock(pos) instanceof SlabBlock)) BlockUtil.placeSlabBlock(pos, target, Direction.DOWN, false);
                        }
                    }
                } else {
                    BlockUtil.placeBlock(pos, target, false);
                }
                if (BlockUtil.needSneak(BlockUtil.getBlock(pos.offset(target)))) {
                    mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
                    hasSneak = false;
                }
                Rotation.snapBack();
                event.renderer.box(new Box(pos), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                if (inventorySwap.get()) {
                    doSwap(slot);
                } else {
                    doSwap(old);
                }
            }
        }
    }
    public static SlabType getSlabType(BlockState state) {
        if (state.getBlock() instanceof SlabBlock) {
            return state.get(SlabBlock.TYPE);
        }
        return null;
    }
    public void blockFacing(Direction i){
        if (i == Direction.EAST) {
            Rotation.snapAt(-90.0f, 5.0f);
        } else if (i == Direction.WEST) {
            Rotation.snapAt(90.0f, 5.0f);
        } else if (i == Direction.NORTH) {
            Rotation.snapAt(180.0f, 5.0f);
        } else if (i == Direction.SOUTH) {
            Rotation.snapAt(0.0f, 5.0f);
        } else if (i == Direction.UP) {
            Rotation.snapAt(5.0f, -90.0f);
        } else if (i == Direction.DOWN) {
            Rotation.snapAt(5.0f, 90.0f);
        }
    }
    public static boolean isRedstoneComponent(BlockState state) {
        Block block = state.getBlock();

        return block instanceof RedstoneWireBlock
                || block instanceof AbstractRedstoneGateBlock
                || block instanceof PressurePlateBlock
                || block instanceof ObserverBlock
                || block instanceof TargetBlock
                || block instanceof TripwireHookBlock
                || block instanceof DaylightDetectorBlock
                || block instanceof PistonBlock
                || block instanceof RedstoneLampBlock
                || block instanceof FurnaceBlock;
    }
    public boolean checkState(BlockPos pos, BlockState targetState, Direction i) {
        Vec3d directionVec = new Vec3d(pos.getX() + 0.5 + i.getVector().getX() * 0.5, pos.getY() + 0.5 + i.getVector().getY() * 0.5, pos.getZ() + 0.5 + i.getVector().getZ() * 0.5);
        BlockHitResult hit = new BlockHitResult(
                directionVec,
                i,
                pos,
                false
        );
        ItemPlacementContext ctx = new ItemPlacementContext(
                mc.player,
                Hand.MAIN_HAND,
                mc.player.getMainHandStack(),
                hit
        );
        BlockState result = targetState.getBlock().getPlacementState(ctx);
        if (result != null && isSameFacing(result, targetState)) {
            return true;
        } else if (result == null) {
            if (debug.get()) mc.player.sendMessage(Text.of("result: null"));
        }
        return false;
    }
    public static Direction getBlockFacing(BlockState state) {
        if (state.getBlock() instanceof HopperBlock) {
            return state.get(HopperBlock.FACING);
        }
        if (state.contains(Properties.HORIZONTAL_FACING)) {
            return state.get(Properties.HORIZONTAL_FACING);
        }
        if (state.contains(Properties.FACING)) {
            return state.get(Properties.FACING);
        }
        if (state.contains(Properties.AXIS)) {
            switch (state.get(Properties.AXIS)) {
                case X: return Direction.EAST;
                case Y: return Direction.UP;
                case Z: return Direction.SOUTH;
            }
        }

        return null;
    }
    private boolean isSameFacing(BlockState a, BlockState b) {
        if (a.getBlock() != b.getBlock()) return false;

        Direction fa = getBlockFacing(a);
        Direction fb = getBlockFacing(b);



        if (debug.get()) mc.player.sendMessage(Text.of("fa: " + fa + " fb: " + fb));
        if (fa == null || fb == null) return true;

        return fa == fb;
    }
    private void doSwap(int slot) {
        if (!inventorySwap.get()) {
            InventoryUtil.switchToSlot(slot);
        } else {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        }
    }
}
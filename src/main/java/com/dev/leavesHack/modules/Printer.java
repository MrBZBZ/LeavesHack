package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.events.MoveEvent;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.dev.leavesHack.utils.world.BlockUtil;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import java.util.ArrayList;
import java.util.List;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.FurnaceBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ObserverBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.TargetBlock;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class Printer extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgShift = this.settings.createGroup("IgnoreSneak");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("Rotate")
        .description("转头")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> printingRange = sgGeneral.add(new IntSetting.Builder()
        .name("PrintingRange")
        .description("打印距离")
        .defaultValue(4)
        .min(1)
        .sliderMax(6)
        .build()
    );
    private final Setting<Boolean> inventorySwap = sgGeneral.add(new BoolSetting.Builder()
        .name("InventorySwap")
        .description("背包鬼手")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> safeWalk = sgGeneral.add(new BoolSetting.Builder()
        .name("SafeWalk")
        .description("安全行走")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> ignoreSneak = sgShift.add(new BoolSetting.Builder()
        .name("IgnoreSneak")
        .description("忽略潜行")
        .defaultValue(true)
        .build()
    );
    private final Setting<Integer> shiftTime = sgShift.add(new IntSetting.Builder()
        .name("ShiftTime")
        .description("潜行时间")
        .defaultValue(100)
        .min(0)
        .sliderMax(1000)
        .build()
    );
    private final Setting<Integer> sneakSpeed = sgShift.add(new IntSetting.Builder()
        .name("SneakSpeed")
        .description("潜行速度（目前来看站着不动是最好的选择）")
        .defaultValue(0)
        .min(0)
        .sliderMax(20)
        .build()
    );
    private final Setting<ListMode> listMode = sgWhitelist.add(new EnumSetting.Builder<ListMode>()
        .name("ListMode")
        .description("选择模式")
        .defaultValue(ListMode.Blacklist)
        .build()
    );

    private final Setting<List<Block>> blacklist = sgWhitelist.add(new BlockListSetting.Builder()
        .name("BlackList")
        .description("黑名单")
        .visible(() -> listMode.get() == ListMode.Blacklist)
        .build()
    );

    private final Setting<List<Block>> whitelist = sgWhitelist.add(new BlockListSetting.Builder()
        .name("WhiteList")
        .description("白名单")
        .visible(() -> listMode.get() == ListMode.Whitelist)
        .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("ShapeMode")
        .defaultValue(ShapeMode.Both)
        .build()
    );
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("LineColor")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("SideColor")
        .defaultValue(new SettingColor(255, 255, 255, 50))
        .build()
    );
    public Printer() {
        super(LeavesHack.LEAVES_MISC, "Printer", "打印机");
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
            mc.player.setShiftKeyDown(false);
            hasSneak = false;
        }
    }
    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.level == null) return;
        WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
        if (schematic == null) return;
        if (!shiftTimer.passedMs(shiftTime.get()) && hasSneak && ignoreSneak.get()) {
            return;
        }
        List<BlockPos> sphere = BlockUtil.getSphere(printingRange.get());
        int placed = 0;
        for (BlockPos pos : sphere) {
            BlockState required = schematic.getBlockState(pos);
            if (listMode.get() == ListMode.Blacklist && blacklist.get().contains(required.getBlock())) continue;
            if (listMode.get() == ListMode.Whitelist && !whitelist.get().contains(required.getBlock())) continue;
            if (!required.isAir() && !required.isRedstoneConductor(mc.level, pos) && (mc.level.isEmptyBlock(pos) || BlockUtil.canReplace(pos)) && !BlockUtil.hasEntity(pos, false)) {
                if (placed >= 1) {
                    return;
                }
                int slot = inventorySwap.get() ? InventoryUtil.findBlockInventory(required.getBlock()) : InventoryUtil.findBlock(required.getBlock());
                if (slot == -1) continue;
                int old = mc.player.getInventory().getSelectedSlot();
                ArrayList<Direction> sides = BlockUtil.getPlaceSides(pos, null, ignoreSneak.get());
                if (sides.isEmpty()) continue;
                event.renderer.box(new AABB(pos), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                Direction target = sides.getFirst();
                Direction facing = getBlockFacing(required);
                if (facing != null && !isRedstoneComponent(required)) {
                    boolean find = false;
                    for (Direction i : sides) {
                        if (checkState(pos.relative(i), required, i.getOpposite())) {
                            find = true;
                            target = i;
                        }
                    }
                    if (!find) {
                        continue;
                    }
                }
                if (required.getBlock() instanceof RedStoneWireBlock && (mc.level.isEmptyBlock(pos.below()) || mc.level.getBlockState(pos.below()).canBeReplaced())) continue;
                if (BlockUtil.needSneak(BlockUtil.getBlock(pos.relative(target))) && !hasSneak) {
                    mc.player.setShiftKeyDown(true);
                    hasSneak = true;
                    mc.player.setShiftKeyDown(true);
                    shiftTimer.reset();
                    return;
                }
                placed++;
                doSwap(slot);
                if (rotate.get()) {
                    Vec3 directionVec = new Vec3(pos.getX() + 0.5 + target.getUnitVec3i().getX() * 0.5, pos.getY() + 0.5 + target.getUnitVec3i().getY() * 0.5, pos.getZ() + 0.5 + target.getUnitVec3i().getZ() * 0.5);
                    Rotation.snapAt(directionVec);
                }
                if (facing != null && isRedstoneComponent(required)) {
                    if ((required.getBlock() instanceof ObserverBlock)) {
                        blockFacing(facing);
                    } else {
                        blockFacing(facing.getOpposite());
                    }
                }
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
                if (hasSneak && ignoreSneak.get()) {
                    mc.player.setShiftKeyDown(false);
                    hasSneak = false;
                }
                Rotation.snapBack();
                event.renderer.box(new AABB(pos), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                if (inventorySwap.get()) {
                    doSwap(slot);
                } else {
                    doSwap(old);
                }
            }
        }
    }
    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundOpenScreenPacket packet) {
            mc.getConnection().send(new ServerboundContainerClosePacket(packet.getContainerId()));
            event.cancel();
        }
    }
    @EventHandler(priority = EventPriority.LOW)
    public void onMove1(MoveEvent event) {
        if (safeWalk.get()) {
            double x = event.getX();
            double y = event.getY();
            double z = event.getZ();
            if (mc.player.onGround()) {
                double increment = 0.05;
                while (x != 0.0 && this.isOffsetBBEmpty(x, -1.0, 0.0)) {
                    if (x < increment && x >= -increment) {
                        x = 0.0;
                        continue;
                    }
                    if (x > 0.0) {
                        x -= increment;
                        continue;
                    }
                    x += increment;
                }
                while (z != 0.0 && this.isOffsetBBEmpty(0.0, -1.0, z)) {
                    if (z < increment && z >= -increment) {
                        z = 0.0;
                        continue;
                    }
                    if (z > 0.0) {
                        z -= increment;
                        continue;
                    }
                    z += increment;
                }
                while (x != 0.0 && z != 0.0 && this.isOffsetBBEmpty(x, -1.0, z)) {
                    x = x < increment && x >= -increment ? 0.0 : (x > 0.0 ? x - increment : x + increment);
                    if (z < increment && z >= -increment) {
                        z = 0.0;
                        continue;
                    }
                    if (z > 0.0) {
                        z -= increment;
                        continue;
                    }
                    z += increment;
                }
            }
            event.setX(x);
            event.setY(y);
            event.setZ(z);
        }
    }

    public boolean isOffsetBBEmpty(double offsetX, double offsetY, double offsetZ) {
        return !mc.level.collidesWithSuffocatingBlock(mc.player, mc.player.getBoundingBox().move(offsetX, offsetY, offsetZ));
    }
    @EventHandler
    public void onMove2(MoveEvent event) {
        if (shiftTimer.passedMs(shiftTime.get() * 2) && ignoreSneak.get() && hasSneak) {
            mc.player.setShiftKeyDown(false);
            hasSneak = false;
            return;
        }
        if (!hasSneak) return;
        double speed = sneakSpeed.get();
        double moveSpeed = 0.2873 / 100 * speed;
        double n = mc.player.input.getMoveVector().y;
        double n2 = mc.player.input.getMoveVector().x;
        double n3 = mc.player.getYRot();
        if (n == 0.0 && n2 == 0.0) {
            event.setX(0.0);
            event.setZ(0.0);
            return;
        } else if (n != 0.0 && n2 != 0.0) {
            n *= Math.sin(0.7853981633974483);
            n2 *= Math.cos(0.7853981633974483);
        }
        event.setX((n * moveSpeed * -Math.sin(Math.toRadians(n3)) + n2 * moveSpeed * Math.cos(Math.toRadians(n3))));
        event.setZ((n * moveSpeed * Math.cos(Math.toRadians(n3)) - n2 * moveSpeed * -Math.sin(Math.toRadians(n3))));
    }
    public static SlabType getSlabType(BlockState state) {
        if (state.getBlock() instanceof SlabBlock) {
            return state.getValue(SlabBlock.TYPE);
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

        return block instanceof RedStoneWireBlock
                || block instanceof DiodeBlock
                || block instanceof PressurePlateBlock
                || block instanceof ObserverBlock
                || block instanceof TargetBlock
                || block instanceof TripWireHookBlock
                || block instanceof DaylightDetectorBlock
                || block instanceof PistonBaseBlock
                || block instanceof RedstoneLampBlock
                || block instanceof FurnaceBlock;
    }
    public boolean checkState(BlockPos pos, BlockState targetState, Direction i) {
        Vec3 directionVec = new Vec3(pos.getX() + 0.5 + i.getUnitVec3i().getX() * 0.5, pos.getY() + 0.5 + i.getUnitVec3i().getY() * 0.5, pos.getZ() + 0.5 + i.getUnitVec3i().getZ() * 0.5);
        BlockHitResult hit = new BlockHitResult(
                directionVec,
                i,
                pos,
                false
        );
        BlockPlaceContext ctx = new BlockPlaceContext(
                mc.player,
                InteractionHand.MAIN_HAND,
                mc.player.getMainHandItem(),
                hit
        );
        BlockState result = targetState.getBlock().getStateForPlacement(ctx);
        if (result != null && isSameFacing(result, targetState)) {
            return true;
        }
        return false;
    }
    public static Direction getBlockFacing(BlockState state) {
        if (state.getBlock() instanceof HopperBlock) {
            return state.getValue(HopperBlock.FACING);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return state.getValue(BlockStateProperties.FACING);
        }
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            switch (state.getValue(BlockStateProperties.AXIS)) {
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
        if (fa == null || fb == null) return true;

        return fa == fb;
    }
    private void doSwap(int slot) {
        if (!inventorySwap.get()) {
            InventoryUtil.switchToSlot(slot);
        } else {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().getSelectedSlot());
        }
    }
    public enum ListMode {
        Whitelist,
        Blacklist
    }
}

package com.dev.leavesHack.utils.world;

import com.dev.leavesHack.utils.rotation.Rotation;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BlockUtil {
    public static boolean canClick(BlockPos pos) {
        return mc.world.getBlockState(pos).isSolid() && (!(shiftBlocks.contains(getBlock(pos)) || getBlock(pos) instanceof BedBlock) || mc.player.isSneaking());
    }
    public static Direction getPlaceSide(BlockPos pos, Predicate<Direction> directionPredicate) {
        if (pos == null) return null;
        double dis = 114514;
        Direction side = null;
        for (Direction i : Direction.values()) {
            if (directionPredicate != null && !directionPredicate.test(i)) continue;
            if (canClick(pos.offset(i)) && !mc.world.getBlockState(pos.offset(i)).isReplaceable()) {
                if (!isGrimDirection(pos.offset(i), i.getOpposite()))continue;
                double vecDis = mc.player.getEyePos().squaredDistanceTo(pos.toCenterPos().add(i.getVector().getX() * 0.5, i.getVector().getY() * 0.5, i.getVector().getZ() * 0.5));
                if (side == null || vecDis < dis) {
                    side = i;
                    dis = vecDis;
                }
            }
        }
        return side;
    }
    private static Box getCombinedBox(BlockPos pos, World level) {
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos).offset(pos.getX(), pos.getY(), pos.getZ());
        Box combined = new Box(pos);
        for (Box box : shape.getBoundingBoxes()) {
            double minX = Math.max(box.minX, combined.minX);
            double minY = Math.max(box.minY, combined.minY);
            double minZ = Math.max(box.minZ, combined.minZ);
            double maxX = Math.min(box.maxX, combined.maxX);
            double maxY = Math.min(box.maxY, combined.maxY);
            double maxZ = Math.min(box.maxZ, combined.maxZ);
            combined = new Box(minX, minY, minZ, maxX, maxY, maxZ);
        }

        return combined;
    }
    private static boolean isIntersected(Box bb, Box other) {
        return other.maxX - VoxelShapes.MIN_SIZE > bb.minX
                && other.minX + VoxelShapes.MIN_SIZE < bb.maxX
                && other.maxY - VoxelShapes.MIN_SIZE > bb.minY
                && other.minY + VoxelShapes.MIN_SIZE < bb.maxY
                && other.maxZ - VoxelShapes.MIN_SIZE > bb.minZ
                && other.minZ + VoxelShapes.MIN_SIZE < bb.maxZ;
    }
    private static final double MIN_EYE_HEIGHT = 0.4;
    private static final double MAX_EYE_HEIGHT = 1.62;
    private static final double MOVEMENT_THRESHOLD = 0.0002;
    public static boolean isGrimDirection(BlockPos pos, Direction direction) {
        // see ac.grim.grimac.checks.impl.scaffolding.PositionPlace
        Box combined = getCombinedBox(pos, mc.world);
        ClientPlayerEntity player = mc.player;
        Box eyePositions = new Box(player.getX(), player.getY() + MIN_EYE_HEIGHT, player.getZ(), player.getX(), player.getY() + MAX_EYE_HEIGHT, player.getZ()).expand(MOVEMENT_THRESHOLD);
        if (isIntersected(eyePositions, combined)) {
            return true;
        }
        return !switch (direction) {
            case NORTH -> eyePositions.minZ > combined.minZ;
            case SOUTH -> eyePositions.maxZ < combined.maxZ;
            case EAST -> eyePositions.maxX < combined.maxX;
            case WEST -> eyePositions.minX > combined.minX;
            case UP -> eyePositions.maxY < combined.maxY;
            case DOWN -> eyePositions.minY > combined.minY;
        };
    }
    public static final List<Block> shiftBlocks = Arrays.asList(
            Blocks.ENDER_CHEST, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.CRAFTING_TABLE,
            Blocks.BIRCH_TRAPDOOR, Blocks.BAMBOO_TRAPDOOR, Blocks.DARK_OAK_TRAPDOOR, Blocks.CHERRY_TRAPDOOR,
            Blocks.ANVIL, Blocks.BREWING_STAND, Blocks.HOPPER, Blocks.DROPPER, Blocks.DISPENSER,
            Blocks.ACACIA_TRAPDOOR, Blocks.ENCHANTING_TABLE, Blocks.WHITE_SHULKER_BOX, Blocks.ORANGE_SHULKER_BOX,
            Blocks.MAGENTA_SHULKER_BOX, Blocks.LIGHT_BLUE_SHULKER_BOX, Blocks.YELLOW_SHULKER_BOX, Blocks.LIME_SHULKER_BOX,
            Blocks.PINK_SHULKER_BOX, Blocks.GRAY_SHULKER_BOX, Blocks.CYAN_SHULKER_BOX, Blocks.PURPLE_SHULKER_BOX,
            Blocks.BLUE_SHULKER_BOX, Blocks.BROWN_SHULKER_BOX, Blocks.GREEN_SHULKER_BOX, Blocks.RED_SHULKER_BOX, Blocks.BLACK_SHULKER_BOX
    );
    public static void placeBlock(BlockPos pos, Direction side, boolean rotate) {
        clickBlock(pos.offset(side), side.getOpposite(), rotate);
    }
    public static Block getBlock(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock();
    }
    public static void clickBlock(BlockPos pos, Direction side, boolean rotate) {
        Vec3d directionVec = new Vec3d(pos.getX() + 0.5 + side.getVector().getX() * 0.5, pos.getY() + 0.5 + side.getVector().getY() * 0.5, pos.getZ() + 0.5 + side.getVector().getZ() * 0.5);
        if (rotate) Rotation.snapAt(directionVec);
        mc.player.swingHand(Hand.MAIN_HAND);
        BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result);
        if (rotate) Rotation.snapBack();
    }
}

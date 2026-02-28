package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.combat.CombatUtil;
import com.dev.leavesHack.utils.world.BlockPosX;
import com.dev.leavesHack.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.PickaxeItem;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class AutoCity extends Module {
    public static AutoCity INSTANCE;
    public AutoCity() {
        super(LeavesHack.CATEGORY, "AutoCity", "Automatically breaks obsidian");
        INSTANCE = this;
    }
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Integer> targetRange = sgGeneral.add(new IntSetting.Builder()
            .name("TargetRange")
            .defaultValue(6)
            .min(0)
            .sliderMax(8)
            .build()
    );
    public final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("Range")
            .defaultValue(6)
            .min(0)
            .sliderMax(8)
            .build()
    );
    private final Setting<Boolean> antiCrawl = sgGeneral.add(new BoolSetting.Builder()
            .name("AntiCrawl")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> preferSelfClick = sgGeneral.add(new BoolSetting.Builder()
            .name("PreferSelfClick")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> head = sgGeneral.add(new BoolSetting.Builder()
            .name("Head")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> burrow = sgGeneral.add(new BoolSetting.Builder()
            .name("Burrow")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> face = sgGeneral.add(new BoolSetting.Builder()
            .name("Face")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> down = sgGeneral.add(new BoolSetting.Builder()
            .name("Down")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> surround = sgGeneral.add(new BoolSetting.Builder()
            .name("Surround")
            .defaultValue(true)
            .build()
    );
    @EventHandler
    public void onTick(TickEvent.Pre event) {
        PlayerEntity player = CombatUtil.getClosestEnemy(targetRange.get());
        if (preferSelfClick.get() && PacketMine.slefClickPos != null) return;
        if (antiCrawl.get() && mc.player.isCrawling()) {
            if (canBreak(mc.player.getBlockPos().up()) && !mc.player.getBlockPos().up().equals(PacketMine.targetPos)) {
                PacketMine.slefClickPos = mc.player.getBlockPos().up();
                PacketMine.mine(mc.player.getBlockPos().up());
                return;
            }
        }
        if (player == null) return;
        doBreak(player);
    }

    private void doBreak(PlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        double[] yOffset = new double[]{-0.8, 0.3, 2.3, 1.1};
        double[] xzOffset = new double[]{0.3, -0.3};
        for (PlayerEntity entity : CombatUtil.getEnemies(targetRange.get())) {
            for (double y : yOffset) {
                for (double x : xzOffset) {
                    for (double z : xzOffset) {
                        BlockPos offsetPos = new BlockPosX(entity.getX() + x, entity.getY() + y, entity.getZ() + z);
                        if (canBreak(offsetPos) && offsetPos.equals(PacketMine.targetPos)) {
                            return;
                        }
                    }
                }
            }
        }
        List<Float> yList = new ArrayList<>();
        if (down.get()) {
            yList.add(-0.8f);
        }
        if (head.get()) {
            yList.add(2.3f);
        }
        if (burrow.get()) {
            yList.add(0.3f);
        }
        if (face.get()) {
            yList.add(1.1f);
        }
        for (double y : yList) {
            for (double offset : xzOffset) {
                BlockPos offsetPos = new BlockPosX(player.getX() + offset, player.getY() + y, player.getZ() + offset);
                if (canBreak(offsetPos)) {
                    PacketMine.mine(offsetPos);
                    return;
                }
            }
        }
        for (double y : yList) {
            for (double offset : xzOffset) {
                for (double offset2 : xzOffset) {
                    BlockPos offsetPos = new BlockPosX(player.getX() + offset2, player.getY() + y, player.getZ() + offset);
                    if (canBreak(offsetPos)) {
                        PacketMine.mine(offsetPos);
                        return;
                    }
                }
            }
        }
        if (surround.get()) {
            for (Direction i : Direction.values()) {
                if (i == Direction.UP || i == Direction.DOWN) continue;
                if (Math.sqrt(mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos())) > range.get()) {
                    continue;
                }
                if ((mc.world.isAir(pos.offset(i)) || pos.offset(i).equals(PacketMine.targetPos)) && canPlaceCrystal(pos.offset(i), false)) {
                    return;
                }
            }
            ArrayList<BlockPos> list = new ArrayList<>();
            for (Direction i : Direction.values()) {
                if (i == Direction.UP || i == Direction.DOWN) continue;
                if (Math.sqrt(mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos())) > range.get()) {
                    continue;
                }
                if (canBreak(pos.offset(i)) && canPlaceCrystal(pos.offset(i), true) && !isSurroundPos(pos.offset(i))) {
                    list.add(pos.offset(i));
                }
            }
            if (!list.isEmpty()) {
                PacketMine.INSTANCE.mine(list.stream().min(Comparator.comparingDouble((E) -> E.getSquaredDistance(mc.player.getEyePos()))).get());
            } else {
                for (Direction i : Direction.values()) {
                    if (i == Direction.UP || i == Direction.DOWN) continue;
                    if (Math.sqrt(mc.player.getEyePos().squaredDistanceTo(pos.offset(i).toCenterPos())) > range.get()) {
                        continue;
                    }
                    if (canBreak(pos.offset(i)) && canPlaceCrystal(pos.offset(i), false)) {
                        list.add(pos.offset(i));
                    }
                }
                if (!list.isEmpty()) {
                    PacketMine.INSTANCE.mine(list.stream().min(Comparator.comparingDouble((E) -> E.getSquaredDistance(mc.player.getEyePos()))).get());
                }
            }
        }
    }
    private boolean isSurroundPos(BlockPos pos) {
        for (Direction i : Direction.values()) {
            if (i == Direction.UP || i == Direction.DOWN) {
                continue;
            }
            BlockPos self = getPlayerPos(true);
            if (self.offset(i).equals(pos)) {
                return true;
            }
        }
        return false;
    }
    public BlockPos getPlayerPos(boolean fix) {
        return new BlockPosX(mc.player.getPos(), fix);
    }
    public Block getBlock(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock();
    }
    public boolean canPlaceCrystal(BlockPos pos, boolean block) {
        BlockPos obsPos = pos.down();
        BlockPos boost = obsPos.up();
        return (getBlock(obsPos) == Blocks.BEDROCK || getBlock(obsPos) == Blocks.OBSIDIAN || !block)
                && BlockUtil.noEntityBlockCrystal(boost, true, true)
                && BlockUtil.noEntityBlockCrystal(boost.up(), true, true)
                ;
    }
    public static final List<Block> hard = Arrays.asList(
            Blocks.OBSIDIAN, Blocks.ENDER_CHEST, Blocks.NETHERITE_BLOCK, Blocks.CRYING_OBSIDIAN, Blocks.RESPAWN_ANCHOR, Blocks.ANCIENT_DEBRIS, Blocks.ANVIL
    );

    private boolean isObsidian(BlockPos pos) {
        return mc.player.getEyePos().distanceTo(pos.toCenterPos()) <= PacketMine.INSTANCE.range.get() && hard.contains(mc.world.getBlockState(pos).getBlock()) && BlockUtil.getClickSideStrict(pos) != null;
    }

    private boolean canBreak(BlockPos pos) {
        return isObsidian(pos) && (BlockUtil.getClickSideStrict(pos) != null || (pos.equals(PacketMine.targetPos)));
    }
}

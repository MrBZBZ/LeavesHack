package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.combat.CombatUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class AutoMine extends Module {
    public static AutoMine INSTANCE;

    public AutoMine() {
        super(LeavesHack.LEAVES_COMBAT, "AutoMine", "自动挖掘 Burrow 和 Surround");
        INSTANCE = this;
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> targetRange = sgGeneral.add(new IntSetting.Builder()
            .name("TargetRange")
            .description("目标距离")
            .defaultValue(6)
            .min(0)
            .sliderMax(8)
            .build()
    );
    private final Setting<Boolean> doubleBreak = sgGeneral.add(new BoolSetting.Builder()
            .name("DoubleBreak")
            .description("双挖")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> ignoreTerrain = sgGeneral.add(new BoolSetting.Builder()
        .name("IgnoreTerrain")
        .description("忽略地形")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> burrow = sgGeneral.add(new BoolSetting.Builder()
            .name("Burrow")
            .description("挖掘目标脚下方块")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> surround = sgGeneral.add(new BoolSetting.Builder()
            .name("Surround")
            .description("挖掘目标水平包围方块")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("Delay")
            .description("两次选择挖掘位置的延迟")
            .defaultValue(300)
            .min(0)
            .sliderMax(1000)
            .build()
    );
    private final Setting<Integer> targetAirDelay = sgGeneral.add(new IntSetting.Builder()
            .name("TargetAirDelay")
            .description("空气检查候时")
            .defaultValue(300)
            .min(0)
            .sliderMax(1000)
            .build()
    );
    private final Setting<Double> minCrystalDamage = sgGeneral.add(new DoubleSetting.Builder()
            .name("MinCrystalDamage")
            .description("当前挖掘位置的最低水晶伤害")
            .defaultValue(4.0)
            .min(0)
            .sliderMax(36)
            .build()
    );

    private final Timer updateTimer = new Timer();
    private final Timer targetAirTimer = new Timer();
    private boolean waitingForEmpty;
    private BlockPos trackedTargetPos;

    @Override
    public void onActivate() {
        updateTimer.passedMs(999999);
        targetAirTimer.passedMs(999999);
        waitingForEmpty = false;
        trackedTargetPos = null;
    }

    @Override
    public void onDeactivate() {
        waitingForEmpty = false;
        trackedTargetPos = null;
    }

    @EventHandler
    public void onBreak(PacketEvent.Send event) {
        if (event.packet instanceof PlayerActionC2SPacket packet) {
            if (packet.getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK) updateTimer.reset();
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null || PacketMine.INSTANCE == null || !PacketMine.INSTANCE.isActive()) return;
        if (!updateTimer.passedMs(delay.get())) return;
        if (doubleBreak.get()) {
            if (waitingForEmpty) {
                if ((PacketMine.targetPos != null && !PacketMine.completed) || PacketMine.secondPos != null) return;
                waitingForEmpty = false;
                return;
            }
        }
        PlayerEntity enemy = CombatUtil.getClosestEnemy(targetRange.get());
        if (enemy == null) return;
        if (PacketMine.targetPos != null && !intersectPos(enemy, PacketMine.targetPos)) {
            if (!PacketMine.targetPos.equals(trackedTargetPos)) {
                trackedTargetPos = PacketMine.targetPos;
                targetAirTimer.reset();
            }
            if (PacketMine.completed) {
                if (ignoreTerrain.get()) {
                    CombatUtil.modifyPos = PacketMine.targetPos;
                    CombatUtil.modifyBlockState = Blocks.AIR.getDefaultState();
                }
                double crystalDamage = DamageUtils.crystalDamage(enemy, PacketMine.targetPos.toCenterPos(), false, PacketMine.targetPos.down());
//                info("伤害：" + crystalDamage);
                if (ignoreTerrain.get()) {
                    CombatUtil.modifyPos = null;
                }
                if (crystalDamage >= minCrystalDamage.get()) {
                    if (!mc.world.isAir(PacketMine.targetPos)) targetAirTimer.reset();
                    if (!mc.world.isAir(PacketMine.targetPos) || !targetAirTimer.passedMs(targetAirDelay.get() * 10)) return;
                }
            }
        } else {
            trackedTargetPos = null;
            targetAirTimer.reset();
        }

        if (!doubleBreak.get() && PacketMine.secondPos != null) return;

        List<BlockPos> positions = new ArrayList<>();
        BlockPos enemyPos = enemy.getBlockPos();
        if (burrow.get() && mc.player.getEyePos().distanceTo(enemyPos.toCenterPos()) <= PacketMine.INSTANCE.range.get()
                && (hard.contains(mc.world.getBlockState(enemyPos).getBlock()) || mc.world.getBlockState(enemyPos).getBlock() == Blocks.GLASS)
                && BlockUtil.getClickSideStrict(enemyPos) != null
                && !enemyPos.equals(PacketMine.targetPos) && !enemyPos.equals(PacketMine.secondPos)) {
            positions.add(enemyPos);
        }
        if (surround.get()) {
            for (Direction direction : Direction.Type.HORIZONTAL) {
                BlockPos position = enemyPos.offset(direction);
                if (positions.contains(position)
                        || mc.player.getEyePos().distanceTo(position.toCenterPos()) > PacketMine.INSTANCE.range.get()
                        || (!hard.contains(mc.world.getBlockState(position).getBlock()) && mc.world.getBlockState(position).getBlock() != Blocks.GLASS)
                        || BlockUtil.getClickSideStrict(position) == null
                        || position.equals(PacketMine.targetPos) || position.equals(PacketMine.secondPos)) {
                    continue;
                }
                positions.add(position);
            }
        }

        if (positions.isEmpty()) return;
        BlockPos position = positions.contains(enemyPos)
                ? enemyPos
                : positions.stream()
                .min(Comparator.comparingDouble(pos -> pos.getSquaredDistance(mc.player.getEyePos())))
                .orElse(null);
        PacketMine.INSTANCE.mine(position);
        updateTimer.reset();
        trackedTargetPos = PacketMine.targetPos;
        targetAirTimer.reset();
        if (doubleBreak.get() && PacketMine.targetPos != null && PacketMine.secondPos != null) waitingForEmpty = true;
    }
    public boolean intersectPos(PlayerEntity player, BlockPos pos) {
        return player.getBoundingBox().intersects(new Box(pos));
    }
    public static final List<Block> hard = Arrays.asList(
        Blocks.OBSIDIAN, Blocks.ENDER_CHEST, Blocks.NETHERITE_BLOCK, Blocks.CRYING_OBSIDIAN, Blocks.RESPAWN_ANCHOR, Blocks.ANCIENT_DEBRIS, Blocks.ANVIL
    );
}

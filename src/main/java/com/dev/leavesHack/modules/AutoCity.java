package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.combat.CombatUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.world.BlockPosX;
import com.dev.leavesHack.utils.world.BlockUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class AutoCity extends Module {
    public static AutoCity INSTANCE;
    public AutoCity() {
        super(LeavesHack.LEAVES_COMBAT, "AutoCity", "自动挖角");
        INSTANCE = this;
    }
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Integer> targetRange = sgGeneral.add(new IntSetting.Builder()
            .name("TargetRange")
            .description("目标距离")
            .defaultValue(6)
            .min(0)
            .sliderMax(8)
            .build()
    );
    public final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("Range")
            .description("操作距离")
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
    public final Setting<Boolean> delay = sgGeneral.add(new BoolSetting.Builder()
            .name("CityDelay")
            .description("挖角延迟")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> antiCrawl = sgGeneral.add(new BoolSetting.Builder()
            .name("AntiCrawl")
            .description("自动反趴下")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> preferSelfClick = sgGeneral.add(new BoolSetting.Builder()
            .name("PreferSelfClick")
            .description("优先处理手动点击的挖掘")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> head = sgGeneral.add(new BoolSetting.Builder()
            .name("Head")
            .description("挖头")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> burrow = sgGeneral.add(new BoolSetting.Builder()
            .name("Burrow")
            .description("挖黑曜石卡身")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> face = sgGeneral.add(new BoolSetting.Builder()
            .name("Face")
            .description("挖脸")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> down = sgGeneral.add(new BoolSetting.Builder()
            .name("Down")
            .description("挖脚底")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> surround = sgGeneral.add(new BoolSetting.Builder()
            .name("Surround")
            .description("挖包围")
            .defaultValue(true)
            .build()
    );
    public final Timer cityTimer = new Timer();
    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof ServerboundPlayerActionPacket packet) {
            if (packet.getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
                cityTimer.reset();
            }
        }
    }
    @EventHandler
    public void onTick(TickEvent.Pre event) {
        Player player = CombatUtil.getClosestEnemy(targetRange.get());
        if (preferSelfClick.get() && PacketMine.selfClickPos != null) return;
        if (delay.get() && !cityTimer.passedMs(PacketMine.INSTANCE.mineDelay.get())) return;
        if (antiCrawl.get() && mc.player.isVisuallyCrawling()) {
            if (canBreak(mc.player.blockPosition().above()) && !mc.player.blockPosition().above().equals(PacketMine.targetPos) && !mc.player.blockPosition().above().equals(PacketMine.secondPos)) {
                PacketMine.selfClickPos = mc.player.blockPosition().above();
                PacketMine.INSTANCE.mine(mc.player.blockPosition().above());
                return;
            }
        }
        if (player == null) return;
        doBreak(player);
    }

    private void doBreak(Player player) {
        BlockPos pos = player.blockPosition();
        double[] yOffset = new double[]{-0.8, 0.3, 2.3, 1.1};
        double[] xzOffset = new double[]{0.3, -0.3};
        if (!doubleBreak.get()) {
            for (Player entity : CombatUtil.getEnemies(targetRange.get())) {
                for (double y : yOffset) {
                    for (double x : xzOffset) {
                        for (double z : xzOffset) {
                            BlockPos offsetPos = new BlockPosX(entity.getX() + x, entity.getY() + y, entity.getZ() + z);
                            if (isObsidian(offsetPos) && BlockUtil.getClickSideStrict(offsetPos) != null && offsetPos.equals(PacketMine.targetPos)) {
                                return;
                            }
                        }
                    }
                }
            }
        } else {
            int count = 0;
            for (Player entity : CombatUtil.getEnemies(targetRange.get())) {
                for (double y : yOffset) {
                    for (double x : xzOffset) {
                        for (double z : xzOffset) {
                            BlockPos offsetPos = new BlockPosX(entity.getX() + x, entity.getY() + y, entity.getZ() + z);
                            if (isObsidian(offsetPos) && BlockUtil.getClickSideStrict(offsetPos) != null && (offsetPos.equals(PacketMine.targetPos) || offsetPos.equals(PacketMine.secondPos))) {
                                count++;
                            }
                        }
                    }
                }
            }
            if (count == 2) {
                return;
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
                    PacketMine.INSTANCE.mine(offsetPos);
                    return;
                }
            }
        }
        for (double y : yList) {
            for (double offset : xzOffset) {
                for (double offset2 : xzOffset) {
                    BlockPos offsetPos = new BlockPosX(player.getX() + offset2, player.getY() + y, player.getZ() + offset);
                    if (canBreak(offsetPos)) {
                        PacketMine.INSTANCE.mine(offsetPos);
                        return;
                    }
                }
            }
        }
        if (surround.get()) {
            for (Direction i : Direction.values()) {
                if (i == Direction.UP || i == Direction.DOWN) continue;
                if (Math.sqrt(mc.player.getEyePosition().distanceToSqr(pos.relative(i).getCenter())) > range.get()) {
                    continue;
                }
                if ((mc.level.isEmptyBlock(pos.relative(i)) || pos.relative(i).equals(PacketMine.targetPos)) && canPlaceCrystal(pos.relative(i), false)) {
                    if (!doubleBreak.get()) return;
                    if (PacketMine.targetPos != null && PacketMine.completed) return;
                }
            }
            ArrayList<BlockPos> list = new ArrayList<>();
            for (Direction i : Direction.values()) {
                if (i == Direction.UP || i == Direction.DOWN) continue;
                if (Math.sqrt(mc.player.getEyePosition().distanceToSqr(pos.relative(i).getCenter())) > range.get()) {
                    continue;
                }
                if (canBreak(pos.relative(i)) && canPlaceCrystal(pos.relative(i), true) && !isSurroundPos(pos.relative(i))) {
                    list.add(pos.relative(i));
                }
            }
            if (!list.isEmpty()) {
                PacketMine.INSTANCE.mine(list.stream().min(Comparator.comparingDouble((E) -> E.distToCenterSqr(mc.player.getEyePosition()))).get());
            } else {
                for (Direction i : Direction.values()) {
                    if (i == Direction.UP || i == Direction.DOWN) continue;
                    if (Math.sqrt(mc.player.getEyePosition().distanceToSqr(pos.relative(i).getCenter())) > range.get()) {
                        continue;
                    }
                    if (canBreak(pos.relative(i)) && canPlaceCrystal(pos.relative(i), false)) {
                        list.add(pos.relative(i));
                    }
                }
                if (!list.isEmpty()) {
                    PacketMine.INSTANCE.mine(list.stream().min(Comparator.comparingDouble((E) -> E.distToCenterSqr(mc.player.getEyePosition()))).get());
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
            if (self.relative(i).equals(pos)) {
                return true;
            }
        }
        return false;
    }
    public BlockPos getPlayerPos(boolean fix) {
        return new BlockPosX(mc.player.position(), fix);
    }
    public Block getBlock(BlockPos pos) {
        return mc.level.getBlockState(pos).getBlock();
    }
    public boolean canPlaceCrystal(BlockPos pos, boolean block) {
        BlockPos obsPos = pos.below();
        BlockPos boost = obsPos.above();
        return (getBlock(obsPos) == Blocks.BEDROCK || getBlock(obsPos) == Blocks.OBSIDIAN || !block)
                && BlockUtil.noEntityBlockCrystal(boost, true, true)
                && BlockUtil.noEntityBlockCrystal(boost.above(), true, true)
                ;
    }
    public static final List<Block> hard = Arrays.asList(
            Blocks.OBSIDIAN, Blocks.ENDER_CHEST, Blocks.NETHERITE_BLOCK, Blocks.CRYING_OBSIDIAN, Blocks.RESPAWN_ANCHOR, Blocks.ANCIENT_DEBRIS, Blocks.ANVIL
    );

    private boolean isObsidian(BlockPos pos) {
        return mc.player.getEyePosition().distanceTo(pos.getCenter()) <= PacketMine.INSTANCE.range.get() && (hard.contains(mc.level.getBlockState(pos).getBlock()) || BlockUtil.getBlock(pos) == Blocks.GLASS) && BlockUtil.getClickSideStrict(pos) != null;
    }

    private boolean canBreak(BlockPos pos) {
        return isObsidian(pos) && BlockUtil.getClickSideStrict(pos) != null && !pos.equals(PacketMine.targetPos) && !pos.equals(PacketMine.secondPos) && (((PacketMine.targetPos == null || PacketMine.secondPos == null) && doubleBreak.get()) || (PacketMine.targetPos == null));
    }
}

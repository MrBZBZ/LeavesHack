package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.math.Timer;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import baritone.api.BaritoneAPI;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.math.Direction;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AntiAntiXray extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgRender = this.settings.createGroup("Render");
    public CopyOnWriteArrayList<BlockPos> breakList = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<BlockPos> ironList = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<BlockPos> goldList = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<BlockPos> diamondList = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<BlockPos> lapisList = new CopyOnWriteArrayList<>();
    int blockCounts = 0;
    int progress = 0;
    private boolean render = false;
    private Timer timer = new Timer();
    private ExecutorService executor = null;
    private BlockPos playerPos = null;
    private int leftIndex;
    private int rightIndex;
    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
        .name("Range")
        .description("range")
        .defaultValue(15)
        .min(0)
        .sliderMax(50)
        .build()
    );
    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("")
            .defaultValue(0)
            .min(0)
            .sliderMax(100)
            .build()
    );
    private final Setting<Integer> step = sgGeneral.add(new IntSetting.Builder()
        .name("Step")
        .description("step")
        .defaultValue(2)
        .min(1)
        .sliderMax(2)
        .build()
    );
    private final Setting<Boolean> baritone = sgGeneral.add(new BoolSetting.Builder()
        .name("baritone")
        .description("Set baritone ore positions to the simulated ones.")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> autoMine = sgGeneral.add(new BoolSetting.Builder()
        .name("autoMine")
        .description("")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> iron = sgRender.add(new BoolSetting.Builder()
        .name("Iron")
        .description("")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> gold = sgRender.add(new BoolSetting.Builder()
        .name("Gold")
        .description("")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> diamond = sgRender.add(new BoolSetting.Builder()
        .name("Diamond")
        .description("")
        .defaultValue(true)
        .build()
    );
    private final Setting<Boolean> lapis = sgRender.add(new BoolSetting.Builder()
        .name("Lapis")
        .description("")
        .defaultValue(true)
        .build()
    );
    public AntiAntiXray() {
        super(LeavesHack.CATEGORY, "AntiAntiXray", "dev Leaves_awa");
    }
    @Override
    public void onActivate() {
        executor = Executors.newSingleThreadExecutor();
        playerPos = mc.player.getBlockPos();
        timer.setMs(999999);
        render = false;
        breakList.clear();
        ironList.clear();
        goldList.clear();
        diamondList.clear();
        lapisList.clear();
        blockCounts = 0;
        progress = 0;
        resetCounter();
    }
    @Override
    public void onDeactivate() {
        executor.shutdown();
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
    }
    public boolean baritone() {
        return isActive() && baritone.get() && BaritoneUtils.IS_AVAILABLE;
    }
    @EventHandler
    private void onRender3d(Render3DEvent event) {
        if (render && breakList.isEmpty()) {
            mc.player.sendMessage(Text.of("[LeavesHack] " + "已结束"));
            if (autoMine.get()) BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
            toggle();
        }
        if (!executor.isShutdown()) {
            executor.submit(() -> {
                traverseCube(range.get());
                traverseCubeCurrent();
            });
        }
        renderBlock(event);
        if (!timer.passedMs(delay.get())) return;
        int r = range.get();
        int size = r * 2 + 1;
        if (progress * step.get() < 100) {
            long totalBlocks = (long)size * size * size;
            double percentage = ((double)blockCounts / totalBlocks) * 100.0;
            progress = (int) Math.round(percentage);
            mc.player.sendMessage(Text.of("[LeavesHack] " + "进度：" + progress*step.get() + "%"));
        }
        if (progress * step.get() == 100 && !render) {
            mc.player.sendMessage(Text.of("[LeavesHack] " + "已完成"));
            if (autoMine.get()) BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("mine minecraft:iron_ore");
            render = true;
        }
        if (leftIndex > rightIndex) {
            return;
        }
        // ===== 左端 =====
        visitIndex(event, playerPos, leftIndex, size, r);
        blockCounts++;
        leftIndex = leftIndex + step.get().intValue();
        // ===== 右端（避免奇数时重复）=====
        if (leftIndex <= rightIndex) {
            visitIndex(event, playerPos, rightIndex, size, r);
            blockCounts++;
            rightIndex = rightIndex - step.get().intValue();
        }
    }
    private void traverseCube(int range) {
        if (playerPos == null) return;
        int r = range;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    checkBlock(pos);
                }
            }
        }
    }
    private void traverseCubeCurrent() {
        BlockPos playerPos = mc.player.getBlockPos();
        int r = 2;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    checkBlock(pos);
                }
            }
        }
    }
    private void checkBlock(BlockPos lastPos) {
        if ((mc.world.getBlockState(lastPos).getBlock() == Blocks.IRON_ORE || mc.world.getBlockState(lastPos).getBlock() == Blocks.DEEPSLATE_IRON_ORE) && !ironList.contains(lastPos)) {
            ironList.add(lastPos);
            if (iron.get() && !breakList.contains(lastPos)) {
                breakList.add(lastPos);
            } else if (!iron.get()){
                breakList.remove(lastPos);
            }
        }
        if ((mc.world.getBlockState(lastPos).getBlock() == Blocks.GOLD_ORE || mc.world.getBlockState(lastPos).getBlock() == Blocks.DEEPSLATE_GOLD_ORE) && !goldList.contains(lastPos)) {
            goldList.add(lastPos);
            if (gold.get() && !breakList.contains(lastPos)) {
                breakList.add(lastPos);
            } else if (!gold.get()){
                breakList.remove(lastPos);
            }
        }
        if ((mc.world.getBlockState(lastPos).getBlock() == Blocks.DIAMOND_ORE || mc.world.getBlockState(lastPos).getBlock() == Blocks.DEEPSLATE_DIAMOND_ORE && !diamondList.contains(lastPos))) {
            diamondList.add(lastPos);
            if (diamond.get() && !breakList.contains(lastPos)) {
                breakList.add(lastPos);
            } else if (!diamond.get()){
                breakList.remove(lastPos);
            }
        }
        if ((mc.world.getBlockState(lastPos).getBlock() == Blocks.LAPIS_ORE || mc.world.getBlockState(lastPos).getBlock() == Blocks.DEEPSLATE_LAPIS_ORE) && !lapisList.contains(lastPos)) {
            lapisList.add(lastPos);
            if (lapis.get() && !breakList.contains(lastPos)) {
                breakList.add(lastPos);
            } else if (!lapis.get()){
                breakList.remove(lastPos);
            }
        }
    }
    private void visitIndex(Render3DEvent event, BlockPos playerPos, int index, int size, int r) {
        if (playerPos == null) return;
        int x = index % size;
        int z = (index / size) % size;
        int y = index / (size * size);
        BlockPos pos = playerPos.add(x - r, y - r, z - r);
        Color color = new Color(50, 232, 252, 80);
        event.renderer.box(pos,color,color,ShapeMode.Both,0);
        mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
            pos,
            Direction.UP
        ));
    }

    private void resetCounter() {
        int size = range.get().intValue() * 2 + 1;
        leftIndex = 0;
        rightIndex = size * size * size - 1;
    }
    private void renderBlock(Render3DEvent event) {
        if (iron.get() && !ironList.isEmpty()){
            for (BlockPos pos : ironList) {
                if (mc.world.isAir(pos)) {
                    ironList.remove(pos);
                    breakList.remove(pos);
                    return;
                }
                event.renderer.box(pos,new Color(227, 121, 8, 61),new Color(227, 121, 8, 61),ShapeMode.Both,0);
            }
        }
        if (!goldList.isEmpty()){
            for (BlockPos pos : goldList) {
                if (mc.world.isAir(pos)) {
                    goldList.remove(pos);
                    breakList.remove(pos);
                    return;
                }
                event.renderer.box(pos,new Color(255, 215, 0, 61),new Color(255, 215, 0, 61),ShapeMode.Both,0);
            }
        }
        if (diamond.get() && !diamondList.isEmpty()){
            for (BlockPos pos : diamondList) {
                if (mc.world.isAir(pos)) {
                    diamondList.remove(pos);
                    breakList.remove(pos);
                    return;
                }
                event.renderer.box(pos,new Color(0, 255, 255, 61),new Color(0, 255, 255, 61),ShapeMode.Both,0);
            }
        }
        if (lapis.get() && !lapisList.isEmpty()){
            for (BlockPos pos : lapisList) {
                if (mc.world.isAir(pos)) {
                    lapisList.remove(pos);
                    breakList.remove(pos);
                    return;
                }
                event.renderer.box(pos,new Color(0, 0, 255, 61),new Color(0, 0, 255, 61),ShapeMode.Both,0);
            }
        }
    }
}

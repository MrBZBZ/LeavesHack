package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.dev.leavesHack.utils.world.BlockUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import meteordevelopment.meteorclient.events.entity.player.BlockBreakingCooldownEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class NukerPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Shape> shape = sgGeneral.add(new EnumSetting.Builder<Shape>()
            .name("shape")
            .description("渲染模式")
            .defaultValue(Shape.Sphere)
            .build()
    );

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("破坏模式")
            .defaultValue(Mode.Flatten)
            .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("range")
            .description("破坏距离")
            .defaultValue(4)
            .min(0)
            .visible(() -> shape.get() != Shape.Cube)
            .build()
    );

    private final Setting<Integer> range_up = sgGeneral.add(new IntSetting.Builder()
            .name("up")
            .description("破坏距离（上方）")
            .defaultValue(1)
            .min(0)
            .visible(() -> shape.get() == Shape.Cube)
            .build()
    );

    private final Setting<Integer> range_down = sgGeneral.add(new IntSetting.Builder()
            .name("down")
            .description("破坏距离（下方）")
            .defaultValue(1)
            .min(0)
            .visible(() -> shape.get() == Shape.Cube)
            .build()
    );

    private final Setting<Integer> range_left = sgGeneral.add(new IntSetting.Builder()
            .name("left")
            .description("破坏距离（左方）")
            .defaultValue(1)
            .min(0)
            .visible(() -> shape.get() == Shape.Cube)
            .build()
    );

    private final Setting<Integer> range_right = sgGeneral.add(new IntSetting.Builder()
            .name("right")
            .description("破坏距离（右方）")
            .defaultValue(1)
            .min(0)
            .visible(() -> shape.get() == Shape.Cube)
            .build()
    );

    private final Setting<Integer> range_forward = sgGeneral.add(new IntSetting.Builder()
            .name("forward")
            .description("破坏距离（前方）")
            .defaultValue(1)
            .min(0)
            .visible(() -> shape.get() == Shape.Cube)
            .build()
    );

    private final Setting<Integer> range_back = sgGeneral.add(new IntSetting.Builder()
            .name("back")
            .description("破坏距离（后方）")
            .defaultValue(1)
            .min(0)
            .visible(() -> shape.get() == Shape.Cube)
            .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("破坏延迟")
            .defaultValue(0)
            .max(100)
            .build()
    );

    private final Setting<Integer> clickDelay = sgGeneral.add(new IntSetting.Builder()
            .name("ClickDelay")
            .description("点击延迟")
            .defaultValue(8)
            .max(100)
            .build()
    );

    private final Setting<Integer> maxBlocksPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("max-blocks-per-tick")
            .description("每刻最大破坏方块数量")
            .defaultValue(1)
            .min(1)
            .sliderRange(1, 6)
            .build()
    );

    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>()
            .name("sort-mode")
            .description("排序模式")
            .defaultValue(SortMode.Closest)
            .build()
    );

    private final Setting<Boolean> swingHand = sgGeneral.add(new BoolSetting.Builder()
            .name("swing-hand")
            .description("挥手")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> packetMine = sgGeneral.add(new BoolSetting.Builder()
            .name("packet-mine")
            .description("开启包挖")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("rotate")
            .description("转头")
            .defaultValue(true)
            .build()
    );

    private final Setting<ListMode> listMode = sgWhitelist.add(new EnumSetting.Builder<ListMode>()
            .name("list-mode")
            .description("选择模式")
            .defaultValue(ListMode.Blacklist)
            .build()
    );

    private final Setting<List<Block>> blacklist = sgWhitelist.add(new BlockListSetting.Builder()
            .name("blacklist")
            .description("黑名单")
            .visible(() -> listMode.get() == ListMode.Blacklist)
            .build()
    );

    private final Setting<List<Block>> whitelist = sgWhitelist.add(new BlockListSetting.Builder()
            .name("whitelist")
            .description("白名单")
            .visible(() -> listMode.get() == ListMode.Whitelist)
            .build()
    );

    private final Setting<Boolean> enableRenderBounding = sgRender.add(new BoolSetting.Builder()
            .name("bounding-box")
            .description("渲染box")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeModeBox = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("nuke-box-mode")
            .description("渲染模式")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColorBox = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("边界盒填充颜色")
            .defaultValue(new SettingColor(255, 255, 255, 0))
            .build()
    );

    private final Setting<SettingColor> lineColorBox = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("边界盒边框颜色")
            .defaultValue(new SettingColor(255, 255, 255, 0))
            .build()
    );

    private final Setting<Boolean> enableRenderBreaking = sgRender.add(new BoolSetting.Builder()
            .name("broken-blocks")
            .description("启用破坏方块渲染")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeModeBreak = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("nuke-block-mode")
            .description("破坏方块渲染模式")
            .defaultValue(ShapeMode.Both)
            .visible(enableRenderBreaking::get)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("方块内部颜色")
            .defaultValue(new SettingColor(255, 255, 255, 80))
            .visible(enableRenderBreaking::get)
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("方块线条颜色")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .visible(enableRenderBreaking::get)
            .build()
    );

    private final List<BlockPos> blocks = new ArrayList<>();

    private boolean firstBlock;
    private final BlockPos.MutableBlockPos lastBlockPos = new BlockPos.MutableBlockPos();

    private int timer;
    private int noBlockTimer;
    private Timer mineTimer = new Timer();

    private final BlockPos.MutableBlockPos pos1 = new BlockPos.MutableBlockPos();
    private final BlockPos.MutableBlockPos pos2 = new BlockPos.MutableBlockPos();
    int maxh = 0;
    int maxv = 0;

    public NukerPlus() {
        super(LeavesHack.LEAVES_MISC, "Nuker+", "范围挖掘");
    }

    @Override
    public void onActivate() {
        mineTimer.setMs(99999);
        firstBlock = true;
        timer = 0;
        noBlockTimer = 0;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (enableRenderBounding.get()) {
            if (shape.get() != Shape.Sphere && mode.get() != Mode.Smash) {
                int minX = Math.min(pos1.getX(), pos2.getX());
                int minY = Math.min(pos1.getY(), pos2.getY());
                int minZ = Math.min(pos1.getZ(), pos2.getZ());
                int maxX = Math.max(pos1.getX(), pos2.getX());
                int maxY = Math.max(pos1.getY(), pos2.getY());
                int maxZ = Math.max(pos1.getZ(), pos2.getZ());
                event.renderer.box(minX, minY, minZ, maxX, maxY, maxZ, sideColorBox.get(), lineColorBox.get(), shapeModeBox.get(), 0);
            }
        }
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        if (timer > 0) {
            timer--;
            return;
        }

        double pX = mc.player.getX();
        double pY = mc.player.getY();
        double pZ = mc.player.getZ();

        double rangeSq = Math.pow(range.get(), 2);

        if (shape.get() == Shape.UniformCube) range.set((double) Math.round(range.get()));

        double pX_ = pX;
        double pZ_ = pZ;
        int r = (int) Math.round(range.get());

        if (shape.get() == Shape.UniformCube) {
            pX_ += 1;
            pos1.set(pX_ - r, pY - r + 1, pZ - r + 1);
            pos2.set(pX_ + r - 1, pY + r, pZ + r);
        } else {
            int direction = Math.round((mc.player.getRotationVector().y % 360) / 90);
            direction = Math.floorMod(direction, 4);

            pos1.set(pX_ - range_forward.get(), Math.ceil(pY) - range_down.get(), pZ_ - range_right.get());
            pos2.set(pX_ + range_back.get() + 1, Math.ceil(pY + range_up.get() + 1), pZ_ + range_left.get() + 1);

            switch (direction) {
                case 0 -> {
                    pZ_ += 1;
                    pX_ += 1;
                    pos1.set(pX_ - (range_right.get() + 1), Math.ceil(pY) - range_down.get(), pZ_ - (range_back.get() + 1));
                    pos2.set(pX_ + range_left.get(), Math.ceil(pY + range_up.get() + 1), pZ_ + range_forward.get());
                }
                case 2 -> {
                    pX_ += 1;
                    pZ_ += 1;
                    pos1.set(pX_ - (range_left.get() + 1), Math.ceil(pY) - range_down.get(), pZ_ - (range_forward.get() + 1));
                    pos2.set(pX_ + range_right.get(), Math.ceil(pY + range_up.get() + 1), pZ_ + range_back.get());
                }
                case 3 -> {
                    pX_ += 1;
                    pos1.set(pX_ - (range_back.get() + 1), Math.ceil(pY) - range_down.get(), pZ_ - range_left.get());
                    pos2.set(pX_ + range_forward.get(), Math.ceil(pY + range_up.get() + 1), pZ_ + range_right.get() + 1);
                }
            }

            maxh = 1 + Math.max(Math.max(Math.max(range_back.get(), range_right.get()), range_forward.get()), range_left.get());
            maxv = 1 + Math.max(range_up.get(), range_down.get());
        }

        if (mode.get() == Mode.Flatten) {
            pos1.setY((int) Math.floor(pY));
        }
        AABB box = new AABB(pos1.getCenter(), pos2.getCenter());

        BlockIterator.register(Math.max((int) Math.ceil(range.get() + 1), maxh), Math.max((int) Math.ceil(range.get()), maxv), (blockPos, blockState) -> {
            switch (shape.get()) {
                case Sphere -> {
                    if (Utils.squaredDistance(pX, pY, pZ, blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5) > rangeSq) return;
                }
                case UniformCube -> {
                    if (chebyshevDist(mc.player.blockPosition().getX(), mc.player.blockPosition().getY(), mc.player.blockPosition().getZ(), blockPos.getX(), blockPos.getY(), blockPos.getZ()) >= range.get()) return;
                }
                case Cube -> {
                    if (!box.contains(Vec3.atCenterOf(blockPos))) return;
                }
            }

            if (!BlockUtils.canBreak(blockPos, blockState)) return;

            if (mode.get() == Mode.Flatten && blockPos.getY() < Math.floor(mc.player.getY())) return;

            if (mode.get() == Mode.Smash && blockState.getDestroySpeed(mc.level, blockPos) != 0) return;

            if (listMode.get() == ListMode.Whitelist && !whitelist.get().contains(blockState.getBlock())) return;
            if (listMode.get() == ListMode.Blacklist && blacklist.get().contains(blockState.getBlock())) return;

            blocks.add(blockPos.immutable());
        });

        BlockIterator.after(() -> {
            if (sortMode.get() == SortMode.TopDown)
                blocks.sort(Comparator.comparingDouble(value -> -value.getY()));
            else if (sortMode.get() != SortMode.None)
                blocks.sort(Comparator.comparingDouble(value -> Utils.squaredDistance(pX, pY, pZ, value.getX() + 0.5, value.getY() + 0.5, value.getZ() + 0.5) * (sortMode.get() == SortMode.Closest ? 1 : -1)));

            if (blocks.isEmpty()) {
                if (noBlockTimer++ >= delay.get()) firstBlock = true;
                return;
            }
            else {
                noBlockTimer = 0;
            }

            if (!firstBlock && !lastBlockPos.equals(blocks.getFirst())) {
                timer = delay.get();

                firstBlock = false;
                lastBlockPos.set(blocks.getFirst());

                if (timer > 0) return;
            }

            int count = 0;

            for (BlockPos block : blocks) {
                if (count >= maxBlocksPerTick.get()) break;

                boolean canInstaMine = BlockUtils.canInstaBreak(block);

                if (rotate.get()) {
                    float[] rot = Rotation.getRotation(mc.player.getEyePosition(), Vec3.atLowerCornerOf(block));
                    mc.player.setYRot(rot[0]);
                    mc.player.setXRot(rot[1]);
                }
                breakBlock(block);

                if (enableRenderBreaking.get()) RenderUtils.renderTickingBlock(block, sideColor.get(), lineColor.get(), shapeModeBreak.get(), 0, 8, true, false);
                lastBlockPos.set(block);

                count++;
                if (!canInstaMine && !packetMine.get()) break;
            }

            firstBlock = false;
            blocks.clear();
        });
    }

    private void breakBlock(BlockPos pos) {
        if (packetMine.get()) {
            if (!mineTimer.passedMs(clickDelay.get() * 50)) return;
            mineTimer.reset();
            PacketMine packetMine = Modules.get().get(PacketMine.class);
            if (packetMine != null && !packetMine.isActive()) {
                ChatUtils.forceNextPrefixClass(getClass());
                ChatUtils.sendMsg(title, Component.nullToEmpty("You must toggle PacketMine"));
                toggle();
            }
            mc.player.swing(InteractionHand.MAIN_HAND);
            mc.gameMode.startDestroyBlock(pos, BlockUtil.getClickSide(pos));
            PacketMine.INSTANCE.mine(pos);
        } else {
            BlockUtils.breakBlock(pos, swingHand.get());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onBlockBreakingCooldown(BlockBreakingCooldownEvent event) {
        event.cooldown = 0;
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }

    public enum Mode {
        All,
        Flatten,
        Smash
    }

    public enum SortMode {
        None,
        Closest,
        Furthest,
        TopDown
    }

    public enum Shape {
        Cube,
        UniformCube,
        Sphere
    }

    public static int chebyshevDist(int x1, int y1, int z1, int x2, int y2, int z2) {
        int dX = Math.abs(x2 - x1);
        int dY = Math.abs(y2 - y1);
        int dZ = Math.abs(z2 - z1);
        return Math.max(Math.max(dX, dY), dZ);
    }
}

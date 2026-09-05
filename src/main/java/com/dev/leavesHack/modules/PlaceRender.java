package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.events.PlaceBlockEvent;
import com.dev.leavesHack.utils.world.BlockUtil;
import java.util.HashMap;
import java.util.Map;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

public class PlaceRender extends Module {
    public PlaceRender() {
        super(LeavesHack.LEAVES_MISC, "PlaceRender", "放置渲染");
    }
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<ShapeMode> shapeMode = sgRender.add(
            new EnumSetting.Builder<ShapeMode>()
                    .name("ShapeMode")
                    .description("渲染模式")
                    .defaultValue(ShapeMode.Both)
                    .build()
    );
    private final Setting<Integer> speed = sgRender.add(
            new IntSetting.Builder()
                    .name("Speed")
                    .description("渲染速度")
                    .defaultValue(4)
                    .sliderRange(1, 100)
                    .build()
    );
    private final Setting<Boolean> renderClientPlace = sgRender.add(
        new BoolSetting.Builder()
            .name("RenderClientPlace")
            .description("客户端放置渲染")
            .defaultValue(true)
            .build()
    );
    private final Setting<Double> animationExp = sgRender.add(
            new DoubleSetting.Builder()
                    .name("Animation Exponent")
                    .description("动画指数")
                    .defaultValue(3)
                    .range(0, 10)
                    .sliderRange(0, 10)
                    .build()
    );
    private final Setting<SettingColor> sideStartColor = sgRender.add(
            new ColorSetting.Builder()
                    .name("SideStart")
                    .description("方块填充开始颜色")
                    .defaultValue(new SettingColor(255, 255, 255, 0))
                    .build()
    );

    private final Setting<SettingColor> sideEndColor = sgRender.add(
            new ColorSetting.Builder()
                    .name("SideEnd")
                    .description("方块填充结束颜色")
                    .defaultValue(new SettingColor(255, 255, 255, 50))
                    .build()
    );

    private final Setting<SettingColor> lineStartColor = sgRender.add(
            new ColorSetting.Builder()
                    .name("LineStart")
                    .description("方块边框开始颜色")
                    .defaultValue(new SettingColor(255, 255, 255, 0))
                    .build()
    );

    private final Setting<SettingColor> lineEndColor = sgRender.add(
            new ColorSetting.Builder()
                    .name("LineEnd")
                    .description("方块边框结束颜色")
                    .defaultValue(new SettingColor(255, 255, 255, 255))
                    .build()
    );
    private final Map<BlockPos, PosEntry> posEntries = new HashMap<>();
    @Override
    public void onActivate() {
        BlockUtil.placeList.clear();
    }
    @EventHandler
    public void onBlockPlace(PlaceBlockEvent event) {
        if (event.blockPos != null && renderClientPlace.get()) {
            BlockUtil.placeList.add(event.blockPos);
        }
    }
    @EventHandler
    public void onRender(Render3DEvent event) {
        if (!BlockUtil.placeList.isEmpty()) {
            for (BlockPos pos : BlockUtil.placeList) {
                if (posEntries.containsKey(pos)) continue;
                posEntries.put(pos, new PosEntry(pos));
            }
            for (BlockPos pos : BlockUtil.placeList) {
                PosEntry entry = posEntries.get(pos);
                if (entry == null || entry.progress <= 0) {
                    posEntries.remove(pos);
                    BlockUtil.placeList.remove(pos);
                    continue;
                }
                double p = 1 - Mth.clamp(entry.progress, 0, 1);
                p = Math.pow(p, animationExp.get());
                p = 1 - p;
                double size = p / 2;
                AABB box = new AABB(
                        pos.getX() + 0.5 - size,
                        pos.getY() + 0.5 - size,
                        pos.getZ() + 0.5 - size,
                        pos.getX() + 0.5 + size,
                        pos.getY() + 0.5 + size,
                        pos.getZ() + 0.5 + size
                );

                Color side = getColor(sideStartColor.get(), sideEndColor.get(), p);
                Color line = getColor(lineStartColor.get(), lineEndColor.get(), p);

                event.renderer.box(box, side, line, shapeMode.get(), 0);
                entry.progress -= speed.get() * 0.01;
            }
        }
    }
    private Color getColor(Color start, Color end, double progress) {
        return new Color(
                lerp(start.r, end.r, progress),
                lerp(start.g, end.g, progress),
                lerp(start.b, end.b, progress),
                lerp(start.a, end.a, progress)
        );
    }
    private int lerp(double start, double end, double d) {
        return (int) Math.round(start + (end - start) * d);
    }
    private static class PosEntry {
        final BlockPos pos;
        double progress = 1;
        PosEntry(BlockPos pos) {
            this.pos = pos;
        }
    }
}

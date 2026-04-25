package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;

public class PlaceRender extends Module {
    public PlaceRender() {
        super(LeavesHack.CATEGORY, "PlaceRender", "Renders a block where you place.");
    }
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<ShapeMode> shapeMode = sgRender.add(
            new EnumSetting.Builder<ShapeMode>()
                    .name("ShapeMode")
                    .defaultValue(ShapeMode.Both)
                    .build()
    );
    private final Setting<Integer> speed = sgRender.add(
            new IntSetting.Builder()
                    .name("Speed")
                    .defaultValue(10)
                    .sliderRange(1, 100)
                    .build()
    );
    private final Setting<Double> animationExp = sgRender.add(
            new DoubleSetting.Builder()
                    .name("Animation Exponent")
                    .defaultValue(3)
                    .range(0, 10)
                    .sliderRange(0, 10)
                    .build()
    );
    private final Setting<SettingColor> sideStartColor = sgRender.add(
            new ColorSetting.Builder()
                    .name("SideStart")
                    .defaultValue(new SettingColor(255, 255, 255, 0))
                    .build()
    );

    private final Setting<SettingColor> sideEndColor = sgRender.add(
            new ColorSetting.Builder()
                    .name("SideEnd")
                    .defaultValue(new SettingColor(255, 255, 255, 50))
                    .build()
    );

    private final Setting<SettingColor> lineStartColor = sgRender.add(
            new ColorSetting.Builder()
                    .name("LineStart")
                    .defaultValue(new SettingColor(255, 255, 255, 0))
                    .build()
    );

    private final Setting<SettingColor> lineEndColor = sgRender.add(
            new ColorSetting.Builder()
                    .name("LineEnd")
                    .defaultValue(new SettingColor(255, 255, 255, 255))
                    .build()
    );
    private final Map<BlockPos, PosEntry> posEntries = new HashMap<>();
    @Override
    public void onActivate() {
        BlockUtil.placeList.clear();
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
                double p = 1 - MathHelper.clamp(entry.progress, 0, 1);
                p = Math.pow(p, animationExp.get());
                p = 1 - p;
                double size = p / 2;
                Box box = new Box(
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

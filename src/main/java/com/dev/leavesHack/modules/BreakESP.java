package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.manager.BreakManager;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3d;

import java.text.DecimalFormat;

public class BreakESP extends Module {
    public static BreakESP INSTANCE;

    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> progress = sgRender.add(new BoolSetting.Builder()
        .name("Progress")
        .description("显示挖掘进度")
        .defaultValue(true)
        .build()
    );
    private final Setting<Double> damage = sgRender.add(new DoubleSetting.Builder()
        .name("Damage")
        .description("进度倍率")
        .defaultValue(1.0)
        .min(0.0)
        .sliderMax(2.0)
        .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("ShapeMode")
        .description("渲染模式")
        .defaultValue(ShapeMode.Both)
        .build()
    );
    private final Setting<SettingColor> box = sgRender.add(new ColorSetting.Builder()
        .name("Box")
        .description("主挖边框颜色")
        .defaultValue(new SettingColor(140, 142, 255, 255))
        .build()
    );
    private final Setting<SettingColor> fill = sgRender.add(new ColorSetting.Builder()
        .name("Fill")
        .description("主挖填充颜色")
        .defaultValue(new SettingColor(140, 142, 255, 50))
        .build()
    );
    private final Setting<SettingColor> boxFriend = sgRender.add(new ColorSetting.Builder()
        .name("FriendBox")
        .description("好友主挖边框颜色")
        .defaultValue(new SettingColor(140, 142, 255, 255))
        .build()
    );
    private final Setting<SettingColor> fillFriend = sgRender.add(new ColorSetting.Builder()
        .name("FriendFill")
        .description("好友主挖填充颜色")
        .defaultValue(new SettingColor(140, 142, 255, 50))
        .build()
    );
    private final Setting<Boolean> second = sgRender.add(new BoolSetting.Builder()
        .name("Second")
        .description("显示副挖")
        .defaultValue(true)
        .build()
    );
    private final Setting<SettingColor> secondBox = sgRender.add(new ColorSetting.Builder()
        .name("SecondBox")
        .description("副挖边框颜色")
        .defaultValue(new SettingColor(140, 142, 255, 255))
        .build()
    );
    private final Setting<SettingColor> secondFill = sgRender.add(new ColorSetting.Builder()
        .name("SecondFill")
        .description("副挖填充颜色")
        .defaultValue(new SettingColor(140, 142, 255, 50))
        .build()
    );
    private final Setting<SettingColor> nameColor = sgRender.add(new ColorSetting.Builder()
        .name("NameColor")
        .description("玩家名颜色")
        .defaultValue(new SettingColor(140, 142, 255, 255))
        .build()
    );
    private final Setting<SettingColor> progressColor = sgRender.add(new ColorSetting.Builder()
        .name("ProgressColor")
        .description("主挖进度颜色")
        .defaultValue(new SettingColor(140, 142, 255, 255))
        .build()
    );
    private final Setting<SettingColor> secondNameColor = sgRender.add(new ColorSetting.Builder()
        .name("SecondNameColor")
        .description("副挖玩家名颜色")
        .defaultValue(new SettingColor(140, 142, 255, 255))
        .build()
    );
    private final Setting<SettingColor> secondProgressColor = sgRender.add(new ColorSetting.Builder()
        .name("SecondProgressColor")
        .description("副挖文本颜色")
        .defaultValue(new SettingColor(140, 142, 255, 255))
        .build()
    );
    private final Setting<Double> textScale = sgRender.add(new DoubleSetting.Builder()
        .name("TextScale")
        .description("文本缩放")
        .defaultValue(1.0)
        .min(0.5)
        .sliderMax(3.0)
        .build()
    );

    private final DecimalFormat df = new DecimalFormat("0.0");

    public BreakESP() {
        super(LeavesHack.LEAVES_MISC, "BreakESP", "挖掘显示");
        INSTANCE = this;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;

        for (BreakManager.BreakData data : BreakManager.INSTANCE.breakMap.values()) {
            if (data == null || data.getEntity() == null) continue;
            PlayerEntity player = (PlayerEntity) data.getEntity();

            double p = getProgress(data);
            renderBox(event, data.pos, p,
                Friends.get().isFriend(player) ? fillFriend.get() : fill.get(),
                Friends.get().isFriend(player) ? boxFriend.get() : box.get());
        }

        if (!second.get()) return;

        for (BreakManager.BreakData data : BreakManager.INSTANCE.doubleMap.values()) {
            if (data == null || data.getEntity() == null || mc.world.isAir(data.pos)) continue;

            BreakManager.BreakData single = BreakManager.INSTANCE.breakMap.get(data.getEntity().getId());
            if (single != null && single.pos.equals(data.pos)) continue;

            renderBox(event, data.pos, getProgress(data), secondFill.get(), secondBox.get());
        }
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.world == null || mc.player == null) return;

        for (BreakManager.BreakData data : BreakManager.INSTANCE.breakMap.values()) {
            if (data == null || data.getEntity() == null) continue;

            PlayerEntity player = (PlayerEntity) data.getEntity();
            renderName(player.getName().getString(), data.pos, 0.15, nameColor.get());
            if (progress.get()) {
                String text = data.failed
                    ? "Failed"
                    : (data.complete
                        ? "Broke"
                        : df.format(Math.min(1, getProgress(data)) * 100));
                renderProgress(text, data.pos, progressColor.get());
            }
        }

        if (!second.get()) return;

        for (BreakManager.BreakData data : BreakManager.INSTANCE.doubleMap.values()) {
            if (data == null || data.getEntity() == null || mc.world.isAir(data.pos)) continue;

            BreakManager.BreakData single = BreakManager.INSTANCE.breakMap.get(data.getEntity().getId());
            if (single != null && single.pos.equals(data.pos)) continue;

            renderName(data.getEntity().getName().getString(), data.pos, 0.15, secondNameColor.get());
            renderProgress("Double", data.pos, secondProgressColor.get());
        }
    }

    private double getProgress(BreakManager.BreakData data) {
        double time = Math.max(data.breakTime * damage.get(), 50);
        if (data.failed) return 1;
        if (data.complete) return 1;
        return MathHelper.clamp(data.timer.getPassedTimeMs() / time, 0, 1);
    }

    private void renderBox(Render3DEvent event, BlockPos pos, double progress, SettingColor side, SettingColor line) {
        Box box = new Box(pos);
        event.renderer.box(box, side, line, shapeMode.get(), 0);
    }

    private void renderName(String name, BlockPos pos, double yOffset, Color color) {
        TextRenderer textRenderer = TextRenderer.get();
        Vector3d renderPos = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5 + yOffset, pos.getZ() + 0.5);
        double scale = NametagUtils.scale * textScale.get();
        if (NametagUtils.to2D(renderPos, 1.5, true)) {
            NametagUtils.begin(renderPos);
            textRenderer.begin(scale, false, true);
            double width = textRenderer.getWidth(name, true);
            double height = textRenderer.getHeight(true);
            textRenderer.render(name, -width / 2, -height / 2, color, true);
            textRenderer.end();
            NametagUtils.end();
        }
    }

    private void renderProgress(String text, BlockPos pos, Color color) {
        TextRenderer textRenderer = TextRenderer.get();
        Vector3d renderPos = new Vector3d(pos.getX() + 0.5, pos.getY() - 0.15, pos.getZ() + 0.5);
        double scale = NametagUtils.scale * textScale.get();
        if (NametagUtils.to2D(renderPos, 1.5, true)) {
            NametagUtils.begin(renderPos);
            textRenderer.begin(scale, false, true);
            double width = textRenderer.getWidth(text, true);
            double height = textRenderer.getHeight(true);
            textRenderer.render(text, -width / 2, -height / 2, color, true);
            textRenderer.end();
            NametagUtils.end();
        }
    }

}

package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;

import java.util.*;
import java.util.stream.Collectors;

public class ModuleList extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> x = sgGeneral.add(new IntSetting.Builder()
            .name("x")
            .defaultValue(1910)
            .sliderRange(0, 1920)
            .build()
    );

    private final Setting<Integer> y = sgGeneral.add(new IntSetting.Builder()
            .name("y")
            .defaultValue(10)
            .sliderRange(0, 1080)
            .build()
    );

    private final Setting<Boolean> additionalInfo = sgGeneral.add(new BoolSetting.Builder()
            .name("additional-info")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> onlyBind = sgGeneral.add(new BoolSetting.Builder()
            .name("OnlyBind")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
            .name("shadow")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> moduleColor = sgGeneral.add(new ColorSetting.Builder()
            .name("module-color")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );

    private final Setting<SettingColor> activeColor = sgGeneral.add(new ColorSetting.Builder()
            .name("active-color")
            .defaultValue(new SettingColor(0, 255, 0))
            .build()
    );

    private final Setting<SettingColor> inactiveColor = sgGeneral.add(new ColorSetting.Builder()
            .name("inactive-color")
            .defaultValue(new SettingColor(255, 0, 0))
            .build()
    );

    private final Setting<SettingColor> background = sgGeneral.add(new ColorSetting.Builder()
            .name("background")
            .defaultValue(new SettingColor(0, 0, 0, 80))
            .build()
    );

    private final Setting<SettingColor> tagColor = sgGeneral.add(new ColorSetting.Builder()
            .name("tag-color")
            .defaultValue(new SettingColor(0, 150, 255))
            .build()
    );

    private final double slideSpeed = 0.2;
    private final double fadeSpeed = 0.15;
    private final double ySpeed = 0.3;

    private final Map<Module, ModuleEntry> moduleEntries = new HashMap<>();

    public ModuleList() {
        super(LeavesHack.CATEGORY, "module-list", "Displays module states on screen (right-aligned).");
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        if (mc.player == null) return;

        TextRenderer textRenderer = TextRenderer.get();
        boolean useShadow = shadow.get();

        for (Module m : Modules.get().getAll()) {
            if (!moduleEntries.containsKey(m)) {
                moduleEntries.put(m, new ModuleEntry(m));
            }
        }

        List<Module> activeModules = Modules.get().getAll().stream()
                .filter(m -> m != this && m.isActive() && (!onlyBind.get() || m.keybind.isSet()))
                .sorted((a, b) -> Integer.compare(
                        (int) textRenderer.getWidth(b.title, useShadow),
                        (int) textRenderer.getWidth(a.title, useShadow)
                ))
                .collect(Collectors.toList());

        double drawY = y.get();

        for (int i = 0; i < activeModules.size(); i++) {
            Module module = activeModules.get(i);
            ModuleEntry entry = moduleEntries.get(module);

            String info = additionalInfo.get() && module.getInfoString() != null ? module.getInfoString() : "";
            String fullText = module.title + (info.isEmpty() ? "" : " " + info);
            double width = textRenderer.getWidth(fullText, useShadow);
            double height = textRenderer.getHeight(useShadow);

            double targetX = x.get() - width;
            entry.x += (targetX - entry.x) * slideSpeed;
            entry.y += (drawY - entry.y) * ySpeed;
            entry.fade += (1 - entry.fade) * fadeSpeed;

            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(entry.x - 4, entry.y - 2, width + 8, height + 4,
                    new SettingColor(
                            background.get().r,
                            background.get().g,
                            background.get().b,
                            (int) (background.get().a * entry.fade)
                    )
            );
            Renderer2D.COLOR.quad(entry.x + width + 4, entry.y - 2, 3, height + 4,
                    new SettingColor(
                            tagColor.get().r,
                            tagColor.get().g,
                            tagColor.get().b,
                            (int) (tagColor.get().a * entry.fade)
                    )
            );
            Renderer2D.COLOR.render(null);

            textRenderer.beginBig();
            double nameWidth = textRenderer.getWidth(module.title, useShadow);
            textRenderer.render(module.title, entry.x, entry.y, moduleColor.get(), useShadow);

            SettingColor stateColor = module.isActive() ? activeColor.get() : inactiveColor.get();
            textRenderer.render(info, entry.x + nameWidth + textRenderer.getWidth(" ", useShadow), entry.y, stateColor, useShadow);
            textRenderer.end();

            drawY += height + 6;
        }

        for (Module m : moduleEntries.keySet()) {
            if (!activeModules.contains(m)) {
                ModuleEntry entry = moduleEntries.get(m);
                entry.fade += (0 - entry.fade) * fadeSpeed;
                entry.x += ((x.get() + 50) - entry.x) * slideSpeed;
            }
        }
    }

    private static class ModuleEntry {
        final Module module;
        double x = 0;
        double y = 0;
        double fade = 0;

        ModuleEntry(Module module) {
            this.module = module;
        }
    }
}

package com.dev.leavesHack.manager;

import com.dev.leavesHack.asm.accessors.IModule;
import com.dev.leavesHack.asm.accessors.SettingAccessor;
import com.dev.leavesHack.asm.accessors.SettingGroupAccessor;
import com.dev.leavesHack.modules.GlobalSetting;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.ServerConnectBeginEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;

import java.util.TimerTask;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ModuleManager {
    public static final ModuleManager INSTANCE = new ModuleManager();
    private ModuleManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }
    public boolean chinese = false;
    @EventHandler
    private void onGameJoined(ServerConnectBeginEvent event) {
        if (GlobalSetting.INSTANCE.chinese.get()) {
            refreshGui();
            refreshGui();
        }
    }
    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (!chinese && GlobalSetting.INSTANCE.chinese.get()) {
            for (Module module : Modules.get().getAll()) {
                if (!module.addon.name.equals("LeavesHack")) continue;
                IModule iModule = (IModule) module;
                String engModule = iModule.getTitle();
                if (iModule.getDescription().isEmpty()) continue;
                iModule.setTitle(iModule.getDescription());
                iModule.setDescription(engModule);
                for (SettingGroup group : module.settings.groups) {
                    for (Setting<?> setting : ((SettingGroupAccessor) group).getSettings()) {
                        String eng = setting.title;
                        if (setting.description.isEmpty()) continue;
                        ((SettingAccessor) setting).setTitle(setting.description);
                        ((SettingAccessor) setting).setDescription(eng);
                    }
                }
            }
            refreshGui();
            refreshGui();
            chinese = true;
        } else if (chinese && !GlobalSetting.INSTANCE.chinese.get()) {
            for (Module module : Modules.get().getAll()) {
                if (!module.addon.name.equals("LeavesHack")) continue;
                IModule iModule = (IModule) module;
                String zhModule = iModule.getTitle();
                if (iModule.getDescription().isEmpty()) continue;
                iModule.setTitle(iModule.getDescription());
                iModule.setDescription(zhModule);
                for (SettingGroup group : module.settings.groups) {
                    for (Setting<?> setting : ((SettingGroupAccessor) group).getSettings()) {
                        String eng = setting.title;
                        if (setting.description.isEmpty()) continue;
                        ((SettingAccessor) setting).setTitle(setting.description);
                        ((SettingAccessor) setting).setDescription(eng);
                    }
                }
            }
            refreshGui();
            refreshGui();
            chinese = false;
        }
    }
    private void refreshGui() {
        mc.currentScreen.close();
        long delay = 0;
        java.util.Timer timer = new java.util.Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                mc.execute(() -> {
                    Tabs.get().getFirst().openScreen(GuiThemes.get());
                });
                timer.cancel();
            }
        }, delay);
    }
}


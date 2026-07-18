package com.dev.leavesHack.manager;

import com.dev.leavesHack.asm.accessors.IModule;
import com.dev.leavesHack.asm.accessors.SettingAccessor;
import com.dev.leavesHack.asm.accessors.SettingGroupAccessor;
import com.dev.leavesHack.modules.GlobalSetting;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.ServerConnectBeginEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.PlayerInput;

import java.util.TimerTask;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ModuleManager {
    public static final ModuleManager INSTANCE = new ModuleManager();
    private ModuleManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }
    public boolean chinese = false;
    public PlayerInput lastInput = null;
    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (event.packet instanceof PlayerInputC2SPacket packet) {
            if (GlobalSetting.INSTANCE.noBadPackets.get() && packet.input().equals(lastInput)) {
                event.cancel();
            }
            lastInput = packet.input();
        }
    }
    @EventHandler
    private void onGameJoined(ServerConnectBeginEvent event) {
        if (GlobalSetting.INSTANCE.chinese.get()) {
            refreshGui(false);
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
            refreshGui(true);
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
            refreshGui(true);
            chinese = false;
        }
    }
    private void refreshGui(boolean finalOpen) {
        if (finalOpen) {
            mc.currentScreen.close();
            long delay = 0;
            java.util.Timer timer = new java.util.Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mc.execute(() -> {
                        Tabs.get().getFirst().openScreen(GuiThemes.get());
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                mc.execute(() -> {
                                    mc.currentScreen.close();
                                    timer.schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            mc.execute(() -> {
                                                Tabs.get().getFirst().openScreen(GuiThemes.get());
                                            });
                                            timer.cancel();
                                        }
                                    }, delay);
                                });
                                timer.cancel();
                            }
                        }, delay);
                    });
                    timer.cancel();
                }
            }, delay);
        } else {
            Tabs.get().getFirst().openScreen(GuiThemes.get());
            long delay = 0;
            java.util.Timer timer = new java.util.Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mc.execute(() -> {
                        mc.currentScreen.close();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                mc.execute(() -> {
                                    Tabs.get().getFirst().openScreen(GuiThemes.get());
                                    timer.schedule(new TimerTask() {
                                        @Override
                                        public void run() {
                                            mc.execute(() -> {
                                                mc.currentScreen.close();
                                            });
                                            timer.cancel();
                                        }
                                    }, delay);
                                });
                                timer.cancel();
                            }
                        }, delay);
                    });
                    timer.cancel();
                }
            }, delay);
        }
    }
}


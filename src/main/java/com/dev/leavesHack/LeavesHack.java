package com.dev.leavesHack;

import com.dev.leavesHack.modules.*;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class LeavesHack extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("LeavesHack");
    public static final HudGroup HUD_GROUP = new HudGroup("LeavesHack");

    @Override
    public void onInitialize() {
        LOG.info("Initializing LeavesHack");

        // Modules
        add(new PlaceRender());
        add(new AutoCrystal());
        add(new AutoAnchor());
        add(new PistonCrystal());
        add(new GlobalSetting());
        add(new AutoTree());
        add(new AutoLogin());
        add(new Printer());
        add(new AutoTorch());
        add(new ScaffoldPlus());
        add(new LegitNoFall());
        add(new AutoCity());
        add(new AutoPlaceBlock());
        add(new NukerPlus());
        add(new ModuleList());
        add(new PacketMine());
        add(new Aura());
        add(new AutoArmorPlus());
        add(new FireworkElytraFly());
        add(new AntiAntiXray());
        add(new AutoRefreshTrade());

//这两模板还用不上后面用得到再研究
        // Commands
//        Commands.add(new CommandExample());

        // HUD
//        Hud.get().register(HudExample.INFO);
    }
    private void add(Module module){
        Modules.get().add(module);
    }
    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }
    @Override
    public String getPackage() {
        return "com.dev.leavesHack";
    }
    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("MeteorDevelopment", "meteor-addon-leaves");
    }
}

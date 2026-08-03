package com.dev.leavesHack;

import com.dev.leavesHack.manager.ModuleManager;
import com.dev.leavesHack.modules.*;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import org.slf4j.Logger;

public class LeavesHack extends MeteorAddon {
    public static long initTime;
    public static final Logger LOG = LogUtils.getLogger();
    public static final Category CATEGORY = new Category("LeavesHack");
    public static final HudGroup HUD_GROUP = new HudGroup("LeavesHack");

    @Override
    public void onInitialize() {
        initTime = System.currentTimeMillis();
        LOG.info("Initializing LeavesHack");
        Rotation.INSTANCE.hashCode();
        InventoryUtil.INSTANCE.hashCode();
        ModuleManager.INSTANCE.hashCode();
        // Modules
        add(new FriendsManager());
        add(new ElytraGrimAccelerate());
        add(new Printer());
        add(new PlaceRender());
        add(new AutoCrystal());
        add(new AutoPlaceBlock());
        add(new AutoAnchor());
        add(new AutoRefreshTrade());
        add(new AutoTree());
        add(new AutoArmorPlus());
        add(new AutoTorch());
        add(new ModuleList());
        add(new Aura());
        add(new ScaffoldPlus());
        add(new FireworkElytraFly());
        add(new AutoCity());
        add(new PacketMine());
        add(new GlobalSetting());
        add(new PacketLogger());
        add(new Follower());
        add(new GlassFiller());
        add(new PistonCrystal());
        add(new AutoLogin());
        add(new NukerPlus());
//        // Commands
//        Commands.add(new CommandExample());
//
//        // HUD
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
        return new GithubRepo("MeteorDevelopment", "meteor-addon-template");
    }
}

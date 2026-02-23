package com.dev.leavesHack;

import com.dev.leavesHack.commands.CommandExample;
import com.dev.leavesHack.hud.HudExample;
import com.dev.leavesHack.modules.*;
import com.mojang.logging.LogUtils;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
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
        Modules.get().add(new AutoPlaceSlab());
        Modules.get().add(new NukerPlus());
        Modules.get().add(new ModuleList());
        Modules.get().add(new PacketMine());
        Modules.get().add(new Aura());
        Modules.get().add(new AutoArmorPlus());
        Modules.get().add(new FireworkElytraFly());
        Modules.get().add(new AntiAntiXray());
        Modules.get().add(new AutoRefreshTrade());

        // Commands
        Commands.add(new CommandExample());

        // HUD
        Hud.get().register(HudExample.INFO);
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

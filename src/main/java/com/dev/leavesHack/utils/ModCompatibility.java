package com.dev.leavesHack.utils;

import net.fabricmc.loader.api.FabricLoader;

public class ModCompatibility {

    public static final boolean IS_BARITONE_PRESENT = FabricLoader.getInstance().isModLoaded("baritone");
    public static final boolean IS_LITEMATICA_PRESENT = FabricLoader.getInstance().isModLoaded("litematica");
    public static final boolean IS_MALILIB_PRESENT = FabricLoader.getInstance().isModLoaded("malilib");

    public static boolean isBaritoneAvailable() {
        return IS_BARITONE_PRESENT;
    }

    public static boolean isLitematicaAvailable() {
        return IS_LITEMATICA_PRESENT && IS_MALILIB_PRESENT;
    }
}
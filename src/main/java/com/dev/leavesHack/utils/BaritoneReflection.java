package com.dev.leavesHack.utils;

import java.lang.reflect.Method;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class BaritoneReflection {

    private static Object baritoneProvider = null;
    private static Object primaryBaritone = null;
    private static Object pathingBehavior = null;
    private static Object commandManager = null;
    private static boolean initialized = false;

    public static boolean init() {
        if (initialized) return true;
        if (!ModCompatibility.isBaritoneAvailable()) return false;

        try {
            Class<?> baritoneAPIClass = Class.forName("baritone.api.BaritoneAPI");
            Method getProviderMethod = baritoneAPIClass.getMethod("getProvider");
            baritoneProvider = getProviderMethod.invoke(null);

            Method getPrimaryBaritoneMethod = baritoneProvider.getClass().getMethod("getPrimaryBaritone");
            primaryBaritone = getPrimaryBaritoneMethod.invoke(baritoneProvider);

            Method getPathingBehaviorMethod = primaryBaritone.getClass().getMethod("getPathingBehavior");
            pathingBehavior = getPathingBehaviorMethod.invoke(primaryBaritone);

            Method getCommandManagerMethod = primaryBaritone.getClass().getMethod("getCommandManager");
            commandManager = getCommandManagerMethod.invoke(primaryBaritone);

            initialized = true;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void cancelEverything() {
        if (!init()) return;
        try {
            Method cancelEverythingMethod = pathingBehavior.getClass().getMethod("cancelEverything");
            cancelEverythingMethod.invoke(pathingBehavior);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void executeCommand(String command) {
        if (!init()) return;
        try {
            Method executeMethod = commandManager.getClass().getMethod("execute", String.class);
            executeMethod.invoke(commandManager, command);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
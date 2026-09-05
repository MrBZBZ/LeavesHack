package com.dev.leavesHack.utils;

import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class LitematicaReflection {

    private static Class<?> schematicWorldHandlerClass = null;
    private static Class<?> worldSchematicClass = null;
    private static Method getSchematicWorldMethod = null;
    private static Method getBlockStateMethod = null;
    private static boolean initialized = false;

    public static boolean init() {
        if (initialized) return true;
        if (!ModCompatibility.isLitematicaAvailable()) return false;

        try {
            schematicWorldHandlerClass = Class.forName("fi.dy.masa.litematica.world.SchematicWorldHandler");
            worldSchematicClass = Class.forName("fi.dy.masa.litematica.world.WorldSchematic");

            getSchematicWorldMethod = schematicWorldHandlerClass.getMethod("getSchematicWorld");
            getBlockStateMethod = worldSchematicClass.getMethod("getBlockState", BlockPos.class);

            initialized = true;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Object getSchematicWorld() {
        if (!init()) return null;
        try {
            return getSchematicWorldMethod.invoke(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static BlockState getBlockState(Object schematicWorld, BlockPos pos) {
        if (schematicWorld == null) return null;
        try {
            return (BlockState) getBlockStateMethod.invoke(schematicWorld, pos);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
package com.dev.leavesHack.utils.world;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class BlockPosX extends BlockPos {

    public BlockPosX(double x, double y, double z) {
        super(Mth.floor(x), Mth.floor(y), Mth.floor(z));
    }

    public BlockPosX(double x, double y, double z, boolean fix) {
        this(x, y + (fix ? 0.5 : 0), z);
    }

    public BlockPosX(Vec3 vec3d) {
        this(vec3d.x, vec3d.y, vec3d.z);
    }

    public BlockPosX(Vec3 vec3d, boolean fix) {
        this(vec3d.x, vec3d.y, vec3d.z, fix);
    }
}

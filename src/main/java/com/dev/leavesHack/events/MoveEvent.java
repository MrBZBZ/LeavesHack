package com.dev.leavesHack.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;

public class MoveEvent extends Cancellable {
    private double x;
    private double y;
    private double z;
    public Vec3d movement;
    public MoveEvent(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    public double getX() {
        return this.x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return this.y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return this.z;
    }

    public void setZ(double z) {
        this.z = z;
    }
}

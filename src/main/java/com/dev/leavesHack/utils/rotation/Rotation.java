package com.dev.leavesHack.utils.rotation;

import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import static meteordevelopment.meteorclient.MeteorClient.mc;
public class Rotation {
    public static float rotationYaw = 0;
    public static float rotationPitch = 0;
    public static void snapAt(float yaw, float pitch) {
        sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround()));
    }
    public static void snapBack() {
        sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), rotationYaw, rotationPitch, mc.player.isOnGround()));
    }
    public static void sendPacket(Packet<?> packet) {
        mc.getNetworkHandler().sendPacket(packet);
    }
    public static void snapAt(Vec3d directionVec) {
        float[] angle = getRotation(directionVec);
        snapAt(angle[0], angle[1]);
    }
    public static void snapAt(Box box) {
        snapAt(getClosestPointToEye(mc.player.getEyePos(), box));
    }
    public static Vec3d getClosestPointToEye(Vec3d eyePos, Box box) {
        double x = eyePos.x;
        double y = eyePos.y;
        double z = eyePos.z;

        if (eyePos.x < box.minX) x = box.minX;
        else if (eyePos.x > box.maxX) x = box.maxX;

        if (eyePos.y < box.minY) y = box.minY;
        else if (eyePos.y > box.maxY) y = box.maxY;

        if (eyePos.z < box.minZ) z = box.minZ;
        else if (eyePos.z > box.maxZ) z = box.maxZ;

        return new Vec3d(x, y, z);
    }
    public static float[] getRotation(Vec3d eyesPos, Vec3d vec) {
        double diffX = vec.x - eyesPos.x;
        double diffY = vec.y - eyesPos.y;
        double diffZ = vec.z - eyesPos.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
        float pitch = (float) (-Math.toDegrees(Math.atan2(diffY, diffXZ)));
        return new float[]{MathHelper.wrapDegrees(yaw), MathHelper.wrapDegrees(pitch)};
    }
    public static float[] getRotation(Vec3d vec) {
        Vec3d eyesPos = mc.player.getEyePos();
        return getRotation(eyesPos, vec);
    }
}

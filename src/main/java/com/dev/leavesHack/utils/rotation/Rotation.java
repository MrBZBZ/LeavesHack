package com.dev.leavesHack.utils.rotation;

import com.dev.leavesHack.events.KeyboardInputEvent;
import com.dev.leavesHack.modules.GlobalSetting;
import com.dev.leavesHack.utils.entity.MoveFixUtil;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import static meteordevelopment.meteorclient.MeteorClient.mc;
public class Rotation {
    public static final Rotation INSTANCE = new Rotation();
    private Rotation() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }
    public static float rotationYaw = 0;
    public static float rotationPitch = 0;
    public static boolean rotation = false;
    public static float targetYaw = 0;
    public static float targetPitch = 0;
    public static float lastYaw = 0;
    public static float lastPitch = 0;
    public static boolean lastGround;
    public static void snapAt(float yaw, float pitch) {
        if (GlobalSetting.INSTANCE.moveFix.get()) {
            rotation = true;
            targetPitch = pitch;
            targetYaw = yaw;
        } else {
            if (GlobalSetting.INSTANCE.grimRotation.get()) {
                sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround()));
            } else {
                sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, mc.player.isOnGround()));
            }
        }
    }
    public static void snapBack() {
        if (!GlobalSetting.INSTANCE.snapBack.get()) return;
        if (GlobalSetting.INSTANCE.moveFix.get()) return;
        sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), rotationYaw, rotationPitch, mc.player.isOnGround()));
    }
    public static void sendPacket(Packet<?> packet) {
        mc.getNetworkHandler().sendPacket(packet);
    }
    public static void snapAt(Vec3d directionVec) {
        float[] angle = getRotation(directionVec);
        snapAt((angle[0]), angle[1]);
    }
    public static void snapAt(Box box) {
        snapAt(getClosestPointToEye(mc.player.getEyePos(), box));
    }
    @EventHandler
    public void onKeyInput(KeyboardInputEvent event) {
        if (!Rotation.rotation) return;
        MoveFixUtil.fixMovement(event, Rotation.targetYaw);
    }
    @EventHandler(priority = -999)
    public void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || event.isCancelled()) return;
        if (event.packet instanceof PlayerMoveC2SPacket packet) {
            if (packet.changesLook()) {
                lastYaw = packet.getYaw(lastYaw);
                lastPitch = packet.getPitch(lastPitch);
            }
            lastGround = packet.isOnGround();
        }
    }
    @EventHandler(priority = EventPriority.HIGH)
    public void onReceivePacket(PacketEvent.Receive event) {
        if (mc.player == null) return;
        if (event.packet instanceof PlayerPositionLookS2CPacket packet) {
            if (packet.getFlags().contains(PositionFlag.X_ROT)) {
                lastYaw = lastYaw + packet.getYaw();
            } else {
                lastYaw = packet.getYaw();
            }

            if (packet.getFlags().contains(PositionFlag.Y_ROT)) {
                lastPitch = lastPitch + packet.getPitch();
            } else {
                lastPitch = packet.getPitch();
            }
        }
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

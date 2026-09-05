package com.dev.leavesHack.utils.rotation;

import com.dev.leavesHack.asm.accessors.IPlayerInterectItemC2SPacket;
import com.dev.leavesHack.events.KeyboardInputEvent;
import com.dev.leavesHack.modules.GlobalSetting;
import com.dev.leavesHack.utils.entity.MoveFixUtil;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
                sendPacket(new ServerboundMovePlayerPacket.PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.onGround(), mc.player.horizontalCollision));
            } else {
                sendPacket(new ServerboundMovePlayerPacket.Rot(yaw, pitch, mc.player.onGround(), mc.player.horizontalCollision));
            }
        }
    }
    public static void elytraSnapAt(float yaw, float pitch) {
        rotation = true;
        targetPitch = pitch;
        targetYaw = yaw;
    }
    public static void snapBack() {
        if (!GlobalSetting.INSTANCE.snapBack.get()) return;
        if (GlobalSetting.INSTANCE.moveFix.get()) return;
        sendPacket(new ServerboundMovePlayerPacket.PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), rotationYaw, rotationPitch, mc.player.onGround(), mc.player.horizontalCollision));
    }
    public static void sendPacket(Packet<?> packet) {
        mc.getConnection().send(packet);
    }
    public static void snapAt(Vec3 directionVec) {
        float[] angle = getRotation(directionVec);
        snapAt((angle[0]), angle[1]);
    }
    public static void snapAt(AABB box) {
        snapAt(getClosestPointToEye(mc.player.getEyePosition(), box));
    }
    @EventHandler
    public void onKeyInput(KeyboardInputEvent event) {
        if (!Rotation.rotation) return;
        MoveFixUtil.fixMovement(event, Rotation.targetYaw);
    }
    @EventHandler(priority = -999)
    public void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || event.isCancelled()) return;
        //BadPacketJ Disabler
        if (event.packet instanceof ServerboundUseItemPacket packet && rotation) {
            ((IPlayerInterectItemC2SPacket) packet).setYaw(targetYaw);
            ((IPlayerInterectItemC2SPacket) packet).setPitch(targetPitch);
        }
        if (event.packet instanceof ServerboundMovePlayerPacket packet) {
            if (packet.hasRotation()) {
                lastYaw = packet.getYRot(lastYaw);
                lastPitch = packet.getXRot(lastPitch);
            }
            lastGround = packet.isOnGround();
        }
    }
    public static Vec3 getClosestPointToEye(Vec3 eyePos, AABB box) {
        double x = eyePos.x;
        double y = eyePos.y;
        double z = eyePos.z;

        if (eyePos.x < box.minX) x = box.minX;
        else if (eyePos.x > box.maxX) x = box.maxX;

        if (eyePos.y < box.minY) y = box.minY;
        else if (eyePos.y > box.maxY) y = box.maxY;

        if (eyePos.z < box.minZ) z = box.minZ;
        else if (eyePos.z > box.maxZ) z = box.maxZ;

        return new Vec3(x, y, z);
    }
    public static float[] getRotation(Vec3 eyesPos, Vec3 vec) {
        double diffX = vec.x - eyesPos.x;
        double diffY = vec.y - eyesPos.y;
        double diffZ = vec.z - eyesPos.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
        float pitch = (float) (-Math.toDegrees(Math.atan2(diffY, diffXZ)));
        return new float[]{Mth.wrapDegrees(yaw), Mth.wrapDegrees(pitch)};
    }
    public static float[] getRotation(Vec3 vec) {
        Vec3 eyesPos = mc.player.getEyePosition();
        return getRotation(eyesPos, vec);
    }
}

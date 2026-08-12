package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.hit.BlockHitResult;

public class PacketLogger extends Module {

    // Settings
    private final Setting<Boolean> moveFull = settings.getDefaultGroup().add(new BoolSetting.Builder().name("move-full").defaultValue(true).build());
    private final Setting<Boolean> movePos = settings.getDefaultGroup().add(new BoolSetting.Builder().name("move-position").defaultValue(true).build());
    private final Setting<Boolean> moveLook = settings.getDefaultGroup().add(new BoolSetting.Builder().name("move-look").defaultValue(true).build());
    private final Setting<Boolean> moveGround = settings.getDefaultGroup().add(new BoolSetting.Builder().name("move-ground").defaultValue(true).build());
    private final Setting<Boolean> vehicleMove = settings.getDefaultGroup().add(new BoolSetting.Builder().name("vehicle-move").defaultValue(true).build());
    private final Setting<Boolean> playerAction = settings.getDefaultGroup().add(new BoolSetting.Builder().name("player-action").defaultValue(true).build());
    private final Setting<Boolean> updateSlot = settings.getDefaultGroup().add(new BoolSetting.Builder().name("update-slot").defaultValue(true).build());
    private final Setting<Boolean> handSwing = settings.getDefaultGroup().add(new BoolSetting.Builder().name("hand-swing").defaultValue(true).build());
    private final Setting<Boolean> pong = settings.getDefaultGroup().add(new BoolSetting.Builder().name("pong").defaultValue(true).build());
    private final Setting<Boolean> interactEntity = settings.getDefaultGroup().add(new BoolSetting.Builder().name("interact-entity").defaultValue(true).build());
    private final Setting<Boolean> interactBlock = settings.getDefaultGroup().add(new BoolSetting.Builder().name("interact-block").defaultValue(true).build());
    private final Setting<Boolean> interactItem = settings.getDefaultGroup().add(new BoolSetting.Builder().name("interact-item").defaultValue(true).build());
    private final Setting<Boolean> closeScreen = settings.getDefaultGroup().add(new BoolSetting.Builder().name("close-screen").defaultValue(true).build());
    private final Setting<Boolean> command = settings.getDefaultGroup().add(new BoolSetting.Builder().name("client-command").defaultValue(true).build());
    private final Setting<Boolean> status = settings.getDefaultGroup().add(new BoolSetting.Builder().name("client-status").defaultValue(true).build());
    private final Setting<Boolean> clickSlot = settings.getDefaultGroup().add(new BoolSetting.Builder().name("click-slot").defaultValue(true).build());
    private final Setting<Boolean> pickInventory = settings.getDefaultGroup().add(new BoolSetting.Builder().name("pick-inventory").defaultValue(true).build());
    private final Setting<Boolean> teleportConfirm = settings.getDefaultGroup().add(new BoolSetting.Builder().name("teleport-confirm").defaultValue(true).build());
    private final Setting<Boolean> s2cVelocity = settings.getDefaultGroup().add(new BoolSetting.Builder().name("s2c-velocity").defaultValue(true).build());
    private String lastLog = "";
    private int sameCount = 0;
    public PacketLogger() {
        super(LeavesHack.LEAVES_MISC, "PacketLogger", "数据包记录");
    }

    private void log(String msg, Object... args) {
        info(String.format(msg, args));
    }

    // C2S packets
    @EventHandler
    private void onSend(PacketEvent.Send event) {

        if (event.packet instanceof PlayerMoveC2SPacket.Full packet && moveFull.get()) {
            StringBuilder b = new StringBuilder("PlayerMove Full - ");
            if (packet.changesPosition()) {
                b.append("x: ").append(packet.getX(0)).append(", y: ").append(packet.getY(0)).append(", z: ").append(packet.getZ(0)).append(" ");
            }
            if (packet.changesLook()) {
                b.append("yaw: ").append(packet.getYaw(0)).append(", pitch: ").append(packet.getPitch(0)).append(" ");
            }
            b.append("onground: ").append(packet.isOnGround());
            log(b.toString());
        }

        if (event.packet instanceof PlayerMoveC2SPacket.PositionAndOnGround packet && movePos.get()) {
            log("PlayerMove PosGround - x: %s y: %s z: %s onground: %s",
                packet.getX(0), packet.getY(0), packet.getZ(0), packet.isOnGround());
        }

        if (event.packet instanceof PlayerMoveC2SPacket.LookAndOnGround packet && moveLook.get()) {
            log("PlayerMove LookGround - yaw: %s pitch: %s onground: %s",
                packet.getYaw(0), packet.getPitch(0), packet.isOnGround());
        }

        if (event.packet instanceof PlayerMoveC2SPacket.OnGroundOnly packet && moveGround.get()) {
            log("PlayerMove Ground - onground: %s", packet.isOnGround());
        }

        if (event.packet instanceof PlayerActionC2SPacket packet && playerAction.get()) {
            if (packet.getDirection() != null) {
                log("PlayerAction - %s pos: %s",
                    packet.getAction().name(),
                    packet.getPos().toShortString());
            }
        }

        if (event.packet instanceof UpdateSelectedSlotC2SPacket packet && updateSlot.get()) {
            log("UpdateSlot - %d", packet.getSelectedSlot());
        }

        if (event.packet instanceof HandSwingC2SPacket packet && handSwing.get()) {
            log("HandSwing - %s", packet.getHand());
        }

        if (event.packet instanceof CommonPongC2SPacket packet && pong.get()) {
            log("Pong - %d", packet.getParameter());
        }

        if (event.packet instanceof PlayerInteractEntityC2SPacket && interactEntity.get()) {
            log("InteractEntity");
        }

        if (event.packet instanceof PlayerInteractBlockC2SPacket packet && interactBlock.get()) {
            BlockHitResult r = packet.getBlockHitResult();
            log("InteractBlock - %s %s",
                r.getBlockPos().toShortString(),
                r.getSide());
        }

        if (event.packet instanceof PlayerInteractItemC2SPacket packet && interactItem.get()) {
            log("InteractItem - %s", packet.getHand());
        }

        if (event.packet instanceof CloseHandledScreenC2SPacket packet && closeScreen.get()) {
            log("CloseScreen - %s", packet.getSyncId());
        }

        if (event.packet instanceof ClientCommandC2SPacket packet && command.get()) {
            log("ClientCommand - %s", packet.getMode());
        }

        if (event.packet instanceof ClientStatusC2SPacket packet && status.get()) {
            log("ClientStatus - %s", packet.getMode());
        }

        if (event.packet instanceof ClickSlotC2SPacket packet && clickSlot.get()) {
            log("ClickSlot - slot:%s button:%s", packet.slot(), packet.button());
        }

        if (event.packet instanceof TeleportConfirmC2SPacket packet && teleportConfirm.get()) {
            log("TeleportConfirm - %s", packet.getTeleportId());
        }
    }
    private void logDedup(String msg, Object... args) {
        String current = String.format(msg, args);

        if (current.equals(lastLog)) {
            sameCount++;
            return; // 一样就直接不输出
        }

        // 如果之前有重复次数，可以补一条总结（可选）
        if (sameCount > 0) {
            info("(x" + (sameCount + 1) + ") " + lastLog);
            sameCount = 0;
        }

        lastLog = current;
        info(current);
    }
}

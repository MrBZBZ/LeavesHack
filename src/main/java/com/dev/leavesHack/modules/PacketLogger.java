package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.phys.BlockHitResult;

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

        if (event.packet instanceof ServerboundMovePlayerPacket.PosRot packet && moveFull.get()) {
            StringBuilder b = new StringBuilder("PlayerMove Full - ");
            if (packet.hasPosition()) {
                b.append("x: ").append(packet.getX(0)).append(", y: ").append(packet.getY(0)).append(", z: ").append(packet.getZ(0)).append(" ");
            }
            if (packet.hasRotation()) {
                b.append("yaw: ").append(packet.getYRot(0)).append(", pitch: ").append(packet.getXRot(0)).append(" ");
            }
            b.append("onground: ").append(packet.isOnGround());
            log(b.toString());
        }

        if (event.packet instanceof ServerboundMovePlayerPacket.Pos packet && movePos.get()) {
            log("PlayerMove PosGround - x: %s y: %s z: %s onground: %s",
                packet.getX(0), packet.getY(0), packet.getZ(0), packet.isOnGround());
        }

        if (event.packet instanceof ServerboundMovePlayerPacket.Rot packet && moveLook.get()) {
            log("PlayerMove LookGround - yaw: %s pitch: %s onground: %s",
                packet.getYRot(0), packet.getXRot(0), packet.isOnGround());
        }

        if (event.packet instanceof ServerboundMovePlayerPacket.StatusOnly packet && moveGround.get()) {
            log("PlayerMove Ground - onground: %s", packet.isOnGround());
        }

        if (event.packet instanceof ServerboundPlayerActionPacket packet && playerAction.get()) {
            if (packet.getDirection() != null) {
                log("PlayerAction - %s pos: %s",
                    packet.getAction().name(),
                    packet.getPos().toShortString());
            }
        }

        if (event.packet instanceof ServerboundSetCarriedItemPacket packet && updateSlot.get()) {
            log("UpdateSlot - %d", packet.getSlot());
        }

        if (event.packet instanceof ServerboundSwingPacket packet && handSwing.get()) {
            log("HandSwing - %s", packet.getHand());
        }

        if (event.packet instanceof ServerboundPongPacket packet && pong.get()) {
            log("Pong - %d", packet.getId());
        }

        if (event.packet instanceof ServerboundInteractPacket && interactEntity.get()) {
            log("InteractEntity");
        }

        if (event.packet instanceof ServerboundUseItemOnPacket packet && interactBlock.get()) {
            BlockHitResult r = packet.getHitResult();
            log("InteractBlock - %s %s",
                r.getBlockPos().toShortString(),
                r.getDirection());
        }

        if (event.packet instanceof ServerboundUseItemPacket packet && interactItem.get()) {
            log("InteractItem - %s", packet.getHand());
        }

        if (event.packet instanceof ServerboundContainerClosePacket packet && closeScreen.get()) {
            log("CloseScreen - %s", packet.getContainerId());
        }

        if (event.packet instanceof ServerboundPlayerCommandPacket packet && command.get()) {
            log("ClientCommand - %s", packet.getAction());
        }

        if (event.packet instanceof ServerboundClientCommandPacket packet && status.get()) {
            log("ClientStatus - %s", packet.getAction());
        }

        if (event.packet instanceof ServerboundContainerClickPacket packet && clickSlot.get()) {
            log("ClickSlot - slot:%s button:%s", packet.slotNum(), packet.buttonNum());
        }

        if (event.packet instanceof ServerboundAcceptTeleportationPacket packet && teleportConfirm.get()) {
            log("TeleportConfirm - %s", packet.getId());
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

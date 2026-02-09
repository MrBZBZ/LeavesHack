package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import static com.dev.leavesHack.utils.rotation.Rotation.sendPacket;

public class FireworkElytraFly extends Module {
    private Timer fireworkTimer = new Timer();
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("Delay")
            .description("")
            .defaultValue(1000)
            .sliderMax(3000)
            .build()
    );
    private final Setting<Double> checkSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("CheckSpeed")
            .description("")
            .defaultValue(80)
            .sliderMax(100)
            .build()
    );
    private final Setting<Boolean> inventorySwap = sgGeneral.add(new BoolSetting.Builder()
            .name("InventorySwap")
            .description("")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> deBug = sgGeneral.add(new BoolSetting.Builder()
            .name("DeBug")
            .description("")
            .defaultValue(false)
            .build()
    );
    public FireworkElytraFly() {
        super(LeavesHack.CATEGORY, "FireworkElytraFly", "Automatically use firework to fly");
    }
    @EventHandler
    public void onTick(TickEvent.Pre event){
        if (deBug.get()) mc.player.sendMessage(Text.of("[LeavesHack]:Speed: " + getSpeed()));
        boolean wearingElytra = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA && ElytraItem.isUsable(mc.player.getEquippedStack(EquipmentSlot.CHEST));
        if (!wearingElytra || mc.player.isOnGround()) {
            mc.player.stopFallFlying();
            return;
        }
        if (!mc.player.isFallFlying() && !mc.player.isOnGround() && mc.options.jumpKey.isPressed()) {
            sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.startFallFlying();
        }
        if (!fireworkTimer.passedMs(delay.get())) return;
        if (wanToMove() && getSpeed() <= checkSpeed.get()) offFirework();
    }

    public void offFirework() {
        int firework;
        if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            fireworkTimer.reset();
        } else if (mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET) {
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            fireworkTimer.reset();
        } else if (inventorySwap.get() && (firework = InventoryUtil.findItemInventorySlot(Items.FIREWORK_ROCKET)) != -1) {
            InventoryUtil.inventorySwap(firework, mc.player.getInventory().selectedSlot);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            InventoryUtil.inventorySwap(firework, mc.player.getInventory().selectedSlot);
            sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
            fireworkTimer.reset();
        } else if ((firework = InventoryUtil.findItem(Items.FIREWORK_ROCKET)) != -1) {
            int old = mc.player.getInventory().selectedSlot;
            InventoryUtil.switchToSlot(firework);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            InventoryUtil.switchToSlot(old);
            fireworkTimer.reset();
        }
    }
    public void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        if (mc.getNetworkHandler() == null || mc.world == null) return;
        try (PendingUpdateManager pendingUpdateManager = mc.world.getPendingUpdateManager().incrementSequence()) {
            int i = pendingUpdateManager.getSequence();
            mc.getNetworkHandler().sendPacket(packetCreator.predict(i));
        }
    }
    private boolean wanToMove() {
        return mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed();
    }
    private double getSpeed() {
        double x = mc.player.getX() - mc.player.prevX;
        double z = mc.player.getZ() - mc.player.prevZ;
        double dist = Math.sqrt(x * x + z * z) / 1000.0;
        double div = 0.05 / 3600.0;
        float timer = 1f;
        double speed = dist / div * timer;
        return speed;
    }
}

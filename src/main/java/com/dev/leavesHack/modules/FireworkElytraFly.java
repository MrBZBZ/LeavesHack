package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.asm.accessors.IVec3d;
import com.dev.leavesHack.events.ElytraUpdateEvent;
import com.dev.leavesHack.events.TravelEvent;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

import static com.dev.leavesHack.utils.rotation.Rotation.*;

public class FireworkElytraFly extends Module {
    public boolean isUsingFirework = false;
    private final Timer fireworkTimer = new Timer();
    private final Timer swapTimer = new Timer();
    public boolean isFallFlying = false;
    public int packetDelayInt = 0;
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("Mode")
            .defaultValue(Mode.Legit)
            .build()
    );
    public final Setting<FireWorkMode> fireWorkMode = sgGeneral.add(new EnumSetting.Builder<FireWorkMode>()
            .name("FireWorkMode")
            .defaultValue(FireWorkMode.Delay)
            .build()
    );
    private final Setting<Double> packetDealy = sgGeneral.add(new DoubleSetting.Builder()
            .name("PacketDelay")
            .defaultValue(3)
            .sliderMax(100)
            .build()
    );
    public final Setting<Boolean> unbreaking = sgGeneral.add(new BoolSetting.Builder()
            .name("Unbreaking")
            .description("")
            .defaultValue(true)
            .build()
    );
    private final Setting<Double> fakeDelay = sgGeneral.add(new DoubleSetting.Builder()
            .name("FakeDelay")
            .defaultValue(800)
            .sliderMax(1000)
            .build()
    );
    public final Setting<Boolean> stand = sgGeneral.add(new BoolSetting.Builder()
            .name("Stand")
            .description("")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> releaseSneak = sgGeneral.add(new BoolSetting.Builder()
            .name("ReleaseSneak")
            .description("")
            .defaultValue(true)
            .build()
    );
    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("FireWorkDelay")
            .description("")
            .defaultValue(1000)
            .visible(() -> fireWorkMode.get() == FireWorkMode.Delay)
            .sliderMax(3000)
            .build()
    );
    private final Setting<Boolean> checkFirework = sgGeneral.add(new BoolSetting.Builder()
            .name("CheckFireWork")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> inventorySwap = sgGeneral.add(new BoolSetting.Builder()
            .name("InventorySwap")
            .description("")
            .defaultValue(true)
            .build()
    );
    private final Setting<Double> upSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("UpSpeed")
            .defaultValue(0.8)
            .sliderRange(0.0, 10.0)
            .build()
    );

    private final Setting<Double> downSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("DownSpeed")
            .defaultValue(1.0)
            .sliderRange(0.1, 10.0)
            .build()
    );

    private final Setting<Double> fallSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("FallSpeed")
            .defaultValue(0.05)
            .sliderRange(0.0, 3.0)
            .build()
    );
    private final Setting<Boolean> deBug = sgGeneral.add(new BoolSetting.Builder()
            .name("DeBug")
            .defaultValue(true)
            .build()
    );
    public static FireworkElytraFly INSTANCE;
    public FireworkElytraFly() {
        super(LeavesHack.CATEGORY, "FireworkElytraFly", "烟花鞘翅飞行");
        INSTANCE = this;
    }
    @Override
    public void onActivate() {
        fireworkTimer.setMs(99999);
        packetDelayInt = 0;
        swapTimer.setMs(99999);
    }
    @Override
    public void onDeactivate() {
        if (releaseSneak.get()) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }
    }
    @EventHandler
    public void onTravel(TravelEvent event) {
        if (!isFallFlying) return;
        if (mode.get() == Mode.Legit) return;
        if (mc.currentScreen instanceof ChatScreen) {
            setY(0);
            return;
        }
        if (!wantToMove()) {
            setX(0);
            setZ(0);
            setY(0);
        }
    }
    private void setY(double f) {
        ((IVec3d) mc.player.getVelocity()).setY(f);
    }
    private void setX(double f) {
        ((IVec3d) mc.player.getVelocity()).setX(f);
    }
    private void setZ(double f) {
        ((IVec3d) mc.player.getVelocity()).setZ(f);
    }
    @Override
    public String getInfoString() {
        int fireworks = 0;
        if (inventorySwap.get()) {
            for (int i = 0; i < 45; ++i) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.getItem() == Items.FIREWORK_ROCKET) fireworks = fireworks + stack.getCount();
            }
        } else {
            for (int i = 0; i < 9; ++i) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.getItem() == Items.FIREWORK_ROCKET) fireworks = fireworks + stack.getCount();
            }
        }
        return "§f[F:" + fireworks + "]";
    }
    @EventHandler
    public void onElytraUpdate(ElytraUpdateEvent event) {
        if (stand.get()) event.cancel();
    }
    @EventHandler
    public void onTick(TickEvent.Pre event){
        packetDelayInt++;
        boolean hasFirework = false;
        if (checkFirework.get()) {
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof FireworkRocketEntity firework) {
                    if (firework.getOwner() == mc.player) {
                        hasFirework = true;
                    }
                }
            }
        }
        isUsingFirework = hasFirework;
        int elytra = InventoryUtil.findItemInventorySlot(Items.ELYTRA);
//        int armor = findChestplate();
        boolean wearingElytra = mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA && ElytraItem.isUsable(mc.player.getEquippedStack(EquipmentSlot.CHEST));
        if (mc.player.isOnGround()) {
            mc.player.stopFallFlying();
        }
        if (wearingElytra && !isFallFlying && !mc.player.isOnGround()) {
            sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.startFallFlying();
        }
        if (wearingElytra && !mc.player.isOnGround() && unbreaking.get() && swapTimer.passedMs(fakeDelay.get())) {
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.startFallFlying();
            swapTimer.reset();
        }
        if (mode.get() == Mode.GrimDurability) {
            if (elytra != -1 && packetDelayInt > packetDealy.get()) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytra, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytra, 0, SlotActionType.PICKUP, mc.player);
                if (!mc.player.isOnGround()) {
                    sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                    mc.player.startFallFlying();
                }
                if (!hasFirework && fireWorkMode.get() == FireWorkMode.Auto) {
                    offFirework();
                } else if (fireWorkMode.get() == FireWorkMode.Delay && wantToMove()){
                    if (!checkFirework.get() || !isUsingFirework){
                        offFirework();
                    }
                }
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytra, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytra, 0, SlotActionType.PICKUP, mc.player);
                packetDelayInt = 0;
            }
        } else {
            if (wearingElytra && isFallFlying) {
                if (!hasFirework && fireWorkMode.get() == FireWorkMode.Auto) {
                    offFirework();
                } else if (fireWorkMode.get() == FireWorkMode.Delay && wantToMove()){
                    if (!checkFirework.get() || !isUsingFirework){
                        offFirework();
                    }
                }
            }
        }
    }

    public void offFirework() {
        if (!fireworkTimer.passedMs(delay.get()) && fireWorkMode.get() == FireWorkMode.Delay) return;
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
    public int findChestplate() {
        for (int slot = 0; slot < 45; slot++) {
            ItemStack stack = mc.player.getInventory().getStack(slot);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof ArmorItem armorItem)) {
                continue;
            }
            if (armorItem.getType() == ArmorItem.Type.CHESTPLATE) {
                return slot < 9 ? slot + 36 : slot;
            }
        }
        return -1;
    }
    public void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        if (mc.getNetworkHandler() == null || mc.world == null) return;
        try (PendingUpdateManager pendingUpdateManager = mc.world.getPendingUpdateManager().incrementSequence()) {
            int i = pendingUpdateManager.getSequence();
            mc.getNetworkHandler().sendPacket(packetCreator.predict(i));
        }
    }
    private boolean wantToMove() {
        return mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed() || mc.options.jumpKey.isPressed() || mc.options.sneakKey.isPressed();
    }
    public enum Mode {
        Legit,
        GrimDurability
    }
    public enum FireWorkMode {
        Auto,
        Delay,
        None
    }
    public boolean isMoving() {
        if (mc.player == null || mc.player.input == null) return false;
        return mc.player.input.movementForward != 0.0 || mc.player.input.movementSideways != 0.0;
    }
    public float getSprintYaw(float yaw) {
        if (mc.options.forwardKey.isPressed() && !mc.options.backKey.isPressed()) {
            if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
                yaw -= 45f;
            } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
                yaw += 45f;
            }
        } else if (mc.options.backKey.isPressed() && !mc.options.forwardKey.isPressed()) {
            yaw += 180f;
            if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
                yaw += 45f;
            } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
                yaw -= 45f;
            }
        } else if (mc.options.leftKey.isPressed() && !mc.options.rightKey.isPressed()) {
            yaw -= 90f;
        } else if (mc.options.rightKey.isPressed() && !mc.options.leftKey.isPressed()) {
            yaw += 90f;
        }
        return yaw;
    }
    private float getPitch(float pitch) {
        if (!(mc.currentScreen instanceof ChatScreen)) {
            if (mc.options.sneakKey.isPressed() && mc.options.jumpKey.isPressed()) {
                pitch = -3;
            } else if (mc.options.jumpKey.isPressed()) {
                if (isMoving()) {
                    pitch = -45;
                } else {
                    pitch = -90;
                }
            } else if (mc.options.sneakKey.isPressed()) {
                if (isMoving()) {
                    pitch = 45;
                } else {
                    pitch = 90;
                }
            }
            if (isMoving() && !mc.options.sneakKey.isPressed() && !mc.options.jumpKey.isPressed()) {
                pitch = -1.9f;
            }
        }
        return pitch;
    }
}

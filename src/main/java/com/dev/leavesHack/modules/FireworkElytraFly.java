package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.asm.accessors.IClientWorld;
import com.dev.leavesHack.asm.accessors.IPlayerMoveC2SPacket;
import com.dev.leavesHack.asm.accessors.IVec3d;
import com.dev.leavesHack.events.ElytraUpdateEvent;
import com.dev.leavesHack.events.KeyboardInputEvent;
import com.dev.leavesHack.events.TravelEvent;
import com.dev.leavesHack.utils.entity.EntityUtil;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.rotation.Rotation;
import java.util.TimerTask;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import static com.dev.leavesHack.utils.entity.InventoryUtil.sendPacket;
import static com.dev.leavesHack.utils.rotation.Rotation.*;

public class FireworkElytraFly extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("Mode")
        .description("运行模式")
        .defaultValue(Mode.GrimDurability)
        .build()
    );
    private final Setting<Double> spearDelay = sgGeneral.add(new DoubleSetting.Builder()
        .name("SpearDelay")
        .description("长矛突进延迟")
        .defaultValue(800)
        .sliderMax(1000)
        .build()
    );
    public final Setting<FireWorkMode> fireWorkMode = sgGeneral.add(new EnumSetting.Builder<FireWorkMode>()
        .name("FireWorkMode")
        .description("烟花使用模式")
        .defaultValue(FireWorkMode.Auto)
        .build()
    );
    private final Setting<Double> packetDealy = sgGeneral.add(new DoubleSetting.Builder()
        .name("PacketDelay")
        .description("发包延迟tick数")
        .defaultValue(3)
        .sliderMax(100)
        .build()
    );
    public final Setting<Boolean> stand = sgGeneral.add(new BoolSetting.Builder()
        .name("Stand")
        .description("站飞")
        .defaultValue(true)
        .build()
    );
    public final Setting<Boolean> noSprint = sgGeneral.add(new BoolSetting.Builder()
        .name("NoSprint")
        .description("自动停止疾跑")
        .defaultValue(true)
        .build()
    );
    public final Setting<Boolean> releaseSneak = sgGeneral.add(new BoolSetting.Builder()
        .name("ReleaseSneak")
        .description("自动松开shift")
        .defaultValue(true)
        .build()
    );
    public final Setting<Boolean> pressSneak = sgGeneral.add(new BoolSetting.Builder()
        .name("PressSneak")
        .description("自动按下shift")
        .defaultValue(true)
        .build()
    );
    public final Setting<Integer> releaseDelay = sgGeneral.add(new IntSetting.Builder()
        .name("ReleaseDelay")
        .description("shift延迟")
        .defaultValue(100)
        .sliderMax(1000)
        .build()
    );
    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
        .name("FireWorkDelay")
        .description("烟花操作延迟")
        .defaultValue(1000)
        .visible(() -> fireWorkMode.get() == FireWorkMode.Delay)
        .sliderMax(3000)
        .build()
    );
    private final Setting<Boolean> checkFirework = sgGeneral.add(new BoolSetting.Builder()
        .name("CheckFireWork")
        .description("自动检查烟花")
        .defaultValue(true)
        .build()
    );
    //    private final Setting<Integer> checkTime = sgGeneral.add(new IntSetting.Builder()
//        .name("CheckTime")
//        .description("烟花寿命")
//        .defaultValue(35)
//        .sliderRange(0, 50)
//        .build()
//    );
    public final Setting<Boolean> inventorySwap = sgGeneral.add(new BoolSetting.Builder()
        .name("InventorySwap")
        .description("背包鬼手")
        .defaultValue(true)
        .build()
    );
    public final Setting<Boolean> control = sgGeneral.add(new BoolSetting.Builder()
        .name("Control")
        .description("甲飞控制")
        .defaultValue(true)
        .build()
    );
    private final Setting<Double> flySpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("Speed")
        .description("飞行速度")
        .defaultValue(1.7)
        .min(0)
        .sliderMax(5.0)
        .build()
    );
    private final Setting<Double> fallSpeed = sgGeneral.add(new DoubleSetting.Builder()
        .name("FallSpeed")
        .description("下落速度")
        .defaultValue(0.02)
        .sliderRange(0.0, 3.0)
        .build()
    );
    private final Setting<Boolean> horizontalNoGravity = sgGeneral.add(new BoolSetting.Builder()
        .name("HorizontalNoGravity")
        .description("关闭重力")
        .defaultValue(false)
        .build()
    );
    private final Setting<Boolean> deBug = sgGeneral.add(new BoolSetting.Builder()
        .name("DeBug")
        .description("dev查bug的，没iq不要开")
        .defaultValue(false)
        .build()
    );
    public static FireworkElytraFly INSTANCE;
    public FireworkElytraFly() {
        super(LeavesHack.LEAVES_COMBAT, "FireworkElytraFly", "烟花鞘翅飞行");
        INSTANCE = this;
    }
    public float yaw = rotationYaw;
    public float pitch = rotationPitch;
    public boolean isUsingFirework = false;
    private final Timer fireworkTimer = new Timer();
    private final Timer delayTimer = new Timer();
    private final Timer swapTimer = new Timer();
    private final Timer spearTimer = new Timer();
    public boolean isFallFlying = false;
    public int packetDelayInt = 0;
    public int spearDelayInt = 0;
    public boolean shouldJump = false, shouldRestore = false;
    public Input bypassInput = null;
    public boolean hasSpear = false;
    private boolean savedNoGravity = false;
    private boolean isNoGravityActive = false;
    @Override
    public void onActivate() {
        if (noSprint.get()) mc.getConnection().send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
        hasSpear = false;
        delayTimer.setMs(99999);
        spearTimer.setMs(99999);
        fireworkTimer.setMs(99999);
        packetDelayInt = 0;
        spearDelayInt = 0;
        shouldJump = false;
        swapTimer.setMs(99999);
    }
    @Override
    public void onDeactivate() {
        if (pressSneak.get()) {
            mc.player.setShiftKeyDown(true);
        }
        if (releaseSneak.get()) {
            long delay = releaseDelay.get();
            java.util.Timer timer = new java.util.Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mc.execute(() -> {
                        mc.player.setShiftKeyDown(true);
                    });
                }
            }, delay);
        }
    }
    @EventHandler
    public void onSprint(PacketEvent.Send send) {
        if (noSprint.get() && send.packet instanceof ServerboundPlayerCommandPacket packet) {
            if (packet.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING) send.cancel();
        }
    }
    @EventHandler
    public void onTickPost(TickEvent.Post event) {
        shouldJump = false;
        if (!shouldRestore) return;
        mc.player.input.keyPresses = bypassInput;
        bypassInput = null;
        shouldRestore = false;
    }
    @EventHandler
    public void onTravel(TravelEvent event) {
        if (!isFallFlying) return;
        if (isNoGravityActive) {
            mc.player.setNoGravity(savedNoGravity);
            isNoGravityActive = false;
            return;
        }
        if (mode.get() == Mode.Legit) return;
        if (!control.get()) return;
        if (Follower.INSTANCE.isActive() && Follower.INSTANCE.canFollow) return;
        if (mc.player.onGround()) {
            shouldJump = true;
            return;
        }
        double speed = flySpeed.get();
        double radYaw = Math.toRadians(yaw);
        double radPitch = Math.toRadians(pitch);
        double x = -Math.sin(radYaw) * Math.cos(radPitch) * speed;
        double y = -Math.sin(radPitch) * speed;
        double z = Math.cos(radYaw) * Math.cos(radPitch) * speed;
        double horLen = Math.sqrt(x * x + z * z);
        if (horLen > 0.001) {
            x = x / horLen * speed;
            z = z / horLen * speed;
        }
        if (mc.screen instanceof ChatScreen) {
            setY(fallSpeed.get());
            return;
        }
        if (!wantToMove()) {
            setX(0);
            setZ(0);
            setY(fallSpeed.get());
        } else {
            setX(x);
            setY(y);
            setZ(z);
        }
        if (horizontalNoGravity.get() && Math.abs(y) <= 0.02 && !mc.player.onGround()) {
            savedNoGravity = mc.player.isNoGravity();
            mc.player.setNoGravity(true);
            isNoGravityActive = true;
        }
    }
    private void setY(double f) {
        ((IVec3d) mc.player.getDeltaMovement()).setY(f);
    }
    private void setX(double f) {
        ((IVec3d) mc.player.getDeltaMovement()).setX(f);
    }
    private void setZ(double f) {
        ((IVec3d) mc.player.getDeltaMovement()).setZ(f);
    }
    @Override
    public String getInfoString() {
        if (mc.player == null || mc.level == null) return null;
        int fireworks = 0;
        if (inventorySwap.get()) {
            for (int i = 0; i < 45; ++i) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.getItem() == Items.FIREWORK_ROCKET) fireworks = fireworks + stack.getCount();
            }
        } else {
            for (int i = 0; i < 9; ++i) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.getItem() == Items.FIREWORK_ROCKET) fireworks = fireworks + stack.getCount();
            }
        }
        return "[F:" + fireworks + "]";
    }
    @EventHandler
    public void onKeyInput(KeyboardInputEvent event) {
        if (shouldJump) {
            event.setJump(true);
            shouldJump = false;
        }
    }
    @EventHandler
    public void onElytraUpdate(ElytraUpdateEvent event) {
        if (stand.get()) event.cancel();
    }
    @EventHandler
    public void onTick(TickEvent.Pre event){
        if (mc.screen != null && deBug.get()) info("screen" + mc.screen.getTitle() + " " + mc.screen.getClass().getSimpleName() + " " + mc.screen.getClass().getSuperclass().getSimpleName() + " " + mc.screen.getTitle());
        if (mc.screen != null && mc.screen instanceof AbstractContainerScreen<?> && !(mc.screen instanceof InventoryScreen || mc.screen instanceof CreativeModeInventoryScreen)) return;
        if (mc.player.onGround() || mc.player.isInLiquid()) {
            shouldJump = true;
            return;
        }
        yaw = getSprintYaw(mc.player.getYRot());
        pitch = getPitch(mc.player.getXRot());
        if (deBug.get()) info("Yaw: " + yaw + " Pitch: " + pitch);
        syncInput();
        packetDelayInt++;
        boolean hasFirework = false;
        if (checkFirework.get()) {
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity instanceof FireworkRocketEntity firework) {
                    if (firework.getOwner() == mc.player) {
                        hasFirework = true;
                    }
                }
            }
        }
        isUsingFirework = hasFirework;
        if (control.get()) {
            Rotation.elytraSnapAt(yaw, pitch);
        } else {
            Rotation.rotation = false;
        }
        int elytra = InventoryUtil.findItemInventorySlot(Items.ELYTRA);
//        int armor = findChestplate();
//        ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        boolean wearingElytra = chest.is(Items.ELYTRA) && chest.isDamageableItem() && chest.getDamageValue() < chest.getMaxDamage();
        if (wearingElytra && !isFallFlying && !mc.player.onGround() && mode.get() == Mode.Legit) {
            shouldJump = true;
            sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            mc.player.startFallFlying();
        }
        if (mode.get() == Mode.GrimDurability && !isFallFlying) {
            if (elytra != -1 && packetDelayInt > packetDealy.get()) {
                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, elytra, 0, ContainerInput.PICKUP, mc.player);
                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, 6, 0, ContainerInput.PICKUP, mc.player);
                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, elytra, 0, ContainerInput.PICKUP, mc.player);
                shouldJump = true;
                sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                mc.player.startFallFlying();
                if (!hasFirework && fireWorkMode.get() == FireWorkMode.Auto) {
                    offFirework();
                } else if (fireWorkMode.get() == FireWorkMode.Delay && wantToMove()){
                    if (!checkFirework.get() || !isUsingFirework){
                        offFirework();
                    }
                }
                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, elytra, 0, ContainerInput.PICKUP, mc.player);
                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, 6, 0, ContainerInput.PICKUP, mc.player);
                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, elytra, 0, ContainerInput.PICKUP, mc.player);
                packetDelayInt = 0;
            }
        }
        if (mode.get() == Mode.Legit) {
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
        if (mode.get() == Mode.AutoSpear) {
            ItemStack stack = mc.player.getInventory().getItem(mc.player.getInventory().getSelectedSlot());
            if (stack.is(ItemTags.SPEARS) && spearTimer.passedMs(spearDelay.get()) && wantToMove()) {
                mc.getConnection().send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STAB, BlockPos.ZERO, Direction.DOWN));
                EntityUtil.attackSwingHand();
                spearTimer.reset();
            }
            if (elytra != -1 && packetDelayInt > packetDealy.get()) {
                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, elytra, 0, ContainerInput.PICKUP, mc.player);
                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, 6, 0, ContainerInput.PICKUP, mc.player);
                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, elytra, 0, ContainerInput.PICKUP, mc.player);
                if (!mc.player.onGround()) {
                    shouldJump = true;
                    sendPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                    mc.player.startFallFlying();
                }
                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, elytra, 0, ContainerInput.PICKUP, mc.player);
                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, 6, 0, ContainerInput.PICKUP, mc.player);
                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, elytra, 0, ContainerInput.PICKUP, mc.player);
                packetDelayInt = 0;
            }
        }
    }
    public void syncInput() {
        if (shouldRestore) return;
        bypassInput = mc.player.input.keyPresses;
        mc.player.input.keyPresses = new Input(false, false, false, false, false, false, false);
        mc.getConnection().send(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, false, false)));
        mc.player.lastSentInput = new Input(false, false, false, false, false, false, false);
        shouldRestore = true;
    }
    private int findSpear() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.is(ItemTags.SPEARS)) {
                return i;
            }
        }
        return -1;
    }
    private int findChestplate() {
        int bestSlot = -1;
        int bestScore = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            // 检查是否为可装备物品或鞘翅
            if (!stack.has(net.minecraft.core.component.DataComponents.EQUIPPABLE) && stack.getItem() != Items.ELYTRA) continue;
            // 获取胸甲槽位类型
            EquipmentSlot slot;
            if (stack.getItem() == Items.ELYTRA) {
                slot = EquipmentSlot.CHEST;
            } else {
                slot = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE).slot();
                if (slot != EquipmentSlot.CHEST) continue;
            }
            // 计算评分
            int score = getChestplateScore(stack);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }
    private int getChestplateScore(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        // 鞘翅有最高优先级
        if (stack.getItem() == Items.ELYTRA) {
            if (!stack.isDamageableItem() || stack.getDamageValue() >= stack.getMaxDamage()) return -1;
            return 10000 + (stack.getMaxDamage() - stack.getDamageValue()); // 剩余耐久越高越好
        }
        if (!stack.has(net.minecraft.core.component.DataComponents.EQUIPPABLE)) return -1;
        int score = 0;
        // 基础护甲值
        if (stack.has(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS)) {
            net.minecraft.world.item.component.ItemAttributeModifiers component = stack.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);
            for (net.minecraft.world.item.component.ItemAttributeModifiers.Entry modifier : component.modifiers()) {
                if (modifier.attribute() == net.minecraft.world.entity.ai.attributes.Attributes.ARMOR) {
                    score += (int) modifier.modifier().amount();
                }
            }
        }
        // 保护附魔
        if (stack.isEnchanted()) {
            score += InventoryUtil.getEnchantmentLevel(stack, Enchantments.PROTECTION) * 5;
            score += InventoryUtil.getEnchantmentLevel(stack, Enchantments.BLAST_PROTECTION) * 5;
            score += InventoryUtil.getEnchantmentLevel(stack, Enchantments.FIRE_PROTECTION) * 3;
            score += InventoryUtil.getEnchantmentLevel(stack, Enchantments.PROJECTILE_PROTECTION) * 3;
        }
        // 耐久附魔
        score += InventoryUtil.getEnchantmentLevel(stack, Enchantments.UNBREAKING) * 2;
        return score;
    }
    public void offFirework() {
        if (!delayTimer.passedMs(700)) return;
        delayTimer.reset();
        if (!fireworkTimer.passedMs(delay.get()) && fireWorkMode.get() == FireWorkMode.Delay) return;
        int firework;
        if (mc.player.getMainHandItem().getItem() == Items.FIREWORK_ROCKET) {
            sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
            fireworkTimer.reset();
        } else if (mc.player.getOffhandItem().getItem() == Items.FIREWORK_ROCKET) {
            sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.OFF_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
            fireworkTimer.reset();
        } else if (inventorySwap.get() && (firework = InventoryUtil.findItemInventorySlot(Items.FIREWORK_ROCKET)) != -1) {
            InventoryUtil.inventorySwap(firework, mc.player.getInventory().getSelectedSlot());
            sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
            InventoryUtil.inventorySwap(firework, mc.player.getInventory().getSelectedSlot());
            fireworkTimer.reset();
        } else if ((firework = InventoryUtil.findItem(Items.FIREWORK_ROCKET)) != -1) {
            int old = mc.player.getInventory().getSelectedSlot();
            InventoryUtil.switchToSlot(firework);
            sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
            InventoryUtil.switchToSlot(old);
            fireworkTimer.reset();
        }
    }
    public void sendSequencedPacket(PredictiveAction packetCreator) {
        if (mc.getConnection() == null || mc.level == null) return;
        try (BlockStatePredictionHandler pendingUpdateManager = ((IClientWorld) mc.level).invokeGetPendingUpdateManager().startPredicting()) {
            int i = pendingUpdateManager.currentSequence();
            mc.getConnection().send(packetCreator.predict(i));
        }
    }
    private boolean wantToMove() {
        return mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown() || mc.options.keyJump.isDown() || mc.options.keyShift.isDown();
    }
    public enum Mode {
        Legit,
        GrimDurability,
        AutoSpear
    }
    public enum FireWorkMode {
        Auto,
        Delay,
        None
    }
    public boolean isMoving() {
        return mc.options.keyUp.isDown() || mc.options.keyDown.isDown() || mc.options.keyRight.isDown() || mc.options.keyLeft.isDown();
    }
    public float getSprintYaw(float yaw) {
        if (mc.options.keyUp.isDown() && !mc.options.keyDown.isDown()) {
            if (mc.options.keyLeft.isDown() && !mc.options.keyRight.isDown()) {
                yaw -= 45f;
            } else if (mc.options.keyRight.isDown() && !mc.options.keyLeft.isDown()) {
                yaw += 45f;
            }
        } else if (mc.options.keyDown.isDown() && !mc.options.keyUp.isDown()) {
            yaw += 180f;
            if (mc.options.keyLeft.isDown() && !mc.options.keyRight.isDown()) {
                yaw += 45f;
            } else if (mc.options.keyRight.isDown() && !mc.options.keyLeft.isDown()) {
                yaw -= 45f;
            }
        } else if (mc.options.keyLeft.isDown() && !mc.options.keyRight.isDown()) {
            yaw -= 90f;
        } else if (mc.options.keyRight.isDown() && !mc.options.keyLeft.isDown()) {
            yaw += 90f;
        }
        return yaw;
    }
    private float getPitch(float pitch) {
        if (!(mc.screen instanceof ChatScreen)) {
            if (mc.options.keyShift.isDown() && mc.options.keyJump.isDown()) {
                pitch = -3;
            } else if (mc.options.keyJump.isDown()) {
                if (isMoving()) {
                    pitch = -45;
                } else {
                    pitch = -90;
                }
            } else if (mc.options.keyShift.isDown()) {
                if (isMoving()) {
                    pitch = 45;
                } else {
                    pitch = 90;
                }
            }
            if (isMoving() && !mc.options.keyShift.isDown() && !mc.options.keyJump.isDown() && !mc.options.keyUp.isDown()) {
                pitch = horizontalNoGravity.get() ? 0 : -1.9f;
            }
        }
        return pitch;
    }
}

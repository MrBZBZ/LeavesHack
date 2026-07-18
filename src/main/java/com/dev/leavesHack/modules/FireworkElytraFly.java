package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.asm.accessors.IClientWorld;
import com.dev.leavesHack.asm.accessors.IVec3d;
import com.dev.leavesHack.events.ElytraUpdateEvent;
import com.dev.leavesHack.events.KeyboardInputEvent;
import com.dev.leavesHack.events.TravelEvent;
import com.dev.leavesHack.utils.entity.EntityUtil;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.dev.leavesHack.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;

import java.util.TimerTask;

import static com.dev.leavesHack.utils.entity.InventoryUtil.sendPacket;
import static com.dev.leavesHack.utils.rotation.Rotation.*;

public class FireworkElytraFly extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    public final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("Mode")
            .description("运行模式(Legit合法，GrimDurability甲飞)")
            .defaultValue(Mode.Legit)
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
            .description("烟花使用模式(Delay延迟放，Auto自动放)")
            .defaultValue(FireWorkMode.Delay)
            .build()
    );
    private final Setting<Double> packetDealy = sgGeneral.add(new DoubleSetting.Builder()
            .name("PacketDelay")
            .description("发包延迟tick数")
            .defaultValue(3)
            .sliderMax(100)
            .build()
    );
    public final Setting<Boolean> unbreaking = sgGeneral.add(new BoolSetting.Builder()
            .name("Unbreaking")
            .description("无限耐久")
            .description("")
            .defaultValue(true)
            .build()
    );
    private final Setting<Double> fakeDelay = sgGeneral.add(new DoubleSetting.Builder()
            .name("FakeDelay")
            .description("无限耐久操作延迟")
            .defaultValue(800)
            .sliderMax(1000)
            .build()
    );
    public final Setting<Boolean> stand = sgGeneral.add(new BoolSetting.Builder()
            .name("Stand")
            .description("站飞")
            .description("")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> releaseSneak = sgGeneral.add(new BoolSetting.Builder()
            .name("ReleaseSneak")
            .description("自动shift")
            .description("")
            .defaultValue(true)
            .build()
    );
    public final Setting<Boolean> pressSneak = sgGeneral.add(new BoolSetting.Builder()
            .name("PressSneak")
            .description("自动shift")
            .description("")
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
    private final Setting<Double> fallSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("FallSpeed")
            .description("下落速度")
            .defaultValue(0.02)
            .sliderRange(0.0, 3.0)
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
        super(LeavesHack.CATEGORY, "FireworkElytraFly", "烟花鞘翅飞行");
        INSTANCE = this;
    }
    public float yaw = rotationYaw;
    public float pitch = rotationPitch;
    public boolean isUsingFirework = false;
    private final Timer fireworkTimer = new Timer();
    private final Timer swapTimer = new Timer();
    private final Timer spearTimer = new Timer();
    public boolean isFallFlying = false;
    public int packetDelayInt = 0;
    public int spearDelayInt = 0;
    public boolean shouldJump = false, shouldRestore = false;
    public PlayerInput bypassInput = null;
    public boolean hasSpear = false;
    @Override
    public void onActivate() {
        hasSpear = false;
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
            mc.player.setSneaking(true);
        }
        if (releaseSneak.get()) {
            long delay = releaseDelay.get();
            java.util.Timer timer = new java.util.Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mc.execute(() -> {
                        mc.player.setSneaking(true);
                    });
                }
            }, delay);
        }
    }
    @EventHandler
    public void onTickPost(TickEvent.Post event) {
        shouldJump = false;
        if (!shouldRestore) return;
        mc.player.input.playerInput = bypassInput;
        bypassInput = null;
        shouldRestore = false;
    }
    @EventHandler
    public void onTravel(TravelEvent event) {
        if (!isFallFlying) return;
        if (mode.get() != Mode.GrimDurability) return;
        if (!control.get()) return;
        if (mc.currentScreen instanceof ChatScreen) {
            setY(fallSpeed.get());
            return;
        }
        if (!wantToMove()) {
            setX(0);
            setZ(0);
            setY(fallSpeed.get());
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
        if (mc.player == null || mc.world == null) return null;
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
        if (mc.currentScreen != null && deBug.get()) info("screen" + mc.currentScreen.getTitle() + " " + mc.currentScreen.getClass().getSimpleName() + " " + mc.currentScreen.getClass().getSuperclass().getSimpleName() + " " + mc.currentScreen.getTitle());
        if (mc.currentScreen != null && mc.currentScreen instanceof HandledScreen<?> && !(mc.currentScreen instanceof InventoryScreen || mc.currentScreen instanceof CreativeInventoryScreen)) return;
        yaw = getSprintYaw(mc.player.getYaw());
        pitch = getPitch(mc.player.getPitch());
//        if (mode.get() == Mode.AutoSpear && wantToMove()) {
//            Rotation.snapAt(mc.player.getYaw(), -45);
//        }
        if (deBug.get()) info("Yaw: " + yaw + " Pitch: " + pitch);
        syncInput();
        if (control.get()) {
            if (GlobalSetting.INSTANCE.moveFix.get()) {
                Rotation.snapAt(yaw, pitch);
            } else {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision));
            }
        }
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
        ItemStack chestStack = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        boolean wearingElytra = chest.isOf(Items.ELYTRA) && chest.isDamageable() && chest.getDamage() < chest.getMaxDamage();
        if (wearingElytra && !isFallFlying && !mc.player.isOnGround() && mode.get() != Mode.AutoSpear) {
            shouldJump = true;
            sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.startGliding();
        }
        if (mode.get() == Mode.Legit && wearingElytra && isFallFlying && !mc.player.isOnGround() && unbreaking.get() && swapTimer.passedMs(fakeDelay.get())) {
            shouldJump = true;
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
//            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            mc.player.startGliding();
            swapTimer.reset();
        }
        if (mode.get() == Mode.GrimDurability) {
            if (elytra != -1 && packetDelayInt > packetDealy.get()) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytra, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytra, 0, SlotActionType.PICKUP, mc.player);
                if (!mc.player.isOnGround()) {
                    shouldJump = true;
                    sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                    mc.player.startGliding();
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
            ItemStack stack = mc.player.getInventory().getStack(mc.player.getInventory().getSelectedSlot());
            if (stack.isIn(ItemTags.SPEARS) && spearTimer.passedMs(spearDelay.get()) && wantToMove()) {
                mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STAB, BlockPos.ORIGIN, Direction.DOWN));
                EntityUtil.attackSwingHand();
                spearTimer.reset();
            }
            if (elytra != -1 && packetDelayInt > packetDealy.get()) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytra, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytra, 0, SlotActionType.PICKUP, mc.player);
                if (!mc.player.isOnGround()) {
                    shouldJump = true;
                    sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                    mc.player.startGliding();
                }
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytra, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 6, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, elytra, 0, SlotActionType.PICKUP, mc.player);
                packetDelayInt = 0;
            }
        }
    }
    public void syncInput() {
        if (shouldRestore) return;
        bypassInput = mc.player.input.playerInput;
        mc.player.input.playerInput = new PlayerInput(false, false, false, false, false, false, false);
        mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, false)));
        mc.player.lastPlayerInput = new PlayerInput(false, false, false, false, false, false, false);
        shouldRestore = true;
    }
    private int findSpear() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isIn(ItemTags.SPEARS)) {
                return i;
            }
        }
        return -1;
    }
    private int findChestplate() {
        int bestSlot = -1;
        int bestScore = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            // 检查是否为可装备物品或鞘翅
            if (!stack.contains(net.minecraft.component.DataComponentTypes.EQUIPPABLE) && stack.getItem() != Items.ELYTRA) continue;
            // 获取胸甲槽位类型
            EquipmentSlot slot;
            if (stack.getItem() == Items.ELYTRA) {
                slot = EquipmentSlot.CHEST;
            } else {
                slot = stack.get(net.minecraft.component.DataComponentTypes.EQUIPPABLE).slot();
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
            if (!stack.isDamageable() || stack.getDamage() >= stack.getMaxDamage()) return -1;
            return 10000 + (stack.getMaxDamage() - stack.getDamage()); // 剩余耐久越高越好
        }
        if (!stack.contains(net.minecraft.component.DataComponentTypes.EQUIPPABLE)) return -1;
        int score = 0;
        // 基础护甲值
        if (stack.contains(net.minecraft.component.DataComponentTypes.ATTRIBUTE_MODIFIERS)) {
            net.minecraft.component.type.AttributeModifiersComponent component = stack.get(net.minecraft.component.DataComponentTypes.ATTRIBUTE_MODIFIERS);
            for (net.minecraft.component.type.AttributeModifiersComponent.Entry modifier : component.modifiers()) {
                if (modifier.attribute() == net.minecraft.entity.attribute.EntityAttributes.ARMOR) {
                    score += (int) modifier.modifier().value();
                }
            }
        }
        // 保护附魔
        if (stack.hasEnchantments()) {
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
        if (!fireworkTimer.passedMs(delay.get()) && fireWorkMode.get() == FireWorkMode.Delay) return;
        int firework;
        if (mc.player.getMainHandStack().getItem() == Items.FIREWORK_ROCKET) {
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            fireworkTimer.reset();
        } else if (mc.player.getOffHandStack().getItem() == Items.FIREWORK_ROCKET) {
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            fireworkTimer.reset();
        } else if (inventorySwap.get() && (firework = InventoryUtil.findItemInventorySlot(Items.FIREWORK_ROCKET)) != -1) {
            InventoryUtil.inventorySwap(firework, mc.player.getInventory().getSelectedSlot());
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            InventoryUtil.inventorySwap(firework, mc.player.getInventory().getSelectedSlot());
            sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
            fireworkTimer.reset();
        } else if ((firework = InventoryUtil.findItem(Items.FIREWORK_ROCKET)) != -1) {
            int old = mc.player.getInventory().getSelectedSlot();
            InventoryUtil.switchToSlot(firework);
            sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            InventoryUtil.switchToSlot(old);
            fireworkTimer.reset();
        }
    }
    public void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        if (mc.getNetworkHandler() == null || mc.world == null) return;
        try (PendingUpdateManager pendingUpdateManager = ((IClientWorld) mc.world).invokeGetPendingUpdateManager().incrementSequence()) {
            int i = pendingUpdateManager.getSequence();
            mc.getNetworkHandler().sendPacket(packetCreator.predict(i));
        }
    }
    private boolean wantToMove() {
        return mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed() || mc.options.jumpKey.isPressed() || mc.options.sneakKey.isPressed();
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
        if (mc.player == null || mc.player.input == null) return false;
        Vec2f mv = mc.player.input.getMovementInput();
        return mv.x != 0.0F || mv.y != 0.0F;
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

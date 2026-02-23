package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;

public class PacketMine extends Module {

    public PacketMine() {
        super(LeavesHack.CATEGORY, "PacketMine+", "PacketMine for grim");
    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> silentSwitch = sgGeneral.add(
            new BoolSetting.Builder()
                    .name("SilentSwitch")
                    .defaultValue(true)
                    .build()
    );
    private final Setting<Boolean> instantMine = sgGeneral.add(
            new BoolSetting.Builder()
                    .name("InstantMine")
                    .defaultValue(true)
                    .build()
    );
    private final Setting<Boolean> checkGround = sgGeneral.add(
            new BoolSetting.Builder()
                    .name("CheckGround")
                    .defaultValue(true)
                    .build()
    );
    private final Setting<Boolean> bypassGround = sgGeneral.add(
            new BoolSetting.Builder()
                    .name("BypassGround")
                    .defaultValue(true)
                    .build()
    );
    private final Setting<Integer> switchTime = sgGeneral.add(
            new IntSetting.Builder()
                    .name("SwitchTime")
                    .defaultValue(100)
                    .min(0)
                    .sliderMax(1000)
                    .build()
    );
    private final Setting<Integer> mineDelay = sgGeneral.add(
            new IntSetting.Builder()
                    .name("MineDelay")
                    .defaultValue(0)
                    .min(0)
                    .sliderMax(1000)
                    .build()
    );
    private final Setting<Double> mineDamage = sgGeneral.add(
            new DoubleSetting.Builder()
                    .name("Damage")
                    .defaultValue(1.38)
                    .sliderMax(2.0)
                    .build()
    );

    private final Setting<Double> animationExp = sgRender.add(
            new DoubleSetting.Builder()
                    .name("Animation Exponent")
                    .defaultValue(3)
                    .range(0, 10)
                    .sliderRange(0, 10)
                    .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(
            new EnumSetting.Builder<ShapeMode>()
                    .name("Shape Mode")
                    .defaultValue(ShapeMode.Both)
                    .build()
    );

    private final Setting<SettingColor> sideStartColor = sgRender.add(
            new ColorSetting.Builder()
                    .name("Side Start")
                    .defaultValue(new SettingColor(255, 255, 255, 0))
                    .build()
    );

    private final Setting<SettingColor> sideEndColor = sgRender.add(
            new ColorSetting.Builder()
                    .name("Side End")
                    .defaultValue(new SettingColor(255, 255, 255, 50))
                    .build()
    );

    private final Setting<SettingColor> lineStartColor = sgRender.add(
            new ColorSetting.Builder()
                    .name("Line Start")
                    .defaultValue(new SettingColor(255, 255, 255, 0))
                    .build()
    );

    private final Setting<SettingColor> lineEndColor = sgRender.add(
            new ColorSetting.Builder()
                    .name("Line End")
                    .defaultValue(new SettingColor(255, 255, 255, 255))
                    .build()
    );
    private static boolean completed = false;
    public static BlockPos lastPos;
    public static BlockPos targetPos;
    private static float progress;
    private long lastTime;
    private static boolean started;
    private double render = 1;
    private int oldSlot = -1;
    private Timer timer = new Timer();
    private Timer mineTimer = new Timer();
    private boolean hasSwitch = false;

    @Override
    public void onActivate() {
        hasSwitch = false;
        mineTimer.setMs(999999);
        timer.setMs(999999);
        targetPos = null;
        started = false;
        progress = 0;
        lastTime = System.currentTimeMillis();
        render = 1;
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (!mineTimer.passedMs(mineDelay.get())) return;
        if (!BlockUtils.canBreak(event.blockPos)) return;
        event.cancel();
        if (targetPos == null || !targetPos.equals(event.blockPos)) {
            mineTimer.reset();
            mine(event.blockPos);
        }
    }

    public static void mine(BlockPos pos) {
        completed = false;
        targetPos = pos;
        lastPos = targetPos;
        started = false;
        progress = 0;
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;
        if (timer.passedMs(switchTime.get()) && hasSwitch) {
            InventoryUtil.switchToSlot(oldSlot);
            hasSwitch = false;
        }
        if (targetPos == null) return;
        double max = getMineTicks(getTool(targetPos));
        if (instantMine.get() && completed) {
            Color side = getColor(sideStartColor.get(), sideEndColor.get(), 1);
            Color line = getColor(lineStartColor.get(), lineEndColor.get(), 1);
            event.renderer.box(new Box(targetPos), side, line, shapeMode.get(), 0);
            if (!mc.world.isAir(targetPos) && !mc.world.getBlockState(targetPos).isReplaceable()) {
                if (bypassGround.get() && !mc.player.isFallFlying() && targetPos != null && !isAir(targetPos)){
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY() + 1.0e-9,
                            mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), true));
                    mc.player.onLanding();
                }
                sendStop();
            }
            return;
        }
        double delta = (System.currentTimeMillis() - lastTime) / 1000d;
        lastTime = System.currentTimeMillis();
        if (!started) {
            sendStart();
            return;
        }
        Double damage = mineDamage.get();
        if (!checkGround.get() || mc.player.isOnGround()) {
            progress += delta * 20;
        } else if (checkGround.get() && !mc.player.isOnGround()){
            progress += delta * 4;
        }
        renderAnimation(event, delta, damage);
        if (progress >= max * damage) {
            if (bypassGround.get() && !mc.player.isFallFlying() && targetPos != null && !isAir(targetPos)){
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY() + 1.0e-9,
                        mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), true));
                mc.player.onLanding();
            }
            sendStop();
            completed = true;
            if (!instantMine.get()) targetPos = null;
        }
    }

    private void sendStart() {
        sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, targetPos, BlockUtil.getClickSide(targetPos), id));
        mc.player.swingHand(Hand.MAIN_HAND);
        started = true;
        progress = 0;
    }

    private void sendStop() {
        int bestSlot = getTool(targetPos);
        if (!hasSwitch) oldSlot = mc.player.getInventory().selectedSlot;
        if (silentSwitch.get() && bestSlot != -1) {
            InventoryUtil.switchToSlot(bestSlot);
            timer.reset();
            hasSwitch = true;
        }
        mc.player.swingHand(Hand.MAIN_HAND);
        sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, targetPos, BlockUtil.getClickSide(targetPos), id));
    }
    private boolean isAir(BlockPos breakPos) {
        return mc.world.isAir(breakPos) || BlockUtil.getBlock(breakPos) == Blocks.FIRE && BlockUtil.hasCrystal(breakPos);
    }
    private float getMineTicks(int slot) {
        if (targetPos == null || mc.world == null || mc.player == null) return 20;
        BlockState state = mc.world.getBlockState(targetPos);
        float hardness = state.getHardness(mc.world, targetPos);
        if (hardness < 0) return Float.MAX_VALUE;
        if (hardness == 0) return 1;
        ItemStack stack = slot == -1
                ? ItemStack.EMPTY
                : mc.player.getInventory().getStack(slot);
        boolean canHarvest = stack.isSuitableFor(state);
        float speed = stack.getMiningSpeedMultiplier(state);
        int efficiency = InventoryUtil.getEnchantmentLevel(stack, Enchantments.EFFICIENCY);
        if (efficiency > 0 && !stack.isEmpty()) {
            speed += efficiency * efficiency + 1;
        }
        if (mc.player.hasStatusEffect(StatusEffects.HASTE)) {
            int amp = mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier();
            speed *= 1.0f + (amp + 1) * 0.2f;
        }
        if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            int amp = mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier();
            speed *= switch (amp) {
                case 0 -> 0.3f;
                case 1 -> 0.09f;
                case 2 -> 0.0027f;
                default -> 0.00081f;
            };
        }
        float damage = speed / hardness / (canHarvest ? 30f : 100f);
        if (damage <= 0) return Float.MAX_VALUE;
        return 1f / damage;
    }

    private void renderAnimation(Render3DEvent event, double delta, double damage) {
        render = MathHelper.clamp(render + delta * 2, -2, 2);
        double max = getMineTicks(getTool(targetPos));
        double p = 1 - MathHelper.clamp(progress / (max * damage), 0, 1);
        p = Math.pow(p, animationExp.get());
        p = 1 - p;

        double size = p / 2;
        Box box = new Box(
                targetPos.getX() + 0.5 - size,
                targetPos.getY() + 0.5 - size,
                targetPos.getZ() + 0.5 - size,
                targetPos.getX() + 0.5 + size,
                targetPos.getY() + 0.5 + size,
                targetPos.getZ() + 0.5 + size
        );

        Color side = getColor(sideStartColor.get(), sideEndColor.get(), p);
        Color line = getColor(lineStartColor.get(), lineEndColor.get(), p);

        event.renderer.box(box, side, line, shapeMode.get(), 0);
    }

    private Color getColor(Color start, Color end, double progress) {
        return new Color(
                lerp(start.r, end.r, progress),
                lerp(start.g, end.g, progress),
                lerp(start.b, end.b, progress),
                lerp(start.a, end.a, progress)
        );
    }

    private int lerp(double start, double end, double d) {
        return (int) Math.round(start + (end - start) * d);
    }
    private int getTool(BlockPos pos) {
        int index = -1;
        float CurrentFastest = 1.0f;
        for (int i = 0; i < 9; ++i) {
            final ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack != ItemStack.EMPTY) {
                final float digSpeed = InventoryUtil.getEnchantmentLevel(stack, Enchantments.EFFICIENCY);
                final float destroySpeed = stack.getMiningSpeedMultiplier(mc.world.getBlockState(pos));
                if (digSpeed + destroySpeed > CurrentFastest) {
                    CurrentFastest = digSpeed + destroySpeed;
                    index = i;
                }
            }
        }
        return index;
    }
    public void sendSequencedPacket(SequencedPacketCreator packetCreator) {
        if (mc.getNetworkHandler() == null || mc.world == null) return;
        try (PendingUpdateManager pendingUpdateManager = mc.world.getPendingUpdateManager().incrementSequence()) {
            int i = pendingUpdateManager.getSequence();
            mc.getNetworkHandler().sendPacket(packetCreator.predict(i));
        }
    }
}
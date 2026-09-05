package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.asm.accessors.IClientWorld;
import com.dev.leavesHack.utils.entity.EntityUtil;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.entity.InventoryUtil.MineSwitchMode;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.world.BlockPosX;
import com.dev.leavesHack.utils.world.BlockUtil;
import java.util.TimerTask;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import static com.dev.leavesHack.utils.entity.InventoryUtil.sendPacket;

public class PacketMine extends Module {
    public static PacketMine INSTANCE;
    public PacketMine() {
        super(LeavesHack.LEAVES_COMBAT, "PacketMine+", "数据包挖掘");
        INSTANCE = this;
    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Boolean> usingPause = sgGeneral.add(new BoolSetting.Builder()
            .name("UsingPause")
            .description("使用暂停")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> onlyMain = sgGeneral.add(new BoolSetting.Builder()
            .name("OnlyMain")
            .description("仅检查主手")
            .defaultValue(true)
            .visible(usingPause::get)
            .build()
    );
    public final Setting<MineSwitchMode> autoSwitch = sgGeneral.add(new EnumSetting.Builder<MineSwitchMode>()
            .name("AutoSwitch")
            .description("自动切镐")
            .defaultValue(MineSwitchMode.Silent)
            .build()
    );
    public final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder()
            .name("Range")
            .description("操作距离")
            .defaultValue(6)
            .min(0)
            .sliderMax(12)
            .build()
    );
    public final Setting<Integer> maxBreaks = sgGeneral.add(new IntSetting.Builder()
            .name("TryBreakTime")
            .description("最大尝试挖掘次数")
            .defaultValue(6)
            .min(0)
            .sliderMax(10)
            .build()
    );
    private final Setting<Boolean> farCancel = sgGeneral.add(new BoolSetting.Builder()
            .name("FarCancel")
            .description("过远取消")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
            .name("SwingHand")
            .description("挥手")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> instantMine = sgGeneral.add(new BoolSetting.Builder()
            .name("InstantMine")
            .description("秒挖")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> instantDelay = sgGeneral.add(new IntSetting.Builder()
            .name("InstantDelay")
            .description("秒挖延迟")
            .defaultValue(10)
            .min(0)
            .sliderMax(1000)
            .build()
    );
    private final Setting<Boolean> fastBypass = sgGeneral.add(new BoolSetting.Builder()
            .name("FastBypass")
            .description("快速挖掘绕过")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> doubleBreak = sgGeneral.add(new BoolSetting.Builder()
            .name("DoubleBreak")
            .description("双挖")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> checkGround = sgGeneral.add(new BoolSetting.Builder()
            .name("CheckGround")
            .description("检查是否在地面上")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> bypassGround = sgGeneral.add(new BoolSetting.Builder()
            .name("BypassGround")
            .description("滞空挖掘绕过")
            .defaultValue(false)
            .build()
    );
    private final Setting<Integer> switchDamage = sgGeneral.add(new IntSetting.Builder()
            .name("SwitchDamage")
            .description("自动切镐挖掘进度阈值")
            .defaultValue(95)
            .min(0)
            .sliderMax(100)
            .build()
    );
    private final Setting<Integer> switchTime = sgGeneral.add(new IntSetting.Builder()
            .name("SwitchTime")
            .description("持镐时间")
            .defaultValue(100)
            .min(0)
            .sliderMax(1000)
            .build()
    );
    public final Setting<Integer> mineDelay = sgGeneral.add(new IntSetting.Builder()
            .name("MineDelay")
            .description("挖掘选择延迟")
            .defaultValue(300)
            .min(0)
            .sliderMax(1000)
            .build()
    );
    private final Setting<Integer> packetDelay = sgGeneral.add(new IntSetting.Builder()
            .name("PacketDelay")
            .description("绕过包发送延迟")
            .defaultValue(0)
            .min(0)
            .sliderMax(1000)
            .build()
    );
    private final Setting<Double> mineDamage = sgGeneral.add(new DoubleSetting.Builder()
            .name("Damage")
            .description("总挖掘进度设置")
            .defaultValue(0.8)
            .sliderMax(2.0)
            .build()
    );
    private final Setting<Double> animationExp = sgRender.add(new DoubleSetting.Builder()
            .name("Animation Exponent")
            .defaultValue(3)
            .range(0, 10)
            .sliderRange(0, 10)
            .build()
    );
//    private final Setting<Boolean> renderProgress = sgRender.add(new BoolSetting.Builder()
//            .name("RenderProgress")
//            .description("渲染进度")
//            .defaultValue(true)
//            .build()
//    );
//    private final Setting<SettingColor> targetColor = sgRender.add(new ColorSetting.Builder()
//            .name("TargetColor")
//            .description("主挖文本颜色")
//            .defaultValue(new SettingColor(255, 255, 255, 50))
//            .build()
//    );
//    private final Setting<SettingColor> secondColor = sgRender.add(new ColorSetting.Builder()
//            .name("SecondColor")
//            .description("副挖文本颜色")
//            .defaultValue(new SettingColor(255, 255, 255, 50))
//            .build()
//    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("ShapeMode")
            .defaultValue(ShapeMode.Both)
            .build()
    );
    private final Setting<SettingColor> sideStartColor = sgRender.add(new ColorSetting.Builder()
            .name("SideStart")
            .defaultValue(new SettingColor(255, 255, 255, 0))
            .build()
    );

    private final Setting<SettingColor> sideEndColor = sgRender.add(new ColorSetting.Builder()
            .name("SideEnd")
            .defaultValue(new SettingColor(255, 255, 255, 50))
            .build()
    );

    private final Setting<SettingColor> lineStartColor = sgRender.add(new ColorSetting.Builder()
            .name("LineStart")
            .defaultValue(new SettingColor(255, 255, 255, 0))
            .build()
    );

    private final Setting<SettingColor> lineEndColor = sgRender.add(new ColorSetting.Builder()
            .name("LineEnd")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
    );
    private final Setting<SettingColor> secondSideStartColor = sgRender.add(new ColorSetting.Builder()
            .name("SecondSideStart")
            .defaultValue(new SettingColor(255, 255, 255, 0))
            .build()
    );

    private final Setting<SettingColor> secondSideEndColor = sgRender.add(new ColorSetting.Builder()
            .name("SecondSideEnd")
            .defaultValue(new SettingColor(255, 255, 255, 50))
            .build()
    );

    private final Setting<SettingColor> secondLineStartColor = sgRender.add(new ColorSetting.Builder()
            .name("SecondLineStart")
            .defaultValue(new SettingColor(255, 255, 255, 0))
            .build()
    );

    private final Setting<SettingColor> secondLineEndColor = sgRender.add(new ColorSetting.Builder()
            .name("SecondLineEnd")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
    );
    public static BlockPos selfClickPos = null;
    public static int maxBreaksCount;
    public static int publicProgress = 0, secondPublicProgress = 0;
    public static boolean completed = false;
    public static BlockPos targetPos,secondPos;
    private static float progress,secondProgress;
    private long lastTime,secondLastTime;
    private static boolean started, secondStarted;
    private double render = 1, secondRender = 1;
    private int oldSlot = -1;
    private final Timer bypassTimer = new Timer();
    private final Timer timer = new Timer();
    private final Timer secondTimer = new Timer();
    public final Timer mineTimer = new Timer();
    private final Timer instantTimer = new Timer();
    private boolean hasSwitch = false, secondHasSwitch = false;

    @Override
    public void onActivate() {
        maxBreaksCount = 0;
        hasSwitch = false;
        secondHasSwitch = false;
        bypassTimer.setMs(999999);
        mineTimer.setMs(999999);
        instantTimer.setMs(999999);
        timer.setMs(999999);
        secondTimer.setMs(999999);
        targetPos = null;
        secondPos = null;
        started = false;
        secondStarted = false;
        publicProgress = 0;
        secondPublicProgress = 0;
        progress = 0;
        secondProgress = 0;
        lastTime = System.currentTimeMillis();
        secondLastTime = System.currentTimeMillis();
        render = 1;
    }
    @Override
    public void onDeactivate() {
        if (hasSwitch) {
            InventoryUtil.switchToSlot(oldSlot);
            hasSwitch = false;
        }
        if (secondHasSwitch) {
            InventoryUtil.switchToSlot(oldSlot);
            secondHasSwitch = false;
        }
    }
    @EventHandler
    public void onBreak(PacketEvent.Send event) {
        if (event.packet instanceof ServerboundPlayerActionPacket packet) {
            if (packet.getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) mineTimer.reset();
        }
    }
    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (!BlockUtils.canBreak(event.blockPos)) return;
        event.cancel();
        if (mineDelay.get() > 0 && !mineTimer.passedMs(mineDelay.get()) && completed) {
            targetPos = null;
            publicProgress = 0;
            started = false;
            progress = 0;
            completed = false;
            return;
        }
        if (!mineTimer.passedMs(mineDelay.get())) return;
        selfClickPos = event.blockPos;
        mine(event.blockPos);
    }
    public void mine(BlockPos pos) {
        if (AutoCity.INSTANCE.isActive() && AutoCity.INSTANCE.delay.get() && !mineTimer.passedMs(mineDelay.get())) return;
        mineTimer.reset();
        maxBreaksCount = 0;
        if (doubleBreak.get()) {
            if (targetPos != null && secondPos == null && !targetPos.equals(pos)) {
                if (completed) {
                    targetPos = pos;
                    secondStarted = false;
                    secondProgress = 0;
                    secondPublicProgress = 0;
                    publicProgress = 0;
                    started = false;
                    progress = 0;
                    completed = false;
                } else {
                    secondPos = targetPos;
                    targetPos = pos;
                    secondStarted = false;
                    secondProgress = 0;
                    secondPublicProgress = 0;
                    started = false;
                }
            } else if (targetPos == null || !targetPos.equals(pos)){
                publicProgress = 0;
                targetPos = pos;
                started = false;
                progress = 0;
                completed = false;
            }
        } else {
            if (!pos.equals(targetPos)) {
                publicProgress = 0;
                targetPos = pos;
                started = false;
                progress = 0;
                completed = false;
            }
        }
        mineTimer.reset();
    }
    @Override
    public String getInfoString() {
        if (targetPos == null) return null;
        double max = getMineTicks(getTool(targetPos));
        if (progress >= max * mineDamage.get()) return "[100%]";
        return "[" + publicProgress + "%]";
    }
//    @EventHandler
//    private void onMyRender(RenderLeaves3DEvent event) {
//        if (!renderProgress.get()) return;
//        if (targetPos != null) {
//            Render3DUtil.renderText3D(completed ? "Done" : publicProgress + "%", targetPos.toCenterPos(), targetColor.get().getPacked());
//        }
//        if (secondPos != null) {
//            Render3DUtil.renderText3D(secondPublicProgress + "%", secondPos.toCenterPos(), secondColor.get().getPacked());
//        }
//    }
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.level == null || mc.player == null) return;
        if ((targetPos == null || completed) && secondPos == null) selfClickPos = null;
        if (publicProgress >= 100) {
            if (!instantMine.get()) targetPos = null;
        }
        if (secondPublicProgress >= 100) {
            secondPos = null;
        }
        if (timer.passedMs(switchTime.get()) && hasSwitch && autoSwitch.get() != MineSwitchMode.None) {
            if (autoSwitch.get() == MineSwitchMode.Delay) InventoryUtil.switchToSlot(oldSlot);
            if (autoSwitch.get() == MineSwitchMode.Silent) sendPacket(new ServerboundSetCarriedItemPacket(oldSlot));
            hasSwitch = false;
        }
        if (maxBreaksCount >= maxBreaks.get() * 10) {
            maxBreaksCount = 0;
            targetPos = null;
        }
        if (secondPos != null && doubleBreak.get()) {
            if (farCancel.get() && Math.sqrt(mc.player.getEyePosition().distanceToSqr(secondPos.getCenter())) > range.get()){
                secondPos = null;
                return;
            }
            double secondMax = getMineTicks2(getTool(secondPos));
            double secondDelta = (System.currentTimeMillis() - secondLastTime) / 1000d;
            secondPublicProgress = (int) (secondProgress / (secondMax * mineDamage.get()) * 100);
            secondLastTime = System.currentTimeMillis();
            if (!secondStarted) {
                sendStart(secondPos);
                secondStarted = true;
                secondProgress = 0;
                return;
            }
            Double secondDamage = mineDamage.get();
            if (!checkGround.get() || mc.player.onGround()) {
                secondProgress += secondDelta * 20;
            } else if (checkGround.get() && !mc.player.onGround()){
                secondProgress += secondDelta * 4;
            }
            renderSecondAnimation(event, secondDelta, secondDamage);
            if (secondProgress >= secondMax * secondDamage) {
                sendStopSecond();
//                selfClickPos = null;
//                secondCompleted = true;
//                secondPos = null;
            }
        }
        if (doubleBreak.get()) {
            if (!usingPause.get() || !checkPause(onlyMain.get())) {
                if ((secondPublicProgress >= switchDamage.get() || publicProgress >= switchDamage.get())&& !hasSwitch && secondPos != null) {
                    int bestSlot = getTool(secondPos);
                    if (!hasSwitch) oldSlot = mc.player.getInventory().getSelectedSlot();
                    if (autoSwitch.get() != MineSwitchMode.None && bestSlot != -1) {
                        if (autoSwitch.get() == MineSwitchMode.Delay) InventoryUtil.switchToSlot(bestSlot);
                        if (autoSwitch.get() == MineSwitchMode.Silent) sendPacket(new ServerboundSetCarriedItemPacket(bestSlot));
                        timer.reset();
                        hasSwitch = true;
                    }
                }
            }
        }
        if (targetPos != null) {
            if (farCancel.get() && Math.sqrt(mc.player.getEyePosition().distanceToSqr(targetPos.getCenter())) > range.get()){
                targetPos = null;
                return;
            }
            double max = getMineTicks(getTool(targetPos));
            publicProgress = (int) (progress / (max * mineDamage.get()) * 100);
            if (progress >= max * mineDamage.get() && completed) {
                if (isAir(targetPos) || mc.level.getBlockState(targetPos).canBeReplaced()) maxBreaksCount = 0;
                if (!isAir(targetPos) && !mc.level.getBlockState(targetPos).canBeReplaced() && !(usingPause.get() && checkPause(onlyMain.get())))
                    maxBreaksCount++;
            }
            if (instantMine.get() && completed) {
                Color side = getColor(sideStartColor.get(), sideEndColor.get(), 1);
                Color line = getColor(lineStartColor.get(), lineEndColor.get(), 1);
                event.renderer.box(new AABB(targetPos), side, line, shapeMode.get(), 0);
                if (!mc.level.isEmptyBlock(targetPos) && !mc.level.getBlockState(targetPos).canBeReplaced() && instantTimer.passedMs(instantDelay.get())) {
                    sendStop();
                    instantTimer.reset();
                }
                return;
            }
            double delta = (System.currentTimeMillis() - lastTime) / 1000d;
            lastTime = System.currentTimeMillis();
            if (!started) {
                sendStart(targetPos);
                return;
            }
            Double damage = mineDamage.get();
            if (!checkGround.get() || mc.player.onGround()) {
                progress += delta * 20;
            } else if (checkGround.get() && !mc.player.onGround()) {
                progress += delta * 4;
            }
            renderAnimation(event, delta, damage);
            if (progress >= max * damage) {
                sendStop();
                completed = true;
                if (!instantMine.get() && secondPos == null) targetPos = null;
            }
        }
    }

    private void sendStart(BlockPos pos) {
        sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, BlockUtil.getClickSide(pos)));
        if (fastBypass.get()) {
            BlockPos bypassPos = new BlockPosX(mc.player.getX(), 321, mc.player.getZ());
            sendSequencedPacket(id -> new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, bypassPos, Direction.DOWN, id));
        }
        if (doubleBreak.get()) {
            long delay = packetDelay.get();
            java.util.Timer timer = new java.util.Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mc.execute(() -> {
                        sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, BlockUtil.getClickSide(pos)));
                    });
                    timer.cancel();
                }
            }, delay);
        }
        mc.player.swing(InteractionHand.MAIN_HAND);
        if (pos.equals(targetPos)) {
            started = true;
            progress = 0;
        } else {
            secondStarted = true;
            secondProgress = 0;
        }
    }

    private void sendStop() {
        if (usingPause.get() && checkPause(onlyMain.get())) {
            return;
        }
        if (!doubleBreak.get() || secondPos == null) {
            int bestSlot = getTool(targetPos);
            if (!hasSwitch) oldSlot = mc.player.getInventory().getSelectedSlot();
            if (autoSwitch.get() != MineSwitchMode.None && bestSlot != -1) {
                if (autoSwitch.get() == MineSwitchMode.Delay) InventoryUtil.switchToSlot(bestSlot);
                if (autoSwitch.get() == MineSwitchMode.Silent) sendPacket(new ServerboundSetCarriedItemPacket(bestSlot));
                timer.reset();
                hasSwitch = true;
            }
        }
        if (bypassGround.get() && !mc.player.isFallFlying() && targetPos != null && !isAir(targetPos) && !mc.player.onGround()){
            mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(mc.player.getX(), mc.player.getY() + 1.0e-9, mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), true, mc.player.horizontalCollision));
            mc.player.resetFallDistance();
        }
        if (swing.get()) EntityUtil.attackSwingHand();
        sendSequencedPacket(id -> new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, targetPos, BlockUtil.getClickSide(targetPos), id));
    }
    private void sendStopSecond() {
        if (bypassGround.get() && !mc.player.isFallFlying() && secondPos != null && !isAir(secondPos) && !mc.player.onGround()){
            mc.getConnection().send(new ServerboundMovePlayerPacket.PosRot(mc.player.getX(), mc.player.getY() + 1.0e-9, mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), true, mc.player.horizontalCollision));
            mc.player.resetFallDistance();
        }
        if (secondPos != null && !mc.level.isEmptyBlock(secondPos)) mc.level.setBlockAndUpdate(secondPos, Blocks.AIR.defaultBlockState());
        //sendSequencedPacket(id -> new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, secondPos, BlockUtil.getClickSide(secondPos), id));
    }
    private boolean isAir(BlockPos breakPos) {
        return mc.level.isEmptyBlock(breakPos) || BlockUtil.getBlock(breakPos) == Blocks.FIRE && BlockUtil.hasCrystal(breakPos);
    }
    private float getMineTicks(int slot) {
        if (targetPos == null || mc.level == null || mc.player == null) return 20;
        BlockState state = mc.level.getBlockState(targetPos);
        float hardness = state.getDestroySpeed(mc.level, targetPos);
        if (hardness < 0) return Float.MAX_VALUE;
        if (hardness == 0) return 1;
        ItemStack stack = slot == -1
                ? ItemStack.EMPTY
                : mc.player.getInventory().getItem(slot);
        boolean canHarvest = stack.isCorrectToolForDrops(state);
        float speed = stack.getDestroySpeed(state);
        int efficiency = InventoryUtil.getEnchantmentLevel(stack, Enchantments.EFFICIENCY);
        if (efficiency > 0 && speed > 1.0f) {
            speed += efficiency * efficiency + 1;
        }
        if (mc.player.hasEffect(MobEffects.HASTE)) {
            int amp = mc.player.getEffect(MobEffects.HASTE).getAmplifier();
            speed *= 1.0f + (amp + 1) * 0.2f;
        }
        if (mc.player.hasEffect(MobEffects.MINING_FATIGUE)) {
            int amp = mc.player.getEffect(MobEffects.MINING_FATIGUE).getAmplifier();
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
    private float getMineTicks2(int slot) {
        if (secondPos == null || mc.level == null || mc.player == null) return 20;
        BlockState state = mc.level.getBlockState(secondPos);
        float hardness = state.getDestroySpeed(mc.level, secondPos);
        if (hardness < 0) return Float.MAX_VALUE;
        if (hardness == 0) return 1;
        ItemStack stack = slot == -1
                ? ItemStack.EMPTY
                : mc.player.getInventory().getItem(slot);
        boolean canHarvest = stack.isCorrectToolForDrops(state);
        float speed = stack.getDestroySpeed(state);
        int efficiency = InventoryUtil.getEnchantmentLevel(stack, Enchantments.EFFICIENCY);
        if (efficiency > 0 && speed > 1.0f) {
            speed += efficiency * efficiency + 1;
        }
        if (mc.player.hasEffect(MobEffects.HASTE)) {
            int amp = mc.player.getEffect(MobEffects.HASTE).getAmplifier();
            speed *= 1.0f + (amp + 1) * 0.2f;
        }
        if (mc.player.hasEffect(MobEffects.MINING_FATIGUE)) {
            int amp = mc.player.getEffect(MobEffects.MINING_FATIGUE).getAmplifier();
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
        render = Mth.clamp(render + delta * 2, -2, 2);
        double max = getMineTicks(getTool(targetPos));
        double p = 1 - Mth.clamp(progress / (max * damage), 0, 1);
        p = Math.pow(p, animationExp.get());
        p = 1 - p;
        double size = p / 2;
        AABB box = new AABB(
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
    private void renderSecondAnimation(Render3DEvent event, double delta, double damage) {
        secondRender = Mth.clamp(secondRender + delta * 2, -2, 2);
        double max = getMineTicks2(getTool(secondPos));
        double p = 1 - Mth.clamp(secondProgress / (max * damage), 0, 1);
        p = Math.pow(p, animationExp.get());
        p = 1 - p;

        double size = p / 2;
        AABB box = new AABB(
                secondPos.getX() + 0.5 - size,
                secondPos.getY() + 0.5 - size,
                secondPos.getZ() + 0.5 - size,
                secondPos.getX() + 0.5 + size,
                secondPos.getY() + 0.5 + size,
                secondPos.getZ() + 0.5 + size
        );

        Color side = getColor(secondSideStartColor.get(), secondSideEndColor.get(), p);
        Color line = getColor(secondLineStartColor.get(), secondLineEndColor.get(), p);

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
            final ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack != ItemStack.EMPTY) {
                final float digSpeed = InventoryUtil.getEnchantmentLevel(stack, Enchantments.EFFICIENCY);
                final float destroySpeed = stack.getDestroySpeed(mc.level.getBlockState(pos));
                if (digSpeed + destroySpeed > CurrentFastest) {
                    CurrentFastest = digSpeed + destroySpeed;
                    index = i;
                }
            }
        }
        return index;
    }
    public void sendSequencedPacket(PredictiveAction packetCreator) {
        if (mc.getConnection() == null || mc.level == null) return;
        try (BlockStatePredictionHandler pendingUpdateManager = ((IClientWorld) mc.level).invokeGetPendingUpdateManager().startPredicting()) {
            int i = pendingUpdateManager.currentSequence();
            mc.getConnection().send(packetCreator.predict(i));
        }
    }
    public boolean checkPause(boolean onlyMain) {
        return mc.options.keyUse.isDown() && (!onlyMain || mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND);
    }
}

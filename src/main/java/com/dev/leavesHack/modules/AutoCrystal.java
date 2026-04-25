package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.events.RenderLeaves3DEvent;
import com.dev.leavesHack.utils.combat.CombatUtil;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.DamageUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.render.Render3DUtil;
import com.dev.leavesHack.utils.world.BlockPosX;
import com.dev.leavesHack.utils.world.BlockUtil;
import com.mojang.authlib.GameProfile;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.UUID;

public class AutoCrystal extends Module {
    public static AutoCrystal INSTANCE;
    public AutoCrystal() {
        super(LeavesHack.CATEGORY, "AutoCrystal", "自动水晶");
        INSTANCE = this;
    }
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("TargetRange")
            .description("目标距离")
            .defaultValue(12.0)
            .sliderRange(1, 20)
            .build()
    );
    private final Setting<Double> placeRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("PlaceRange")
            .description("放置距离")
            .defaultValue(4.5)
            .sliderRange(1, 6)
            .build()
    );
    private final Setting<Double> breakRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("BreakRange")
            .description("破坏距离")
            .defaultValue(4.5)
            .sliderRange(1, 6)
            .build()
    );
    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
            .name("PlaceDelay")
            .description("放置延迟")
            .defaultValue(50)
            .sliderRange(0, 500)
            .build()
    );
    private final Setting<Integer> breakDelay = sgGeneral.add(new IntSetting.Builder()
            .name("BreakDelay")
            .description("破坏延迟")
            .defaultValue(50)
            .sliderRange(0, 500)
            .build()
    );
    public final Setting<CalcMode> calcMode = sgGeneral.add(new EnumSetting.Builder<CalcMode>()
            .name("CalcMode")
            .description("计算模式")
            .defaultValue(CalcMode.AlienV4)
            .build()
    );
    private final Setting<Double> minDamage = sgGeneral.add(new DoubleSetting.Builder()
            .name("MinDamage")
            .description("最小伤害")
            .defaultValue(4.0)
            .sliderRange(1, 36)
            .build()
    );
    private final Setting<Double> maxSelfDmg = sgGeneral.add(new DoubleSetting.Builder()
            .name("MaxSelfDmg")
            .description("最大自伤")
            .defaultValue(12)
            .sliderRange(1, 36)
            .build()
    );
    private final Setting<Integer> predict = sgGeneral.add(new IntSetting.Builder()
            .name("Predict")
            .description("预判值")
            .defaultValue(4)
            .sliderRange(0, 12)
            .build()
    );
    public final Setting<PreferMode> preferMode = sgGeneral.add(new EnumSetting.Builder<PreferMode>()
            .name("PreferMode")
            .description("优先模式")
            .defaultValue(PreferMode.PreferAnchor)
            .build()
    );
    private final Setting<Boolean> autoBase = sgGeneral.add(new BoolSetting.Builder()
            .name("AutoBase")
            .description("自动底座")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> baseDelay = sgGeneral.add(new IntSetting.Builder()
            .name("BaseDelay")
            .description("底座延迟")
            .defaultValue(500)
            .sliderRange(0, 1000)
            .build()
    );
    private final Setting<Boolean> usingPause = sgGeneral.add(new BoolSetting.Builder()
            .name("UsingPause")
            .description("使用暂停")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> onlyMain = sgGeneral.add(new BoolSetting.Builder()
            .name("OnlyMain")
            .description("仅主手")
            .defaultValue(true)
            .visible(usingPause::get)
            .build()
    );
    private final Setting<Boolean> noSuicide = sgGeneral.add(new BoolSetting.Builder()
            .name("NoSuicide")
            .description("防止自杀")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> inventory = sgGeneral.add(new BoolSetting.Builder()
            .name("InventorySwap")
            .description("静默切换背包物品")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> placeAfterBreak = sgGeneral.add(new BoolSetting.Builder()
            .name("PlaceAfterBreak")
            .description("破坏后放置")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("Render")
            .description("渲染")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> renderDmg = sgRender.add(new BoolSetting.Builder()
            .name("RenderDmg")
            .description("渲染伤害")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> dmgColor = sgRender.add(new ColorSetting.Builder()
            .name("DamageColor")
            .description("伤害文本颜色")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
    );
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("渲染模式")
            .defaultValue(ShapeMode.Both)
            .build()
    );
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("框内颜色")
            .defaultValue(new SettingColor(255, 255, 255, 50))
            .build()
    );
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("边框颜色")
            .defaultValue(new SettingColor(255, 255, 255, 255))
            .build()
    );
    private final Setting<Double> renderSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("RenderSpeed")
            .description("渲染速度")
            .defaultValue(0.05)
            .sliderRange(0, 1)
            .build()
    );
    private final Setting<Double> renderH = sgGeneral.add(new DoubleSetting.Builder()
            .name("RenderHeight")
            .description("渲染高度")
            .defaultValue(0.1)
            .sliderRange(0, 1)
            .build()
    );

    private final Timer placeTimer = new Timer();
    private final Timer breakTimer = new Timer();
    private final Timer baseTimer = new Timer();
    private int dmg = 0;
    private PlayerEntity target;
    public BlockPos crystalPos;
    public BlockPos lastBestPos;
    private static class RenderPos {
        double x, y, z;
    }
    private final RenderPos renderPos = new RenderPos();
    public String getInfoString() {
        return target == null ? null : "§f[" + target.getName().getString() + "]";
    }
    @Override
    public void onActivate() {
        breakTimer.setMs(9999999);
        breakTimer.setMs(9999999);
        placeTimer.setMs(9999999);
        renderPos.x = 0;
        renderPos.y = 0;
        renderPos.z = 0;
    }
    @EventHandler
    private void onRender(RenderLeaves3DEvent event) {
        if (!renderDmg.get() || crystalPos == null) return;
        Vec3d vec = new Vec3d(renderPos.x + 0.5, renderPos.y + (1 - renderH.get()/2), renderPos.z + 0.5);
        Render3DUtil.renderText3D(dmg + "f", vec, dmgColor.get().getPacked());
    }
    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!render.get()) return;
        if (crystalPos == null) {
            renderPos.x = 0;
            renderPos.y = 0;
            renderPos.z = 0;
            return;
        }
        if (renderPos.x == 0 && renderPos.y == 0 && renderPos.z == 0) {
            renderPos.x = mc.player.getX();
            renderPos.y = mc.player.getY();
            renderPos.z = mc.player.getZ();
        }
        renderPos.x += (crystalPos.getX() - renderPos.x) * renderSpeed.get();
        renderPos.y += (crystalPos.getY() - 1 - renderPos.y) * renderSpeed.get();
        renderPos.z += (crystalPos.getZ() - renderPos.z) * renderSpeed.get();
        Box box = new Box(
                renderPos.x , renderPos.y + (1 - renderH.get()), renderPos.z,
                renderPos.x + 1, renderPos.y + 1, renderPos.z + 1
        );
        event.renderer.box(
                box,
                sideColor.get(),
                lineColor.get(),
                shapeMode.get(),
                0
        );
    }
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        if (crystalPos != null && !PlayerUtils.isWithin(crystalPos.toCenterPos(), placeRange.get())) {
            crystalPos = null;
            lastBestPos = null;
        }
        if (crystalPos != null && !PlayerUtils.isWithin(crystalPos.toCenterPos(), breakRange.get())) {
            crystalPos = null;
            lastBestPos = null;
        }
        if (crystalPos != null && BlockUtil.getBlock(crystalPos.down()) != Blocks.OBSIDIAN && BlockUtil.getBlock(crystalPos.down()) != Blocks.BEDROCK) {
            crystalPos = null;
            lastBestPos = null;
        }
        target = CombatUtil.getClosestEnemy(targetRange.get());
        if (target == null) {
            crystalPos = null;
            lastBestPos = null;
            return;
        }
        if (shouldPause()) return;
        int crystalSlot = inventory.get()
                ? InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL)
                : InventoryUtil.findItem(Items.END_CRYSTAL);

        if (crystalSlot == -1) return;
        if (breakTimer.passedMs(breakDelay.get())) {
            breakCrystal();
            breakTimer.reset();
        }

        findBestPos();

        if (crystalPos != null && placeTimer.passedMs(placeDelay.get())) {
            if (!BlockUtil.hasCrystal(crystalPos)) {
                placeCrystal(crystalPos, crystalSlot);
                baseTimer.reset();
                placeTimer.reset();
            } else if (BlockUtil.hasCrystal(crystalPos)) {
                CombatUtil.attackCrystal(crystalPos, true, false);
            }
        }
    }
    private void findBestPos() {
        PlayerEntity predictTarget = predictTarget(target);
        float bestDamage = 0;
        BlockPos best = null;
        ArrayList<BlockPos> placeList = new ArrayList<>();
        for (BlockPos pos : BlockUtil.getSphere(placeRange.get())) {
            if (autoBase.get() && !BlockUtil.canPlaceCrystal(pos) && BlockUtil.canPlace(pos.down()) && mc.world.isAir(pos) && baseTimer.passedMs(baseDelay.get())) placeList.add(pos);
            if (BlockUtil.canPlaceCrystal(pos) || (BlockUtil.hasCrystal(pos) && mc.world.isAir(pos))) placeList.add(pos);
        }
        if (placeList.isEmpty()) return;
        for (BlockPos pos : placeList) {
            if (autoBase.get()) {
                CombatUtil.modifyPos = pos.down();
                CombatUtil.modifyBlockState = Blocks.OBSIDIAN.getDefaultState();
            }
            Vec3d vec = new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            float dmg = calcMode.get() == CalcMode.Meteor ? DamageUtils.crystalDamage(predictTarget, vec, false, pos.down()) : DamageUtil.calculateDamage(pos, predictTarget);
            float self = calcMode.get() == CalcMode.Meteor ? DamageUtils.crystalDamage(mc.player, vec, false, pos.down()) : DamageUtil.calculateDamage(pos, mc.player);
            if (autoBase.get()) CombatUtil.modifyPos = null;
            if (dmg < minDamage.get()) continue;
            if (self > maxSelfDmg.get()) continue;
            if (noSuicide.get() && self > EntityUtils.getTotalHealth(mc.player)) continue;
            if (dmg > bestDamage) {
                bestDamage = dmg;
                best = pos;
            }
        }
        if (lastBestPos != null) {
            if (autoBase.get()) {
                CombatUtil.modifyPos = lastBestPos.down();
                CombatUtil.modifyBlockState = Blocks.OBSIDIAN.getDefaultState();
            }
            Vec3d vec = new Vec3d(lastBestPos.getX() + 0.5, lastBestPos.getY(), lastBestPos.getZ() +0.5);
            float last = calcMode.get() == CalcMode.Meteor ? DamageUtils.crystalDamage(predictTarget, vec,false, lastBestPos.down()) : DamageUtil.calculateDamage(lastBestPos, predictTarget);
            float lastSelf = calcMode.get() == CalcMode.Meteor ? DamageUtils.crystalDamage(mc.player, vec,false, lastBestPos.down()) : DamageUtil.calculateDamage(lastBestPos, mc.player);
            if (autoBase.get()) CombatUtil.modifyPos = null;
            if (best != null && last >= bestDamage * 0.95 && lastSelf < maxSelfDmg.get() && (!noSuicide.get() || lastSelf < EntityUtils.getTotalHealth(mc.player))) {
                crystalPos = lastBestPos;
                dmg = (int) last;
                return;
            }
            if (best == null && last >= minDamage.get()) {
                crystalPos = lastBestPos;
                dmg = (int) last;
                return;
            }
        }
        if (best != null && autoBase.get() && BlockUtil.canPlace(best.down()) && baseTimer.passedMs(baseDelay.get())) doBase(best.down());
        lastBestPos = best;
        crystalPos = best;
        dmg = (int) bestDamage;
    }

    private PlayerEntity predictTarget(PlayerEntity target) {
        if (predict.get() > 0) {
            double prevX = target.prevX;
            double prevY = target.prevY;
            double prevZ = target.prevZ;
            double predictX = target.getX() + (target.getX() - prevX) * predict.get();
            double predictY = target.getY() + (target.getY() - prevY) * predict.get();
            double predictZ = target.getZ() + (target.getZ() - prevZ) * predict.get();
            BlockPos predictPos = new BlockPosX(predictX, predictY, predictZ);
            PlayerEntity predicTarget = new PlayerEntity(mc.world, predictPos, target.getYaw(), new GameProfile(UUID.fromString("66123666-1234-5432-6666-667563866600"), "PredictEntity339")) {
                @Override
                public boolean isSpectator() {return false;}
                @Override
                public boolean isCreative() {return false;}
                @Override
                public boolean isOnGround() {return target.isOnGround();}
            };
            predicTarget.setHealth(target.getHealth());
            predicTarget.prevX = target.prevX;
            predicTarget.prevZ = target.prevZ;
            predicTarget.prevY = target.prevY;
            predicTarget.setOnGround(target.isOnGround());
            predicTarget.getInventory().clone(target.getInventory());
            predicTarget.setPose(target.getPose());
            for (StatusEffectInstance se : new ArrayList<>(target.getStatusEffects())) {
                predicTarget.addStatusEffect(se);
            }
            return predicTarget;
        } else {
            return target;
        }
    }

    private void doBase(BlockPos pos) {
        int old = mc.player.getInventory().selectedSlot;
        int obsSlot = inventory.get() ? InventoryUtil.findItemInventorySlot(Items.OBSIDIAN) : InventoryUtil.findItem(Items.OBSIDIAN);
        Direction side = BlockUtil.getPlaceSide(pos, null);
        if (obsSlot == -1 || side == null) return;
        doSwap(obsSlot);
        BlockUtil.placeBlock(pos, side, true);
        if (inventory.get()) {
            doSwap(obsSlot);
        } else {
            doSwap(old);
        }
        breakTimer.reset();
    }

    private void placeCrystal(BlockPos pos, int slot) {
        BlockPos base = pos.down();
        var side = BlockUtil.getClickSide(base);
        if (side == null) return;

        int old = mc.player.getInventory().selectedSlot;
        boolean holdItem = checkItem(old);
        if (!holdItem) doSwap(slot);

        BlockUtil.clickBlock(base, side, true);

        if (inventory.get() && !holdItem) {
            doSwap(slot);
        } else {
            doSwap(old);
        }
    }

    private boolean checkItem(int old) {
        return mc.player.getInventory().getStack(old).getItem() == Items.END_CRYSTAL;
    }

    private boolean shouldPause() {
        if (preferMode.get() == PreferMode.PreferAnchor) {
            return AutoAnchor.INSTANCE.currentPos != null;
        }
        return !usingPause.get() || checkPause(onlyMain.get());
    }
    public boolean checkPause(boolean onlyMain) {
        return (mc.options.useKey.isPressed() || mc.player.isUsingItem()) && (!onlyMain || mc.player.getActiveHand() == Hand.MAIN_HAND);
    }
    private void breakCrystal() {
        for (var entity : mc.world.getEntities()) {
            if (!(entity instanceof EndCrystalEntity crystal)) continue;
            if (!CombatUtil.isValid(entity,breakRange.get())) continue;
            PlayerEntity predictTarget = predictTarget(target);
            float damage = calcMode.get() == CalcMode.Meteor ? DamageUtils.crystalDamage(predictTarget, crystal.getPos(), false, crystal.getBlockPos().down()) : DamageUtil.calculateDamage(crystal.getPos(), predictTarget);
            float self = calcMode.get() == CalcMode.Meteor ? DamageUtils.crystalDamage(mc.player, crystal.getPos(), false, crystal.getBlockPos().down()) : DamageUtil.calculateDamage(crystal.getPos(), mc.player);
            if (damage < minDamage.get()) continue;
            if (self > maxSelfDmg.get()) continue;
            if (noSuicide.get() && self > EntityUtils.getTotalHealth(mc.player)) continue;
            BlockPos pos = new BlockPosX(crystal.getX(), crystal.getY() + 0.5, crystal.getZ());
            CombatUtil.attackCrystal(pos, true, false);
            if (crystalPos != null && BlockUtil.hasCrystal(crystalPos)) {
                if (placeAfterBreak.get()) {
                    int crystalSlot = inventory.get()
                            ? InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL)
                            : InventoryUtil.findItem(Items.END_CRYSTAL);
                    placeCrystal(crystalPos, crystalSlot);
                }
            }
            return;
        }
    }
    private void doSwap(int slot) {
        if (!inventory.get()) {
            InventoryUtil.switchToSlot(slot);
        } else {
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
        }
    }
    public enum PreferMode {
        PreferCrystal,
        PreferAnchor
    }
    public enum CalcMode {
        AlienV4,
        Meteor
    }
}
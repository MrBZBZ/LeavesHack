package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.combat.CombatUtil;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.world.BlockPosX;
import com.dev.leavesHack.utils.world.BlockUtil;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.UUID;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class AutoCrystal extends Module {
    public static AutoCrystal INSTANCE;
    public AutoCrystal() {
        super(LeavesHack.LEAVES_COMBAT, "AutoCrystal", "自动水晶");
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
            .defaultValue(4.0)
            .sliderRange(1, 6)
            .build()
    );
    private final Setting<Double> breakRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("BreakRange")
            .description("破坏距离")
            .defaultValue(4.0)
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
    private final Setting<Boolean> attackFire = sgGeneral.add(new BoolSetting.Builder()
            .name("AttackFire")
            .description("自动灭火")
            .defaultValue(true)
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
//    private final Setting<Boolean> renderDmg = sgRender.add(new BoolSetting.Builder()
//            .name("RenderDmg")
//            .description("渲染伤害")
//            .defaultValue(true)
//            .build()
//    );
//    private final Setting<SettingColor> dmgColor = sgRender.add(new ColorSetting.Builder()
//            .name("DamageColor")
//            .description("伤害文本颜色")
//            .defaultValue(new SettingColor(255, 255, 255, 255))
//            .build()
//    );
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
    private Player target;
    public BlockPos crystalPos;
    public BlockPos lastBestPos;
    private static class RenderPos {
        double x, y, z;
    }
    private final RenderPos renderPos = new RenderPos();
    public String getInfoString() {
        return target == null ? null : "[" + target.getName().getString() + "]";
    }
    @Override
    public void onDeactivate() {
        crystalPos = null;
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
//    @EventHandler
//    private void onRender(RenderLeaves3DEvent event) {
//        if (!renderDmg.get() || crystalPos == null) return;
//        Vec3 vec = new Vec3(renderPos.x + 0.5, renderPos.y + (1 - renderH.get()/2), renderPos.z + 0.5);
//        Render3DUtil.renderText3D(dmg + "f", vec, dmgColor.get().getPacked());
//    }
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
        AABB box = new AABB(
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
        if (mc.player == null || mc.level == null) return;
        if (crystalPos != null && !PlayerUtils.isWithin(crystalPos.getCenter(), placeRange.get())) {
            crystalPos = null;
            lastBestPos = null;
        }
        if (crystalPos != null && !PlayerUtils.isWithin(crystalPos.getCenter(), breakRange.get())) {
            crystalPos = null;
            lastBestPos = null;
        }
        if (crystalPos != null && ((BlockUtil.getBlock(crystalPos.below()) != Blocks.OBSIDIAN && BlockUtil.getBlock(crystalPos.below()) != Blocks.BEDROCK) || !mc.level.isEmptyBlock(crystalPos))) {
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
        int crystalSlot = inventory.get() ? InventoryUtil.findItemInventorySlot(Items.END_CRYSTAL) : InventoryUtil.findItem(Items.END_CRYSTAL);

        if (crystalSlot == -1) {
            crystalPos = null;
            return;
        }
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
        Player predictTarget = predictTarget(target);
        float bestDamage = 0;
        BlockPos best = null;
        ArrayList<BlockPos> placeList = new ArrayList<>();
        for (BlockPos pos : BlockUtil.getSphere(placeRange.get())) {
            if (autoBase.get() && !BlockUtil.canPlaceCrystal(pos, attackFire.get()) && BlockUtil.canPlace(pos.below())  && !BlockUtil.hasEntity(pos, true) && mc.level.isEmptyBlock(pos) && pos.getY() <= target.getY() &&baseTimer.passedMs(baseDelay.get())) placeList.add(pos);
            if (BlockUtil.canPlaceCrystal(pos, attackFire.get()) || (BlockUtil.hasCrystalPlaceAccurate(pos) && mc.level.isEmptyBlock(pos))) placeList.add(pos);
        }
        if (placeList.isEmpty()) return;
        for (BlockPos pos : placeList) {
            if (autoBase.get()) {
                CombatUtil.modifyPos = pos.below();
                CombatUtil.modifyBlockState = Blocks.OBSIDIAN.defaultBlockState();
            }
            Vec3 vec = new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            float dmg = DamageUtils.crystalDamage(predictTarget, vec, false, pos.below());
            float self = DamageUtils.crystalDamage(mc.player, vec, false, pos.below());
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
                CombatUtil.modifyPos = lastBestPos.below();
                CombatUtil.modifyBlockState = Blocks.OBSIDIAN.defaultBlockState();
            }
            Vec3 vec = new Vec3(lastBestPos.getX() + 0.5, lastBestPos.getY(), lastBestPos.getZ() +0.5);
            float last = DamageUtils.crystalDamage(predictTarget, vec,false, lastBestPos.below());
            float lastSelf = DamageUtils.crystalDamage(mc.player, vec,false, lastBestPos.below());
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
        if (best != null && autoBase.get() && BlockUtil.canPlace(best.below()) && baseTimer.passedMs(baseDelay.get())) doBase(best.below());
        lastBestPos = best;
        crystalPos = best;
        dmg = (int) bestDamage;
    }
    private Player predictTarget(Player target) {
        if (predict.get() <= 0) return target;
        int ticks = predict.get();
        double dx = target.getX() - target.xo;
        double dy = target.getY() - target.yo;
        double dz = target.getZ() - target.zo;
        double predictX = target.getX() + dx * ticks;
        double predictY = target.getY() + dy * ticks;
        double predictZ = target.getZ() + dz * ticks;
        Player fake = new Player(
            mc.level,
            new GameProfile(UUID.randomUUID(), "Predict")
        ) {
            @Override
            public @Nullable GameType gameMode() {
                return null;
            }
        };
        fake.snapTo(predictX, predictY, predictZ, target.getYRot(), target.getXRot());
        fake.setPose(target.getPose());
        fake.setOnGround(target.onGround());
        fake.setDeltaMovement(target.getDeltaMovement());
        fake.getAttributes().assignAllValues(target.getAttributes());
        fake.setHealth(target.getHealth());
        for (MobEffectInstance se : target.getActiveEffects()) {
            fake.addEffect(new MobEffectInstance(se));
        }
        fake.getInventory().replaceWith(target.getInventory());
        fake.refreshDimensions();
        return fake;
    }

    private void doBase(BlockPos pos) {
        int old = mc.player.getInventory().getSelectedSlot();
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
        BlockPos base = pos.below();
        if (mc.level.getBlockState(pos).is(BlockTags.FIRE) && attackFire.get()) {
            ArrayList<Direction> sides = BlockUtil.getPlaceSides(pos, null);
            if (!sides.isEmpty() && sides.contains(Direction.DOWN)) {
                mc.gameMode.startDestroyBlock(pos, Direction.DOWN);
            }
        }
        var side = BlockUtil.getClickSide(base);
        if (side == null) return;

        int old = mc.player.getInventory().getSelectedSlot();
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
        return mc.player.getInventory().getItem(old).getItem() == Items.END_CRYSTAL;
    }

    private boolean shouldPause() {
        if (preferMode.get() == PreferMode.PreferAnchor) {
            return AutoAnchor.INSTANCE.currentPos != null;
        }
        return usingPause.get() && checkPause(onlyMain.get());
    }
    public boolean checkPause(boolean onlyMain) {
        return (mc.options.keyUse.isDown() || mc.player.isUsingItem()) && (!onlyMain || mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND);
    }
    private void breakCrystal() {
        for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal crystal)) continue;
            if (!CombatUtil.isValid(entity,breakRange.get())) continue;
            Player predictTarget = predictTarget(target);
            float damage = DamageUtils.crystalDamage(predictTarget, crystal.position(), false, crystal.blockPosition().below());
            float self = DamageUtils.crystalDamage(mc.player, crystal.position(), false, crystal.blockPosition().below());
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
            InventoryUtil.inventorySwap(slot, mc.player.getInventory().getSelectedSlot());
        }
    }
    public enum PreferMode {
        PreferCrystal,
        PreferAnchor
    }
}

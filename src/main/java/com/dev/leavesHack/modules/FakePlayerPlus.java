package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.entity.EntityStatuses;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * FakePlayerPlus（假玩家+）
 * <p>
 * 生成一个纯客户端假人，用于测试爆炸类战斗模块（水晶/床/锚）的实战效果。
 * 基座复用 Meteor 自带的 {@link FakePlayerEntity}（生成/移除/皮肤/背包克隆），
 * 在此之上移植自 Alien FakePlayer 的增强功能：
 * <ul>
 *   <li>无限药水 Buff（回生/抗性/伤害吸收）</li>
 *   <li>自动图腾：副手保持不死图腾，濒死时自动触发图腾复活动画</li>
 *   <li>爆炸伤害模拟：监听爆炸包，按距离结算水晶/床爆炸伤害并扣减假人血量</li>
 * </ul>
 */
public class FakePlayerPlus extends Module {
    public static FakePlayerPlus INSTANCE;
    public FakePlayerEntity fakePlayer;

    /** 需要镜像到实体 equipment 的人形槽位（1.21.9+ 实体装备与背包分离） */
    private static final EquipmentSlot[] EQUIPMENT_COPY_SLOTS = {
        EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
        EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD
    };

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> fakeName = sgGeneral.add(new StringSetting.Builder()
        .name("Name")
        .description("假人名字")
        .defaultValue("FakePlayer")
        .build()
    );

    private final Setting<Integer> health = sgGeneral.add(new IntSetting.Builder()
        .name("Health")
        .description("假人血量(>20 的部分转为伤害吸收盾)")
        .defaultValue(20)
        .range(1, 40)
        .sliderRange(1, 40)
        .build()
    );

    private final Setting<Boolean> copyInventory = sgGeneral.add(new BoolSetting.Builder()
        .name("CopyInventory")
        .description("复制自身背包与装备")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> regen = sgGeneral.add(new BoolSetting.Builder()
        .name("Regeneration")
        .description("无限回生 II(注意：会持续回血，影响伤害/图腾测试)")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> resistance = sgGeneral.add(new BoolSetting.Builder()
        .name("Resistance")
        .description("无限抗性 II(注意：减少 40% 模拟伤害)")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> absorption = sgGeneral.add(new BoolSetting.Builder()
        .name("Absorption")
        .description("无限伤害吸收 III(注意：提供 12 点吸收盾，会优先抵消模拟伤害)")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> autoTotem = sgGeneral.add(new BoolSetting.Builder()
        .name("AutoTotem")
        .description("副手自动保持不死图腾，濒死自动弹出")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> explosionDamage = sgGeneral.add(new BoolSetting.Builder()
        .name("ExplosionDamage")
        .description("模拟爆炸伤害(水晶/床/锚)并扣减假人血量")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> debugInfo = sgGeneral.add(new BoolSetting.Builder()
        .name("DebugInfo")
        .description("在聊天栏输出每次爆炸的结算明细，便于验证模拟是否生效")
        .defaultValue(true)
        .build()
    );

    public FakePlayerPlus() {
        super(LeavesHack.LEAVES_MISC, "FakePlayer+", "假人测试");
        INSTANCE = this;
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }

        fakePlayer = new FakePlayerEntity(mc.player, fakeName.get(), health.get().floatValue(), copyInventory.get());
        fakePlayer.doNotPush = true;
        fakePlayer.hideWhenInsideCamera = true;
        // 1.21.9+ 实体装备(副手/盔甲)存于 LivingEntity.equipment，与 PlayerInventory 是两套存储；
        // meteor 的背包克隆只写 PlayerInventory，这里必须用 equipStack 镜像到实体，否则
        // getOffHandStack()/伤害计算/渲染读到的永远是空
        if (copyInventory.get()) {
            for (EquipmentSlot slot : EQUIPMENT_COPY_SLOTS) {
                fakePlayer.equipStack(slot, mc.player.getEquippedStack(slot).copy());
            }
        }
        applyEffects();
        if (autoTotem.get()) equipTotem();
        fakePlayer.spawn();
        if (debugInfo.get()) ChatUtils.info("FakePlayer+: 假人已生成 (%.0f 血)", fakePlayer.getHealth());
    }

    @Override
    public void onDeactivate() {
        if (fakePlayer != null) {
            fakePlayer.despawn();
            fakePlayer = null;
        }
    }

    @Override
    public String getInfoString() {
        // 模块列表实时显示假人血量，便于确认伤害模拟是否生效（保持简短）
        return fakePlayer == null ? null : String.format("[%.1f❤]", fakePlayer.getHealth());
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.world == null || fakePlayer == null || fakePlayer.isRemoved()) {
            if (isActive()) toggle();
            return;
        }

        // 无限 Buff：每 tick 重新施加短时长药水
        applyEffects();

        // 副手图腾维持
        if (autoTotem.get() && !(fakePlayer.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING)) {
            equipTotem();
        }

        // 濒死自动图腾：1.21.2+ 原版把图腾结算改为服务端私有方法，
        // 此处按原版逻辑在客户端侧等效模拟，并回 10 血（本地粒子发射器+音效）
        if (fakePlayer.isDead() && autoTotem.get()) {
            boolean fired = simulateTotem();
            if (debugInfo.get()) {
                if (fired) {
                    ChatUtils.info("FakePlayer+: 图腾触发 → 本地特效已施加(粒子发射器40t+音效)");
                } else {
                    ChatUtils.info("FakePlayer+: 图腾触发失败 → 副手无图腾 (autoTotem 会自动补装)");
                }
            }
            if (fired) fakePlayer.setHealth(10.0f);
        }
    }

    /** 客户端侧等效图腾判定（对应原版 LivingEntity.tryUseDeathProtector） */
    private boolean simulateTotem() {
        ItemStack offhand = fakePlayer.getOffHandStack();
        if (offhand.getItem() != Items.TOTEM_OF_UNDYING) return false;

        offhand.decrement(1);
        fakePlayer.setHealth(1.0f);
        fakePlayer.clearStatusEffects();
        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
        fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
        // 图腾特效直接本地触发（与 1.21.11 onEntityStatus case 35 等效：40tick 图腾粒子发射器+音效）。
        // 不走 EntityStatusS2CPacket.apply()：该路径经 forceMainThread/包批处理器，存在被延迟/吞包的不确定性
        mc.particleManager.addEmitter(fakePlayer, ParticleTypes.TOTEM_OF_UNDYING, 40);
        mc.world.playSound(null, fakePlayer.getX(), fakePlayer.getY(), fakePlayer.getZ(),
            SoundEvents.ITEM_TOTEM_USE, fakePlayer.getSoundCategory(), 1.0f, 1.0f);
        return true;
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (!explosionDamage.get() || fakePlayer == null) return;
        if (!(event.packet instanceof ExplosionS2CPacket packet)) return;

        handleExplosion(packet);
    }

    /**
     * 爆炸伤害结算：距爆炸中心 10 格内才参与结算；
     * 爆炸点下方是重生锚按锚伤害计算，否则按末影水晶伤害计算。
     */
    private void handleExplosion(ExplosionS2CPacket packet) {
        // PacketEvent.Receive 在网络线程触发，切回主线程结算，避免与世界数据竞争
        mc.execute(() -> settleExplosion(packet));
    }

    private void settleExplosion(ExplosionS2CPacket packet) {
        if (fakePlayer == null || fakePlayer.isRemoved()) return;

        // 1.21.2+ 爆炸包为 Record：center() 返回爆炸中心
        Vec3d explosionPos = packet.center();
        double x = explosionPos.x;
        double y = explosionPos.y;
        double z = explosionPos.z;

        // 与原版一致：爆炸(水晶/床)的实际作用半径为 12 格，超出后原版伤害为 0，
        // 此前 10 格的门槛会把 10~12 格内的残余伤害误判为无伤
        double distance = fakePlayer.getEntityPos().distanceTo(explosionPos);
        if (distance > 12) return;

        BlockPos below = BlockPos.ofFloored(x, y - 0.5, z);

        float damage;
        Block under = BlockUtil.getBlock(below);
        if (under == Blocks.RESPAWN_ANCHOR) {
            damage = DamageUtils.anchorDamage(fakePlayer, explosionPos);
        } else {
            BlockPos obsidianPos = under == Blocks.OBSIDIAN || under == Blocks.BEDROCK ? below : null;
            damage = DamageUtils.crystalDamage(fakePlayer, explosionPos, false, obsidianPos);
        }

        applyDamage(damage);
        if (debugInfo.get()) {
            ChatUtils.info("FakePlayer+: 爆炸结算 距离%.1f格 伤害%.1f → 血量%.1f/%.1f (吸收盾%.1f)",
                distance, damage, fakePlayer.getHealth(), fakePlayer.getMaxHealth(), fakePlayer.getAbsorptionAmount());
        }
    }

    /** 先扣伤害吸收盾，再扣血量 */
    private void applyDamage(float damage) {
        if (damage <= 0) return;

        float absorption = fakePlayer.getAbsorptionAmount();
        if (absorption > 0) {
            if (damage <= absorption) {
                fakePlayer.setAbsorptionAmount(absorption - damage);
            } else {
                fakePlayer.setAbsorptionAmount(0);
                fakePlayer.setHealth(Math.max(0.0f, fakePlayer.getHealth() - (damage - absorption)));
            }
        } else {
            fakePlayer.setHealth(Math.max(0.0f, fakePlayer.getHealth() - damage));
        }

        // 受伤动画：红闪 + 受击倾斜（等价原版服务端广播的 status 2）
        fakePlayer.animateDamage((float) (Math.random() * 360.0));
        if (debugInfo.get() && fakePlayer.getHealth() <= 0) {
            ChatUtils.info("FakePlayer+: 假人濒死 (血量=0), 濒死图腾流程启动");
        }
    }

    private void applyEffects() {
        // ambient=false + showParticles=true：让假人周围出现药水漩涡粒子，便于确认效果已应用
        if (regen.get()) {
            fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 40, 1, false, true));
        }
        if (resistance.get()) {
            fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 1, false, true));
        }
        if (absorption.get()) {
            fakePlayer.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 40, 2, false, true));
        }
    }

    private void equipTotem() {
        // 关键修复：必须写实体 equipment(通过 equipStack)，
        // inventory.setStack(40) 写的是 PlayerInventory 的另一套存储，getOffHandStack() 读不到
        fakePlayer.equipStack(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
    }
}

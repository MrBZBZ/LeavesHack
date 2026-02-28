package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.asm.accessors.ILivingEntity;
import com.dev.leavesHack.utils.combat.CombatUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.rotation.Rotation;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.MaceItem;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.Set;

import static com.dev.leavesHack.utils.world.BlockUtil.getClosestPointToBox;

public class Aura extends Module {
    public static Aura INSTANCE;
    public Aura() {
        super(LeavesHack.CATEGORY, "Aura", "");
        INSTANCE = this;
    }
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Integer> targetRange = sgGeneral.add(new IntSetting.Builder()
            .name("TargetRange")
            .defaultValue(6)
            .min(0)
            .sliderMax(8)
            .build()
    );
    public final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("Range")
            .defaultValue(3.2)
            .min(0)
            .sliderMax(8)
            .build()
    );
    private final Setting<Boolean> reset = sgGeneral.add(new BoolSetting.Builder()
            .name("Reset")
            .defaultValue(true)
            .build()
    );
    public final Setting<Integer> hurtTime = sgGeneral.add(new IntSetting.Builder()
            .name("HurtTime")
            .defaultValue(10)
            .min(0)
            .sliderMax(10)
            .build()
    );
    public final Setting<Double> cooldown = sgGeneral.add(new DoubleSetting.Builder()
            .name("Cooldown")
            .defaultValue(0.55)
            .min(0)
            .sliderMax(1)
            .build()
    );
    public final Setting<Double> wallRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("WallRange")
            .defaultValue(3.2)
            .min(0.1)
            .sliderMax(7)
            .build()
    );
    private final Setting<Boolean> usingPause = sgGeneral.add(new BoolSetting.Builder()
            .name("UsingPause")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> weaponOnly = sgGeneral.add(new BoolSetting.Builder()
            .name("WeaponOnly")
            .defaultValue(true)
            .build()
    );
    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("Entities to attack.")
            .onlyAttackable()
            .defaultValue(EntityType.PLAYER)
            .build()
    );
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("Rotate")
            .defaultValue(true)
            .build()
    );
    private final Timer tick = new Timer();
    public static Entity target;
    @Override
    public void onActivate() {
        tick.setMs(9999999);
    }
    @Override
    public String getInfoString() {
        return target == null ? null : "§f[" + target.getName().getString() + "]";
    }
    @EventHandler
    public void onPacket(PacketEvent.Send event) {
        if (reset.get()) {
            Packet<?> packet = event.packet;
            if (packet instanceof PlayerInteractEntityC2SPacket) {
                PlayerInteractEntityC2SPacket.InteractTypeHandler handler = getInteractTypeHandler((PlayerInteractEntityC2SPacket) packet);
                PlayerInteractEntityC2SPacket.InteractType type = handler.getType();
                if (type == PlayerInteractEntityC2SPacket.InteractType.ATTACK) {
                    tick.reset();
                }
            }
            if (packet instanceof HandSwingC2SPacket) {
                tick.reset();
            }
        }
    }
    public static PlayerInteractEntityC2SPacket.InteractTypeHandler getInteractTypeHandler(PlayerInteractEntityC2SPacket packet) {
        return packet.type;
    }
    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player ==  null || mc.world == null) return;
        if (weaponOnly.get() && !isHoldingWeapon(mc.player)) {
            target = null;
            return;
        }
        target = getTarget(range.get());
        if (target == null) {
            target = getTarget(targetRange.get());
            return;
        }
        doAura();
    }
    private void doAura() {
        if (!check()) {
            return;
        }
        if (rotate.get()) {
            Vec3d hitVec = getAttackVec(target);
            Rotation.snapAt(hitVec);
        }
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        mc.player.resetLastAttackedTicks();
        mc.player.swingHand(Hand.MAIN_HAND);
        tick.reset();
        if (rotate.get()) {
            Rotation.snapBack();
        }
    }
    private boolean check() {
        if (!tick.passedMs(cooldown.get() * 1000)) {
            return false;
        }
        if (target instanceof LivingEntity entity && entity.hurtTime > hurtTime.get()) {
            return false;
        }
        return usingPause.get() || !mc.player.isUsingItem();
    }
    public Entity getTarget(double range) {
        Entity target = null;
        double distance = range;
        for (Entity entity : mc.world.getEntities()) {
            if (!entities.get().contains(entity.getType())) continue;
            Vec3d hitVec = getAttackVec(entity);
            if (mc.player.getEyePos().distanceTo(hitVec) > range) continue;
            if (!mc.player.canSee(entity) && mc.player.getEyePos().distanceTo(hitVec) > wallRange.get()) {
                continue;
            }
            if (!CombatUtil.isValid(entity)) continue;
            if (target == null) {
                target = entity;
                distance = mc.player.getEyePos().distanceTo(hitVec);
            } else {
                if (mc.player.getEyePos().distanceTo(hitVec) < distance) {
                    target = entity;
                    distance = mc.player.getEyePos().distanceTo(hitVec);
                }
            }
        }
        return target;
    }
    private Vec3d getAttackVec(Entity entity) {
        return getClosestPointToBox(mc.player.getEyePos(), entity.getBoundingBox());
    }
    public static boolean isHoldingWeapon(PlayerEntity player) {
        return player.getMainHandStack().getItem() instanceof SwordItem || player.getMainHandStack().getItem() instanceof AxeItem || player.getMainHandStack().getItem() instanceof MaceItem || player.getMainHandStack().getItem() instanceof TridentItem;
    }
    public float getAttackCooldownProgressPerTick() {
        return (float) (1.0 / mc.player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED) * 20.0);
    }
}
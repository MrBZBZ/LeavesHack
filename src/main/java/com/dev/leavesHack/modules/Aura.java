package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.events.RenderLeaves3DEvent;
import com.dev.leavesHack.utils.combat.CombatUtil;
import com.dev.leavesHack.utils.entity.EntityUtil;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.render.Render2DUtil;
import com.dev.leavesHack.utils.render.Render3DUtil;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.dev.leavesHack.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.Set;
import java.util.function.Predicate;
import static com.dev.leavesHack.utils.world.BlockUtil.getClosestPointToBox;

public class Aura extends Module {
    public static Aura INSTANCE;
    public Aura() {
        super(LeavesHack.CATEGORY, "Aura", "杀戮光环");
        INSTANCE = this;
    }
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final SettingGroup sgRender = this.settings.createGroup("Render");
    private final Setting<Integer> targetRange = sgGeneral.add(new IntSetting.Builder()
            .name("TargetRange")
            .description("目标距离")
            .defaultValue(6)
            .min(0)
            .sliderMax(8)
            .build()
    );
    public final Setting<Double> attackRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("Range")
            .description("攻击距离")
            .defaultValue(3.5)
            .min(0)
            .sliderMax(8)
            .build()
    );
    private final Setting<Weapon> weapon = sgGeneral.add(new EnumSetting.Builder<Weapon>()
            .name("Weapon")
            .description("武器选择")
            .defaultValue(Weapon.Sword)
            .build()
    );
    private final Setting<SwitchMode> autoSwitch = sgGeneral.add(new EnumSetting.Builder<SwitchMode>()
            .name("AutoSwitch")
            .description("自动切换武器")
            .defaultValue(SwitchMode.Silent)
            .build()
    );
    private final Setting<Boolean> reset = sgGeneral.add(new BoolSetting.Builder()
            .name("Reset")
            .description("自动重置冷却")
            .defaultValue(true)
            .build()
    );
    public final Setting<Integer> hurtTime = sgGeneral.add(new IntSetting.Builder()
            .name("HurtTime")
            .description("伤害时间")
            .defaultValue(10)
            .min(0)
            .sliderMax(10)
            .build()
    );
    public final Setting<Double> cooldown = sgGeneral.add(new DoubleSetting.Builder()
            .name("Cooldown")
            .description("攻击冷却")
            .defaultValue(0.55)
            .min(0)
            .sliderMax(1)
            .build()
    );
    public final Setting<Double> wallRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("WallRange")
            .description("穿墙距离")
            .defaultValue(3.5)
            .min(0.1)
            .sliderMax(7)
            .build()
    );
    private final Setting<Boolean> usingPause = sgGeneral.add(new BoolSetting.Builder()
            .name("UsingPause")
            .description("使用物品时暂停")
            .defaultValue(true)
            .build()
    );
    private final Setting<Set<EntityType<?>>> entities = sgGeneral.add(new EntityTypeListSetting.Builder()
            .name("entities")
            .description("攻击目标")
            .onlyAttackable()
            .defaultValue(EntityType.PLAYER)
            .build()
    );
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("Rotate")
            .description("转头")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> ignoreNamed = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-named")
            .description("忽略带有命名的生物")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> ignorePassive = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-passive")
            .description("忽略中立生物")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> ignoreTamed = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-tamed")
            .description("忽略已驯服的生物")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> targetESP = sgRender.add(new BoolSetting.Builder()
            .name("TargetESP")
            .description("目标ESP渲染")
            .defaultValue(true)
            .build()
    );
    private final Setting<SettingColor> espColor = sgRender.add(new ColorSetting.Builder()
            .name("ESPColor")
            .description("ESP渲染颜色")
            .defaultValue(new SettingColor(255, 255, 255, 255))
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
        if (mc.world == null || mc.player == null) return null;
        return target == null ? null : "§f[" + target.getName().getString() + "]";
    }
    @EventHandler
    public void onMyRender3D(RenderLeaves3DEvent event) {
        if (target != null && targetESP.get()) {
            Render3DUtil.drawTargetEsp(event.matrixStack, target, new Color(espColor.get().getPacked()));
        }
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
        target = getTarget(targetRange.get());
        if (target == null) {
            return;
        }
        if (GlobalSetting.INSTANCE.moveFix.get() && rotate.get()) {
            Vec3d hitVec = getAttackVec(target);
            Rotation.snapAt(hitVec);
        }
        doAura();
    }
    private void doAura() {
        if (!check()) {
            return;
        }
        boolean found = false;
        int previousSlot = -1;
        if (autoSwitch.get() != SwitchMode.None && !itemInHand()) {
            Predicate<ItemStack> predicate = switch (weapon.get()) {
                case Axe -> stack -> stack.getItem() instanceof AxeItem;
                case Sword -> stack -> stack.getItem() instanceof SwordItem;
                case Mace -> stack -> stack.getItem() instanceof MaceItem;
                case Trident -> stack -> stack.getItem() instanceof TridentItem;
                case All -> stack -> stack.getItem() instanceof AxeItem || stack.getItem() instanceof SwordItem || stack.getItem() instanceof MaceItem || stack.getItem() instanceof TridentItem;
                default -> o -> true;
            };
            FindItemResult weaponResult = InvUtils.findInHotbar(predicate);
            previousSlot  = mc.player.getInventory().selectedSlot;
            if (weaponResult.found()) {
                InventoryUtil.switchToSlot(weaponResult.slot());
                found = true;
            }
        }
        if (!itemInHand() && autoSwitch.get() != SwitchMode.Silent) {
            return;
        }
        if (autoSwitch.get() == SwitchMode.Silent && !found && !itemInHand()) {
            return;
        }
        if (rotate.get()) {
            Vec3d hitVec = getAttackVec(target);
            Rotation.snapAt(hitVec);
        }
        mc.getNetworkHandler().sendPacket(PlayerInteractEntityC2SPacket.attack(target, mc.player.isSneaking()));
        mc.player.resetLastAttackedTicks();
        EntityUtil.attackSwingHand();
        tick.reset();
        if (rotate.get()) {
            Rotation.snapBack();
        }
        if (autoSwitch.get() == SwitchMode.Silent && previousSlot != -1) {
            InventoryUtil.switchToSlot(previousSlot);
        }
    }
    private boolean itemInHand() {
        return switch (weapon.get()) {
            case Axe -> mc.player.getMainHandStack().getItem() instanceof AxeItem;
            case Sword -> mc.player.getMainHandStack().getItem() instanceof SwordItem;
            case Mace -> mc.player.getMainHandStack().getItem() instanceof MaceItem;
            case Trident -> mc.player.getMainHandStack().getItem() instanceof TridentItem;
            case All -> mc.player.getMainHandStack().getItem() instanceof AxeItem || mc.player.getMainHandStack().getItem() instanceof SwordItem || mc.player.getMainHandStack().getItem() instanceof MaceItem || mc.player.getMainHandStack().getItem() instanceof TridentItem;
            default -> true;
        };
    }

    private boolean check() {
        if (!CombatUtil.isValid(target, attackRange.get())) return false;
        if (!mc.player.canSee(target) && mc.player.distanceTo(target) > wallRange.get()) return false;
        if (!tick.passedMs(cooldown.get() * 1000)) {
            return false;
        }
        if (target instanceof LivingEntity entity && entity.hurtTime > hurtTime.get()) {
            return false;
        }
        return !usingPause.get() || !mc.player.isUsingItem();
    }
    private Entity getTarget(double range) {
        Entity target = null;
        double distance = range;
        for (Entity entity : mc.world.getEntities()) {
            if (!entities.get().contains(entity.getType())) continue;
            if (ignoreNamed.get() && entity.hasCustomName()) continue;
            if (ignoreTamed.get()) {
                if (entity instanceof Tameable tameable
                        && tameable.getOwnerUuid() != null
                        && tameable.getOwnerUuid().equals(mc.player.getUuid())
                ) continue;
            }
            if (ignorePassive.get()) {
                if (entity instanceof EndermanEntity enderman && !enderman.isAngry()) continue;
                if (entity instanceof ZombifiedPiglinEntity piglin && !piglin.isAttacking()) continue;
                if (entity instanceof WolfEntity wolf && !wolf.isAttacking()) continue;
            }
            if (!CombatUtil.isValid(entity,targetRange.get())) continue;
            if (target == null) {
                target = entity;
                distance = mc.player.distanceTo(entity);
            } else {
                if (mc.player.distanceTo(entity) < distance) {
                    target = entity;
                    distance = mc.player.distanceTo(entity);
                }
            }
        }
        return target;
    }
    private Vec3d getAttackVec(Entity entity) {
        return getClosestPointToBox(mc.player.getEyePos(), entity.getBoundingBox());
    }
    public enum Weapon {
        Sword,
        Axe,
        Mace,
        Trident,
        All,
        Any
    }
    public enum SwitchMode {
        Normal,
        Silent,
        None
    }
}
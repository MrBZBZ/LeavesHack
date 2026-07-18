package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.asm.accessors.IVec3d;
import com.dev.leavesHack.events.TravelEvent;
import com.dev.leavesHack.utils.combat.CombatUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.dev.leavesHack.utils.world.BlockPosX;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import static com.dev.leavesHack.utils.world.BlockUtil.getClosestPointToBox;

public class Follower extends Module {
    public static Follower INSTANCE;
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Boolean> autoDisable = sgGeneral.add(new BoolSetting.Builder()
            .name("AutoDisable")
            .description("自动关闭")
            .defaultValue(true)
            .build()
    );
    private final Setting<Integer> returnTime = sgGeneral.add(new IntSetting.Builder()
            .name("ReturnTime")
            .description("返回时间")
            .defaultValue(300)
            .min(0)
            .sliderMax(1000)
            .build()
    );
    private final Setting<Integer> targetRange = sgGeneral.add(new IntSetting.Builder()
            .name("TargetRange")
            .description("目标距离")
            .defaultValue(60)
            .min(0)
            .sliderMax(50)
            .build()
    );
    private final Setting<Double> attackRange = sgGeneral.add(new DoubleSetting.Builder()
            .name("AttackRange")
            .description("攻击距离")
            .defaultValue(3.5)
            .min(0)
            .sliderMax(6.0)
            .build()
    );
    private final Setting<Double> followSpeed = sgGeneral.add(new DoubleSetting.Builder()
            .name("Speed")
            .description("跟随速度")
            .defaultValue(1.7)
            .min(0)
            .sliderMax(5.0)
            .build()
    );
    public Follower() {
        super(LeavesHack.CATEGORY, "Follower", "自动追人");
        INSTANCE = this;
    }
    public PlayerEntity target;
    public float yaw;
    public float pitch;
    public boolean canFollow = false;
    public Timer returnTimer = new Timer();
    @Override
    public void onActivate() {
        returnTimer.setMs(9999999);
    }
    @Override
    public String getInfoString() {
        return target == null ? null : "[" + target.getName().getString() + "]";
    }
    @Override
    public void onDeactivate() {
        canFollow = false;
    }
    @EventHandler
    public void onTravel(TravelEvent event) {
        if (!FireworkElytraFly.INSTANCE.isFallFlying || !canFollow) return;
        double speed = followSpeed.get();
        double radYaw = Math.toRadians(yaw);
        double radPitch = Math.toRadians(pitch);
        double x = -Math.sin(radYaw) * Math.cos(radPitch) * speed;
        double y = -Math.sin(radPitch) * speed;
        double z = Math.cos(radYaw) * Math.cos(radPitch) * speed;
        setX(x);
        setY(y);
        setZ(z);
    }
    @EventHandler
    public void onTick(TickEvent.Pre event){
        if (!FireworkElytraFly.INSTANCE.isActive()) {
            if (autoDisable.get()) toggle();
            return;
        }
        target = CombatUtil.getClosestEnemy(targetRange.get());
        if (target == null || wantToMove()) {
            canFollow = false;
            return;
        }
        double myX = mc.player.getX();
        double myY = mc.player.getY();
        double myZ = mc.player.getZ();
        double x = target.getX();
        double z = target.getZ();
        double y = target.getY();
        double d = Math.sqrt(Math.pow(myX - x, 2) + Math.pow(myY - y, 2) + Math.pow(myZ - z, 2));
        double r = attackRange.get();
        BlockPos targetPos = new BlockPosX(x + r/d * (myX - x), y + r/d * (myY - y), z + r/d * (myZ - z));
        boolean shouldReturn = false;
        canFollow = true;
        if (!returnTimer.passedMs(returnTime.get())) shouldReturn = true;
        if (myY <= targetPos.getY()) {
            returnTimer.reset();
            shouldReturn = true;
        }
        if (mc.player.isOnGround()) {
            mc.player.jump();
            shouldReturn = true;
            returnTimer.reset();
        }
        Vec3d attackVec = getAttackVec(target);
        pitch = shouldReturn ? -90 : Rotation.getRotation(mc.player.getEyePos(), attackVec)[1];
        yaw = shouldReturn ? 0 : Rotation.getRotation(mc.player.getEyePos(), attackVec)[0];
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround(), mc.player.horizontalCollision));
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
    private Vec3d getAttackVec(Entity entity) {
        return getClosestPointToBox(mc.player.getEyePos(), entity.getBoundingBox());
    }
    private boolean wantToMove() {
        return mc.options.forwardKey.isPressed() || mc.options.backKey.isPressed() || mc.options.leftKey.isPressed() || mc.options.rightKey.isPressed() || mc.options.jumpKey.isPressed() || mc.options.sneakKey.isPressed();
    }
}

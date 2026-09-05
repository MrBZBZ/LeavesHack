package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.asm.accessors.IPlayerMoveC2SPacket;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public class ElytraGrimAccelerate extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Double> maxAccelerateVelocity = sgGeneral.add(new DoubleSetting.Builder()
        .name("MaxAccelerateVelocity")
        .description("超过此速度停止加速")
        .defaultValue(4.0)
        .min(0.0)
        .sliderMax(10.0)
        .build()
    );

    public final Setting<Double> minAccelerateVelocity = sgGeneral.add(new DoubleSetting.Builder()
        .name("MinAccelerateVelocity")
        .description("低于此速度开始加速")
        .defaultValue(3.6)
        .min(0.0)
        .sliderMax(10.0)
        .build()
    );

    public final Setting<SetBackMode> setBackMode = sgGeneral.add(new EnumSetting.Builder<SetBackMode>()
        .name("SetBackMode")
        .description("回退模式")
        .defaultValue(SetBackMode.SIMULATION)
        .build()
    );

    public final Setting<Boolean> filterVelocity = sgGeneral.add(new BoolSetting.Builder()
        .name("FilterVelocity")
        .description("过滤服务器速度更新包")
        .defaultValue(true)
        .build()
    );

    private boolean working = false;
    private long lastWorkingTick = 0;
    private Vec3 vec3d = Vec3.ZERO;
    private int tickCounter = 0;
    // 防止递归的标志
    private boolean isModifyingPacket = false;

    public ElytraGrimAccelerate() {
        super(LeavesHack.LEAVES_MISC, "ElytraGrimAccelerate", "GrimAC鞘翅加速");
    }

    @Override
    public void onActivate() {
        working = false;
        lastWorkingTick = 0;
        tickCounter = 0;
        vec3d = Vec3.ZERO;
        isModifyingPacket = false;
    }

    @Override
    public void onDeactivate() {
        working = false;
        isModifyingPacket = false;
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;

        tickCounter++;

        // 检查玩家是否正在鞘翅飞行
        if (!mc.player.isFallFlying()) {
            working = false;
            return;
        }

        // 获取当前速度
        vec3d = mc.player.getDeltaMovement();
        double horizontalSpeed = Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z);

        // 检查是否在移动
        if (horizontalSpeed < 0.01) {
            return;
        }

        // Pre-tick modify: 设置加速标志
        if (horizontalSpeed < minAccelerateVelocity.get() && !working) {
            working = true;
            lastWorkingTick = mc.player.level().getGameTime();
        }

        // 速度过高时停止加速
        if (horizontalSpeed > maxAccelerateVelocity.get()) {
            working = false;
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || !isActive()) return;
        // 防止递归
        if (isModifyingPacket) return;

        // 处理移动包
        if (event.packet instanceof ServerboundMovePlayerPacket packet) {
            if (!mc.player.isFallFlying()) return;

            double horizontalSpeed = Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z);

            // 如果正在加速，修改数据包
            if (working && horizontalSpeed < minAccelerateVelocity.get()) {
                isModifyingPacket = true;
                try {
                    modifyPacket(packet);
                } finally {
                    isModifyingPacket = false;
                }
            }
        }

        // 检测服务器回退包
        if (event.packet instanceof ServerboundAcceptTeleportationPacket) {
            // 服务器要求回退，停止加速
            if (working) {
                working = false;
                info("检测到服务器回退，停止加速");
            }
        }
    }

    /**
     * 修改数据包为虚假位置
     */
    private void modifyPacket(ServerboundMovePlayerPacket packet) {
        if (mc.player == null) return;

        Vec3 pos = mc.player.position();
        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();
        boolean onGround = mc.player.onGround();

        double fakeX, fakeY, fakeZ;

        switch (setBackMode.get()) {
            case SIMULATION -> {
                // SIMULATION 模式: Y轴偏移
                fakeX = pos.x;
                fakeY = pos.y + 2.5 * ((tickCounter % 3) + 1);
                fakeZ = pos.z;
            }
            case CRASH_PACKETS -> {
                // CRASH_PACKETS 模式: 异常大值
                fakeX = 3.9999999E7D;
                fakeY = pos.y + 2.5 * ((tickCounter % 3) + 1);
                fakeZ = Double.NEGATIVE_INFINITY;
            }
            default -> {
                return;
            }
        }

        // 使用 accessor 修改数据包
        IPlayerMoveC2SPacket accessor = (IPlayerMoveC2SPacket) packet;
        accessor.setY(fakeY);
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null || !isActive()) return;

        // 处理服务器速度更新包
        if (event.packet instanceof ClientboundSetEntityMotionPacket packet) {
            if (!filterVelocity.get()) return;
            if (packet.id() != mc.player.getId()) return;
            if (!mc.player.isFallFlying()) return;

            // 获取服务器发送的速度
            double velX = packet.movement().x / 8000.0;
            double velZ = packet.movement().z / 8000.0;
            double horizontalLengthSquared = velX * velX + velZ * velZ;
            info("服务器速度: " + horizontalLengthSquared);

            // 如果速度接近零，取消该包（防止被拉回）
            if (horizontalLengthSquared < 1E-4) {
                event.cancel();
                return;
            }

            // 如果速度与当前速度方向相反，取消该包（防止被减速）
            double dotProduct = vec3d.x * velX + vec3d.z * velZ;
            if (dotProduct < 0) {
                event.cancel();
            }
        }
    }

    @Override
    public String getInfoString() {
        if (!working) return null;
        double horizontalSpeed = Math.sqrt(vec3d.x * vec3d.x + vec3d.z * vec3d.z);
        return String.format("[%.2f]", horizontalSpeed);
    }

    public enum SetBackMode {
        SIMULATION,
        CRASH_PACKETS
    }
}

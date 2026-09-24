package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

public class DeathRender extends Module {
    public static DeathRender INSTANCE;
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    public DeathRender() {
        super(LeavesHack.LEAVES_MISC, "DeathRender", "死亡渲染");
        INSTANCE = this;
    }
    public final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("Range")
        .description("搜索范围")
        .defaultValue(12.0)
        .min(0.0)
        .max(20.0)
        .build()
    );
    @EventHandler
    public void onPacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket packet)) return;
        if (packet.getStatus() != 3) return;
        Entity entity = packet.getEntity(mc.world);
        if (!(entity instanceof PlayerEntity death)) return;
        if (death == mc.player) return;
        if (!PlayerUtils.isWithin(death, range.get())) return;
        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);
        lightning.setPosition(death.getX(), death.getY(), death.getZ());
        mc.world.addEntity(lightning);
    }
}

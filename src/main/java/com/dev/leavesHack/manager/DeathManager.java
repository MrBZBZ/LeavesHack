package com.dev.leavesHack.manager;

import com.dev.leavesHack.events.DeathEvent;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.AbstractClientPlayerEntity;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class DeathManager {
    public static DeathManager INSTANCE = new DeathManager();
    public DeathManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }
    @EventHandler
    public void onUpdate(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (player == null || !player.isDead()) {
                continue;
            }
            MeteorClient.EVENT_BUS.post(DeathEvent.get(player));
        }
    }
}

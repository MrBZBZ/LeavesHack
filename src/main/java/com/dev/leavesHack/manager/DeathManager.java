package com.dev.leavesHack.manager;

import com.dev.leavesHack.events.DeathEvent;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.player.AbstractClientPlayer;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class DeathManager {
    public static DeathManager INSTANCE = new DeathManager();
    public DeathManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }
    @EventHandler
    public void onUpdate(TickEvent.Pre event) {
        if (mc.player == null || mc.level == null) return;
        for (AbstractClientPlayer player : mc.level.players()) {
            if (player == null || !player.isDeadOrDying()) {
                continue;
            }
            MeteorClient.EVENT_BUS.post(DeathEvent.get(player));
        }
    }
}

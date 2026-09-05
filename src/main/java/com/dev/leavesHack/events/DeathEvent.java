package com.dev.leavesHack.events;

import net.minecraft.world.entity.player.Player;

public class DeathEvent {
    private DeathEvent() {
    }

    private static final DeathEvent INSTANCE = new DeathEvent();
    private Player player;

    public static DeathEvent get(Player player) {
        INSTANCE.player = player;
        return INSTANCE;
    }

    public Player getPlayer() {
        return player;
    }
}

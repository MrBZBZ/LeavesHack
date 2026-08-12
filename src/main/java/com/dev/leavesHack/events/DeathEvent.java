package com.dev.leavesHack.events;

import net.minecraft.entity.player.PlayerEntity;

public class DeathEvent {
    private DeathEvent() {
    }

    private static final DeathEvent INSTANCE = new DeathEvent();
    private PlayerEntity player;

    public static DeathEvent get(PlayerEntity player) {
        INSTANCE.player = player;
        return INSTANCE;
    }

    public PlayerEntity getPlayer() {
        return player;
    }
}

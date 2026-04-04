package com.dev.leavesHack.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.entity.player.PlayerEntity;

public class TravelEvent extends Cancellable {

    private final PlayerEntity entity;


    public TravelEvent(PlayerEntity entity) {
        this.entity = entity;
    }

    public PlayerEntity getEntity() {
        return entity;
    }
}
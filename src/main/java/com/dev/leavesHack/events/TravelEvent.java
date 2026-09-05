package com.dev.leavesHack.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.world.entity.player.Player;

public class TravelEvent extends Cancellable {

    private final Player entity;


    public TravelEvent(Player entity) {
        this.entity = entity;
    }

    public Player getEntity() {
        return entity;
    }
}

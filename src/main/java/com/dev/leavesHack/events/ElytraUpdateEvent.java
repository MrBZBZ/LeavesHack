package com.dev.leavesHack.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.entity.Entity;

public class ElytraUpdateEvent extends Cancellable {
    private final Entity entity;

    public ElytraUpdateEvent(Entity entity) {
        this.entity = entity;
    }

    public Entity getEntity() {
        return entity;
    }
}
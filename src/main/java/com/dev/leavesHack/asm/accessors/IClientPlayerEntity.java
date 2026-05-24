package com.dev.leavesHack.asm.accessors;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPlayerEntity.class)
public interface IClientPlayerEntity {
    @Accessor("ticksSinceLastPositionPacketSent")
    void setTicksSinceLastPositionPacketSent(int yaw);

    @Accessor("lastYaw")
    float getLastYaw();

    @Accessor("lastYaw")
    void setLastYaw(float yaw);

    @Accessor("lastPitch")
    float getLastPitch();

    @Accessor("lastPitch")
    void setLastPitch(float pitch);
}

package com.dev.leavesHack.asm.accessors;

import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundUseItemPacket.class)
public interface IPlayerInterectItemC2SPacket {
    @Mutable
    @Accessor("yRot")
    void setYaw(float yaw);

    @Mutable
    @Accessor("xRot")
    void setPitch(float pitch);
}

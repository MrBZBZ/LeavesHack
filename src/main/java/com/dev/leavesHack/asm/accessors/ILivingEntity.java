package com.dev.leavesHack.asm.accessors;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface ILivingEntity {

    @Accessor("noJumpDelay")
    int getLastJumpCooldown();

    @Accessor("noJumpDelay")
    void setLastJumpCooldown(int val);

    @Accessor("swimAmount")
    float getLeaningPitch();

    @Accessor("swimAmount")
    void setLeaningPitch(float val);

    @Accessor("swimAmountO")
    void setLastLeaningPitch(float val);
}

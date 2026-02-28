package com.dev.leavesHack.asm.accessors;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface ILivingEntity {
    @Accessor("lastAttackedTicks")
    int getLastAttackedTicks();

    @Accessor("jumpingCooldown")
    int getLastJumpCooldown();

    @Accessor("jumpingCooldown")
    void setLastJumpCooldown(int val);

    @Accessor("leaningPitch")
    float getLeaningPitch();

    @Accessor("leaningPitch")
    void setLeaningPitch(float val);

    @Accessor("lastLeaningPitch")
    void setLastLeaningPitch(float val);
}

package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.utils.combat.CombatUtil;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(World.class)
public class MixinWorld {
    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    public void blockStateHook(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (mc.world != null && mc.world.isInBuildLimit(pos)) {
            BlockPos modifyPos = CombatUtil.modifyPos;
            BlockState modifyBlockState = CombatUtil.modifyBlockState;
            if (modifyPos != null && modifyBlockState != null) {
                if (pos.equals(modifyPos)) {
                    cir.setReturnValue(modifyBlockState);
                }
            }
        }
    }
}

package com.dev.leavesHack.asm.mixin;

import baritone.api.utils.BlockOptionalMetaLookup;
import baritone.pathing.movement.CalculationContext;
import baritone.process.MineProcess;
import com.dev.leavesHack.modules.AntiAntiXray;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

@Mixin(value = MineProcess.class)
public class MineProcessMixin {

    @Shadow(remap = false)
    private List<BlockPos> a; // knownOreLocations
    @Inject(method = "a(Ljava/util/List;Lbaritone/pathing/movement/CalculationContext;)V", at = @At("TAIL"), remap = false)
    private void onRescan(List<BlockPos> already, CalculationContext context, CallbackInfo ci) {
        AntiAntiXray antiAntiXray = Modules.get().get(AntiAntiXray.class);
        if (antiAntiXray == null || !antiAntiXray.baritone())
            return;
        a = antiAntiXray.breakList;
    }
//    @Redirect(method = "a(Lbaritone/pathing/movement/CalculationContext;Lbaritone/api/utils/BlockOptionalMetaLookup;Ljava/util/List;Lnet/minecraft/util/math/BlockPos;)Z",
//            at = @At(value = "INVOKE", target = "Lbaritone/api/utils/BlockOptionalMetaLookup;has(Lnet/minecraft/block/BlockState;)Z"))
//    private static boolean onPruneStream(BlockOptionalMetaLookup instance, BlockState blockState) {
//        AntiAntiXray antiAntiXray = Modules.get().get(AntiAntiXray.class);
//        if (antiAntiXray == null || !antiAntiXray.baritone())
//            return instance.has(blockState);
//        return !blockState.isAir();
//    }
    @WrapOperation(
        method = "a(Lbaritone/pathing/movement/CalculationContext;Lbaritone/api/utils/BlockOptionalMetaLookup;Ljava/util/List;Lnet/minecraft/util/math/BlockPos;)Z",
        at = @At(
            value = "INVOKE",
            target = "Lbaritone/api/utils/BlockOptionalMetaLookup;has(Lnet/minecraft/block/BlockState;)Z"
        )
    )
    private static boolean wrapHas(BlockOptionalMetaLookup instance, BlockState state, Operation<Boolean> original) {
        AntiAntiXray antiAntiXray = Modules.get().get(AntiAntiXray.class);
        if (antiAntiXray != null && antiAntiXray.baritone()) {
            return !state.isAir();
        }
        return original.call(instance, state);
    }
}

package com.dev.leavesHack.asm.mixin;


import com.dev.leavesHack.events.PlaceBlockEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class MixinBlockItem {
    @Inject(method = "placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z", at = @At("RETURN"))
    private void onPlace(@NotNull BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> info) {
        if (context.getLevel().isClientSide())
            MeteorClient.EVENT_BUS.post(new PlaceBlockEvent(context.getClickedPos(), state.getBlock()));
    }
}

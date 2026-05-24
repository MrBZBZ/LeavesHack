package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.modules.GlobalSetting;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.handler.DecoderHandler;
import net.minecraft.network.handler.PacketEncoderException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DecoderHandler.class)
public class MixinDecoderHandler {
    @WrapOperation(method = "decode", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/codec/PacketCodec;decode(Ljava/lang/Object;)Ljava/lang/Object;"))
    public Object onDecodeException(PacketCodec instance, Object o, Operation<Object> original){
        try {
            return original.call(instance, o);
        } catch (DecoderException e) {
            if (GlobalSetting.INSTANCE.packetKickFix.get() && o instanceof ByteBuf buf) {
                buf.skipBytes(buf.readableBytes());
            }
            throw e;
        }
    }
}

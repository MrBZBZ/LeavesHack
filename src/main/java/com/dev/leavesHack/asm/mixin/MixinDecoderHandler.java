package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.modules.GlobalSetting;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import net.fabricmc.loader.impl.lib.tinyremapper.extension.mixin.common.Logger;
import net.minecraft.network.NetworkState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.handler.DecoderHandler;
import net.minecraft.network.handler.NetworkStateTransitionHandler;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.List;

@Mixin(DecoderHandler.class)
public class MixinDecoderHandler<T extends PacketListener> {
    @Shadow private NetworkState<T> state;
    @Inject(method = "decode", at = @At("HEAD"), cancellable = true)
    private void safeDecode(ChannelHandlerContext context, ByteBuf buf, List<Object> objects, CallbackInfo ci) {
        if (!GlobalSetting.INSTANCE.packetKickFix.get()) return;
        int size = buf.readableBytes();
        if (size == 0) return;
        try {
            Packet<? super T> packet = this.state.codec().decode(buf);
            if (buf.readableBytes() > 0) {
                throw new IOException("Extra bytes after packet decode");
            }
            objects.add(packet);
            NetworkStateTransitionHandler.onDecoded(context, packet);
        } catch (Throwable t) {
            ci.cancel();
            buf.skipBytes(buf.readableBytes());
        }
    }
}

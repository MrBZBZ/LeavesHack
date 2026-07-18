package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.modules.GlobalSetting;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.handler.DecoderHandler;
import net.minecraft.network.handler.NetworkStateTransitionHandler;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.PacketType;
import net.minecraft.network.state.NetworkState;
import net.minecraft.util.profiling.jfr.FlightProfiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DecoderHandler.class)
public class MixinDecoderHandler<T extends PacketListener> {
    @Shadow private NetworkState<T> state;

    @Inject(method = "decode", at = @At("HEAD"), cancellable = true)
    private void safeDecode(ChannelHandlerContext context, ByteBuf buf, List<Object> objects, CallbackInfo ci) throws Exception {
        if (GlobalSetting.INSTANCE == null || !GlobalSetting.INSTANCE.packetKickFix.get()) return;

        int i = buf.readableBytes();
        if (i == 0) return;

        try {
            Packet<? super T> packet = this.state.codec().decode(buf);
            PacketType<? extends Packet<? super T>> packetType = packet.getPacketType();
            FlightProfiler.INSTANCE.onPacketReceived(this.state.id(), packetType, context.channel().remoteAddress(), i);
            if (buf.readableBytes() > 0) {
                buf.skipBytes(buf.readableBytes());
            } else {
                objects.add(packet);
                NetworkStateTransitionHandler.onDecoded(context, packet);
            }
            ci.cancel();
        } catch (Exception e) {
            ci.cancel();
            buf.skipBytes(buf.readableBytes());
        }
    }
}
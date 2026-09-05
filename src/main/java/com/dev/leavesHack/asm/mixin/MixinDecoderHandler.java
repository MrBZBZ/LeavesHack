package com.dev.leavesHack.asm.mixin;

import com.dev.leavesHack.modules.GlobalSetting;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.List;
import net.minecraft.network.PacketDecoder;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.ProtocolSwapHandler;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PacketDecoder.class)
public class MixinDecoderHandler<T extends PacketListener> {
    @Shadow @Final private ProtocolInfo<T> protocolInfo;

    @Inject(method = "decode", at = @At("HEAD"), cancellable = true)
    private void safeDecode(ChannelHandlerContext context, ByteBuf buf, List<Object> objects, CallbackInfo ci) throws Exception {
        if (GlobalSetting.INSTANCE == null || !GlobalSetting.INSTANCE.packetKickFix.get()) return;

        int i = buf.readableBytes();
        if (i == 0) return;

        try {
            Packet<? super T> packet = this.protocolInfo.codec().decode(buf);
            PacketType<? extends Packet<? super T>> packetType = packet.type();
            JvmProfiler.INSTANCE.onPacketReceived(this.protocolInfo.id(), packetType, context.channel().remoteAddress(), i);
            if (buf.readableBytes() > 0) {
                buf.skipBytes(buf.readableBytes());
            } else {
                objects.add(packet);
                ProtocolSwapHandler.handleInboundTerminalPacket(context, packet);
            }
            ci.cancel();
        } catch (Exception e) {
            ci.cancel();
            buf.skipBytes(buf.readableBytes());
        }
    }
}
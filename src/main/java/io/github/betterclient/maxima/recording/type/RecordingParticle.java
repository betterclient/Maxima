package io.github.betterclient.maxima.recording.type;

import io.github.betterclient.maxima.util.access.ParticlePacketWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.registry.DynamicRegistryManager;

public class RecordingParticle {
    public final byte[] data;

    public RecordingParticle(ParticleS2CPacket particle, DynamicRegistryManager regMan) {
        ByteBuf byteBuf = Unpooled.buffer();
        ((ParticlePacketWriter) particle).maxima$writePacket(new RegistryByteBuf(byteBuf, regMan));
        data = byteBuf.array();
        byteBuf.release();
    }

    public RecordingParticle(byte[] data) {
        this.data = data;
    }

    public ParticleS2CPacket create() {
        return new ParticleS2CPacket(new RegistryByteBuf(Unpooled.wrappedBuffer(data), MinecraftClient.getInstance().world.getRegistryManager()));
    }
}
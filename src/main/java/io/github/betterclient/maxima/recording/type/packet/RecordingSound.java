package io.github.betterclient.maxima.recording.type.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.DynamicRegistryManager;

public class RecordingSound {
    public final byte[] data;

    public RecordingSound(PlaySoundS2CPacket sound, DynamicRegistryManager regMan) {
        ByteBuf byteBuf = Unpooled.buffer();
        sound.write(new RegistryByteBuf(byteBuf, regMan));
        data = byteBuf.array();
        byteBuf.release();
    }

    public RecordingSound(byte[] data) {
        this.data = data;
    }

    public PlaySoundS2CPacket create() {
        return new PlaySoundS2CPacket(new RegistryByteBuf(Unpooled.wrappedBuffer(data), MinecraftClient.getInstance().world.getRegistryManager()));
    }
}
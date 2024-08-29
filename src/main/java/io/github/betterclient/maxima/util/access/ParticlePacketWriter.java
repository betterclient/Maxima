package io.github.betterclient.maxima.util.access;

import net.minecraft.network.RegistryByteBuf;

public interface ParticlePacketWriter {
    void maxima$writePacket(RegistryByteBuf buf);
}

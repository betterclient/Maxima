package io.github.betterclient.maxima.util;

import net.minecraft.network.PacketByteBuf;

public interface ChunkInvoker {
    void maxima$writeHeightmap(PacketByteBuf pbb);
    void maxima$writeData(PacketByteBuf pbb);
}

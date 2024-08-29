package io.github.betterclient.maxima.util.access;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;

public interface ChunkInvoker {
    void maxima$writeHeightmap(PacketByteBuf pbb);
    void maxima$writeData(PacketByteBuf pbb);
    void maxima$writeBlockEntities(RegistryByteBuf pbb);
}

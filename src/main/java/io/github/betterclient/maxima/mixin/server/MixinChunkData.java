package io.github.betterclient.maxima.mixin.server;

import io.github.betterclient.maxima.util.ChunkInvoker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkData.class)
public class MixinChunkData implements ChunkInvoker {
    @Shadow @Final private NbtCompound heightmap;

    @Shadow @Final private byte[] sectionsData;

    @Override
    public void maxima$writeHeightmap(PacketByteBuf pbb) {
        pbb.writeNbt(this.heightmap);
    }

    @Override
    public void maxima$writeData(PacketByteBuf pbb) {
        pbb.writeBytes(this.sectionsData);
    }
}

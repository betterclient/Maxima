package io.github.betterclient.maxima.mixin.server;

import io.github.betterclient.maxima.MaximaClient;
import io.github.betterclient.maxima.recording.MaximaRecording;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkDataS2CPacket.class)
public class MixinChunkDataPacket {
    @Shadow @Final private int chunkX;

    @Shadow @Final private int chunkZ;

    @Shadow @Final private ChunkData chunkData;
    @Unique private boolean add = false;

    @Inject(method = "apply(Lnet/minecraft/network/listener/ClientPlayPacketListener;)V", at = @At("HEAD"))
    public void start(ClientPlayPacketListener clientPlayPacketListener, CallbackInfo ci) {
        MaximaRecording recording = MaximaClient.instance.recording;
        if(recording == null || !recording.shouldAddChunks || !MaximaClient.instance.isRecording) {
            add = false;
            return;
        }

        recording.shouldAddChunks = false;
        add = true;
    }

    @Inject(method = "apply(Lnet/minecraft/network/listener/ClientPlayPacketListener;)V", at = @At("RETURN"))
    public void apply(ClientPlayPacketListener clientPlayPacketListener, CallbackInfo ci) {
        MaximaRecording recording = MaximaClient.instance.recording;
        if(recording == null || add || !MaximaClient.instance.isRecording) return;

        recording.update(MinecraftClient.getInstance().world.getChunk(this.chunkX, this.chunkZ), this.chunkData);
        recording.shouldAddChunks = true;
    }
}

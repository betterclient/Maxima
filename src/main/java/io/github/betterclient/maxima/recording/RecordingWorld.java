package io.github.betterclient.maxima.recording;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.Map;

public class RecordingWorld {
    public Map<ChunkPos, ChunkData> chunkData = new HashMap<>();
    public Map<ChunkPos, ReadableChunkData> readData = new HashMap<>();
    public MaximaRecording recording;

    public RecordingWorld(MaximaRecording recording, boolean isFirst) {
        this.recording = recording;
        ClientChunkManager cm = MinecraftClient.getInstance().world.getChunkManager();

        for (int i = 0; i < cm.chunks.chunks.length(); i++) {
            WorldChunk chunk = cm.chunks.chunks.get(i);
            if(chunk != null) {
                chunkData.put(chunk.getPos(), new ChunkData(chunk));
            }
        }
    }

    public RecordingWorld(MaximaRecording recording) {
        this.recording = recording;
        //empty world(changes append here!!)
    }
}

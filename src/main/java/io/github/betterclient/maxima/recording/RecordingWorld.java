package io.github.betterclient.maxima.recording;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

import java.util.HashMap;
import java.util.Map;

public class RecordingWorld {
    public Map<ChunkPos, RecordChunk> chunks = new HashMap<>();
    public MaximaRecording recording;

    public RecordingWorld(MaximaRecording recording, boolean isFirst) {
        this.recording = recording;
        ClientChunkManager cm = MinecraftClient.getInstance().world.getChunkManager();

        for (int i = 0; i < cm.chunks.chunks.length(); i++) {
            WorldChunk chunk = cm.chunks.chunks.get(i);
            if(chunk != null) {
                chunks.put(chunk.getPos(), new RecordChunk(chunk, null));
            }
        }
    }

    public RecordChunk getChunk(int x, int z) {
        return chunks.get(new ChunkPos(x, z));
    }

    public RecordChunk getChunk(Chunk chunk) {
        return chunks.get(chunk.getPos());
    }

    public RecordingWorld(MaximaRecording recording) {
        this.recording = recording;
        //empty world(changes append here!!)
    }
}

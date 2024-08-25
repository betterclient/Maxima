package io.github.betterclient.maxima.recording;

import io.github.betterclient.maxima.MaximaClient;
import io.github.betterclient.maxima.util.recording.RecordingRenderer;
import io.github.betterclient.maxima.util.recording.WorldGeneration;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;

public class MaximaRecording {
    public static MaximaRecording loadedRecording;
    public static boolean generatingWorld = false;
    public static String genProg = "";
    public static int currentTick = 0;
    public static BlockPos lastGenPos;
    public static volatile boolean isPaused = true;
    public static long lastPauseTime = 0;

    public List<RecordingWorld> worlds = new ArrayList<>();
    public List<List<RecordingEntity>> entities = new ArrayList<>();
    public boolean shouldAddChunks = false;
    public int tickCount = 0;

    public MaximaRecording(boolean empty) {}

    public MaximaRecording() {
        if (MinecraftClient.getInstance().world == null) return;

        worlds.add(new RecordingWorld(this, true));
        entities.add(RecordingEntity.getCurrentList(MinecraftClient.getInstance().world));
    }

    public static void load(MaximaRecording recording) {
        WorldGeneration.load(recording);
    }

    public static void world(DrawContext context) {
        RecordingRenderer.render(context, loadedRecording);
    }

    public static void generateWorld() {
        WorldGeneration.generate();
    }

    public void tick() {
        worlds.add(new RecordingWorld(this));
        tickCount++;
        shouldAddChunks = true;
    }

    public void update(WorldChunk chunk) {
        update(chunk, new ChunkData(chunk));
    }

    public void update(WorldChunk chunk, ChunkData chunkData) {
        if(MaximaClient.instance.isSaving) return;

        this.worlds.getLast().chunkData.remove(chunk.getPos());
        this.worlds.getLast().chunkData.put(chunk.getPos(), chunkData);
    }

    public void tickEntities() {
        if (MinecraftClient.getInstance().world == null) return;
        entities.add(RecordingEntity.getCurrentList(MinecraftClient.getInstance().world));
    }
}

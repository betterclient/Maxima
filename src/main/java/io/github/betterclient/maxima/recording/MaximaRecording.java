package io.github.betterclient.maxima.recording;

import io.github.betterclient.maxima.MaximaClient;
import io.github.betterclient.maxima.recording.type.RecordWorldTime;
import io.github.betterclient.maxima.recording.type.RecordingEntity;
import io.github.betterclient.maxima.recording.type.packet.RecordingParticle;
import io.github.betterclient.maxima.recording.type.RecordingWorld;
import io.github.betterclient.maxima.recording.type.packet.RecordingSound;
import io.github.betterclient.maxima.ui.RecordingRenderer;
import io.github.betterclient.maxima.util.TickTracker;
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
    public List<List<RecordingParticle>> particlePackets = new ArrayList<>();
    public List<List<RecordingSound>> soundPackets = new ArrayList<>();
    public List<RecordWorldTime> worldTimes = new ArrayList<>();
    public boolean shouldAddChunks = false;
    public int tickCount = 0;

    public MaximaRecording(boolean empty) {}

    public MaximaRecording() {
        if (MinecraftClient.getInstance().world == null) return;

        worlds.add(new RecordingWorld(this, true));
        entities.add(RecordingEntity.getCurrentList(MinecraftClient.getInstance().world));
        particlePackets.add(new ArrayList<>());
        soundPackets.add(new ArrayList<>());
        worldTimes.add(new RecordWorldTime(MinecraftClient.getInstance().world.getTimeOfDay()));
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

    public static void setPaused(boolean b) {
        isPaused = b;
        MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("tick " + (b ? "" : "un") + "freeze");

        if (!b) {
            MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("tick rate " + TickTracker.CURRENT_TRACKER.tickRate);
        }
    }

    public void tick() {
        worldTimes.add(new RecordWorldTime(MinecraftClient.getInstance().world.getTimeOfDay()));
        worlds.add(new RecordingWorld(this));
        particlePackets.add(new ArrayList<>());
        soundPackets.add(new ArrayList<>());
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

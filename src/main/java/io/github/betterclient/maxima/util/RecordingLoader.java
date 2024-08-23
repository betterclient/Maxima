package io.github.betterclient.maxima.util;

import io.github.betterclient.maxima.MaximaClient;
import io.github.betterclient.maxima.recording.MaximaRecording;
import io.github.betterclient.maxima.recording.ReadableChunkData;
import io.github.betterclient.maxima.recording.RecordingWorld;
import net.minecraft.util.math.ChunkPos;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static io.github.betterclient.maxima.MaximaClient.LOGGER;

public class RecordingLoader {
    public static void loadRecording(File file) {
        MaximaClient.instance.isPlayback = true;
        try {
            MaximaRecording recording = new MaximaRecording(true);
            ZipFile f = new ZipFile(file);

            parseWorlds(f, recording);

            f.close();
            LOGGER.info("Loaded {}!", file.getName().replace(".mxr", ""));
            LOGGER.info("Creating world!");

            MaximaRecording.load(recording);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void parseWorlds(ZipFile file, MaximaRecording recording) throws IOException {
        int amountTicks = Integer.parseInt(new String(readAndClose(file.getInputStream(file.getEntry("world.txt")))));
        recording.tickCount = amountTicks;
        for (int i = 0; i < amountTicks; i++) {
            recording.worlds.add(new RecordingWorld(recording));
        }

        var entries = file.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if (!entry.getName().startsWith("block/") || entry.isDirectory()) continue;
            byte[] data = readAndClose(file.getInputStream(entry));
            parseIntoChunk(entry.getName(), data, recording);
        }
    }

    private static void parseIntoChunk(String name, byte[] data, MaximaRecording recording) {
        String trimmed = name.substring(name.indexOf("/") + 1, name.lastIndexOf("."));
        String[] parts = trimmed.split("/");
        String[] dataXY = parts[1].split(",");
        int count = Integer.parseInt(parts[0]);
        int x = Integer.parseInt(dataXY[0]);
        int z = Integer.parseInt(dataXY[1]);

        if (name.endsWith(".data")) {
            recording.worlds.get(count).readData.computeIfAbsent(new ChunkPos(x, z), chunkPos -> new ReadableChunkData()).chunkData = data;
        } else if(name.endsWith(".heightmap")) {
            recording.worlds.get(count).readData.computeIfAbsent(new ChunkPos(x, z), chunkPos -> new ReadableChunkData()).heightmap = data;
        }
    }

    private static byte[] readAndClose(InputStream inputStream) throws IOException {
        byte[] out = inputStream.readAllBytes();
        inputStream.close();
        return out;
    }
}

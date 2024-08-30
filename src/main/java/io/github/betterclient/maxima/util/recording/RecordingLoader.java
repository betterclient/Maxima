package io.github.betterclient.maxima.util.recording;

import io.github.betterclient.maxima.MaximaClient;
import io.github.betterclient.maxima.recording.*;
import io.github.betterclient.maxima.recording.type.RecordWorldTime;
import io.github.betterclient.maxima.recording.type.RecordingEntity;
import io.github.betterclient.maxima.recording.type.packet.RecordingParticle;
import io.github.betterclient.maxima.recording.type.RecordingWorld;
import io.github.betterclient.maxima.recording.type.packet.RecordingSound;
import io.github.betterclient.maxima.recording.util.ReadableChunkData;
import net.minecraft.SharedConstants;
import net.minecraft.util.math.ChunkPos;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static io.github.betterclient.maxima.MaximaClient.LOGGER;

public class RecordingLoader {
    public static boolean loadRecording(File file) {
        MaximaClient.instance.isPlayback = true;
        try {
            MaximaRecording recording = new MaximaRecording(true);
            ZipFile f = new ZipFile(file);

            String version = new String(readAndClose(f.getInputStream(f.getEntry("minecraft.version"))));
            if (!version.equals(SharedConstants.getGameVersion().getName())) {
                LOGGER.warn("Unsupported recording (minecraft version {} ≠ {})", version, SharedConstants.getGameVersion().getName());
                return false;
            }

            parseWorlds(f, recording);
            parseEntities(f, recording);
            parseParticles(f, recording);
            parseSounds(f, recording);
            parseWorldTimes(readAndClose(f.getInputStream(f.getEntry("worldtime.json"))), recording);

            f.close();
            LOGGER.info("Loaded {}!", file.getName().replace(".mxr", ""));
            LOGGER.info("Creating world!");

            MaximaRecording.load(recording);
            return true;
        } catch (IOException e) {
            LOGGER.error("Failed parsing", e);
            return false;
        }
    }

    private static void parseWorldTimes(byte[] data, MaximaRecording recording) {
        JSONObject obj = new JSONObject(new String(data));
        int maxTime = 0;
        for (String s : obj.keySet()) {
            if (Integer.parseInt(s) > maxTime) {
                maxTime = Integer.parseInt(s);
            }
        }

        for (int i = 0; i < maxTime; i++) {
            recording.worldTimes.add(new RecordWorldTime(obj.getLong(i + "")));
        }
    }

    private static void parseSounds(ZipFile file, MaximaRecording recording) throws IOException {
        var entriesPrior = file.entries();
        int maxTick = 0;
        while (entriesPrior.hasMoreElements()) {
            ZipEntry entry = entriesPrior.nextElement();
            String name = entry.getName();
            if (!name.startsWith("sound/") || entry.isDirectory()) continue;

            String trimmed = name.substring(name.indexOf("/") + 1, name.lastIndexOf("."));
            String[] parts = trimmed.split("/");
            int tick = Integer.parseInt(parts[0]);

            if (tick > maxTick) {
                maxTick = tick;
            }
        }

        for (int i = -1; i < maxTick; i++) {
            recording.soundPackets.add(new ArrayList<>());
        }

        var entries = file.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if (!entry.getName().startsWith("sound/") || entry.isDirectory()) continue;
            byte[] data = readAndClose(file.getInputStream(entry));
            parseSound(entry.getName(), data, recording);
        }
    }

    private static void parseSound(String name, byte[] data, MaximaRecording recording) {
        String trimmed = name.substring(name.indexOf("/") + 1, name.lastIndexOf("."));
        String[] parts = trimmed.split("/");
        int tick = Integer.parseInt(parts[0]);
        recording.soundPackets.get(tick).add(new RecordingSound(data));
    }

    private static void parseParticles(ZipFile file, MaximaRecording recording) throws IOException {
        var entriesPrior = file.entries();
        int maxTick = 0;
        while (entriesPrior.hasMoreElements()) {
            ZipEntry entry = entriesPrior.nextElement();
            String name = entry.getName();
            if (!name.startsWith("particle/") || entry.isDirectory()) continue;

            String trimmed = name.substring(name.indexOf("/") + 1, name.lastIndexOf("."));
            String[] parts = trimmed.split("/");
            int tick = Integer.parseInt(parts[0]);

            if (tick > maxTick) {
                maxTick = tick;
            }
        }

        for (int i = -1; i < maxTick; i++) {
            recording.particlePackets.add(new ArrayList<>());
        }

        var entries = file.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if (!entry.getName().startsWith("particle/") || entry.isDirectory()) continue;
            byte[] data = readAndClose(file.getInputStream(entry));
            parseParticle(entry.getName(), data, recording);
        }
    }

    private static void parseParticle(String name, byte[] data, MaximaRecording recording) {
        String trimmed = name.substring(name.indexOf("/") + 1, name.lastIndexOf("."));
        String[] parts = trimmed.split("/");
        int tick = Integer.parseInt(parts[0]);
        recording.particlePackets.get(tick).add(new RecordingParticle(data));
    }

    private static void parseEntities(ZipFile file, MaximaRecording recording) throws IOException {
        var entries = file.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if (!entry.getName().startsWith("entity/") || entry.isDirectory()) continue;
            byte[] data = readAndClose(file.getInputStream(entry));
            parseEntity(entry.getName(), data, recording);
        }
    }

    private static void parseEntity(String name, byte[] data, MaximaRecording recording) throws IOException {
        String trimmed = name.substring(name.indexOf("/") + 1, name.lastIndexOf("."));
        String[] parts = trimmed.split("/");
        int tick = Integer.parseInt(parts[0]);

        if(recording.entities.size() - 1 < tick) {
            recording.entities.add(new ArrayList<>());
        }

        RecordingEntity entity = new RecordingEntity(data);
        recording.entities.getLast().add(entity);
        entity.uuid = parts[1];
        entity.isPlayer = name.endsWith("P");

        if (entity.isPlayer) {
            Pattern pattern = Pattern.compile("\\d+");
            entity.uuid = getBuffer(pattern, entity).toString();
            entity.updateUUID();
        }
    }

    //Shuffle UUIDs
    private static @NotNull StringBuffer getBuffer(Pattern pattern, RecordingEntity entity) {
        Matcher matcher = pattern.matcher(entity.uuid);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String match = matcher.group();
            if (match.length() > 1) {
                continue;
            }

            int number = Integer.parseInt(match);

            // Increment the number, and wrap around if it is 9
            if (number == 9) {
                matcher.appendReplacement(sb, "0");
            } else {
                matcher.appendReplacement(sb, String.valueOf(number + 1));
            }
        }
        matcher.appendTail(sb);
        return sb;
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
        } else if(name.endsWith(".blockEntity")) {
            recording.worlds.get(count).readData.computeIfAbsent(new ChunkPos(x, z), chunkPos -> new ReadableChunkData()).blockEntities = data;
        }
    }

    private static byte[] readAndClose(InputStream inputStream) throws IOException {
        byte[] out = inputStream.readAllBytes();
        inputStream.close();
        return out;
    }
}

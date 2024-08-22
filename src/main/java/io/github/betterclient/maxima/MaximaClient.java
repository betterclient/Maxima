package io.github.betterclient.maxima;

import io.github.betterclient.maxima.recording.MaximaRecording;
import io.github.betterclient.maxima.recording.RecordChunk;
import io.github.betterclient.maxima.recording.RecordingWorld;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.buffer.UnpooledUnsafeDirectByteBuf;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.math.ChunkPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.util.*;

public class MaximaClient implements ClientModInitializer {
    public static int OP_key = GLFW.GLFW_KEY_F4;

    public static final Logger LOGGER = LogManager.getLogger("Maxima");
    public static MaximaClient instance;
    public static File config;
    public static File recordings = new File(MinecraftClient.getInstance().runDirectory, "maxima-recordings");

    public boolean isRecording = false;
    public boolean isSaving = false;
    public MaximaRecording recording;
    public float saveProgress;
    public boolean isWaitingForWorld = false;
    public boolean isPlayback;

    @Override
    public void onInitializeClient() {
        instance = this;
        LOGGER.info("Loading Maxima!");

        try {
            Files.createDirectories(recordings.toPath());

            File f = FabricLoader.getInstance().getConfigDir().toFile();
            f = new File(f, "maxima.conf");
            config = f;

            if(!f.exists()) {
                LOGGER.info("Missing config! Creating default config. (First launch?)");
                this.save(f);
            } else {
                LOGGER.info("Loading config!");
                this.load(f);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("Loaded Maxima!");
    }

    private void load(File config) throws FileNotFoundException, IllegalAccessException {
        Scanner scanner = new Scanner(config);

        while (scanner.hasNext()) {
            String curLine = scanner.nextLine();

            for (Field declaredField : MaximaClient.class.getDeclaredFields()) {
                if(Modifier.isStatic(declaredField.getModifiers()) && curLine.startsWith(declaredField.getName() + "=") && declaredField.getName().startsWith("OP_")) {
                    declaredField.set(null, Integer.parseInt(curLine.replace(declaredField.getName() + "=", "")));
                }
            }
        }

        scanner.close();
    }

    public void save(File config) throws IOException, IllegalAccessException {
        FileOutputStream fileOutputStream = new FileOutputStream(config);

        StringBuilder file = new StringBuilder();

        for (Field declaredField : MaximaClient.class.getDeclaredFields()) {
            if(Modifier.isStatic(declaredField.getModifiers()) && declaredField.getName().startsWith("OP_")) {
                file.append(declaredField.getName()).append("=").append(declaredField.get(null)).append("\n");
            }
        }

        fileOutputStream.write(file.toString().getBytes());
        fileOutputStream.close();
    }

    public void saveRecording() throws IOException {
        Calendar cal = Calendar.getInstance();
        String fileName = cal.get(Calendar.DAY_OF_MONTH) + "-" + (cal.get(Calendar.MONTH) + 1) + "-" + cal.get(Calendar.YEAR) + " " + cal.get(Calendar.HOUR) + "." + cal.get(Calendar.MINUTE) + "." + cal.get(Calendar.SECOND);
        File f = new File(recordings, fileName + ".mxr");
        isSaving = true;
        new Thread(() -> {
            try {
                FileOutputStream fos = new FileOutputStream(f);
                JSONObject obj = new JSONObject();
                obj.put("amountTicks", this.recording.tickCount);

                JSONArray array = new JSONArray();

                for (RecordingWorld world : this.recording.worlds) {
                    if(world.chunkData.isEmpty()) continue;

                    array.put(saveWorld(world));
                    saveProgress = ((float) (this.recording.worlds.indexOf(world) + 1) / this.recording.worlds.size()) * 100;
                }

                obj.put("worldTicks", array);

                fos.write(obj.toString().getBytes());
                fos.close();
                LOGGER.info("Saved to: \"{}\"!", fileName);
                isSaving = false;
            } catch (IOException e) {
                LOGGER.error(e);
            }
        }).start();
    }

    public JSONObject saveWorld(RecordingWorld world) {
        JSONObject obj = new JSONObject();
        JSONArray chunks = new JSONArray();

        int index = 0;
        List<ChunkPos> poss = new ArrayList<>(world.chunkData.keySet());
        for (ChunkData chunk1 : world.chunkData.values()) {
            JSONObject chunk = new JSONObject();

            chunk.put("x,z", poss.get(index).x + "," + poss.get(index).z);
            chunk.put("sectionData", chunk1.getSectionsDataBuf().array());
            chunk.put("heightMap", chunk1.getHeightmap().toString());

            chunks.put(chunk);
            index++;
        }

        obj.put("tickCount", this.recording.worlds.indexOf(world));
        obj.put("chunks", chunks);

        return obj;
    }

    public void loadRecording(File file) {
        isPlayback = true;
        try {
            MaximaRecording recording = new MaximaRecording(true);
            JSONObject toRead = new JSONObject(Files.readString(file.toPath()));

            recording.tickCount = toRead.getInt("amountTicks");
            recording.shouldAddChunks = false;
            recording.worlds = parseWorlds(toRead.getJSONArray("worldTicks"), recording);

            LOGGER.info("Loaded {}!", file.getName().replace(".mxr", ""));
            LOGGER.info("Creating world!");

            MaximaRecording.load(recording);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<RecordingWorld> parseWorlds(JSONArray toRead, MaximaRecording recording) {
        List<RecordingWorld> worlds = new ArrayList<>();

        for (int i = 0; i < recording.tickCount; i++) {
            worlds.add(new RecordingWorld(recording));
        }

        for (int i = 0; i < toRead.length(); i++) {
            JSONObject obj = toRead.getJSONObject(i);
            RecordingWorld world = worlds.get(i);

            int index = obj.getInt("tickCount");
            JSONArray chunks = obj.getJSONArray("chunks");

            world.chunkData = parseChunks(chunks);

            worlds.set(index, world);
        }

        return worlds;
    }

    private Map<ChunkPos, ChunkData> parseChunks(JSONArray chunks) {
        Map<ChunkPos, ChunkData> chunks1 = new HashMap<>();

        for (int i = 0; i < chunks.length(); i++) {
            JSONObject obj = chunks.getJSONObject(i);

            List<Integer> pos = Arrays.stream(obj.getString("x,z").split(",")).map(Integer::parseInt).toList();
            ChunkPos position = new ChunkPos(pos.get(0), pos.get(1));

            String nbtVal = obj.getString("");
            byte[] sectionData = (byte[]) obj.get("sectionData");

            ByteBuf yea = new UnpooledUnsafeDirectByteBuf(new UnpooledByteBufAllocator(true), 0, Integer.MAX_VALUE);
            RegistryByteBuf rbb = new RegistryByteBuf(yea, DynamicRegistryManager.EMPTY);
           // rbb.writeNbt(NbtCompound);

            //chunks1.put(position, new ChunkData(rbb, position.x, position.z));
        }

        return chunks1;
    }
}

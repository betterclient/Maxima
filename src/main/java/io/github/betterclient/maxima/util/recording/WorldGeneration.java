package io.github.betterclient.maxima.util.recording;

import io.github.betterclient.maxima.MaximaClient;
import io.github.betterclient.maxima.recording.MaximaRecording;
import io.github.betterclient.maxima.recording.ReadableChunkData;
import io.github.betterclient.maxima.recording.RecordingEntity;
import io.github.betterclient.maxima.recording.RecordingWorld;
import io.netty.buffer.Unpooled;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.client.world.GeneratorOptionsHolder;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.EntityList;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.gen.WorldPresets;

import java.io.IOException;
import java.util.*;

//PLAYER DOESNT FUCKING SHOW

public class WorldGeneration {
    private static Set<String> LAST_GEN_UUIDS = new HashSet<>();
    private static int lastGenTick = 999;
    private static int timeTick = 0;

    public static void generate() {
        if (MinecraftClient.getInstance().player == null) return;
        if (MinecraftClient.getInstance().world == null) return;
        if (MaximaClient.instance.stopGeneration) return;
        if (MaximaRecording.generatingWorld) return;

        if (MaximaRecording.lastGenPos == null) MaximaRecording.lastGenPos = BlockPos.ORIGIN;
        else if (MaximaRecording.lastGenPos.equals(MinecraftClient.getInstance().player.getBlockPos())) return;
        else MaximaRecording.lastGenPos = MinecraftClient.getInstance().player.getBlockPos();
        MaximaClient.LOGGER.info("Start worldgen!");

        MaximaRecording.generatingWorld = true;
        /*if(true) {
            generatingWorld = false;
            return;
        }*/

        long start = System.currentTimeMillis();
        if (MaximaClient.instance.stopGeneration) return;

        Map<ChunkPos, ReadableChunkData> rd;

        if (MaximaRecording.isPaused)
            rd = mergeUpto(MaximaRecording.currentTick);
        else
            rd = MaximaRecording.loadedRecording.worlds.get(MaximaRecording.currentTick).readData;

        ServerWorld world = MinecraftClient.getInstance().getServer().getOverworld();

        List<RecordingEntity> recordingEntities = MaximaRecording.loadedRecording.entities.get(MaximaRecording.currentTick);

        if (LAST_GEN_UUIDS.isEmpty()) {
            for (RecordingEntity recordingEntity : recordingEntities) {
                try {
                    Entity e = recordingEntity.generate(world);
                    if (e == null) continue;

                    world.spawnEntity(e);
                    LAST_GEN_UUIDS.add(recordingEntity.uuid);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            if (MaximaRecording.currentTick != lastGenTick) {
                Set<String> currentApplied = new HashSet<>();
                List<String> currentUUIDS = recordingEntities.stream().map(recordingEntity -> recordingEntity.uuid).distinct().toList();

                //removed
                LAST_GEN_UUIDS.stream().filter(string -> !currentUUIDS.contains(string)).forEach(string -> {
                    world.getEntity(UUID.fromString(string)).remove(Entity.RemovalReason.DISCARDED);
                    currentApplied.add(string);
                });

                //updated
                LAST_GEN_UUIDS.stream().filter(currentUUIDS::contains).forEach(string -> {
                    for (RecordingEntity recordingEntity : recordingEntities) {
                        if (recordingEntity.uuid.equals(string)) {
                            try {
                                for (Entity entity : MinecraftClient.getInstance().world.entityList.entities.values().stream().toList()) {
                                    if (entity.getUuidAsString().equals(recordingEntity.uuid)) {
                                        recordingEntity.apply(entity);
                                        currentApplied.add(string);
                                    }
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                });

                //added
                for (RecordingEntity recordingEntity : recordingEntities) {
                    if (!currentApplied.contains(recordingEntity.uuid)) {
                        try {
                            Entity e = recordingEntity.generate(world);

                            if(e == null) continue;
                            if(!world.spawnEntity(e)) {
                                e = recordingEntity.generate(MinecraftClient.getInstance().world);
                                if (e == null) continue;

                                MinecraftClient.getInstance().world.addEntity(e);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                LAST_GEN_UUIDS = new HashSet<>(currentUUIDS);
            }
        }
        lastGenTick = MaximaRecording.currentTick;

        if (MaximaClient.instance.stopGeneration) return;
        List<ChunkPos> chunks = new ArrayList<>(rd.keySet());

        for (Map.Entry<ChunkPos, ReadableChunkData> chunkPosEntry : rd.entrySet()) {
            ChunkPos pos = chunkPosEntry.getKey();
            ReadableChunkData readData = chunkPosEntry.getValue();
            MaximaRecording.genProg = chunks.indexOf(pos) + "/" + chunks.size();

            PacketByteBuf chunkData = new PacketByteBuf(Unpooled.wrappedBuffer(readData.chunkData));
            PacketByteBuf heightmap = new PacketByteBuf(Unpooled.wrappedBuffer(readData.heightmap));

            MinecraftClient.getInstance().world.getChunkManager().loadChunkFromPacket(pos.x, pos.z, chunkData, heightmap.readNbt(), blockEntityVisitor -> {});

            if (timeTick % 5 == 0) {
                int px = pos.x * 16;
                int pz = pos.z * 16;
                int torchPos = 256;

                for (int x = 0; x < 16; x+=2) {
                    for (int z = 0; z < 16; z+=2) {
                        update(new BlockPos(px + x, torchPos, pz + z));
                    }
                }
            }

            MinecraftClient.getInstance().world.enqueueChunkUpdate(() -> {
                WorldChunk worldChunk = MinecraftClient.getInstance().world.getChunkManager().getWorldChunk(pos.x, pos.z, false);
                if (worldChunk != null) {
                    LightingProvider lightingProvider = MinecraftClient.getInstance().world.getChunkManager().getLightingProvider();
                    ChunkSection[] chunkSections = worldChunk.getSectionArray();
                    ChunkPos chunkPos = worldChunk.getPos();

                    for(int i = 0; i < chunkSections.length; ++i) {
                        ChunkSection chunkSection = chunkSections[i];
                        int j = MinecraftClient.getInstance().world.sectionIndexToCoord(i);
                        lightingProvider.setSectionStatus(ChunkSectionPos.from(chunkPos, j), chunkSection.isEmpty());
                        MinecraftClient.getInstance().world.scheduleBlockRenders(pos.x, j, pos.z);
                    }

                    lightingProvider.doLightUpdates();
                }

            });
        }

        MaximaClient.LOGGER.info("Loaded world in: {} seconds", (System.currentTimeMillis() - start) / 1000f);
        MaximaRecording.generatingWorld = false;
        timeTick++;
    }

    private static void update(BlockPos blockPos) {
        BlockState previous = MinecraftClient.getInstance().world.getBlockState(blockPos);
        MinecraftClient.getInstance().world.setBlockState(blockPos, Blocks.TORCH.getDefaultState());
        MinecraftClient.getInstance().world.setBlockState(blockPos, previous);
    }

    public static Map<ChunkPos, ReadableChunkData> mergeUpto(int currentTick) {
        HashMap<ChunkPos, ReadableChunkData> chunkDataHashMap = new HashMap<>(MaximaRecording.loadedRecording.worlds.getFirst().readData);
        if (currentTick == 0) return chunkDataHashMap;

        for (RecordingWorld recordingWorld : MaximaRecording.loadedRecording.worlds.subList(1, currentTick)) {
            for (Map.Entry<ChunkPos, ReadableChunkData> chunkPosReadableChunkDataEntry : recordingWorld.readData.entrySet()) {
                if(chunkDataHashMap.containsKey(chunkPosReadableChunkDataEntry.getKey())) {
                    chunkDataHashMap.remove(chunkPosReadableChunkDataEntry.getKey());
                    chunkDataHashMap.put(chunkPosReadableChunkDataEntry.getKey(), chunkPosReadableChunkDataEntry.getValue());
                }
            }
        }

        return chunkDataHashMap;
    }

    public static void load(MaximaRecording recording) {
        MaximaRecording.loadedRecording = recording;
        CreateWorldScreen.create(MinecraftClient.getInstance(), MinecraftClient.getInstance().currentScreen);
        CreateWorldScreen world = (CreateWorldScreen) MinecraftClient.getInstance().currentScreen;

        world.getWorldCreator().setDifficulty(Difficulty.EASY);
        world.getWorldCreator().setGameMode(WorldCreator.Mode.CREATIVE);
        world.getWorldCreator().setWorldName("maxima@" + System.currentTimeMillis());

        GeneratorOptionsHolder generatorOptionsHolder = world.getWorldCreator().getGeneratorOptionsHolder();

        WorldCreator.WorldType type = new WorldCreator.WorldType(WorldCreator.getWorldPreset(generatorOptionsHolder, Optional.of(WorldPresets.FLAT)).orElse(null));
        world.getWorldCreator().setWorldType(type);
        world.getWorldCreator().setCheatsEnabled(true);
        MaximaClient.instance.isWaitingForWorld = true;
        world.createLevel();
    }
}

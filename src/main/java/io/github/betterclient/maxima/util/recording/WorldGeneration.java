package io.github.betterclient.maxima.util.recording;

import io.github.betterclient.maxima.MaximaClient;
import io.github.betterclient.maxima.recording.*;
import io.github.betterclient.maxima.ui.SelectTickScreen;
import io.netty.buffer.Unpooled;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.client.world.GeneratorOptionsHolder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.gen.WorldPresets;

import java.io.IOException;
import java.util.*;

public class WorldGeneration {
    private static Set<String> LAST_GEN_UUIDS = new HashSet<>();
    private static int lastGenTick = 999;
    private static int timeTick = 0;

    public static void interpolateAll(int interpolationTick) throws IOException {
        if (MaximaClient.interpolation == 0) return;
        if (MaximaRecording.currentTick + 1 >= MaximaRecording.loadedRecording.entities.size()) return;

        List<RecordingEntity> start = MaximaRecording.loadedRecording.entities.get(MaximaRecording.currentTick);
        List<RecordingEntity> end = MaximaRecording.loadedRecording.entities.get(MaximaRecording.currentTick+1);
        List<RecordingEntity> interpolationResult = new ArrayList<>();

        for (RecordingEntity entity : end) {
            RecordingEntity fromStart = start.stream().filter(recordingEntity -> recordingEntity.uuid.equals(entity.uuid)).filter(recordingEntity -> recordingEntity.getPText().equals(entity.getPText())).findFirst().orElse(null);
            if (fromStart == null) continue;

            interpolationResult.add(EntityInterpolation.interpolateStep(fromStart, entity.generate(), interpolationTick, MaximaClient.interpolation));
        }

        for (String string : LAST_GEN_UUIDS) {
            boolean done = false;
            for (RecordingEntity recordingEntity : interpolationResult) {
                if (recordingEntity.uuid.equals(string)) {
                    try {
                        for (Entity entity : MinecraftClient.getInstance().world.entityList.entities.values().stream().toList()) {
                            if (entity.getUuidAsString().equals(recordingEntity.uuid) && MinecraftClient.getInstance().player != entity) {
                                recordingEntity.apply(entity);
                                done = true;
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            if (!done) {
                for (RecordingEntity entity : interpolationResult) {
                    if (entity.uuid.equals(string)) {
                        PlayerEntity pe = MinecraftClient.getInstance().world.getPlayerByUuid(UUID.fromString(string));
                        if(pe == null) continue;

                        entity.apply(pe);
                    }
                }
            }
        }
    }

    private static void generateEntities(ServerWorld world) {
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
                    Entity myEnt = world.getEntity(UUID.fromString(string));
                    if (myEnt == null) return;

                    myEnt.remove(Entity.RemovalReason.KILLED);
                    currentApplied.add(string);
                });

                //updated
                LAST_GEN_UUIDS.stream().filter(currentUUIDS::contains).forEach(string -> {
                    for (RecordingEntity recordingEntity : recordingEntities) {
                        if (recordingEntity.uuid.equals(string)) {
                            try {
                                for (Entity entity : MinecraftClient.getInstance().world.entityList.entities.values().stream().toList()) {
                                    if (entity.getUuidAsString().equals(recordingEntity.uuid) && MinecraftClient.getInstance().player != entity) {
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
                            boolean applied = false;
                            for (Entity entity : MinecraftClient.getInstance().world.entityList.entities.values().stream().toList()) {
                                if (entity.getUuidAsString().equals(recordingEntity.uuid)) {
                                    recordingEntity.apply(entity); //Added previously but somehow not applied?????
                                    applied = true;
                                }
                            }
                            if (applied) continue;
                            else {
                                PlayerEntity e = MinecraftClient.getInstance().world.getPlayerByUuid(UUID.fromString(recordingEntity.uuid));
                                if (e != null) {
                                    recordingEntity.apply(e); //Somehow still not applied?
                                    continue;
                                }
                            }
                            spawn(world, recordingEntity);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                LAST_GEN_UUIDS = new HashSet<>(currentUUIDS);
            }
        }

        SelectTickScreen.lastTime = System.currentTimeMillis();
        SelectTickScreen.interpolation = 0;
    }

    private static void spawn(ServerWorld world, RecordingEntity recordingEntity) throws IOException {
        Entity e = recordingEntity.generate(world);

        if (e == null) return;
        if (!world.spawnEntity(e)) {
            e = recordingEntity.generate(MinecraftClient.getInstance().world);
            if (e == null) return;

            MinecraftClient.getInstance().world.addEntity(e);
        }
    }

    public static void generate() {
        if (MinecraftClient.getInstance().player == null) return;
        if (MinecraftClient.getInstance().world == null) return;
        if (MaximaClient.instance.stopGeneration) return;
        if (MaximaRecording.generatingWorld) return;

        if (MaximaRecording.lastGenPos == null) MaximaRecording.lastGenPos = BlockPos.ORIGIN;
        else if (MaximaRecording.lastGenPos.equals(MinecraftClient.getInstance().player.getBlockPos())) return;
        else MaximaRecording.lastGenPos = MinecraftClient.getInstance().player.getBlockPos();

        MaximaRecording.generatingWorld = true;
        /*if(true) {
            generatingWorld = false;
            return;
        }*/

        if (MaximaClient.instance.stopGeneration) return;

        Map<ChunkPos, ReadableChunkData> rd;

        if (MaximaRecording.isPaused)
            rd = mergeUpto(MaximaRecording.currentTick);
        else
            rd = MaximaRecording.loadedRecording.worlds.get(MaximaRecording.currentTick).readData;

        generateEntities(MinecraftClient.getInstance().getServer().getOverworld());

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

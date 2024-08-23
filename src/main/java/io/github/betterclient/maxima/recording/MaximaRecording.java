package io.github.betterclient.maxima.recording;

import io.github.betterclient.maxima.MaximaClient;
import io.github.betterclient.maxima.keybinds.GoToTickBind;
import io.github.betterclient.maxima.ui.SelectTickScreen;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.client.world.GeneratorOptionsHolder;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.network.packet.s2c.play.LightData;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.entity.SimpleEntityLookup;
import net.minecraft.world.gen.WorldPresets;

import java.util.*;

public class MaximaRecording {
    public List<RecordingWorld> worlds = new ArrayList<>();
    public boolean shouldAddChunks = false;
    public int tickCount = 0;

    public MaximaRecording(boolean empty) {}

    public MaximaRecording() {
        worlds.add(new RecordingWorld(this, true));
    }

    public static MaximaRecording loadedRecording;
    public static boolean generatingWorld = false;
    public static String genProg = "";
    public static int currentTick = 0;

    public static void load(MaximaRecording recording) {
        loadedRecording = recording;
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

    public static boolean isFirst = true;
    private static final Set<String> set = new HashSet<>();
    public static BlockPos lastGenPos;
    private static float totalTicks;
    private static boolean regen = false;

    public static boolean isPaused = true;
    public static long lastPauseTime = 0;

    public static void world(DrawContext context) {
        if(isFirst) {
            totalTicks = loadedRecording.tickCount / 20F;
            new Thread(() -> new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (MaximaClient.instance.isPlayback)
                        regen = true;
                }
            }, 0L, 1000L)).start();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if (MaximaClient.instance.isPlayback)
                        MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("tick freeze");
                }
            }, 2000L);

            isPaused = true;
            isFirst = false;
            lastPauseTime = 0;
        }
        if (regen) {
            MaximaRecording.generateWorld();
            regen = false;
        }

        if (!isPaused) {
            if (System.currentTimeMillis() >= lastPauseTime) {
                lastPauseTime = System.currentTimeMillis() + 50;
                currentTick++;

                if (currentTick == loadedRecording.tickCount) {
                    isPaused = true;
                    return;
                }

                MaximaRecording.lastGenPos = BlockPos.ORIGIN;
                generateWorld();
            }
        }

        ServerWorld w = MinecraftClient.getInstance().getServer().getOverworld();
        ObjectSet<Int2ObjectMap.Entry<Entity>> set = ((SimpleEntityLookup<Entity>)w.entityManager.getLookup()).index.idToEntity.int2ObjectEntrySet();
        for(Int2ObjectMap.Entry<Entity> e : set) {
            Entity entity = e.getValue();
            if(entity == null || Objects.equals(entity.getUuidAsString(), MinecraftClient.getInstance().player != null ? MinecraftClient.getInstance().player.getUuidAsString() : null)) continue;

            if(!entity.getCommandTags().contains("maxima") && entity != MinecraftClient.getInstance().player && MaximaRecording.set.add(entity.getUuidAsString())) {
                MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("kill " + entity.getUuidAsString());
            }
        }

        if(MinecraftClient.getInstance().interactionManager.getCurrentGameMode() != GameMode.SPECTATOR)
            MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("gamemode spectator");
        if(MinecraftClient.getInstance().getServer().getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING))
            MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("gamerule doMobSpawning false");
        if(MinecraftClient.getInstance().getServer().getGameRules().getBoolean(GameRules.DO_MOB_LOOT))
            MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("gamerule doMobLoot false");
        if(MinecraftClient.getInstance().getServer().getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS))
            MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("gamerule doEntityDrops false");
        if(MinecraftClient.getInstance().getServer().getGameRules().getBoolean(GameRules.DO_FIRE_TICK))
            MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("gamerule doFireTick false");
        if(MinecraftClient.getInstance().getServer().getGameRules().getBoolean(GameRules.DO_TRADER_SPAWNING))
            MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("gamerule doTraderSpawning false");

        MinecraftClient.getInstance().inGameHud.getChatHud().clear(true);

        context.drawText(MinecraftClient.getInstance().textRenderer, (currentTick / 20D) + "/" + totalTicks, 2, 2, -1, true);
        Text text = Text.translatable("text.presstogo", GoToTickBind.instance.getBoundKeyLocalizedText());
        context.drawText(MinecraftClient.getInstance().textRenderer, text, context.getScaledWindowWidth() - MinecraftClient.getInstance().textRenderer.getWidth(text) - 10, 2, -1, true);

        if(GoToTickBind.instance.isPressed()) {
            MinecraftClient.getInstance().setScreen(new SelectTickScreen());
        }

        int height = context.getScaledWindowHeight();
        int width = context.getScaledWindowWidth();

        {
            context.fill(0, height - 120, width, height, 0xFF000000);
            context.fill(6, height - 20, width - 6, height - 80, 0xFF111111);

            context.fill(10, height - 52, width - 10, height - 48, -1);
            context.fill(8, height - 20, 10, height - 80, -1);
            context.fill(width - 8, height - 20, width - 10, height - 80, -1);
            int size = width - 10;

            int pos = (int) map(currentTick, 0, loadedRecording.tickCount, 10, size);
            context.fill(pos - 1, height - 20, pos + 1, height - 80, -1);
            context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, currentTick / 20D + "", pos, height - 90, -1);

            context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, "0.0", 10, height - 20, -1);
            context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, totalTicks + "", width - 10, height - 20, -1);

            context.getMatrices().push();
            context.getMatrices().translate(10, height - 115, 0);
            context.getMatrices().scale(.25f, .25f, 1);
            context.getMatrices().translate(-10, -(height - 115), 0);
            if (isPaused) {
                context.drawTexture(Identifier.of("maxima", "textures/pausebutton.png"), 10, height - 115, 0, 0, 64, 64, 64, 64);
            } else {
                context.drawTexture(Identifier.of("maxima", "textures/playingbutton.png"), 10, height - 115, 0, 0, 64, 64, 64, 64);
            }
            context.getMatrices().pop();
        }

        if (generatingWorld) {
            context.drawText(MinecraftClient.getInstance().textRenderer, genProg, context.getScaledWindowWidth() - MinecraftClient.getInstance().textRenderer.getWidth(genProg) - 5, context.getScaledWindowHeight() - 10, -1, true);
        }
    }

    public static void generateWorld() {
        if (MinecraftClient.getInstance().player == null) return;
        if (MinecraftClient.getInstance().world == null) return;
        if (MaximaClient.instance.stopGeneration) return;
        if (generatingWorld) return;

        if (lastGenPos == null) lastGenPos = BlockPos.ORIGIN;
        else if (lastGenPos.equals(MinecraftClient.getInstance().player.getBlockPos())) return;
        else lastGenPos = MinecraftClient.getInstance().player.getBlockPos();
        MaximaClient.LOGGER.info("Start worldgen!");

        generatingWorld = true;
        /*if(true) {
            generatingWorld = false;
            return;
        }*/

        long start = System.currentTimeMillis();
        if (MaximaClient.instance.stopGeneration) return;

        Map<ChunkPos, ReadableChunkData> rd;

        if (isPaused)
            rd = mergeUpto(currentTick);
        else
            rd = loadedRecording.worlds.get(currentTick).readData;

        if (MaximaClient.instance.stopGeneration) return;
        List<ChunkPos> chunks = new ArrayList<>(rd.keySet());

        for (Map.Entry<ChunkPos, ReadableChunkData> chunkPosEntry : rd.entrySet()) {
            ChunkPos pos = chunkPosEntry.getKey();
            ReadableChunkData readData = chunkPosEntry.getValue();
            genProg = chunks.indexOf(pos) + "/" + chunks.size();

            PacketByteBuf chunkData = new PacketByteBuf(Unpooled.wrappedBuffer(readData.chunkData));
            PacketByteBuf heightmap = new PacketByteBuf(Unpooled.wrappedBuffer(readData.heightmap));

            MinecraftClient.getInstance().world.getChunkManager().loadChunkFromPacket(pos.x, pos.z, chunkData, heightmap.readNbt(), blockEntityVisitor -> {});
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
                }

            });
        }

        MaximaClient.LOGGER.info("Loaded world in: {} seconds", (System.currentTimeMillis() - start) / 1000f);
        generatingWorld = false;
    }

    public static Map<ChunkPos, ReadableChunkData> mergeUpto(int currentTick) {
        HashMap<ChunkPos, ReadableChunkData> chunkDataHashMap = new HashMap<>(loadedRecording.worlds.getFirst().readData);
        if (currentTick == 0) return chunkDataHashMap;

        for (RecordingWorld recordingWorld : loadedRecording.worlds.subList(1, currentTick)) {
            for (Map.Entry<ChunkPos, ReadableChunkData> chunkPosReadableChunkDataEntry : recordingWorld.readData.entrySet()) {
                if(chunkDataHashMap.containsKey(chunkPosReadableChunkDataEntry.getKey())) {
                    chunkDataHashMap.remove(chunkPosReadableChunkDataEntry.getKey());
                    chunkDataHashMap.put(chunkPosReadableChunkDataEntry.getKey(), chunkPosReadableChunkDataEntry.getValue());
                }
            }
        }

        return chunkDataHashMap;
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

    public static double map(double val, double in_min, double in_max, double out_min, double out_max) {
        return (val - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    public static boolean basicCollisionCheck(double mouseX, double mouseY, double x, double y, double endX, double endY) {
        double val = x;
        if(endX < x) {
            x = endX;
            endX = val;
        }

        val = y;
        if(endY < y) {
            y = endY;
            endY = val;
        }

        return mouseX >= x & mouseX <= endX & mouseY >= y & mouseY <= endY;
    }
}

package io.github.betterclient.maxima.recording;

import io.github.betterclient.maxima.MaximaClient;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.client.world.GeneratorOptionsHolder;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.entity.SimpleEntityLookup;
import net.minecraft.world.gen.WorldPresets;

import java.util.*;

public class MaximaRecording {
    public List<RecordingWorld> worlds = new ArrayList<>();
    public boolean shouldAddChunks = false;
    public int tickCount = 0;
    public Map<ChunkPos, RecordChunk> latestChunks = new HashMap<>();

    public MaximaRecording(boolean empty) {}

    public MaximaRecording() {
        worlds.add(new RecordingWorld(this, true));
    }

    private static MaximaRecording toLoad;
    public static boolean generatingWorld = false;
    private static String genProg = "";

    public static void load(MaximaRecording recording) {
        toLoad = recording;
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

    static boolean isFirst = true;
    private static final Set<String> set = new HashSet<>();

    public static void world(DrawContext context) {
        if(isFirst) {
            new Thread(MaximaRecording::generateWorld).start();

            isFirst = false;
        }
        if(generatingWorld) {
            context.fill(0, 0, context.getScaledWindowWidth(), context.getScaledWindowHeight(), -16777216);
            context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, Text.translatable("text.wait_for_gen", genProg), context.getScaledWindowWidth() / 2,  context.getScaledWindowHeight() / 2, -1);
        } else {
            ServerWorld w = MinecraftClient.getInstance().getServer().getOverworld();
            ObjectSet<Int2ObjectMap.Entry<Entity>> set = ((SimpleEntityLookup<Entity>)w.entityManager.getLookup()).index.idToEntity.int2ObjectEntrySet();
            for(Int2ObjectMap.Entry<Entity> e : new ArrayList<>(set)) {
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

            context.drawText(MinecraftClient.getInstance().textRenderer,  "Tick 0", 2, 2, -1, true);
            String text = "Press %s to go to another tick!";
            context.drawText(MinecraftClient.getInstance().textRenderer, text, context.getScaledWindowWidth() - MinecraftClient.getInstance().textRenderer.getWidth(text) - 10, 2, -1, true);
        }
    }

    private static void generateWorld() {
        generatingWorld = true;
        /*if(true) {
            generatingWorld = false;
            return;
        }*/

        ServerWorld sw = MinecraftClient.getInstance().getServer().getOverworld();

        long start = System.currentTimeMillis();

        List<RecordChunk> chunks = new ArrayList<>(toLoad.worlds.getFirst().chunks.values().stream().toList());

        for (RecordChunk chunk : chunks) {
            genProg = chunks.indexOf(chunk) + "/" + chunks.size();

            WorldChunk chunkToReplace = sw.getChunk(chunk.chunkX, chunk.chunkZ);
            if(chunkToReplace == null) continue;

            chunkToReplace.clear();

            int highestY = 0;
            for (RecordChunk.BlockRecord block : chunk.record.values()) {
                chunkToReplace.setBlockState(block.pos(), block.block(), false);
                if(highestY < block.pos().getY())
                    highestY = block.pos().getY();
            }

            chunkToReplace.setLightOn(true);
            chunkToReplace.getChunkSkyLight().refreshSurfaceY(chunkToReplace);
            chunkToReplace.setBlockState(new BlockPos(7, highestY + 2, 7), Blocks.TORCH.getDefaultState(), false);

            chunkToReplace.setBlockState(new BlockPos(7, highestY + 2, 7), Blocks.AIR.getDefaultState(), false);
        }

        MaximaClient.LOGGER.info("Loaded world in: {} seconds", (System.currentTimeMillis() - start) / 1000f);
        generatingWorld = false;
    }

    public void tick() {
        worlds.add(new RecordingWorld(this));
        tickCount++;
        shouldAddChunks = true;
    }

    public void update(Chunk chunk) {
        if(MaximaClient.instance.isSaving) return;

        RecordChunk chunk1 = latestChunks.get(chunk.getPos());
        if(chunk1 != null) {
            RecordChunk rc = new RecordChunk(MinecraftClient.getInstance().world.getChunk(chunk.getPos().x, chunk.getPos().z), chunk1);
            this.worlds.getLast().chunks.remove(chunk.getPos());
            this.worlds.getLast().chunks.put(chunk.getPos(), rc);

            this.latestChunks.remove(chunk.getPos());
            latestChunks.put(chunk.getPos(), chunk1.upgrade(rc));
        } else {
            RecordChunk last = this.worlds.getFirst().getChunk(chunk);
            for (RecordingWorld recordingWorld : this.worlds.stream().filter(recordingWorld -> recordingWorld.getChunk(chunk) != null).toList()) {
                if(last == null) {
                    last = recordingWorld.getChunk(chunk);
                } else {
                    last = last.upgrade(recordingWorld.getChunk(chunk));
                }
            }

            RecordChunk c = new RecordChunk(MinecraftClient.getInstance().world.getChunk(chunk.getPos().x, chunk.getPos().z), last);

            this.worlds.getLast().chunks.remove(chunk.getPos());
            this.worlds.getLast().chunks.put(chunk.getPos(), c);

            this.latestChunks.remove(chunk.getPos());
            this.latestChunks.put(chunk.getPos(), Objects.requireNonNullElse(last, c).upgrade(c));
        }
    }
}

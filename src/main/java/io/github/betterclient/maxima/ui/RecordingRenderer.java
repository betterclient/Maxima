package io.github.betterclient.maxima.ui;

import io.github.betterclient.maxima.MaximaClient;
import io.github.betterclient.maxima.keybinds.GoToTickBind;
import io.github.betterclient.maxima.recording.MaximaRecording;
import io.github.betterclient.maxima.recording.RecordingEntity;
import io.github.betterclient.maxima.util.TickTracker;
import io.github.betterclient.maxima.util.recording.WorldGeneration;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;

import java.io.IOException;
import java.util.*;

public class RecordingRenderer {
    private static long lastT = 0;
    public static boolean isFirst = true;
    private static float totalTicks;
    private static boolean regen = false;
    public static boolean firstGen = true;

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

    public static void render(DrawContext context, MaximaRecording loadedRecording) {
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
                        Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand("tick freeze");
                }
            }, 2000L);

            MaximaRecording.isPaused = true;
            isFirst = false;
            MaximaRecording.lastPauseTime = 0;
        }

        if (SelectTickScreen.wantsInterp) {
            SelectTickScreen.wantsInterp = false;
            try {
                WorldGeneration.interpolateAll(SelectTickScreen.wantedInterp);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (regen) {
            MaximaRecording.generateWorld();

            if (firstGen) {
                firstGen = false;
                Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand("tp " + RecordingEntity.PX + " " + RecordingEntity.PY  + " " + RecordingEntity.PZ);
            }
            regen = false;
        }

        if (!MaximaRecording.isPaused) {
            for (int i = 0; i < TickTracker.CURRENT_TRACKER.getTicksToStep(); i++) {
                MaximaRecording.currentTick++;

                if (MaximaRecording.currentTick >= loadedRecording.tickCount) {
                    MaximaRecording.currentTick = loadedRecording.tickCount;
                    MaximaRecording.isPaused = true;
                    return;
                }

                MaximaRecording.lastGenPos = BlockPos.ORIGIN;
            }

            MaximaRecording.generateWorld();
        }

        assert MinecraftClient.getInstance().interactionManager != null;
        if(MinecraftClient.getInstance().interactionManager.getCurrentGameMode() != GameMode.SPECTATOR)
            Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand("gamemode spectator");
        if(Objects.requireNonNull(MinecraftClient.getInstance().getServer()).getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING))
            Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand("gamerule doMobSpawning false");
        if(MinecraftClient.getInstance().getServer().getGameRules().getBoolean(GameRules.DO_MOB_LOOT))
            Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand("gamerule doMobLoot false");
        if(MinecraftClient.getInstance().getServer().getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS))
            Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand("gamerule doEntityDrops false");
        if(MinecraftClient.getInstance().getServer().getGameRules().getBoolean(GameRules.DO_FIRE_TICK))
            Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand("gamerule doFireTick false");
        if(MinecraftClient.getInstance().getServer().getGameRules().getBoolean(GameRules.DO_TRADER_SPAWNING))
            Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand("gamerule doTraderSpawning false");

        if (lastT < System.currentTimeMillis()) {
            lastT = System.currentTimeMillis() + (lastT == 0 ? 1000 : 10000);
            Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand("kill @e[type=!player, tag=!maxima]");
        }

        MinecraftClient.getInstance().inGameHud.getChatHud().clear(true);

        context.drawText(MinecraftClient.getInstance().textRenderer, (MaximaRecording.currentTick / 20D) + "/" + totalTicks, 2, 2, -1, true);
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

            int pos = (int) map(MaximaRecording.currentTick, 0, loadedRecording.tickCount, 10, size);
            context.fill(pos - 1, height - 20, pos + 1, height - 80, -1);
            context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, MaximaRecording.currentTick / 20D + "", pos, height - 90, -1);

            context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, "0.0", 10, height - 20, -1);
            context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, totalTicks + "", width - 10, height - 20, -1);

            context.getMatrices().push();
            context.getMatrices().translate(10, height - 115, 0);
            context.getMatrices().scale(.25f, .25f, 1);
            context.getMatrices().translate(-10, -(height - 115), 0);
            if (MaximaRecording.isPaused) {
                context.drawTexture(Identifier.of("maxima", "textures/pausebutton.png"), 10, height - 115, 0, 0, 64, 64, 64, 64);
            } else {
                context.drawTexture(Identifier.of("maxima", "textures/playingbutton.png"), 10, height - 115, 0, 0, 64, 64, 64, 64);
            }
            context.getMatrices().pop();
        }

        if (MaximaRecording.generatingWorld) {
            context.drawText(MinecraftClient.getInstance().textRenderer, MaximaRecording.genProg, context.getScaledWindowWidth() - MinecraftClient.getInstance().textRenderer.getWidth(MaximaRecording.genProg) - 5, context.getScaledWindowHeight() - 10, -1, true);
        }
    }
}

package io.github.betterclient.maxima.ui;

import io.github.betterclient.maxima.recording.MaximaRecording;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import static io.github.betterclient.maxima.recording.MaximaRecording.*;

public class SelectTickScreen extends Screen {
    private boolean isHold = false;

    public SelectTickScreen() {
        super(Text.of(""));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && basicCollisionCheck(mouseX, mouseY, 6, height - 20, width - 6, height - 80)) {
            int tickCount = (int) map(mouseX, 10, width - 10, 0, loadedRecording.tickCount);
            if (tickCount < 0) tickCount = 0; else if (tickCount > loadedRecording.tickCount) tickCount = loadedRecording.tickCount;

            MaximaRecording.lastGenPos = BlockPos.ORIGIN;
            currentTick = tickCount;
            MaximaRecording.generateWorld();
            isHold = true;
            isPaused = true;
        }
        if (button == 0 && basicCollisionCheck(mouseX, mouseY, 10, height - 115, 26, height - 99)) {
            isPaused = !isPaused;

            if (!isPaused) MaximaRecording.lastPauseTime = 0;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        MaximaRecording.lastGenPos = new BlockPos(0, 0, 0);
        isHold = false;
        MaximaRecording.generateWorld();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!isHold) return;

        int tickCount = (int) map(mouseX, 10, width - 10, 0, loadedRecording.tickCount);
        if (tickCount < 0) tickCount = 0; else if (tickCount > loadedRecording.tickCount) tickCount = loadedRecording.tickCount;

        if (tickCount != currentTick) {
            MaximaRecording.lastGenPos = new BlockPos(0, 0, 0);
            currentTick = tickCount;
            MaximaRecording.generateWorld();
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
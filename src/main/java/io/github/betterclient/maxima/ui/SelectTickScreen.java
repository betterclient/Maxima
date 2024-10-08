package io.github.betterclient.maxima.ui;

import io.github.betterclient.maxima.MaximaClient;
import io.github.betterclient.maxima.recording.MaximaRecording;
import io.github.betterclient.maxima.util.TickTracker;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import static io.github.betterclient.maxima.recording.MaximaRecording.*;
import static io.github.betterclient.maxima.ui.RecordingRenderer.*;

public class SelectTickScreen extends Screen {
    public static long lastTime = 0;
    private boolean isHold = false;
    public static int interpolation = 0;
    public static boolean wantsInterp = false;
    public static int wantedInterp = 0;

    private static final Runnable interpolationThread = () -> {
        lastTime = System.currentTimeMillis();
        while (!isPaused) {
            interpolation = TickTracker.CURRENT_TRACKER.getInterpolationSteps();
            wantedInterp = interpolation+1;
            wantsInterp = true;

            if (interpolation >= MaximaClient.interpolation) {
                interpolation = 0;
            }
        }
    };

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
            setPaused(true);
        }
        if (button == 0 && basicCollisionCheck(mouseX, mouseY, 10, height - 115, 26, height - 99)) {
            synchronized (new Object()) {
                setPaused(!isPaused);
                TickTracker.CURRENT_TRACKER.setLastTick();
            }

            if (!isPaused) {
                MaximaRecording.lastPauseTime = 0;
                if (currentTick == loadedRecording.tickCount)
                    currentTick = 0;

                new Thread(interpolationThread).start();
            }
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
    protected void init() {
        ButtonWidget b05 = ButtonWidget.builder(Text.literal(".5"), button -> {
            TickTracker.S05.setCurrent();
            update(button);
        }).dimensions(5, height - 150, 20, 20).build();

        ButtonWidget b1 = ButtonWidget.builder(Text.literal("1"), button -> {
            TickTracker.S1.setCurrent();
            update(button);
        }).dimensions(30, height - 150, 20, 20).build();

        ButtonWidget b2 = ButtonWidget.builder(Text.literal("2"), button -> {
            TickTracker.S2.setCurrent();
            update(button);
        }).dimensions(55, height - 150, 20, 20).build();

        this.addDrawableChild(b05);
        this.addDrawableChild(b1);
        this.addDrawableChild(b2);

        switch (TickTracker.CURRENT_TRACKER.amountTicks) {
            case 100 -> update(b05);
            case 25 -> update(b2);
            default -> update(b1);
        }
    }

    private void update(ButtonWidget button) {
        for (Drawable drawable : this.drawables) {
            if (drawable instanceof ButtonWidget widget) {
                widget.active = true;
            }
        }
        button.active = false;
        lastPauseTime = 0;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        for (Drawable drawable : this.drawables) {
            drawable.render(context, mouseX, mouseY, delta);
        }

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

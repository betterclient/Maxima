package io.github.betterclient.maxima.util;

import io.github.betterclient.maxima.MaximaClient;

public enum TickTracker {
    S05(100, 0.5f),
    S1(50, 1f),
    S2(25, 2f);

    TickTracker(int amountTicks, float interpolationAmount) {
        this.amountTicks = amountTicks;
        this.lastTick = System.currentTimeMillis();
        this.interpolationAmount = interpolationAmount;
        this.interpolationTickTime = amountTicks / MaximaClient.OP_interpolationTick;
    }

    public void setCurrent() {
        CURRENT_TRACKER = this;
        MaximaClient.interpolation = (int) (interpolationAmount * MaximaClient.OP_interpolationTick);
    }

    public int getTicksToStep() {
        long amountTimePassed = System.currentTimeMillis() - lastTick;
        if (lastTick == 0) {
            lastTick = System.currentTimeMillis();
            return 1;
        }

        int countedTicks = 0;
        while (amountTimePassed >= amountTicks) {
            amountTimePassed-=amountTicks;
            countedTicks++;
        }

        lastTick = System.currentTimeMillis() - amountTimePassed;
        return countedTicks;
    }

    public int getInterpolationSteps() {
        long amountTimePassed = System.currentTimeMillis() - lastTick;
        int countedTicks = 0;

        while (amountTimePassed >= amountTicks) {
            amountTimePassed-=amountTicks;
        }

        while (amountTimePassed > interpolationTickTime) {
            amountTimePassed-=interpolationTickTime;
            countedTicks++;
        }

        return countedTicks;
    }

    public void setLastTick() {
        lastTick = System.currentTimeMillis();
    }

    public static TickTracker CURRENT_TRACKER = S1;

    private long lastTick;
    public final int amountTicks;
    public final float interpolationAmount;
    public final int interpolationTickTime;
}

package io.github.betterclient.maxima.util;

import io.github.betterclient.maxima.MaximaClient;

public enum TickTracker {
    S05(10, 100, 2f),
    S1(20, 50, 1f),
    S2(40, 25, 0.5f);

    TickTracker(int tickRate, int amountTicks, float interpolationAmount) {
        this.tickRate = tickRate;
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
    public final int tickRate;
    public final int amountTicks;
    public final float interpolationAmount;
    public final int interpolationTickTime;
}

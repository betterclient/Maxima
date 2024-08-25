package io.github.betterclient.maxima.recording;

import net.minecraft.nbt.*;

public class EntityInterpolation {
    public static RecordingEntity interpolateStep(NbtCompound from, NbtCompound to, int step, int amount) {
        NbtCompound comp = from.copy();

        NbtList list = comp.getList("Pos", NbtElement.DOUBLE_TYPE);
        NbtList toList = to.getList("Pos", NbtElement.DOUBLE_TYPE);

        list.set(0, NbtDouble.of(interpolate(list.getDouble(0), toList.getDouble(0), step, amount)));
        list.set(1, NbtDouble.of(interpolate(list.getDouble(1), toList.getDouble(1), step, amount)));
        list.set(2, NbtDouble.of(interpolate(list.getDouble(2), toList.getDouble(2), step, amount)));

        comp.put("Pos", list);
        list = comp.getList("Rotation", NbtElement.FLOAT_TYPE);
        toList = to.getList("Rotation", NbtElement.FLOAT_TYPE);

        list.set(0, NbtFloat.of(interpolate(list.getFloat(0), toList.getFloat(0), step, amount)));
        list.set(1, NbtFloat.of(interpolate(list.getFloat(1), toList.getFloat(1), step, amount)));
        comp.put("Rotation", list);

        return new RecordingEntity(comp);
    }

    private static double interpolate(double start, double end, int step, int totalSteps) {
        return start + ((end - start) * step / (float) totalSteps);
    }

    private static float interpolate(float start, float end, int step, int totalSteps) {
        return start + ((end - start) * step / (float) totalSteps);
    }
}

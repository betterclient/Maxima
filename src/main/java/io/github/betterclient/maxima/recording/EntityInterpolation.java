package io.github.betterclient.maxima.recording;

import net.minecraft.nbt.*;

import java.io.IOException;

public class EntityInterpolation {
    public static RecordingEntity interpolateStep(RecordingEntity fe, NbtCompound to, int step, int amount) throws IOException {
        NbtCompound comp = fe.generate().copy();

        NbtList list = comp.getList("Pos", NbtElement.DOUBLE_TYPE).copy();
        NbtList toList = to.getList("Pos", NbtElement.DOUBLE_TYPE);

        list.set(0, NbtDouble.of(interpolate(list.getDouble(0), toList.getDouble(0), step, amount)));
        list.set(1, NbtDouble.of(interpolate(list.getDouble(1), toList.getDouble(1), step, amount)));
        list.set(2, NbtDouble.of(interpolate(list.getDouble(2), toList.getDouble(2), step, amount)));

        comp.put("Pos", list);
        list = comp.getList("Rotation", NbtElement.FLOAT_TYPE).copy();
        toList = to.getList("Rotation", NbtElement.FLOAT_TYPE);

        list.set(0, NbtFloat.of((float) interpolate(list.getFloat(0), toList.getFloat(0), step, amount)));
        list.set(1, NbtFloat.of((float) interpolate(list.getFloat(1), toList.getFloat(1), step, amount)));
        comp.put("Rotation", list);

        RecordingEntity out = new RecordingEntity(comp);
        out.uuid = fe.uuid;
        out.isPlayer = fe.isPlayer;
        out.entityID = fe.entityID;
        return out;
    }

    private static double interpolate(double start, double end, int step, int totalSteps) {
        return start + (step - 1) * ((Math.abs(end - start)) / totalSteps) * ((end > start) ? 1 : -1);
    }
}

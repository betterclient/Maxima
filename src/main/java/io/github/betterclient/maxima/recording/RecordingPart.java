package io.github.betterclient.maxima.recording;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.nbt.NbtCompound;

import java.util.function.Function;

public class RecordingPart {
    public NbtCompound comp = new NbtCompound();
    public final Function<BipedEntityModel<?>, ModelPart> partFunction;

    public RecordingPart(Function<BipedEntityModel<?>, ModelPart> partFunction) {
        this.partFunction = partFunction;
    }
}

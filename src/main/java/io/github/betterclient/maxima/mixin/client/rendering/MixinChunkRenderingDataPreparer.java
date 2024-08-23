package io.github.betterclient.maxima.mixin.client.rendering;

import io.github.betterclient.maxima.MaximaClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.ChunkRenderingDataPreparer;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.chunk.ChunkBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ChunkRenderingDataPreparer.class)
public class MixinChunkRenderingDataPreparer {
    @Inject(method = "method_52834", at = @At("HEAD"))
    public void help(boolean bl, Camera camera, Frustum frustum, List<ChunkBuilder.BuiltChunk> list, CallbackInfo ci) {
        MaximaClient.instance.stopGeneration = true;
    }

    @Inject(method = "method_52834", at = @At("RETURN"))
    public void helpme(boolean bl, Camera camera, Frustum frustum, List<ChunkBuilder.BuiltChunk> list, CallbackInfo ci) {
        MaximaClient.instance.stopGeneration = false;
    }
}

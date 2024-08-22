package io.github.betterclient.maxima.mixin.client;

import io.github.betterclient.maxima.MaximaClient;
import io.github.betterclient.maxima.recording.MaximaRecording;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.class)
public class MixinClientWorld {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    public void setState(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        MaximaRecording recording = MaximaClient.instance.recording;
        if(recording == null || !recording.shouldAddChunks || !MaximaClient.instance.isRecording) return;

        recording.update(MinecraftClient.getInstance().world.getChunk(pos));
    }
}

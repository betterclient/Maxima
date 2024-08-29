package io.github.betterclient.maxima.mixin.server;

import io.github.betterclient.maxima.MaximaClient;
import io.github.betterclient.maxima.recording.MaximaRecording;
import net.minecraft.server.world.ServerChunkLoadingManager;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerChunkManager.class)
public abstract class MixinServerChunkManager {
    @Shadow @Final ServerWorld world;
    @Shadow @Final public ServerChunkLoadingManager chunkLoadingManager;
    @Shadow protected abstract void initChunkCaches();

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerChunkManager;updateChunks()Z", shift = At.Shift.AFTER), cancellable = true)
    public void onTick(BooleanSupplier shouldKeepTicking, boolean tickChunks, CallbackInfo ci) {
        if (MaximaClient.instance.isPlayback) {
            ci.cancel();
            this.world.getProfiler().swap("unload");
            this.chunkLoadingManager.tick(shouldKeepTicking);
            this.world.getProfiler().pop();
            this.initChunkCaches();
        }
    }
}

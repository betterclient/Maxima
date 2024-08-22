package io.github.betterclient.maxima.mixin.server;

import io.github.betterclient.maxima.recording.MaximaRecording;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer<T> {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void cancelTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        if(MaximaRecording.generatingWorld) ci.cancel();
    }
}

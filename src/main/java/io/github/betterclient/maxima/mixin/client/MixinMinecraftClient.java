package io.github.betterclient.maxima.mixin.client;

import io.github.betterclient.maxima.MaximaClient;
import io.github.betterclient.maxima.keybinds.MaximaKeyBinding;
import io.github.betterclient.maxima.recording.MaximaRecording;
import io.github.betterclient.maxima.util.RecordingSaver;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(MinecraftClient.class)
public class MixinMinecraftClient {
    @Shadow @Nullable public ClientWorld world;

    @Shadow @Nullable public ClientPlayerEntity player;

    @Inject(method = "tick", at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        if(this.world == null || this.player == null || !MaximaClient.instance.isRecording || MaximaClient.instance.recording == null) return;

        MaximaClient.instance.recording.tick();
    }

    @Inject(method = "stop", at = @At("HEAD"))
    public void stop(CallbackInfo ci) {
        try {
            MaximaClient.instance.save(MaximaClient.config);
        } catch (IOException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    public void handle(CallbackInfo ci) {
        if(MaximaKeyBinding.instance.wasPressed() && !MaximaClient.instance.isPlayback && !MaximaClient.instance.isSaving) {
            MaximaClient.instance.isRecording = !MaximaClient.instance.isRecording;

            if(MaximaClient.instance.isRecording) {
                MaximaClient.instance.recording = new MaximaRecording();
            } else {
                try {
                    RecordingSaver.saveRecording();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to save recording!", e);
                }
            }
        }
    }
}

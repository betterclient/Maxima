package io.github.betterclient.maxima.mixin.common;

import io.github.betterclient.maxima.MaximaClient;
import io.github.betterclient.maxima.recording.type.packet.RecordingParticle;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleS2CPacket.class)
public class MixinParticleS2CPacket {
    @Inject(method = "<init>(Lnet/minecraft/network/RegistryByteBuf;)V", at = @At("RETURN"))
    public void onReturnInitClient(RegistryByteBuf buf, CallbackInfo ci) {
        if (MaximaClient.instance.isRecording) {
            MaximaClient.instance.recording.particlePackets.getLast().add(new RecordingParticle((ParticleS2CPacket)(Object)this, buf.getRegistryManager()));
        }
    }
}

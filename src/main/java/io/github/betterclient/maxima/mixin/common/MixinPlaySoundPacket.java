package io.github.betterclient.maxima.mixin.common;

import io.github.betterclient.maxima.MaximaClient;
import io.github.betterclient.maxima.recording.type.packet.RecordingSound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlaySoundS2CPacket.class)
public class MixinPlaySoundPacket {
    @Inject(method = "<init>(Lnet/minecraft/network/RegistryByteBuf;)V", at = @At("RETURN"))
    public void onInit(RegistryByteBuf buf, CallbackInfo ci) {
        if (MaximaClient.instance.isRecording) {
            MaximaClient.instance.recording.soundPackets.getLast().add(new RecordingSound((PlaySoundS2CPacket)(Object)this, buf.getRegistryManager()));
        }
    }
}

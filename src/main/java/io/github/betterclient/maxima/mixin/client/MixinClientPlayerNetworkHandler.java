package io.github.betterclient.maxima.mixin.client;

import io.github.betterclient.maxima.MaximaClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayerNetworkHandler {
    @Inject(method = "onEntityPosition", at = @At("HEAD"), cancellable = true)
    public void hello(EntityPositionS2CPacket packet, CallbackInfo ci) {
        if (MaximaClient.instance.isPlayback) {
            ci.cancel();
        }
    }
}

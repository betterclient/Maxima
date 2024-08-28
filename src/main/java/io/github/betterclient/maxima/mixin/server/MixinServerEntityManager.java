package io.github.betterclient.maxima.mixin.server;

import io.github.betterclient.maxima.MaximaClient;
import net.minecraft.server.world.ServerEntityManager;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerEntityManager.class)
public class MixinServerEntityManager {
    @Redirect(method = "addEntityUuid", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V", remap = false))
    public void redirection(Logger instance, String string, Object o) {
        if (!MaximaClient.instance.isPlayback) {
            instance.info(string, o);
        }
    }
}

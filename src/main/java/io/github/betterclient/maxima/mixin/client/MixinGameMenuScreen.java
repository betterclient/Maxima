package io.github.betterclient.maxima.mixin.client;

import io.github.betterclient.maxima.MaximaClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(GameMenuScreen.class)
public class MixinGameMenuScreen {
    @Unique
    private boolean bl;
    @Unique
    private String wn;

    @Inject(method = "disconnect", at = @At("HEAD"))
    public void diconnect_head(CallbackInfo ci) {
        bl = MinecraftClient.getInstance().isInSingleplayer();
        if(bl) {
            wn = MinecraftClient.getInstance().getServer().getOverworld().worldProperties.getLevelName();
            bl = wn.startsWith("maxima@");
        }
    }

    @Inject(method = "disconnect", at = @At("RETURN"))
    public void disconnect_return(CallbackInfo ci) {
        if(bl) {
            LevelStorage levelStorage = MinecraftClient.getInstance().getLevelStorage();

            try {
                LevelStorage.Session session = levelStorage.createSessionWithoutSymlinkCheck(wn);

                try {
                    session.deleteSessionLock();
                } catch (Throwable var7) {
                    if (session != null) {
                        try {
                            session.close();
                        } catch (Throwable var6) {
                            var7.addSuppressed(var6);
                        }
                    }

                    throw var7;
                }

                session.close();
            } catch (IOException var8) {
                MaximaClient.LOGGER.error(var8);
            }

            MaximaClient.instance.isPlayback = false;
        }
    }
}

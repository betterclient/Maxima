package io.github.betterclient.maxima.mixin.common;

import io.github.betterclient.maxima.MaximaClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(World.class)
public class MixinWorld {
    @Inject(method = "tickEntity", at = @At("HEAD"), cancellable = true)
    public <T extends Entity> void doNotTickEntity(Consumer<T> tickConsumer, T entity, CallbackInfo ci) {
        if (MaximaClient.instance.isPlayback && (Object) this instanceof ServerWorld && entity != MinecraftClient.getInstance().player) ci.cancel();
    }
}

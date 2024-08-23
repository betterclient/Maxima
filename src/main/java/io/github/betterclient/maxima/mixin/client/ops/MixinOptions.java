package io.github.betterclient.maxima.mixin.client.ops;

import io.github.betterclient.maxima.keybinds.GoToTickBind;
import io.github.betterclient.maxima.keybinds.MaximaKeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(GameOptions.class)
public class MixinOptions {
    @Shadow @Final @Mutable public KeyBinding[] allKeys;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onInit(MinecraftClient client, File optionsFile, CallbackInfo ci) {
        this.allKeys = ArrayUtils.add(allKeys, new MaximaKeyBinding());
        this.allKeys = ArrayUtils.add(allKeys, new GoToTickBind());
    }
}

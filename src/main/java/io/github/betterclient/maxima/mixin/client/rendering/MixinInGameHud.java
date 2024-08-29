package io.github.betterclient.maxima.mixin.client.rendering;

import io.github.betterclient.maxima.MaximaClient;
import io.github.betterclient.maxima.recording.MaximaRecording;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class MixinInGameHud {
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "render", at = @At("TAIL"))
    public void afterRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if(MaximaClient.instance.isRecording) {
            context.drawText(this.getTextRenderer(), Text.translatable("text.recording"), 2, context.getScaledWindowHeight() - 10, -1, true);
        }

        if(MaximaClient.instance.isSaving) {
            context.drawText(this.getTextRenderer(), Text.translatable("text.saving", MaximaClient.instance.saveProgress), 2, context.getScaledWindowHeight() - 20, -1, true);
        }

        if(MaximaClient.instance.isPlayback) {
            MaximaRecording.world(context);
        }
    }
}

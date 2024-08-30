package io.github.betterclient.maxima.mixin.client;

import io.github.betterclient.maxima.MaximaClient;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public class MixinBipedEntityModel {
    @Shadow public float leaningPitch;

    @Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("RETURN"))
    public void onSetAngles(LivingEntity livingEntity, float f, float g, float h, float i, float j, CallbackInfo ci) {
        MaximaClient.instance.handleSetAngles(livingEntity, ((BipedEntityModel<?>)(Object)this), this.leaningPitch, f, i, j);
    }
}

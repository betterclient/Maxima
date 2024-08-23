package io.github.betterclient.maxima.mixin.client.ops;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Set;

@Mixin(KeyBinding.class)
public class MixinKeybinding {
    @Shadow @Final private static Map<String, Integer> CATEGORY_ORDER_MAP;

    @Shadow @Final private static Set<String> KEY_CATEGORIES;

    @Inject(method = "<init>(Ljava/lang/String;Lnet/minecraft/client/util/InputUtil$Type;ILjava/lang/String;)V", at = @At("RETURN"))
    public void onInit(String translationKey, InputUtil.Type type, int code, String category, CallbackInfo ci) {
        if (CATEGORY_ORDER_MAP.get(category) == null) {
            CATEGORY_ORDER_MAP.put(category, CATEGORY_ORDER_MAP.get(CATEGORY_ORDER_MAP.keySet().toArray(String[]::new)[CATEGORY_ORDER_MAP.size() - 1]) + 1);
            KEY_CATEGORIES.add(category);
        }
    }
}

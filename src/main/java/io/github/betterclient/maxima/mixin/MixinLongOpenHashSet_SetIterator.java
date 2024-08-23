package io.github.betterclient.maxima.mixin;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "it.unimi.dsi.fastutil.longs.LongOpenHashSet$SetIterator")
public class MixinLongOpenHashSet_SetIterator {
    @Shadow(remap = false)
    LongArrayList wrapped;

    @Redirect(method = "nextLong", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongArrayList;getLong(I)J"), remap = false)
    public long helpme(LongArrayList instance, int index) {
        if (wrapped == null) {
            wrapped = new LongArrayList(2);
            return 0;
        }

        return wrapped.getLong(index);
    }
}

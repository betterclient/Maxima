package io.github.betterclient.maxima.mixin;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap$MapIterator")
public class MixinInt2ObjectOpenHashMap_MapIterator {
    @Shadow(remap = false)
    IntArrayList wrapped;

    @Redirect(method = "nextEntry", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/ints/IntArrayList;getInt(I)I", remap = false), remap = false)
    public int redirection(IntArrayList instance, int index) {
        if (instance == null) {
            this.wrapped = new IntArrayList();
            return 0;
        }

        return instance.getInt(index);
    }
}

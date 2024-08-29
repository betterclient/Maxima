package io.github.betterclient.maxima.mixin.common;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.entity.Entity;
import net.minecraft.world.EntityList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EntityList.class)
public class MixinEntityList {
    //It was not safe.
    @Redirect(method = "ensureSafe", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;put(ILjava/lang/Object;)Ljava/lang/Object;", remap = false))
    public Object redirection(Int2ObjectMap<Entity> instance, int i, Object o) {
        try {
            return instance.put(i, (Entity) o);
        } catch (Exception e) {
            return o;
        }
    }
}
package io.github.betterclient.maxima;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class MaximaKeyBinding extends KeyBinding {
    public static MaximaKeyBinding instance;

    public MaximaKeyBinding() {
        super("key.maxima_start_stop", InputUtil.Type.KEYSYM, MaximaClient.OP_key, "key.categories.misc");
        instance = this;
    }

    @Override
    public void setBoundKey(InputUtil.Key boundKey) {
        super.setBoundKey(boundKey);
        MaximaClient.OP_key = boundKey.getCode();
    }
}

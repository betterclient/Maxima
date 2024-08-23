package io.github.betterclient.maxima.keybinds;

import io.github.betterclient.maxima.MaximaClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class GoToTickBind extends KeyBinding {
    public static GoToTickBind instance;

    public GoToTickBind() {
        super("key.maxima_gotick", InputUtil.Type.KEYSYM, MaximaClient.OP_keyGoTick, "key.categories.maxima_keys");
        instance = this;
    }

    @Override
    public void setBoundKey(InputUtil.Key boundKey) {
        super.setBoundKey(boundKey);
        MaximaClient.OP_keyGoTick = boundKey.getCode();
    }
}

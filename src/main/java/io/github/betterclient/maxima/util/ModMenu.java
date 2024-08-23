package io.github.betterclient.maxima.util;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.betterclient.maxima.ui.MaximaUI;
import net.minecraft.client.gui.screen.Screen;

public class ModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        return parent -> new MaximaUI();
    }
}

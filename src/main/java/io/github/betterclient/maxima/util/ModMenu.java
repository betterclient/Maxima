package io.github.betterclient.maxima.util;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.betterclient.maxima.MaximaClient;
import io.github.betterclient.maxima.keybinds.GoToTickBind;
import io.github.betterclient.maxima.keybinds.MaximaKeyBinding;
import io.github.betterclient.maxima.ui.MaximaUI;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.io.IOException;

public class ModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create();
            builder.setTitle(Text.literal("Maxima"));

            builder.alwaysShowTabs();
            builder.setDoesConfirmSave(false);
            builder.setSavingRunnable(() -> {
                try {
                    MaximaClient.instance.save(MaximaClient.config);
                } catch (IOException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });

            builder.setAfterInitConsumer(screen -> screen.addDrawableChild(ButtonWidget.builder(Text.translatable("text.recordings"), button -> MinecraftClient.getInstance().setScreen(new MaximaUI())).dimensions(10, 10, 100, 20).build()));

            ConfigCategory category = builder.getOrCreateCategory(Text.translatable("text.cloth-config.config"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            category.addEntry(entryBuilder.startIntSlider(Text.translatable("text.interpolation"), MaximaClient.OP_interpolationTick, 0, 25).setDefaultValue(MaximaClient.OP_interpolationTick).setSaveConsumer(integer -> MaximaClient.OP_interpolationTick = integer).build().setTextGetter(integer -> integer == 0 ? Text.translatable("text.off") : Text.literal(integer * 20 + " FPS")));
            category.addEntry(entryBuilder.fillKeybindingField(Text.translatable(MaximaKeyBinding.instance.getTranslationKey()), MaximaKeyBinding.instance).build());
            category.addEntry(entryBuilder.fillKeybindingField(Text.translatable(GoToTickBind.instance.getTranslationKey()), GoToTickBind.instance).build());

            return builder.build();
        };
    }
}

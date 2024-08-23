package io.github.betterclient.maxima.ui;

import io.github.betterclient.maxima.util.RecordingLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class MaximaUI extends Screen {
    public MaximaSelectionWidget selectionWidget;
    public MaximaUI() {
        super(Text.of(""));
    }

    @Override
    protected void init() {
        if(this.client.world != null) return;

        selectionWidget = this.addDrawableChild(new MaximaSelectionWidget(this.width, this.height - 110, 40, 30));
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("text.join"), button -> {
            if(selectionWidget.getFocused() != null) {
                RecordingLoader.loadRecording(selectionWidget.getFocused().file);
            }
        }).dimensions(width / 2 - 110, height - 30, 100, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("text.delete"), button -> {
            if(selectionWidget.getFocused() != null) {
                selectionWidget.getFocused().file.delete();
                selectionWidget.removeEntry(selectionWidget.getFocused());
            }
        }).dimensions(width / 2 + 10, height - 30, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if(this.client.world != null) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("text.only_render_on_menu"), width / 2, (int) (height / 2d - 4.5), -1);
        } else {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("text.recordings"), width / 2, 10, -1);
        }
    }
}

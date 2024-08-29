package io.github.betterclient.maxima.ui;

import io.github.betterclient.maxima.MaximaClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.text.Text;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Objects;

public class MaximaSelectionWidget extends AlwaysSelectedEntryListWidget<MaximaSelectionWidget.Entry> {
    public MaximaSelectionWidget(int i, int j, int k, int l) {
        super(MinecraftClient.getInstance(), i, j, k, l);

        for (File file : Objects.requireNonNull(MaximaClient.recordings.listFiles())) {
            this.addEntry(new Entry(file));
        }
    }

    public boolean removeEntry(Entry entry) {
        return super.removeEntry(entry);
    }

    public static class Entry extends AlwaysSelectedEntryListWidget.Entry<MaximaSelectionWidget.Entry> {
        public final File file;
        public String size;

        public Entry(File file) {
            this.file = file;
            this.size = new DecimalFormat("0.0").format(file.length() / 1048576d) + " mb";
        }

        @Override
        public Text getNarration() {
            return Text.of(""); //no
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            TextRenderer renderer = MinecraftClient.getInstance().textRenderer;

            context.drawText(renderer, file.getName().replace(".mxr", ""), x + 2, y + 2, -1, true);
            context.drawText(renderer, this.size, x + 2, y + entryHeight - 10, -1, true);
        }
    }
}

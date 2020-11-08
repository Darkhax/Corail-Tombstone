package ovh.corail.tombstone.gui;

import net.minecraft.client.GameSettings;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.settings.SliderPercentageOption;
import net.minecraft.util.text.StringTextComponent;

import java.util.List;
import java.util.function.Consumer;

public class ColorButtonHandler {
    private final int x, y, width, height;
    private int r, g, b;
    private TBColorSelectionButton button1, button2, button3;

    ColorButtonHandler(GameSettings settings, List<Widget> list, List<IGuiEventListener> children, int x, int y, int width, int height, int r, int g, int b, String title, Consumer<Boolean> dirty) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.x = x;
        this.y = y;
        this.button1 = new TBColorSelectionButton(settings, x, y, width, height,
                new SliderPercentageOption(title + "_R", 0d, 255d, 1f,
                        s -> (double) this.r, (s, d) -> this.r = d.intValue(), (s, d) -> new StringTextComponent(title + "_R")), this::getMinColorR, this::getMaxColorR, dirty
        );
        list.add(button1);
        children.add(button1);
        this.button2 = new TBColorSelectionButton(settings, x, y + 6, width, height,
                new SliderPercentageOption(title + "_G", 0d, 255d, 1f,
                        s -> (double) this.g, (s, d) -> this.g = d.intValue(), (s, d) -> new StringTextComponent(title + "_G")), this::getMinColorG, this::getMaxColorG, dirty
        );
        list.add(button2);
        children.add(button2);
        this.button3 = new TBColorSelectionButton(settings, x, y + 12, width, height,
                new SliderPercentageOption(title + "_B", 0d, 255d, 1f,
                        s -> (double) this.b, (s, d) -> this.b = d.intValue(), (s, d) -> new StringTextComponent(title + "_B")), this::getMinColorB, this::getMaxColorB, dirty
        );
        list.add(button3);
        children.add(button3);
        this.width = width;
        this.height = height + 6;
    }

    private int getMinColorR() {
        return this.g * 0x0100 + this.b;
    }

    private int getMaxColorR() {
        return getMinColorR() + 0xff0000;
    }

    private int getMinColorG() {
        return this.r * 0x010000 + this.b;
    }

    private int getMaxColorG() {
        return getMinColorG() + 0xff00;
    }

    private int getMinColorB() {
        return this.r * 0x010000 + this.g * 0x0100;
    }

    private int getMaxColorB() {
        return getMinColorB() + 0xff;
    }

    public int getColor() {
        return 0x010000 * this.r + 0x0100 * this.g + this.b;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    int getWidth() {
        return this.width;
    }

    int getHeight() {
        return this.height;
    }

    void enableButtons() {
        this.button1.active = true;
        this.button1.visible = true;
        this.button2.active = true;
        this.button2.visible = true;
        this.button3.active = true;
        this.button3.visible = true;
    }

    void disableButtons() {
        this.button1.active = false;
        this.button1.visible = false;
        this.button2.active = false;
        this.button2.visible = false;
        this.button3.active = false;
        this.button3.visible = false;
    }
}

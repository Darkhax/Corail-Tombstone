package ovh.corail.tombstone.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.OptionSlider;
import net.minecraft.client.settings.SliderPercentageOption;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.helper.Helper;

import java.util.function.Consumer;
import java.util.function.IntSupplier;

@OnlyIn(Dist.CLIENT)
public class TBColorSelectionButton extends OptionSlider {
    private final IntSupplier intSupplier1, intSupplier2;
    private final Consumer<Boolean> dirty;

    public TBColorSelectionButton(GameSettings settings, int x, int y, int width, int height, SliderPercentageOption sliderPercentageOption, IntSupplier intSupplier1, IntSupplier intSupplier2, Consumer<Boolean> dirty) {
        super(settings, x, y, width, height, sliderPercentageOption);
        this.intSupplier1 = intSupplier1;
        this.intSupplier2 = intSupplier2;
        this.dirty = dirty;
    }

    @Override
    protected void renderBg(MatrixStack matrixStack, Minecraft minecraft, int x, int y) {
        minecraft.getTextureManager().bindTexture(WIDGETS_LOCATION);
        fill(matrixStack, this.x - 1, this.y - 1, this.x + this.width + 1, this.y + this.height + 1, 0xff000000);
        Helper.fillGradient(matrixStack.getLast().getMatrix(), this.x, this.y, this.x + this.width, this.y + this.height, this.intSupplier1.getAsInt() + 0xff000000, this.intSupplier2.getAsInt() + 0xff000000, getBlitOffset(), true);
        fillGradient(matrixStack, this.x + (int) (this.width * this.sliderValue) - 1, this.y, this.x + (int) (this.width * this.sliderValue) + 1, this.y + this.height, 0xffc0c0c0, 0xff000000);
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int x, int y, float partialTicks) {
        renderBg(matrixStack, Minecraft.getInstance(), x, y);
    }

    @Override
    protected void func_230972_a_() {
        super.func_230972_a_();
        this.dirty.accept(true);
    }
}

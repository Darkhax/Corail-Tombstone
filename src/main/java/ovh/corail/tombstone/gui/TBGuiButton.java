package ovh.corail.tombstone.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TBGuiButton extends Button {
    public boolean forceHighlight = false;

    TBGuiButton(int x, int y, int widthIn, int heightIn, ITextComponent buttonText, Button.IPressable onPress) {
        super(x, y, widthIn, heightIn, buttonText, onPress);
        setFGColor(0xffffffff);
    }

    @Override
    protected void renderBg(MatrixStack matrixStack, Minecraft minecraft, int i, int j) {
        if (isHovered()) {
            fillGradient(matrixStack, this.x - 1, this.y - 1, this.x + this.width + 1, this.y + this.height + 1, 0xff202D5F, 0xff202D5F);
        }
        fillGradient(matrixStack, this.x, this.y, this.x + this.width, this.y + this.height, 0x70000000, 0x30000000);
    }

    @Override
    public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        renderBg(matrixStack, minecraft, mouseX, mouseY);
        int j = isHovered() || this.forceHighlight ? 0xff897235 : getFGColor();
        this.drawCenteredString(matrixStack, minecraft.fontRenderer, getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, j | MathHelper.ceil(this.alpha * 255f) << 24);
    }
}

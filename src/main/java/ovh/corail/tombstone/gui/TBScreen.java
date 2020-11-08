package ovh.corail.tombstone.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

@OnlyIn(Dist.CLIENT)
public abstract class TBScreen extends Screen {
    private static final ResourceLocation GUI_BACKGROUND = new ResourceLocation(MOD_ID, "textures/painting/crow.png");
    int xSize = 200, ySize = 200, halfWidth, halfHeight, guiLeft, guiTop, guiRight, guiBottom;
    final int borderColor = 0xffffffff;
    final int textColor = 0xffffffff;
    final int disableColor = 0xff808080;
    final int bonusColor = 0x872bcd;
    final int maxColor = 0xffe1c87c;
    final int defaultColor = 0xff4050ff;

    TBScreen(ITextComponent titleIn) {
        super(titleIn);
    }

    @Override
    public void init() {
        this.halfWidth = this.width / 2;
        this.halfHeight = this.height / 2;
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
        this.guiRight = this.guiLeft + this.xSize;
        this.guiBottom = this.guiTop + this.ySize;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void renderBackground(MatrixStack matrixStack) {
        super.renderBackground(matrixStack);
        fill(matrixStack, this.guiLeft - 3, this.guiTop - 3, this.guiLeft + this.xSize + 3, this.guiTop + this.ySize + 3, this.borderColor);
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        getMinecraft().textureManager.bindTexture(GUI_BACKGROUND);
        blit(matrixStack, this.guiLeft, this.guiTop, 0, 0, this.xSize, this.ySize, this.xSize, this.ySize);
    }
}

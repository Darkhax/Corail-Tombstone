package ovh.corail.tombstone.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.SkullTileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.block.BlockGraveBase;
import ovh.corail.tombstone.block.GraveModel;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.config.ConfigTombstone.Client.GraveSkinRule;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.tileentity.TileEntityWritableGrave;

import java.text.SimpleDateFormat;
import java.util.Date;

import static ovh.corail.tombstone.block.GraveModel.GRAVE_CROSS;
import static ovh.corail.tombstone.block.GraveModel.GRAVE_ORIGINAL;

@OnlyIn(Dist.CLIENT)
@SuppressWarnings("deprecation")
public class RenderWritableGrave<T extends TileEntityWritableGrave> extends TileEntityRenderer<T> {
    private static final ResourceLocation TEXTURE_SKELETON_HEAD = new ResourceLocation("minecraft", "textures/entity/skeleton/skeleton.png");

    public RenderWritableGrave(TileEntityRendererDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public void render(T te, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer iRenderTypeBuffer, int light, int destroyStage) {
        if (te == null || te.getWorld() == null) {
            return;
        }
        boolean renderHalloween = ConfigTombstone.client.graveSkinRule.get() == GraveSkinRule.FORCE_HALLOWEEN || (Helper.isDateAroundHalloween() && ConfigTombstone.client.graveSkinRule.get() != GraveSkinRule.FORCE_NORMAL);
        if (!te.hasOwner() && !renderHalloween) {
            return;
        }
        BlockState knownState = te.getWorld().getBlockState(te.getPos());
        if (!(knownState.getBlock() instanceof BlockGraveBase)) {
            return;
        }
        Direction facing = knownState.get(BlockGraveBase.FACING);
        BlockGraveBase grave = (BlockGraveBase) knownState.getBlock();
        GraveModel graveModel = grave.getGraveType();
        if (renderHalloween) {
            renderHalloween(matrixStack, iRenderTypeBuffer, graveModel, facing, light, Helper.isNight(te.getWorld()));
        }
        light = 0xf000f0;
        if (!te.hasOwner()) {
            return;
        }
        int rotationIndex;
        float modX = 0.5f, modY, modZ = 0.5f;

        float value;
        switch (graveModel) {
            case GRAVE_CROSS:
                value = 0.25f;
                modY = 0.06375f;
                break;
            case GRAVE_NORMAL:
                value = 0.12625f;
                modY = 0.5f;
                break;
            case TOMBSTONE:
                value = 0.56375f;
                modY = 0.25f;
                break;
            case SUBARAKI_GRAVE:
                value = 0.64f;
                modY = 0.65f;
                break;
            case GRAVE_ORIGINAL:
                value = 0.99f;
                modY = 0.2f;
                break;
            case GRAVE_SIMPLE:
            default:
                value = 0.18875f;
                modY = 0.4f;
                break;
        }
        boolean is_cross = graveModel == GRAVE_CROSS;
        boolean is_original = graveModel == GRAVE_ORIGINAL;
        switch (facing) {
            case SOUTH:
                rotationIndex = 0;
                if (is_cross) {
                    modZ = 1f - value;
                } else {
                    modZ = value;
                }
                break;
            case WEST:
                rotationIndex = -1;
                if (is_cross) {
                    modX = value;
                } else {
                    modX = 1f - value;
                }
                break;
            case EAST:
                rotationIndex = 1;
                if (is_cross) {
                    modX = 1f - value;
                } else {
                    modX = value;
                }
                break;
            case NORTH:
            default:
                rotationIndex = 2;
                if (is_cross) {
                    modZ = value;
                } else {
                    modZ = 1f - value;
                }
        }

        matrixStack.push();
        matrixStack.translate(modX, modY, modZ);
        matrixStack.rotate(Vector3f.XP.rotationDegrees(180f));
        // TODO adapt this
        if (is_cross) {
            switch (facing) {
                case SOUTH:
                    matrixStack.rotate(Vector3f.XP.rotationDegrees(-90f));
                    break;
                case WEST:
                    matrixStack.rotate(Vector3f.ZP.rotationDegrees(90f));
                    break;
                case EAST:
                    matrixStack.rotate(Vector3f.ZP.rotationDegrees(-90f));
                    break;
                case NORTH:
                default:
                    matrixStack.rotate(Vector3f.XP.rotationDegrees(90f));
                    break;
            }
        }
        matrixStack.rotate(Vector3f.YP.rotationDegrees(-90f * rotationIndex)); // horizontal rot
        FontRenderer fontRender = this.renderDispatcher.fontRenderer;

        // rip message
        showString(TextFormatting.BOLD + LangKey.MESSAGE_RIP.getClientTranslation(), matrixStack, iRenderTypeBuffer, fontRender, (is_original ? 8 : 0), ConfigTombstone.client.textColorRIP.get() + 0xff000000, 0.007f, light);

        // owner message
        showString(TextFormatting.BOLD + te.getOwnerName(), matrixStack, iRenderTypeBuffer, fontRender, (is_original ? 14 : 11), ConfigTombstone.client.textColorOwner.get() + 0xff000000, 0.005f, light);

        // death date message
        float scaleForDate = ConfigTombstone.client.dateInMCTime.get() ? 0.005f : 0.004f;
        showString(TextFormatting.BOLD + LangKey.MESSAGE_DIED_ON.getClientTranslation(), matrixStack, iRenderTypeBuffer, fontRender, 26, ConfigTombstone.client.textColorDeathDate.get() + 0xff000000, scaleForDate, light);

        if (ConfigTombstone.client.dateInMCTime.get()) {
            // time goes 72 times faster than real time
            long days = te.countTicks / 24000; // TODO incorrect, tiles don't always tick, store gametime
            String dateString = LangKey.MESSAGE_DAY.getClientTranslation(days);
            showString(TextFormatting.BOLD + dateString, matrixStack, iRenderTypeBuffer, fontRender, 36, ConfigTombstone.client.textColorDeathDate.get() + 0xff000000, scaleForDate, light);
        } else {
            Date date = new Date(te.getOwnerDeathTime());
            String dateString = new SimpleDateFormat("dd/MM/yyyy").format(date);
            String timeString = LangKey.MESSAGE_AT.getClientTranslation() + " " + new SimpleDateFormat("HH:mm:ss").format(date);
            showString(TextFormatting.BOLD + dateString, matrixStack, iRenderTypeBuffer, fontRender, 36, ConfigTombstone.client.textColorDeathDate.get() + 0xff000000, scaleForDate, light);
            showString(TextFormatting.BOLD + timeString, matrixStack, iRenderTypeBuffer, fontRender, 46, ConfigTombstone.client.textColorDeathDate.get() + 0xff000000, scaleForDate, light);
        }

        matrixStack.pop();
    }

    private void showString(String content, MatrixStack matrixStack, IRenderTypeBuffer iRenderTypeBuffer, FontRenderer fontRenderer, int posY, int color, float scale, int light) {
        matrixStack.push();
        matrixStack.scale(scale, scale, scale);
        fontRenderer.renderString(content, (float) (-fontRenderer.getStringWidth(content) / 2), posY - 30, color, false, matrixStack.getLast().getMatrix(), iRenderTypeBuffer, false, 0, light);
        matrixStack.pop();
    }

    @SuppressWarnings("deprecation")
    private void renderHalloween(MatrixStack matrixStack, IRenderTypeBuffer iRenderTypeBuffer, GraveModel graveModel, Direction facing, int light, boolean isNight) {
        RenderSystem.enableRescaleNormal();
        RenderSystem.pushMatrix();
        RenderSystem.disableCull();
        RenderSystem.enableAlphaTest();
        float decoX = 0.5f, decoY = 0.07f, decoZ = 0.5f;
        switch (graveModel) {
            case GRAVE_NORMAL:
                decoY += 0.35f;
                break;
            case GRAVE_CROSS:
                if (facing == Direction.SOUTH) {
                    decoX -= 0.2f;
                } else if (facing == Direction.WEST) {
                    decoZ -= 0.2f;
                } else if (facing == Direction.EAST) {
                    decoZ += 0.2f;
                } else {
                    decoX += 0.2f;
                }
                break;
            case TOMBSTONE:
                decoY += 0.6f;
                break;
            case SUBARAKI_GRAVE:
                decoY += 0.37f;
                if (facing == Direction.SOUTH) {
                    decoX += 0.35f;
                    decoZ += 0.35f;
                } else if (facing == Direction.WEST) {
                    decoX -= 0.35f;
                    decoZ += 0.35f;
                } else if (facing == Direction.EAST) {
                    decoX += 0.35f;
                    decoZ -= 0.35f;
                } else {
                    decoX -= 0.35f;
                    decoZ -= 0.35f;
                }
                break;
            case GRAVE_ORIGINAL:
                if (facing == Direction.SOUTH) {
                    decoZ += 0.35f;
                    decoX += 0.575f;
                } else if (facing == Direction.WEST) {
                    decoZ += 0.575f;
                    decoX -= 0.35f;
                } else if (facing == Direction.EAST) {
                    decoZ -= 0.575f;
                    decoX += 0.35f;
                } else {
                    decoZ -= 0.35f;
                    decoX -= 0.575f;
                }
                break;
            case GRAVE_SIMPLE:
            default:
                decoY += 0.1f;
                break;
        }
        Minecraft.getInstance().textureManager.bindTexture(TEXTURE_SKELETON_HEAD);
        matrixStack.push();
        matrixStack.translate(decoX, decoY, decoZ);
        matrixStack.rotate(Vector3f.YP.rotationDegrees(facing.getHorizontalAngle() + (facing == Direction.SOUTH || facing == Direction.NORTH ? 180 : 0)));
        if (graveModel == GraveModel.GRAVE_NORMAL || graveModel == GraveModel.GRAVE_SIMPLE) {
            matrixStack.scale(0.2f, 0.2f, 0.2f);
            ItemStack stack = new ItemStack(isNight ? Blocks.JACK_O_LANTERN : Blocks.PUMPKIN);
            Minecraft.getInstance().getItemRenderer().renderItem(stack, net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType.NONE, false, matrixStack, iRenderTypeBuffer, 15728880, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, Minecraft.getInstance().getItemRenderer().getItemModelWithOverrides(stack, null, null));
        } else {
            matrixStack.scale(0.3f, 0.3f, 0.3f);
            SkullTileEntityRenderer.render(null, 1f, SkullBlock.Types.SKELETON, null, 0f, matrixStack, iRenderTypeBuffer, isNight ? 0xf000f0 : light);
        }
        matrixStack.pop();
        RenderSystem.popMatrix();
    }
}

package ovh.corail.tombstone.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.NBTStackHelper;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.registry.ModBlocks;
import ovh.corail.tombstone.registry.ModTabs;

import javax.annotation.Nullable;
import java.util.List;

public class ItemBlockGrave extends BlockItem {
    public static final int MAX_MODEL_TEXTURE = 1;

    public ItemBlockGrave(Block grave) {
        super(grave, new Item.Properties().group(ModTabs.mainTab).maxStackSize(1));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        String engravedName = getEngravedName(stack);
        if (engravedName.isEmpty()) {
            tooltip.add(LangKey.MESSAGE_ENGRAVABLE.getText(StyleType.TOOLTIP_DESC, new StringTextComponent("[" + I18n.format(Items.IRON_INGOT.getTranslationKey()) + "]").mergeStyle(StyleType.TOOLTIP_ITEM)));
        } else {
            tooltip.add(LangKey.MESSAGE_ENGRAVED.getText(StyleType.TOOLTIP_DESC, new StringTextComponent('"' + engravedName + '"').setStyle(StyleType.TOOLTIP_ITEM)));
        }
        super.addInformation(stack, world, tooltip, flag);
    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack) {
        TranslationTextComponent baseTranslation = new TranslationTextComponent(getTranslationKey());
        return getEngravedName(stack).isEmpty() ? baseTranslation : LangKey.MESSAGE_ENGRAVED_ITEM.getText(StyleType.MESSAGE_SPECIAL, baseTranslation);
    }

    @Override
    protected boolean placeBlock(BlockItemUseContext context, BlockState state) {
        return context.getWorld().getBlockState(context.getPos().down()).isNormalCube(context.getWorld(), context.getPos().down()) && super.placeBlock(context, state);
    }

    public static boolean setEngravedName(ItemStack stack, String engraved_name) {
        if (stack.getItem() instanceof ItemBlockGrave) {
            NBTStackHelper.setString(stack, "engraved_name", engraved_name);
            return true;
        }
        return false;
    }

    public static boolean isEngraved(ItemStack stack) {
        return !getEngravedName(stack).isEmpty();
    }

    public static String getEngravedName(ItemStack stack) {
        return stack.getItem() instanceof ItemBlockGrave ? NBTStackHelper.getString(stack, "engraved_name") : "";
    }

    public static ItemStack setModelTexture(ItemStack stack, int modelTexture) {
        if (stack.getItem() instanceof ItemBlockGrave) {
            NBTStackHelper.setInteger(stack, "model_texture", MathHelper.clamp(modelTexture, 0, MAX_MODEL_TEXTURE));
        }
        return stack;
    }

    public static int getModelTexture(ItemStack stack) {
        return stack.getItem() instanceof ItemBlockGrave ? MathHelper.clamp(NBTStackHelper.getInteger(stack, "model_texture"), 0, MAX_MODEL_TEXTURE) : 0;
    }

    public static ItemStack createRandomDecorativeStack() {
        return createDecorativeStack(GraveModel.getRandom(), Helper.random.nextInt(MAX_MODEL_TEXTURE));
    }

    public static ItemStack createDecorativeStack(GraveModel graveModel, int textureId) {
        return createDecorativeStack(graveModel, textureId, "");
    }

    public static ItemStack createDecorativeStack(GraveModel model, int textureId, String engravedName) {
        ItemStack stack = ItemBlockGrave.setModelTexture(new ItemStack(ModBlocks.decorative_graves.get(model)), textureId);
        return engravedName.isEmpty() ? stack : NBTStackHelper.setString(stack, "engraved_name", engravedName);
    }
}

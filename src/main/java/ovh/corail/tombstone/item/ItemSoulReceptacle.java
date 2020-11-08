package ovh.corail.tombstone.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.StyleType;

import javax.annotation.Nullable;
import java.util.List;

public class ItemSoulReceptacle extends ItemGeneric {

    public ItemSoulReceptacle() {
        super("soul_receptacle", true);
    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack) {
        return super.getDisplayName(stack).copyRaw().setStyle(StyleType.MESSAGE_SPECIAL);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        if (Helper.canShowTooltip(world, stack)) {
            addItemDesc(list);
            addItemUse(list, "1");
            addItemUse(list, "2");
        } else {
            addInfoShowTooltip(list);
        }
        super.addInformation(stack, world, list, flag);
    }
}

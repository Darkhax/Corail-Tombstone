package ovh.corail.tombstone.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Food;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.helper.EffectHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.StyleType;

import javax.annotation.Nullable;
import java.util.List;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

public class ItemLollipop extends ItemGeneric {

    public enum ModelColor {YELLOW, PURPLE, GREEN, MAGENTA, RED}

    private final ModelColor color;

    public ItemLollipop(ModelColor color) {
        super("lollipop", new Properties().group(null).maxStackSize(64).food(new Food.Builder().hunger(2).saturation(2f).setAlwaysEdible().build()));
        this.color = color;
    }

    public String getSimpleName() {
        return this.name + "_" + color.ordinal();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        if (Helper.canShowTooltip(world, stack)) {
            list.add(new TranslationTextComponent(getTranslationKey(stack) + ".desc").setStyle(StyleType.TOOLTIP_DESC));
        } else {
            list.add(LangKey.TOOLTIP_MORE_INFO.getText(StyleType.TOOLTIP_DESC));
        }
        super.addInformation(stack, world, list, flag);
    }

    public ItemStack onItemUseFinish(ItemStack stack, World world, LivingEntity entity) {
        super.onItemUseFinish(stack, world, entity);
        if (entity instanceof ServerPlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            EffectHelper.addRandomEffect(player, 12000, true);
        }
        return stack;
    }

    @Override
    public String getTranslationKey() {
        return MOD_ID + ".item." + name;
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return getTranslationKey();
    }
}

package ovh.corail.tombstone.enchantment;

import net.minecraft.client.resources.I18n;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.StyleType;

import java.util.ArrayList;
import java.util.List;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

public abstract class TombstoneEnchantment extends Enchantment {
    private final String customName;

    TombstoneEnchantment(String name, Rarity rarity, EnchantmentType type, EquipmentSlotType[] slots) {
        super(rarity, type, slots);
        this.customName = name;
    }

    protected abstract boolean isEnabled();

    @Override
    public int getMinLevel() {
        return 1;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public int getMinEnchantability(int lvl) {
        return 1;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return isEnabled() && super.canApplyAtEnchantingTable(stack);
    }

    @Override
    public boolean isAllowedOnBooks() {
        return isEnabled();
    }

    @OnlyIn(Dist.CLIENT)
    public List<String> getTooltipInfos(ItemStack stack) {
        List<String> infos = new ArrayList<>();
        if (stack.getItem() == Items.ENCHANTED_BOOK) {
            infos.add(Helper.getFormattingCode(StyleType.TOOLTIP_ENCHANT) + I18n.format(getName() + ".desc"));
        }
        if (!isEnabled()) {
            infos.add(LangKey.MESSAGE_DISABLED.getClientTranslationWithStyle(StyleType.COLOR_OFF));
        }
        return infos;
    }

    @Override
    public String getName() {
        return "enchantment." + MOD_ID + "." + this.customName;
    }
}

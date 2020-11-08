package ovh.corail.tombstone.registry;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.block.GraveModel;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;
import static ovh.corail.tombstone.ModTombstone.MOD_NAME;

public class ModTabs {
    public static final ItemGroup mainTab = new ItemGroup(MOD_ID) {
        @Override
        @OnlyIn(Dist.CLIENT)
        public ItemStack createIcon() {
            return new ItemStack(ModBlocks.decorative_graves.get(GraveModel.TOMBSTONE));
        }

        @Override
        public ITextComponent getGroupName() {
            return new StringTextComponent(MOD_NAME);
        }

        @OnlyIn(Dist.CLIENT)
        public void fill(NonNullList<ItemStack> items) {
            super.fill(items);
            items.add(ModItems.tablet_of_recall.createAncient());
            items.add(ModItems.tablet_of_home.createAncient());
            fillWithEnchantedBook(items, ModEnchantments.soulbound, 1);
            for (int lvl = 1; lvl <= 5; lvl++) {
                fillWithEnchantedBook(items, ModEnchantments.shadow_step, lvl);
            }
            for (int lvl = 1; lvl <= 5; lvl++) {
                fillWithEnchantedBook(items, ModEnchantments.magic_siphon, lvl);
            }
            for (int lvl = 1; lvl <= 3; lvl++) {
                fillWithEnchantedBook(items, ModEnchantments.plague_bringer, lvl);
            }
        }
    };

    private static void fillWithEnchantedBook(NonNullList<ItemStack> items, Enchantment enchant, int level) {
        items.add(EnchantedBookItem.getEnchantedItemStack(new EnchantmentData(enchant, level)));
    }
}

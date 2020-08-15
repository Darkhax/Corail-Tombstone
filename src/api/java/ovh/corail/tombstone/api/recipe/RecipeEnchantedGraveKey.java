package ovh.corail.tombstone.api.recipe;

import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.registries.ObjectHolder;

import java.util.stream.IntStream;

import static ovh.corail.tombstone.api.TombstoneAPIProps.OWNER;

@SuppressWarnings({ "WeakerAccess" })
public class RecipeEnchantedGraveKey extends ShapelessRecipe {
    @ObjectHolder("tombstone:grave_key")
    public static final Item GRAVE_KEY = Items.AIR;
    private static final ITag.INamedTag<Item> ENCHANTED_GRAVE_KEY_INGREDIENTS = ItemTags.makeWrapperTag(OWNER + ":enchanted_grave_key_ingredients");

    public RecipeEnchantedGraveKey(ResourceLocation rl) {
        // default recipe "tombstone:enchanted_grave_key" as example
        this(rl, NonNullList.withSize(1, Ingredient.EMPTY));
    }

    public RecipeEnchantedGraveKey(ResourceLocation rl, NonNullList<Ingredient> ingredients) {
        super(rl, "enchanted_grave_key", setEnchant(new ItemStack(GRAVE_KEY), false), ingredients);
    }

    @Override
    public boolean matches(CraftingInventory inv, World world) {
        if (GRAVE_KEY != Items.AIR && ((IDisableable) GRAVE_KEY).isEnabled()) {
            boolean keyFound = false, compoFound = false;
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    if (!keyFound && stack.getItem() == GRAVE_KEY && (stack.getTag() == null || !stack.getTag().getBoolean("enchant"))) {
                        keyFound = true;
                        continue;
                    } else if (!compoFound && ENCHANTED_GRAVE_KEY_INGREDIENTS.contains(stack.getItem())) {
                        compoFound = true;
                        continue;
                    }
                    return false;
                }
            }
            return keyFound && compoFound;
        }
        return false;
    }

    @Override
    public ItemStack getCraftingResult(CraftingInventory inv) {
        return IntStream.range(0, inv.getSizeInventory()).mapToObj(inv::getStackInSlot).filter(stack -> stack.getItem() == GRAVE_KEY).findFirst().map(stack -> setEnchant(stack.copy(), true)).orElse(ItemStack.EMPTY);
    }

    /**
     * This method only exists to imitate what is done when a key is enchanted
     *
     * @param key a key not yet enchanted
     * @return the result of this recipe, an enchanted key
     */
    public static ItemStack setEnchant(ItemStack key, boolean checkCompound) {
        if (key.getItem() == GRAVE_KEY) {
            CompoundNBT nbt = key.getOrCreateTag();
            if (checkCompound && nbt.contains("enchant", Constants.NBT.TAG_BYTE) && nbt.getBoolean("enchant")) {
                return ItemStack.EMPTY; // the key is already enchanted
            }
            nbt.putBoolean("enchant", true);
        }
        return key;
    }
}

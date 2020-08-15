package ovh.corail.tombstone.recipe;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.Tags;
import ovh.corail.tombstone.capability.TBCapabilityProvider;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.registry.ModItems;

import javax.annotation.Nullable;

public class RecipeFamiliarReceptacle extends ShapedRecipe {

    public RecipeFamiliarReceptacle(ResourceLocation rl) {
        this(rl, 3, 3, NonNullList.withSize(1, Ingredient.EMPTY));
    }

    public RecipeFamiliarReceptacle(ResourceLocation rl, int width, int height, NonNullList<Ingredient> ingredients) {
        super(rl, "familiar_receptacle", width, height, ingredients, new ItemStack(ModItems.familiar_receptacle));
    }

    @Override
    public boolean matches(CraftingInventory inv, World world) {
        NonNullList<Ingredient> ing = getIngredients();
        if (ing.size() == 1 && ing.get(0) == Ingredient.EMPTY) {
            ing.clear();
            Ingredient tear = Ingredient.fromStacks(new ItemStack(Items.GHAST_TEAR));
            Ingredient iron = Ingredient.fromTag(Tags.Items.INGOTS_IRON);
            ing.add(tear);
            ing.add(iron);
            ing.add(tear);
            ing.add(iron);
            ing.add(Ingredient.fromStacks(new ItemStack(ModItems.impregnated_diamond)));
            ing.add(iron);
            ing.add(tear);
            ing.add(iron);
            ing.add(tear);
        }
        return super.matches(inv, world);
    }

    @Override
    public ItemStack getCraftingResult(CraftingInventory inv) {
        PlayerEntity player = getPlayer(inv);
        return player == null ? ItemStack.EMPTY : player.getCapability(TBCapabilityProvider.TB_CAPABILITY, null).map(cap -> cap.getTotalPerkPoints() >= SharedConfigTombstone.general.familiarReceptacleRequiredLevel.get() ? getFinalResult(player, inv) : ItemStack.EMPTY).orElse(ItemStack.EMPTY);
    }

    private ItemStack getFinalResult(PlayerEntity player, CraftingInventory inv) {
        String impregnatedType = "";
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.getItem() == ModItems.impregnated_diamond) {
                impregnatedType = ModItems.impregnated_diamond.getEntityType(stack);
                break;
            }
        }
        return Helper.isTameable(player.world, impregnatedType) ? ModItems.familiar_receptacle.setCapturableType(getRecipeOutput().copy(), impregnatedType) : getRecipeOutput().copy();
    }

    @Nullable
    private static PlayerEntity getPlayer(final CraftingInventory inventory) {
        return inventory.eventHandler.inventorySlots.stream()
                .map(slot -> slot.inventory)
                .filter(PlayerInventory.class::isInstance)
                .map(PlayerInventory.class::cast)
                .map(inv -> inv.player)
                .findFirst().orElse(null);
    }
}

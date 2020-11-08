package ovh.corail.tombstone.recipe;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.item.ItemVoodooPoppet.PoppetProtections;
import ovh.corail.tombstone.registry.ModItems;
import ovh.corail.tombstone.registry.ModPerks;
import ovh.corail.tombstone.registry.ModTags;

import javax.annotation.Nullable;

public class RecipeVoodooPoppetProtection extends ShapelessRecipe {
    private static final NonNullList<Ingredient> INGREDIENTS = NonNullList.create();

    static {
        INGREDIENTS.add(Ingredient.fromStacks(new ItemStack(ModItems.voodoo_poppet)));
        INGREDIENTS.add(Ingredient.fromStacks(new ItemStack(ModItems.grave_dust)));
    }

    public RecipeVoodooPoppetProtection(ResourceLocation rl) {
        super(rl, "voodoo_poppet_protection", new ItemStack(ModItems.voodoo_poppet), NonNullList.from(Ingredient.EMPTY, INGREDIENTS.get(0), INGREDIENTS.get(1), Ingredient.fromItems(Items.LILY_PAD, Items.MAGMA_CREAM, Items.STICK, Items.FEATHER, Items.POISONOUS_POTATO)));
    }

    @Override
    public boolean matches(CraftingInventory inv, World world) {
        if (!ModItems.voodoo_poppet.isEnabled()) {
            return false;
        }
        boolean voodooPoppetFound = false, dustFound = false, compoFound = false;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (!voodooPoppetFound && stack.getItem() == ModItems.voodoo_poppet && !ModItems.voodoo_poppet.isEnchanted(stack)) {
                    voodooPoppetFound = true;
                    continue;
                } else if (!dustFound && INGREDIENTS.get(1).test(stack)) {
                    dustFound = true;
                    continue;
                } else if (!compoFound && ModTags.Items.VOODOO_POPPET_INGREDIENTS.contains(stack.getItem())) {
                    compoFound = true;
                    continue;
                }
                return false;
            }
        }
        return voodooPoppetFound && dustFound && compoFound;
    }

    @Override
    public ItemStack getCraftingResult(CraftingInventory inv) {
        ItemStack poppet = ItemStack.EMPTY;
        ItemStack compo = ItemStack.EMPTY;
        PoppetProtections prot = null;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (poppet.isEmpty() && stack.getItem() == ModItems.voodoo_poppet) {
                poppet = stack;
            } else if (compo.isEmpty() && ModTags.Items.VOODOO_POPPET_INGREDIENTS.contains(stack.getItem())) {
                prot = getPoppetProtection(stack);
            }
        }
        if (poppet.isEmpty() || prot == null || ModItems.voodoo_poppet.hasProtection(poppet, prot)) {
            return ItemStack.EMPTY;
        }
        if (EntityHelper.getPerkLevelWithBonus(getPlayer(inv), ModPerks.voodoo_poppet) > prot.ordinal()) {
            return ModItems.voodoo_poppet.addProtection(poppet.copy(), prot);
        }
        return ItemStack.EMPTY;
    }

    @Nullable
    private PoppetProtections getPoppetProtection(ItemStack stack) {
        if (stack.getItem() == Blocks.LILY_PAD.asItem()) {
            return PoppetProtections.SUFFOCATION;
        } else if (stack.getItem() == Items.MAGMA_CREAM) {
            return PoppetProtections.BURN;
        } else if (stack.getItem() == Items.STICK) {
            return PoppetProtections.LIGHTNING;
        } else if (stack.getItem() == Items.FEATHER) {
            return PoppetProtections.FALL;
        } else if (stack.getItem() == Items.POISONOUS_POTATO) {
            return PoppetProtections.DEGENERATION;
        }
        return null;
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

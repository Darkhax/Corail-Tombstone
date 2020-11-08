package ovh.corail.tombstone.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.ItemHandlerHelper;
import ovh.corail.tombstone.api.magic.ISoulConsumer;
import ovh.corail.tombstone.api.magic.TBSoulConsumerProvider;
import ovh.corail.tombstone.compatibility.SupportMods;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.registry.ModPerks;
import ovh.corail.tombstone.registry.ModTriggers;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ItemBookOfDisenchantment extends ItemGeneric implements ISoulConsumer {

    public ItemBookOfDisenchantment() {
        super("book_of_disenchantment", getBuilder(true), SharedConfigTombstone.allowed_magic_items.allowBookOfDisenchantment::get);
    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack) {
        return super.getDisplayName(stack).copyRaw().setStyle(StyleType.MESSAGE_SPECIAL);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        if (Helper.canShowTooltip(world, stack)) {
            addItemDesc(list);
            addItemUse(list);
        } else {
            addInfoShowTooltip(list);
        }
        super.addInformation(stack, world, list, flag);
    }

    @Override
    public boolean isEnchanted(ItemStack stack) {
        return false;
    }

    @Override
    public boolean canEnchant(World world, BlockPos gravePos, PlayerEntity player, ItemStack stack) {
        return isEnabled();
    }

    @Override
    public boolean setEnchant(World world, BlockPos gravePos, PlayerEntity player, ItemStack stackPar) {
        if (stackPar.getItem() != this) {
            return false;
        }
        ItemStack offhand = player.getHeldItemOffhand();
        if (offhand.isEmpty() || offhand.getItem() == Items.ENCHANTED_BOOK || !offhand.isEnchanted()) {
            return false;
        }
        if (SupportMods.TETRA.isLoaded() && ConfigTombstone.compatibility.disableDisenchantmentForTetra.get()) {
            ResourceLocation registryName = offhand.getItem().getRegistryName();
            if (registryName != null && registryName.getNamespace().equals(SupportMods.TETRA.getString()) && registryName.getPath().startsWith("modular_")) {
                return false;
            }
        }
        Map<Enchantment, Integer> list = EnchantmentHelper.getEnchantments(offhand);
        if (list.size() == 0) {
            return false;
        }
        Iterator<Map.Entry<Enchantment, Integer>> it = list.entrySet().iterator();
        stackPar.shrink(1);
        int enchantMax = EntityHelper.getPerkLevelWithBonus(player, ModPerks.disenchanter) + 1;
        int enchantCount = 0;
        while (enchantCount < enchantMax && it.hasNext()) {
            Map.Entry<Enchantment, Integer> entry = it.next();
            if (entry.getKey() != null && entry.getValue() != null) {
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                Map<Enchantment, Integer> map = new HashMap<>();
                map.put(entry.getKey(), entry.getValue());
                EnchantmentHelper.setEnchantments(map, book);
                ItemHandlerHelper.giveItemToPlayer(player, book);
                it.remove();
                enchantCount++;
            }
        }
        if (list.isEmpty()) {
            offhand.getOrCreateTag().remove("RepairCost");
        }
        EnchantmentHelper.setEnchantments(list, offhand);
        if (!world.isRemote) {
            ModTriggers.USE_DISENCHANTMENT.trigger((ServerPlayerEntity) player);
        }
        return true;
    }

    @Override
    public ITextComponent getEnchantSuccessMessage(PlayerEntity player) {
        return LangKey.MESSAGE_DISENCHANTMENT_SUCCESS.getText();
    }

    @Override
    public ITextComponent getEnchantFailedMessage(PlayerEntity player) {
        return LangKey.MESSAGE_DISENCHANTMENT_FAILED.getText();
    }

    @Override
    @Nullable
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundNBT nbt) {
        return new TBSoulConsumerProvider(this);
    }
}

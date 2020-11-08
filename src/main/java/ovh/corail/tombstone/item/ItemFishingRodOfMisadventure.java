package ovh.corail.tombstone.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import ovh.corail.tombstone.api.magic.ISoulConsumer;
import ovh.corail.tombstone.api.magic.TBSoulConsumerProvider;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.NBTStackHelper;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.registry.ModTabs;

import javax.annotation.Nullable;
import java.util.List;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

public class ItemFishingRodOfMisadventure extends FishingRodItem implements ISoulConsumer {
    private final String name = "fishing_rod_of_misadventure";

    public ItemFishingRodOfMisadventure() {
        super(new Properties().group(ModTabs.mainTab).maxStackSize(1).defaultMaxDamage(32)); // half durability
    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack) {
        ITextComponent name = super.getDisplayName(stack);
        return (isEnchanted(stack) ? LangKey.MESSAGE_ENCHANTED_ITEM.getText(name) : name.copyRaw()).setStyle(StyleType.MESSAGE_SPECIAL);
    }

    public String getSimpleName() {
        return this.name;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        if (Helper.canShowTooltip(world, stack)) {
            int id = isEnchanted(stack) ? 2 : 1;
            list.add(new TranslationTextComponent(getTranslationKey(stack) + ".desc" + id).setStyle(StyleType.TOOLTIP_DESC));
            list.add(new TranslationTextComponent(getTranslationKey(stack) + ".use" + id).setStyle(StyleType.TOOLTIP_USE));
        } else {
            list.add(LangKey.TOOLTIP_MORE_INFO.getText(StyleType.TOOLTIP_DESC));
        }
        super.addInformation(stack, world, list, flag);
    }

    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        boolean isEnchanted = isEnchanted(stack);
        if (player.fishingBobber != null) {
            int damage = player.fishingBobber.handleHookRetraction(stack);
            if (!isEnchanted) {
                stack.damageItem(damage, player, c -> c.sendBreakAnimation(hand));
            }
            world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(), SoundEvents.ENTITY_FISHING_BOBBER_RETRIEVE, SoundCategory.NEUTRAL, 1.0F, 0.4F / (random.nextFloat() * 0.4F + 0.8F));
        } else {
            world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(), SoundEvents.ENTITY_FISHING_BOBBER_THROW, SoundCategory.NEUTRAL, 0.5F, 0.4F / (random.nextFloat() * 0.4F + 0.8F));
            if (!world.isRemote) {
                //int speedBonus = EnchantmentHelper.getFishingSpeedBonus(stack);
                FishingBobberEntity fishingBobber = new FishingBobberEntity(player, world, 0, isEnchanted ? 2 : 0);
                fishingBobber.luck = -(isEnchanted ? 50 : 10);
                world.addEntity(fishingBobber);
            }
            player.addStat(Stats.ITEM_USED.get(this));
        }
        return new ActionResult<>(ActionResultType.SUCCESS, stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return isEnchanted(stack);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return !isEnchanted(stack) && super.showDurabilityBar(stack);
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {
        if (!isEnchanted(stack)) {
            super.setDamage(stack, damage);
        }
    }

    @Override
    public int getItemEnchantability() {
        return 0;
    }

    @Override
    public boolean isEnchanted(ItemStack stack) {
        return stack.getItem() == this && NBTStackHelper.getBoolean(stack, "enchant");
    }

    @Override
    public boolean setEnchant(World world, BlockPos gravePos, PlayerEntity player, ItemStack stack) {
        if (stack.getItem() != this) {
            return false;
        }
        stack.setDamage(0);
        NBTStackHelper.setBoolean(stack, "enchant", true);
        return true;
    }

    @Override
    public ITextComponent getEnchantSuccessMessage(PlayerEntity player) {
        return LangKey.MESSAGE_ENCHANT_FISHING_ROD_SUCCESS.getText();
    }

    @Override
    public ITextComponent getEnchantFailedMessage(PlayerEntity player) {
        return LangKey.MESSAGE_ENCHANT_ITEM_FAILED.getText();
    }

    @Override
    public String getTranslationKey() {
        return MOD_ID + ".item." + name;
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return getTranslationKey();
    }

    @Override
    @Nullable
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundNBT nbt) {
        return new TBSoulConsumerProvider(this);
    }
}

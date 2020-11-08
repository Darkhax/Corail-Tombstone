package ovh.corail.tombstone.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.NBTStackHelper;
import ovh.corail.tombstone.registry.ModTriggers;

import javax.annotation.Nullable;
import java.util.List;

public class ItemScrollOfKnowledge extends ItemScroll {

    public ItemScrollOfKnowledge() {
        super("scroll_of_knowledge", SharedConfigTombstone.allowed_magic_items.allowScrollOfKnowledge::get);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        if (Helper.canShowTooltip(world, stack)) {
            if (!isEnchanted(stack)) {
                addItemDesc(list, "1");
                addItemUse(list, "1");
            } else {
                addItemDesc(list, "2");
                addInfo(list, LangKey.MESSAGE_STORED_EXPERIENCE, getStoredXp(stack));
                addItemUse(list, "2");
            }
        }
        super.addInformation(stack, world, list, flag);
    }

    @Override
    public boolean isEnchanted(ItemStack stack) {
        return getStoredXp(stack) > 0;
    }

    @Override
    public ITextComponent getEnchantSuccessMessage(PlayerEntity player) {
        return LangKey.MESSAGE_LOSE_EXPERIENCE_SUCCESS.getText();
    }

    @Override
    public ITextComponent getEnchantFailedMessage(PlayerEntity player) {
        return LangKey.MESSAGE_LOSE_EXPERIENCE_FAILED.getText();
    }

    @Override
    public boolean setEnchant(World world, BlockPos gravePos, PlayerEntity player, ItemStack stack) {
        boolean valid = setStoredXp(stack, EntityHelper.getPlayerTotalXp(player));
        if (valid) {
            player.experienceTotal = player.experienceLevel = 0;
            player.experience = 0f;
        }
        return valid;
    }

    @Override
    protected boolean doEffects(World world, ServerPlayerEntity player, ItemStack stack) {
        int storedXp = getStoredXp(stack);
        int i = Integer.MAX_VALUE - player.experienceTotal;
        if (storedXp > i) {
            LangKey.MESSAGE_EARN_EXPERIENCE_FAILED.sendMessage(player);
            return false;
        }
        EntityHelper.setPlayerXp(player, EntityHelper.getPlayerTotalXp(player) + storedXp);
        LangKey.MESSAGE_EARN_EXPERIENCE_SUCCESS.sendMessage(player);
        ModTriggers.USE_KNOWLEDGE.trigger(player);
        return true;
    }

    public int getStoredXp(ItemStack stack) {
        int storedXp = NBTStackHelper.getInteger(stack, "stored_xp");
        return storedXp > 0 ? storedXp : 0;
    }

    public boolean setStoredXp(ItemStack stack, int xp) {
        if (stack.getItem() != this || xp <= 0) {
            return false;
        }
        NBTStackHelper.setInteger(stack, "stored_xp", xp);
        return true;
    }
}

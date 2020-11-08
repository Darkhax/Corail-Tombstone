package ovh.corail.tombstone.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.NBTStackHelper;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.helper.TimeHelper;

import javax.annotation.Nullable;
import java.util.List;

public class ItemBoneNeedle extends ItemGeneric implements IImpregnable {
    public ItemBoneNeedle() {
        super("bone_needle", getBuilder(true).maxStackSize(1));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        if (Helper.canShowTooltip(world, stack)) {
            addItemDesc(list);
            ITextComponent impregnated = getTooltipDisplay(stack);
            if (impregnated == null) {
                addItemUse(list);
            } else {
                list.add(impregnated);
            }
        } else {
            addInfoShowTooltip(list);
        }
        super.addInformation(stack, world, list, flag);
        int impregnatedTime = getImpregnatedTime(world, stack);
        if (impregnatedTime > 0) {
            addWarn(list, LangKey.MESSAGE_IMPREGNATE_DURATION, TimeHelper.getTimeString(impregnatedTime));
        }
    }

    @Override
    public ActionResultType itemInteractionForEntity(ItemStack stack, PlayerEntity player, LivingEntity target, Hand hand) {
        ItemStack mainHandstack;
        if (EntityHelper.isValidPlayer(player) && hand == Hand.MAIN_HAND && (mainHandstack = player.getHeldItemMainhand()).getItem() == this && !EntityHelper.hasCooldown(player, this)) {
            EntityHelper.setCooldown(player, this, 10);
            if (!player.world.isRemote) {
                boolean valid = impregnate(mainHandstack, target);
                if (valid) {
                    NBTStackHelper.setLong(stack, IMPREGNATED_TIME_NBT_LONG, TimeHelper.worldTicks(player.world) + IMPREGNATED_MAX_TIME);
                }
                player.sendMessage(valid ? LangKey.MESSAGE_IMPREGNATE_NEEDLE_SUCCESS.getText(StyleType.MESSAGE_SPECIAL, target.getName()) : LangKey.MESSAGE_IMPREGNATE_NEEDLE_FAILED.getText(StyleType.MESSAGE_SPECIAL), Util.DUMMY_UUID);
            }
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.FAIL;
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slotId, boolean isSelected) {
        if (!world.isRemote && stack.getItem() == this && EntityHelper.isValidPlayer(entity) && !getEntityType(stack).isEmpty() && getImpregnatedTime(world, stack) == 1) {
            NBTStackHelper.removeKeyName(stack, IMPREGNATED_TIME_NBT_LONG);
            NBTStackHelper.removeKeyName(stack, ENTITY_TYPE_NBT_STRING);
        }
    }

    private int getImpregnatedTime(@Nullable World world, ItemStack stack) {
        if (world != null && stack.getItem() == this) {
            long impregnatedTime = NBTStackHelper.getLong(stack, IMPREGNATED_TIME_NBT_LONG, 0L);
            if (impregnatedTime > 0) {
                int cd = (int) (impregnatedTime - TimeHelper.worldTicks(world));
                if (cd > IMPREGNATED_MAX_TIME) { // invalid cooldown
                    NBTStackHelper.setLong(stack, IMPREGNATED_TIME_NBT_LONG, TimeHelper.worldTicks(world) + IMPREGNATED_MAX_TIME);
                    return IMPREGNATED_MAX_TIME;
                }
                return Math.max(cd, 0);
            }
        }
        return 0;
    }

    private static final int IMPREGNATED_MAX_TIME = TimeHelper.tickFromMinute(30);
    private static final String IMPREGNATED_TIME_NBT_LONG = "impregnated_time";
}

package ovh.corail.tombstone.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.helper.CallbackHandler;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.Location;
import ovh.corail.tombstone.helper.NBTStackHelper;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.helper.TimeHelper;
import ovh.corail.tombstone.registry.ModTriggers;

import javax.annotation.Nullable;
import java.util.List;

public class ItemTabletOfRecall extends ItemTablet {

    public ItemTabletOfRecall() {
        super("tablet_of_recall", SharedConfigTombstone.allowed_magic_items.allowTabletOfRecall::get);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        if (Helper.canShowTooltip(world, stack)) {
            addItemDesc(list);
            boolean isAncient = isAncient(stack);
            if (isAncient) {
                addItemDesc(list, "_ancient");
            }
            if (!isEnchanted(stack)) {
                addItemUse(list, "1");
            } else {
                Location location = getTombPos(stack);
                if (!location.isOrigin()) {
                    addItemPosition(list, location);
                    addItemUse(list, "2");
                    addItemUse(list, "3");
                }
            }
            if (isAncient) {
                addInfoInBeta(list);
            }
        }
        super.addInformation(stack, world, list, flag);
    }

    private boolean setTombPos(ItemStack stack, Location location) {
        if (stack.getItem() != this || location.isOrigin()) {
            return false;
        }
        NBTStackHelper.setLocation(stack, "tombPos", location);
        return true;
    }

    public Location getTombPos(ItemStack stack) {
        if (stack.getItem() != this) {
            return Location.ORIGIN;
        }
        return NBTStackHelper.getLocation(stack, "tombPos");
    }

    @Override
    public boolean isEnchanted(ItemStack stack) {
        return !getTombPos(stack).isOrigin();
    }

    @Override
    public boolean setEnchant(World world, BlockPos gravePos, PlayerEntity player, ItemStack stack) {
        boolean valid = setTombPos(stack, new Location(gravePos, world));
        if (valid) {
            setUseCount(stack, getUseMax());
        }
        return valid;
    }

    @Override
    public ITextComponent getEnchantSuccessMessage(PlayerEntity player) {
        return LangKey.MESSAGE_ITEM_BOUND_TO_PLACE.getText();
    }

    @Override
    public void onSneakGrave(World world, BlockPos gravePos, PlayerEntity player, ItemStack stack) {
        if (!world.isRemote && isEnchanted(stack) && TimeHelper.atInterval(player.ticksExisted, 20)) {
            setTombPos(stack, new Location(gravePos, world));
            player.getCooldownTracker().setCooldown(this, 100);
            player.sendMessage(getEnchantSuccessMessage(player).copyRaw().setStyle(StyleType.MESSAGE_NORMAL), Util.DUMMY_UUID);
        }
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        if (!player.isSneaking()) {
            return super.onItemRightClick(world, player, hand);
        }
        return new ActionResult<>(ActionResultType.PASS, player.getHeldItem(hand));
    }

    @Override
    protected boolean doEffects(World world, ServerPlayerEntity player, ItemStack stack) {
        Location location = getTombPos(stack);
        boolean isSameDim = location.isSameDimension(world);
        if (isSameDim && location.isInRange(player.getPosition(), 10)) {
            LangKey.MESSAGE_TELEPORT_TOO_CLOSE_FROM_GRAVE.sendMessage(player);
            return false;
        }
        if (!isSameDim && !ConfigTombstone.general.teleportDim.get()) {
            LangKey.MESSAGE_TELEPORT_SAME_DIMENSION.sendMessage(player);
            return false;
        }
        CallbackHandler.addCallback(1, () -> {
            boolean isAncient = isAncient(stack);
            AxisAlignedBB area = isAncient ? player.getBoundingBox().grow(3d, 0d, 3d) : null;
            ServerPlayerEntity newPlayer = Helper.teleportEntity(player, new Location(location.getPos().up(), location.dim));
            if (isAncient) {
                List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(player, area);
                entities.forEach(entity -> Helper.teleportEntity(entity, new Location(newPlayer)));
            }
            LangKey.MESSAGE_TELEPORT_SUCCESS.sendMessage(newPlayer);
            ModTriggers.USE_RECALL.trigger(player);
        });
        return true;
    }
}

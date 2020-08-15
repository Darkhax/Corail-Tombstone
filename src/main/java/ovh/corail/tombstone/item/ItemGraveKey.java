package ovh.corail.tombstone.item;

import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.items.CapabilityItemHandler;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.helper.CallbackHandler;
import ovh.corail.tombstone.helper.EffectHelper;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.Location;
import ovh.corail.tombstone.helper.NBTStackHelper;
import ovh.corail.tombstone.helper.TimeHelper;
import ovh.corail.tombstone.registry.ModEffects;
import ovh.corail.tombstone.registry.ModItems;
import ovh.corail.tombstone.registry.ModPerks;
import ovh.corail.tombstone.registry.ModTags;
import ovh.corail.tombstone.registry.ModTriggers;
import ovh.corail.tombstone.tileentity.TileEntityGrave;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.IntStream;

public class ItemGraveKey extends ItemGraveMagic {

    public ItemGraveKey() {
        super("grave_key", getBuilder().group(null), SharedConfigTombstone.allowed_magic_items.allowGraveKey::get);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        if (Helper.canShowTooltip(world, stack)) {
            String id = isEnchanted(stack) ? "2" : "1";
            addItemDesc(list, id);
            Location location = getTombPos(stack);
            if (!location.isOrigin()) {
                addItemPosition(list, location);
            }
            addItemUse(list, id);
        }
        super.addInformation(stack, world, list, flag);
    }

    public boolean setTombPos(ItemStack stack, Location location) {
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

    /**
     * remove the key if the grave doesn't exist anymore
     */
    @Override
    @SuppressWarnings("deprecation")
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slotId, boolean isSelected) {
        if (!world.isRemote && EntityHelper.isValidPlayer(entity) && stack.getItem() == this) {
            Location location = getTombPos(stack);
            boolean isBadKey = location.isOrigin();
            if (!isBadKey && TimeHelper.atInterval(entity.ticksExisted, TimeHelper.tickFromSecond(isSelected && entity.getDistanceSq(location.getPos().getX(), location.getPos().getY(), location.getPos().getZ()) < 100 ? 10 : 120))) {
                if (!location.isSameDimension(world)) {
                    return;
                }
                if (!World.isOutsideBuildHeight(location.getPos())) {
                    if (!world.isBlockLoaded(location.getPos())) {
                        world.getBlockState(location.getPos());
                    }
                    isBadKey = !(world.getTileEntity(location.getPos()) instanceof TileEntityGrave);
                } else {
                    isBadKey = true;
                }
            }
            if (isBadKey) {
                stack.shrink(1);
                ((PlayerEntity) entity).container.detectAndSendChanges();
            }
        }
    }

    public ItemStack findFirstKeyInInventory(PlayerEntity player) {
        return player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).map(itemHandler -> IntStream.range(0, itemHandler.getSlots()).mapToObj(itemHandler::getStackInSlot).filter(stack -> stack.getItem() == ModItems.grave_key).findFirst().orElse(ItemStack.EMPTY)).orElse(ItemStack.EMPTY);
    }

    public boolean removeKeyForGraveInInventory(PlayerEntity player, Location graveLoc) {
        return player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).map(inventPlayer -> {
            for (int i = 0; i < inventPlayer.getSlots(); i++) {
                ItemStack stack = inventPlayer.getStackInSlot(i);
                if (stack.getItem() == this && getTombPos(stack).equals(graveLoc)) {
                    inventPlayer.extractItem(i, 1, false);
                    return true;
                }
            }
            return false;
        }).orElse(false);
    }

    public int countKeyInInventory(PlayerEntity player) {
        return (int) player.inventory.mainInventory.stream().filter(p -> p.getItem() == this).count();
    }

    @Override
    public boolean isEnchanted(ItemStack stack) {
        return stack.getItem() == this && NBTStackHelper.getBoolean(stack, ENCHANT_NBT_BOOL);
    }

    @Override
    public boolean setEnchant(World world, BlockPos gravePos, PlayerEntity player, ItemStack stack) {
        if (stack.getItem() != this) {
            return false;
        }
        NBTStackHelper.setBoolean(stack, ENCHANT_NBT_BOOL, true);
        return true;
    }

    @Override
    public ITextComponent getEnchantFailedMessage(PlayerEntity player) {
        return LangKey.MESSAGE_CANT_ENCHANT_GRAVE_KEY.getText();
    }

    @Override
    protected boolean doEffects(World world, ServerPlayerEntity player, ItemStack stack) {
        Location location = getTombPos(stack);
        if (location.isOrigin()) {
            LangKey.MESSAGE_INVALID_LOCATION.sendMessage(player);
            return false;
        }
        if (!location.isSameDimension(world) && !ConfigTombstone.general.teleportDim.get()) {
            LangKey.MESSAGE_TELEPORT_SAME_DIMENSION.sendMessage(player);
            return false;
        }
        assert player.getServer() != null;
        ServerWorld targetWorld = player.getServer().getWorld(location.dim);
        if (targetWorld == null || !Helper.isValidPos(targetWorld, location.getPos())) {
            LangKey.MESSAGE_INVALID_LOCATION.sendMessage(player);
            return false;
        }
        NBTStackHelper.setBoolean(stack, ENCHANT_NBT_BOOL, false);
        CallbackHandler.addCallback(1, () -> {
            PlayerEntity newPlayer = Helper.teleportToGrave(player, location);
            if (ConfigTombstone.general.nerfGhostlyShape.get()) {
                EffectHelper.capPotionDuration(newPlayer, ModEffects.ghostly_shape, 200);
            }
            LangKey.MESSAGE_TELEPORT_SUCCESS.sendMessage(newPlayer);
            ModTriggers.TELEPORT_TO_GRAVE.trigger(player);
        });
        return true;
    }

    @Override
    public boolean canConsumeOnUse() {
        return false;
    }

    @Override
    public int getCastingCooldown() {
        return 0;
    }

    @Override
    public int getUseMax() {
        return 1;
    }

    public void reenchantOnDeath(PlayerEntity player, ItemStack key) {
        if (key.getItem() == this) {
            int chanceEnchantedKey = SharedConfigTombstone.general.chanceEnchantedGraveKey.get();
            if (chanceEnchantedKey >= 0) {
                chanceEnchantedKey += EntityHelper.getPerkLevelWithBonus(player, ModPerks.jailer) * 20;
                if (chanceEnchantedKey >= 100 || (chanceEnchantedKey > 0 && Helper.getRandom(1, 100) <= chanceEnchantedKey)) {
                    NBTStackHelper.setBoolean(key, ENCHANT_NBT_BOOL, true);
                }
            }
        }
    }

    @Override
    protected boolean canBlockInteractFirst(BlockState state, ItemStack stack) {
        return super.canBlockInteractFirst(state, stack) || state.getBlock().isIn(ModTags.Blocks.player_graves);
    }
}

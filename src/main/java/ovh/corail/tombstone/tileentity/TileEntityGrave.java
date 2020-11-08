package ovh.corail.tombstone.tileentity;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import ovh.corail.tombstone.api.capability.ITBCapability;
import ovh.corail.tombstone.capability.TBCapabilityProvider;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.event.EventFactory;
import ovh.corail.tombstone.helper.DeathHandler;
import ovh.corail.tombstone.helper.EffectHelper;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.InventoryHelper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.Location;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.helper.TimeHelper;
import ovh.corail.tombstone.registry.ModBlocks;
import ovh.corail.tombstone.registry.ModEffects;
import ovh.corail.tombstone.registry.ModPerks;
import ovh.corail.tombstone.registry.ModTags;

public class TileEntityGrave extends TileEntityWritableGrave {
    protected final ItemStackHandler inventory = new ItemStackHandler(120);
    protected final Set<UUID> plunderers = new HashSet<>();

    @Nullable
    private UUID ownerId = null;
    private boolean needAccess = false;

    public TileEntityGrave() {
        super(ModBlocks.tile_grave);
    }

    @Override
    public boolean canShowFog() {
        return true;
    }

    public IItemHandler getInventory() {
        return this.inventory;
    }

    public boolean hasEmptyInventory() {
        return IntStream.range(0, this.inventory.getSlots()).allMatch(i -> this.inventory.getStackInSlot(i).isEmpty());
    }

    public boolean hasEmptySlots(int count) {
        int found = 0;
        for (int i = 0; i < this.inventory.getSlots() && count > found; i++) {
            if (this.inventory.getStackInSlot(i).isEmpty()) {
                found++;
            }
        }
        return count <= found;
    }

    public void giveInventory(@Nullable ServerPlayerEntity player) {
        if (this.world == null || !EntityHelper.isValidPlayerMP(player)) {
            return;
        }
        assert player != null && player.getServer() != null;
        // handle loss of items on death
        boolean hasLosses = SharedConfigTombstone.player_death.decayTime.get() > -1 && (!ConfigTombstone.player_death.lossOnDeathOnlyForAbandonedGrave.get() || isAbandoned(player)) && player.getServer().getTickCounter() > Math.min(SharedConfigTombstone.player_death.decayTime.get(), 6000) && ConfigTombstone.player_death.chanceLossOnDeath.get() > 0 && ConfigTombstone.player_death.percentLossOnDeath.get() > 0 && Helper.getRandom(1, 100) <= ConfigTombstone.player_death.chanceLossOnDeath.get();
        if (hasLosses) {
            float percentLossOnDeath = ConfigTombstone.player_death.percentLossOnDeath.get() * 0.01f;
            int countLoss = 0;
            for (int slotId = 0; slotId < this.inventory.getSlots(); slotId++) {
                ItemStack stack = this.inventory.getStackInSlot(slotId);
                if (stack.isEmpty()) {
                    continue;
                }
                // apply losses if config allow it
                if (stack.isStackable()) {
                    if (!stack.hasTag() || !ConfigTombstone.player_death.lossOnDeathOnlyForStackableItems.get()) {
                        int losses = Math.round(stack.getCount() * percentLossOnDeath);
                        if (losses > 0) {
                            this.inventory.extractItem(slotId, losses, false);
                            countLoss++;
                        }
                    }
                } else if (!ConfigTombstone.player_death.lossOnDeathOnlyForStackableItems.get() && Helper.random.nextFloat() <= percentLossOnDeath) {
                    this.inventory.setStackInSlot(slotId, ItemStack.EMPTY);
                    countLoss++;
                }
            }
            if (countLoss > 0) {

            	LangKey.MESSAGE_LOSSES_ON_DEATH.sendMessage(player, StyleType.MESSAGE_SPECIAL);
            }
        }
        EventFactory.onRestoreInventory(player, this);
        // auto-equip special slots first in reverse order
        for (int i = this.inventory.getSlots() - 1; i >= 0; i--) {
            if (InventoryHelper.autoequip(this.inventory.getStackInSlot(i), player)) {
                this.inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        // favor the tools on the hotbar when recovering a grave
        final NonNullList<ItemStack> swapItems = NonNullList.create();
        if (DeathHandler.INSTANCE.getOptionPriorizeToolOnHotbar(player.getUniqueID())) {
            player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(cap -> {
                int playerSlotId = -1;
                for (int graveSlotId = 0; graveSlotId < this.inventory.getSlots() && playerSlotId < 9; graveSlotId++) {
                    if (InventoryHelper.isTool(this.inventory.getStackInSlot(graveSlotId))) {
                        ItemStack playerStack;
                        do {
                            playerStack = cap.getStackInSlot(++playerSlotId);
                        } while (playerSlotId < 9 && InventoryHelper.isTool(playerStack));
                        if (playerStack.isEmpty()) {
                            ItemHandlerHelper.giveItemToPlayer(player, this.inventory.extractItem(graveSlotId, 1, false), playerSlotId);
                        } else {
                            swapItems.add(cap.extractItem(playerSlotId, 1, false));
                            ItemHandlerHelper.giveItemToPlayer(player, this.inventory.extractItem(graveSlotId, 1, false), playerSlotId);
                        }
                        this.inventory.setStackInSlot(graveSlotId, ItemStack.EMPTY);
                    }
                }
            });
        }
        // give the rest of inventory in normal order
        for (int i = 0; i < this.inventory.getSlots(); i++) {
            ItemStack stack = this.inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                ItemHandlerHelper.giveItemToPlayer(player, stack.copy());
                this.inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        // add the swap items
        if (!swapItems.isEmpty()) {
            swapItems.forEach(stack -> ItemHandlerHelper.giveItemToPlayer(player, stack));
            swapItems.clear();
        }
        removeGraveBy(player);
        EffectHelper.capPotionDuration(player, ModEffects.ghostly_shape, 100);
        player.container.detectAndSendChanges();
        LangKey.MESSAGE_OPEN_GRAVE_SUCCESS.sendMessage(player);
    }

    public void dropOnGroundAndRemove() {
        if (this.world == null) {
            return;
        }
        IntStream.range(0, inventory.getSlots()).forEach(i -> {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                net.minecraft.inventory.InventoryHelper.spawnItemStack(this.world, this.pos.getX(), this.pos.getY(), this.pos.getZ(), stack.copy());
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        });
        removeGraveBy(null);
    }

    public boolean canPlunder(ServerPlayerEntity player) {
        return EntityHelper.getPerkLevelWithBonus(player, ModPerks.tomb_raider) > 0;
    }

    public boolean isAbandoned(@Nullable ServerPlayerEntity player) {
        int knowledgeLevel = player != null ? player.getCapability(TBCapabilityProvider.TB_CAPABILITY, null).map(cap -> Math.max(cap.getTotalPerkPoints(), 10)).orElse(0) : 0;
        return TimeHelper.isSystemTimeElapsed(this.getOwnerDeathTime(), TimeUnit.MINUTES.toMillis(SharedConfigTombstone.player_death.decayTime.get() == -1 ? 1440L : (long) (SharedConfigTombstone.player_death.decayTime.get() * (1f - (knowledgeLevel * 0.05f)))));
    }

    public boolean wasPlunderedBy(ServerPlayerEntity player) {
        return this.plunderers.contains(player.getUniqueID());
    }

    public boolean plunder(ServerPlayerEntity player) {
        // check last itemstack added
        int maxId = -1;
        for (int slotId = this.inventory.getSlots() - 1; slotId >= 0; slotId--) {
            if (!this.inventory.getStackInSlot(slotId).isEmpty()) {
                maxId = slotId + 1;
                break;
            }
        }
        if (maxId == -1) {
            removeGraveBy(player);
            return false;
        }
        boolean valid = false;
        int nbTry = 3 + Helper.random.nextInt(player.getCapability(TBCapabilityProvider.TB_CAPABILITY, null).map(ITBCapability::getTotalPerkPoints).orElse(1));
        for (int i = 0; i < nbTry; i++) {
            int id = Helper.random.nextInt(maxId);
            ItemStack stack = this.inventory.getStackInSlot(id);
            if (!stack.isEmpty()) {
                valid = true;
                ItemHandlerHelper.giveItemToPlayer(player, this.inventory.extractItem(id, stack.getCount(), false));
            }
        }
        if (valid) {
            this.plunderers.add(player.getUniqueID());
            EntityHelper.addAlignment(player, ConfigTombstone.alignment.pointsPlunderPlayerGrave.get());
            if (hasEmptyInventory()) {
                removeGraveBy(player);
            } else {
                markDirty();
            }
            return true;
        }
        return false;
    }

    private void removeGraveBy(@Nullable PlayerEntity player) {
        if (this.world == null) {
            return;
        }
        DeathHandler.INSTANCE.removeGrave(new Location(this.pos, this.world));
        Helper.removeNoEvent(this.world, this.pos);
        if (player != null) {
            this.world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(), SoundEvents.BLOCK_WOODEN_DOOR_CLOSE, SoundCategory.BLOCKS, 1f, 1f);
        }
    }

    public void setOwner(Entity owner, long deathDate, boolean needAccess) {
        super.setOwner(owner, deathDate);
        this.ownerId = owner.getUniqueID();
        this.needAccess = needAccess;
    }

    public boolean isOwner(PlayerEntity owner) {
        return this.ownerId == null ? hasOwner() && this.ownerName.equals(owner.getGameProfile().getName()) : this.ownerId.equals(owner.getUniqueID());
    }

    public boolean getNeedAccess() {
        // decay time in minutes before access are no more needed
        return this.needAccess && (SharedConfigTombstone.player_death.decayTime.get() == -1 || !TimeHelper.isSystemTimeElapsed(this.deathDate, TimeUnit.MINUTES.toMillis(SharedConfigTombstone.player_death.decayTime.get())));
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        super.write(compound);
        compound.put("inventory", this.inventory.serializeNBT());
        if (!this.plunderers.isEmpty()) {
            ListNBT list = new ListNBT();
            this.plunderers.stream().filter(Objects::nonNull).forEach(uuid -> {
                CompoundNBT nbt = new CompoundNBT();
                nbt.putUniqueId("uuid", uuid);
                list.add(nbt);
            });
            compound.put("plunderers", list);
        }
        return compound;
    }

    @Override
    protected CompoundNBT writeShared(CompoundNBT compound) {
        super.writeShared(compound);
        if (this.ownerId != null) {
            compound.putUniqueId("owner_id", this.ownerId);
        }
        compound.putBoolean("need_access", this.needAccess);
        return compound;
    }

    @Override
    public void read(BlockState state, CompoundNBT compound) {
        super.read(state, compound);
        if (compound.contains("inventory", Constants.NBT.TAG_COMPOUND)) {
            this.inventory.deserializeNBT(compound.getCompound("inventory"));
        }
        if (compound.hasUniqueId("owner_id")) {
            this.ownerId = compound.getUniqueId("owner_id");
        }
        this.needAccess = compound.getBoolean("need_access");
        if (compound.contains("plunderers", Constants.NBT.TAG_LIST)) {
            this.plunderers.clear();
            ListNBT list = compound.getList("plunderers", Constants.NBT.TAG_LIST);
            IntStream.range(0, list.size()).mapToObj(list::getCompound).forEach(nbt -> this.plunderers.add(nbt.getUniqueId("uuid")));
        }
    }

    @Override
    public CompoundNBT getUpdateTag() {
        CompoundNBT nbt = super.getUpdateTag();
        if (nbt.contains("inventory", Constants.NBT.TAG_COMPOUND)) {
            nbt.remove("inventory");
        }
        return nbt;
    }

    @Override
    public void remove() {
        if (this.world != null && !this.world.isRemote) {
            if (this.world.getBlockState(this.pos).isIn(ModTags.Blocks.player_graves)) {
                return;
            }
            // when the block is already destroyed
            for (int i = 0; i < this.inventory.getSlots(); i++) {
                ItemStack stack = this.inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    net.minecraft.inventory.InventoryHelper.spawnItemStack(this.world, this.pos.getX(), this.pos.getY(), this.pos.getZ(), this.inventory.extractItem(i, stack.getCount(), false));
                }
            }
        }
        super.remove();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return LazyOptional.of(() -> this.inventory).cast();
        }
        return super.getCapability(cap, side);
    }
}

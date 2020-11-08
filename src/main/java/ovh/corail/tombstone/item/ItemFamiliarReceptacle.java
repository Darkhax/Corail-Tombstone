package ovh.corail.tombstone.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.horse.AbstractChestedHorseEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.CapabilityItemHandler;
import ovh.corail.tombstone.api.magic.ModDamages;
import ovh.corail.tombstone.capability.TBCapabilityProvider;
import ovh.corail.tombstone.command.CommandTBReviveFamiliar;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.event.EventFactory;
import ovh.corail.tombstone.helper.EffectHelper;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.NBTStackHelper;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.helper.TimeHelper;
import ovh.corail.tombstone.registry.ModTriggers;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public class ItemFamiliarReceptacle extends ItemGeneric {
    public ItemFamiliarReceptacle() {
        super("familiar_receptacle", getBuilder(true).maxStackSize(1));
    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack) {
        return super.getDisplayName(stack).copyRaw().setStyle(StyleType.MESSAGE_SPECIAL);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return containSoul(stack) || super.hasEffect(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        if (Helper.canShowTooltip(world, stack)) {
            if (!containSoul(stack)) {
                addItemDesc(list, "1");
                String id = NBTStackHelper.getString(stack, "capturable_type");
                if (!id.isEmpty()) {
                    EntityType<?> entityType = EntityType.byKey(id).orElse(null);
                    list.add(LangKey.MESSAGE_IMPREGNATE.getText(StyleType.MESSAGE_SPECIAL, (entityType == null ? LangKey.MESSAGE_UNKNOWN.getText() : entityType.getName().copyRaw()).setStyle(StyleType.TOOLTIP_ITEM)));
                }
            } else {
                assert stack.getTag() != null;
                CompoundNBT tagPet = (CompoundNBT) stack.getTag().get("dead_pet");
                if (tagPet.contains("id", Constants.NBT.TAG_STRING)) {
                    String id = tagPet.getString("id");
                    EntityType<?> entityType = EntityType.byKey(id).orElse(null);
                    addItemDesc(list, "2", (entityType == null ? LangKey.MESSAGE_UNKNOWN.getText() : entityType.getName().copyRaw()).setStyle(StyleType.TOOLTIP_ITEM));
                    addItemUse(list, getDurabilityForDisplay(stack) == 0f ? "2" : "1");
                }
            }
            ClientPlayerEntity player = Minecraft.getInstance().player;
            int level = SharedConfigTombstone.general.familiarReceptacleRequiredLevel.get();
            if (player == null || (!Helper.isContributor(player) && player.getCapability(TBCapabilityProvider.TB_CAPABILITY, null).map(cap -> cap.getTotalPerkPoints() < level).orElse(false))) {
                addWarn(list, LangKey.MESSAGE_KNOWLEDGE_REQUIRED, level);
            }
        } else {
            addInfoShowTooltip(list);
        }
        super.addInformation(stack, world, list, flag);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slotId, boolean isSelected) {
        if (!world.isRemote && stack.getItem() == this && EntityHelper.isValidPlayer(entity) && TimeHelper.atInterval(entity.ticksExisted, TimeHelper.tickFromMinute(1))) {
            if (containSoul(stack)) {
                CompoundNBT tag = stack.getTag();
                assert tag != null;
                CompoundNBT tagPet = (CompoundNBT) tag.get("dead_pet");
                float health = tagPet.contains("Health", Constants.NBT.TAG_FLOAT) ? Math.max(0, tagPet.getFloat("Health")) : 0f;
                float maxHealth = tagPet.contains("max_life", Constants.NBT.TAG_FLOAT) ? Math.max(1, tagPet.getFloat("max_life")) : 1f;
                if (health < maxHealth) {
                    float gain = maxHealth * 0.1f;
                    PlayerEntity player = (PlayerEntity) entity;
                    tagPet.putFloat("Health", Math.min(health + (player.getHealth() > gain ? gain : player.getHealth()), maxHealth));
                    if (ConfigTombstone.general.allowBeyondTheGraveDamage.get()) {
                        player.attackEntityFrom(ModDamages.BEYOND_THE_GRAVE, gain);
                    }
                }
            }
        }
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return containSoul(stack) && getDurabilityForDisplay(stack) > 0d;
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        return MathHelper.hsvToRGB(Math.max(0f, (float) (1f - getDurabilityForDisplay(stack))) / 1.5f, 1f, 1f);
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        if (tag != null && tag.contains("dead_pet", Constants.NBT.TAG_COMPOUND)) {
            CompoundNBT tagPet = (CompoundNBT) tag.get("dead_pet");
            float health = tagPet.contains("Health", Constants.NBT.TAG_FLOAT) ? Math.max(0, tagPet.getFloat("Health")) : 0f;
            float maxHealth = tagPet.contains("max_life", Constants.NBT.TAG_FLOAT) ? Math.max(1, tagPet.getFloat("max_life")) : 1f;
            return 1f - (health / maxHealth);
        }
        return 1d;
    }

    public boolean containSoul(ItemStack stack) {
        return stack.getItem() == this && stack.getTag() != null && stack.getTag().contains("dead_pet", Constants.NBT.TAG_COMPOUND);
    }

    @SuppressWarnings("deprecation")
    public boolean revive(PlayerEntity player, BlockPos gravePos, ItemStack stack) {
        if (containSoul(stack)) {
            assert stack.getTag() != null;
            CompoundNBT tagPet = (CompoundNBT) stack.getTag().get("dead_pet");
            if (tagPet.contains("id", Constants.NBT.TAG_STRING)) {
                String id = tagPet.getString("id");
                EntityType<?> entityType = EntityType.byKey(id).orElse(null);
                if (entityType != null) {
                    LivingEntity entity = (LivingEntity) entityType.create(player.world);
                    if (entity != null) {
                        tagPet.remove("max_life");
                        entity.read(tagPet);
                        entity.setHealth(entity.getMaxHealth());
                        entity.removed = false;
                        entity.setPosition(gravePos.getX() + 0.5d, gravePos.getY() + 0.5d, gravePos.getZ() + 0.5d);
                        //entity.dimension = player.dimension;
                        if (entity instanceof TameableEntity) {
                            TameableEntity pet = (TameableEntity) entity;
                            pet.setTamedBy(player);
                        } else if (entity instanceof AbstractHorseEntity) {
                            AbstractHorseEntity horse = (AbstractHorseEntity) entity;
                            horse.setTamedBy(player);
                        }
                        EffectHelper.clearBadEffects(entity);
                        player.world.addEntity(entity);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public ItemStack setCapturableType(ItemStack stack, String type) {
        NBTStackHelper.setString(stack, "capturable_type", type);
        return stack;
    }

    private boolean canCaptureSoul(ItemStack stack, LivingEntity entity) {
        String type = stack.getTag() != null ? NBTStackHelper.getString(stack, "capturable_type") : "";
        return type.isEmpty() || type.equals(entity.getType().getRegistryName().toString());
    }

    public boolean captureSoul(@Nullable LivingEntity entity) {
        if (entity == null || entity.world.isRemote) {
            return false;
        }
        boolean isTameable = entity instanceof TameableEntity;
        boolean isHorse = !isTameable && entity instanceof AbstractHorseEntity;
        UUID ownerId = null;
        if (isTameable) {
            ownerId = ((TameableEntity) entity).getOwnerId();
        }
        if (isHorse) {
            ownerId = ((AbstractHorseEntity) entity).getOwnerUniqueId();
        }
        // limited for now to tameable creatures
        if (ownerId != null && entity.getServer() != null) {
            @Nullable ServerPlayerEntity owner = entity.getServer().getPlayerList().getPlayerByUUID(ownerId);
            if (owner == null && entity.getServer().getPlayerProfileCache().getProfileByUUID(ownerId) == null) {
                return false;
            }
            String id = entity.getEntityString();
            if (id != null && !Helper.containRL(ConfigTombstone.decorative_grave.blackListCapturableSouls.get(), new ResourceLocation(id))) {
                ItemStack receptacle = owner == null ? ItemStack.EMPTY : owner.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).map(inventPlayer -> IntStream.range(0, inventPlayer.getSlots()).mapToObj(inventPlayer::getStackInSlot).filter(stack -> stack.getItem() == this && canCaptureSoul(stack, entity) && !containSoul(stack)).findFirst().orElse(ItemStack.EMPTY)).orElse(ItemStack.EMPTY);
                boolean hasReceptacle = !receptacle.isEmpty();
                if (hasReceptacle && EventFactory.onCaptureSoul(owner, entity)) {
                    hasReceptacle = false;
                }
                if (!hasReceptacle && !ConfigTombstone.recovery.recoveryFamiliarEnable.get()) {
                    return false;
                }
                if (hasReceptacle) {
                    if (entity.isPassenger()) {
                        entity.stopRiding();
                    }
                    if (entity.isBeingRidden()) {
                        entity.removePassengers();
                    }
                }
                CompoundNBT pet_tag = entity.serializeNBT();
                pet_tag.remove("Dimension");
                pet_tag.remove("Motion");
                pet_tag.remove("UUID");
                pet_tag.remove("UpdateBlocked");
                pet_tag.putFloat("Health", 0f);
                pet_tag.putFloat("max_life", entity.getMaxHealth());
                if (!hasReceptacle) {
                    // serialize when no receptacle
                    CommandTBReviveFamiliar.saveFamiliar(entity.getServer(), ownerId, pet_tag, entity.getUniqueID().toString());
                } else {
                    entity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).ifPresent(inventory -> IntStream.range(0, inventory.getSlots()).forEach(slot -> {
                        ItemStack currentStack = inventory.getStackInSlot(slot);
                        if (!currentStack.isEmpty()) {
                            inventory.extractItem(slot, currentStack.getCount(), false);
                        }
                    }));
                    if (isHorse && entity instanceof AbstractChestedHorseEntity) {
                        ((AbstractChestedHorseEntity) entity).setChested(false);
                    }
                    receptacle.getOrCreateTag().put("dead_pet", pet_tag);
                    assert owner != null;
                    LangKey.MESSAGE_CAPTURE_FAMILIAR.sendMessage(owner, StyleType.MESSAGE_SPECIAL, entity.getName());
                    ModTriggers.CAPTURE_SOUL.trigger(owner);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    @Nullable
    public CompoundNBT getShareTag(ItemStack stack) {
        if (stack.getTag() != null && stack.getTag().contains("dead_pet", Constants.NBT.TAG_COMPOUND)) {
            CompoundNBT tag = stack.getTag().copy();
            CompoundNBT pet_tag = (CompoundNBT) tag.get("dead_pet");
            if (pet_tag.contains("id", Constants.NBT.TAG_STRING)) {
                CompoundNBT newTag = new CompoundNBT();
                newTag.putString("id", pet_tag.getString("id"));
                newTag.putFloat("Health", pet_tag.getFloat("Health"));
                newTag.putFloat("max_life", pet_tag.getFloat("max_life"));
                tag.put("dead_pet", newTag);
            } else {
                pet_tag.remove("dead_pet");
            }
            return tag;
        }
        return stack.getTag();
    }
}

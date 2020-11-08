package ovh.corail.tombstone.item;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.ZombieVillagerEntity;
import net.minecraft.entity.passive.CatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.UseAction;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.CooldownTracker;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import ovh.corail.tombstone.ModTombstone;
import ovh.corail.tombstone.api.cooldown.CooldownType;
import ovh.corail.tombstone.api.magic.ISoulConsumer;
import ovh.corail.tombstone.api.magic.TBSoulConsumerProvider;
import ovh.corail.tombstone.block.BlockDecorativeGrave;
import ovh.corail.tombstone.capability.TBCapabilityProvider;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.helper.CooldownHandler;
import ovh.corail.tombstone.helper.EffectHelper;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.helper.TimeHelper;
import ovh.corail.tombstone.network.EffectMessage;
import ovh.corail.tombstone.network.PacketHandler;
import ovh.corail.tombstone.registry.ModEffects;
import ovh.corail.tombstone.registry.ModItems;
import ovh.corail.tombstone.registry.ModTriggers;

import javax.annotation.Nullable;
import java.util.List;

public class ItemAnkhOfPray extends ItemGeneric implements ISoulConsumer {

    public ItemAnkhOfPray() {
        super("ankh_of_pray", getBuilder(true).maxStackSize(1).defaultMaxDamage(30));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
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
            ClientPlayerEntity player = Minecraft.getInstance().player;
            if (player != null) {
                player.getCapability(TBCapabilityProvider.TB_CAPABILITY, null).ifPresent(cap -> {
                    if (!hasEffect(stack)) {
                        int cd = CooldownHandler.INSTANCE.getCooldown(player, CooldownType.NEXT_PRAY) / 20;
                        if (cd > 0) {
                            long hour = cd / 3600;
                            StringBuilder timeString = new StringBuilder(Helper.getFormattingCode(StyleType.TOOLTIP_DESC)).append("[");
                            if (hour > 0) {
                                cd -= hour * 3600;
                                timeString.append(String.format("%02d", hour)).append(":");
                            }
                            timeString.append(String.format("%02d", cd / 60)).append(":").append(String.format("%02d", cd % 60)).append("]");
                            addItemUse(list, "1", timeString);
                        }
                    } else {
                        addItemUse(list, "2");
                    }
                    if (CooldownHandler.INSTANCE.getCooldown(player, CooldownType.RESET_PERKS) <= 0) {
                        addItemUse(list, "3");
                    }
                });
            }
        } else {
            addInfoShowTooltip(list);
        }
        super.addInformation(stack, world, list, flag);
    }

    @Override
    public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext context) {
        BlockState state = context.getWorld().getBlockState(context.getPos());
        if (state.getBlock() instanceof BlockDecorativeGrave && state.get(BlockDecorativeGrave.HAS_SOUL)) {
            return ActionResultType.PASS;
        }
        if (context.getPlayer() != null) {
            onItemRightClick(context.getWorld(), context.getPlayer(), context.getHand());
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public ActionResultType itemInteractionForEntity(ItemStack stack, PlayerEntity playerIn, LivingEntity target, Hand hand) {
        if (EntityHelper.isValidPlayer(playerIn) && hand == Hand.MAIN_HAND && playerIn.getHeldItemMainhand().getItem() == this && !EntityHelper.hasCooldown(playerIn, this)) {
            if (target instanceof ZombieVillagerEntity) {
                ZombieVillagerEntity villager = (ZombieVillagerEntity) target;
                EntityHelper.setCooldown(playerIn, this, 10);
                int cd = CooldownHandler.INSTANCE.getCooldown(playerIn, CooldownType.NEXT_PRAY);
                if (cd <= 0) {
                    if (!playerIn.world.isRemote) {
                        ServerPlayerEntity player = (ServerPlayerEntity) playerIn;
                        EffectInstance effectInstance = new EffectInstance(ModEffects.exorcism, 200, 1);
                        EffectHelper.addEffect(villager, effectInstance);
                        PacketHandler.sendToAllTrackingPlayers(new EffectMessage(villager.getEntityId(), effectInstance), villager);
                        villager.converstionStarter = null;
                        villager.conversionTime = 200;
                        villager.getDataManager().set(ZombieVillagerEntity.CONVERTING, Boolean.TRUE);
                        villager.removePotionEffect(Effects.WEAKNESS);
                        villager.world.setEntityState(villager, (byte) 16);

                        CooldownHandler.INSTANCE.resetCooldown(player, CooldownType.NEXT_PRAY);
                        EntityHelper.addKnowledge(player, 3);
                        EntityHelper.addAlignment(player, ConfigTombstone.alignment.pointsExorcismZombieVillager.get());
                        Helper.damageItem(player.getHeldItemMainhand(), 3, player, Hand.MAIN_HAND);
                        LangKey.MESSAGE_EXORCISM.sendMessage(player, StyleType.MESSAGE_SPECIAL);
                        ModTriggers.EXORCISM.trigger(player);
                    }
                }
                return ActionResultType.SUCCESS;
            }
        }
        return ActionResultType.FAIL;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack heldStack = player.getHeldItem(hand);
        if (hand == Hand.MAIN_HAND && heldStack.getItem() == this && !player.getCooldownTracker().hasCooldown(this)) {
            return new ActionResult<>(player.getCapability(TBCapabilityProvider.TB_CAPABILITY, null).map(cap -> {
                if (CooldownHandler.INSTANCE.getCooldown(player, CooldownType.NEXT_PRAY) <= 0) {
                    ITextComponent failedMessage = null;
                    ItemStack offhand = player.getHeldItemOffhand();
                    if (ModItems.familiar_receptacle.containSoul(offhand)) {
                        if (ModItems.familiar_receptacle.getDurabilityForDisplay(offhand) != 0f) {
                            failedMessage = LangKey.MESSAGE_RECOVERING_RECEPTACLE.getText(StyleType.COLOR_OFF);
                        } else if (cap.getTotalPerkPoints() < 10) {
                            failedMessage = LangKey.MESSAGE_KNOWLEDGE_REQUIRED.getText(StyleType.COLOR_OFF, 10);
                        }
                        if (failedMessage != null) {
                            player.getCooldownTracker().setCooldown(this, 10);
                            if (!player.world.isRemote) {
                                player.sendMessage(failedMessage, Util.DUMMY_UUID);
                            }
                            return ActionResultType.FAIL;
                        }
                    }
                    player.setActiveHand(hand);
                    return ActionResultType.SUCCESS;
                }
                return ActionResultType.FAIL;
            }).orElse(ActionResultType.FAIL), heldStack);
        }
        return new ActionResult<>(ActionResultType.PASS, heldStack);
    }

    @Nullable
    private BlockPos findGraveAround(IWorld world, BlockPos startPos) {
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos currentPos = new BlockPos(startPos.getX() + x, startPos.getY() + y, startPos.getZ() + z);
                    if (world.getBlockState(currentPos).getBlock() instanceof BlockDecorativeGrave) {
                        return currentPos;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, @Nullable World world, LivingEntity entity) {
        if (EntityHelper.isValidPlayerMP(entity)) {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            if (!EntityHelper.hasCooldown(player, this)) {
                final BlockPos gravePos = findGraveAround(player.world, player.getPosition());
                if (gravePos != null) {
                    player.getCapability(TBCapabilityProvider.TB_CAPABILITY, null).ifPresent(cap -> {
                        ItemStack offhandStack = player.getHeldItemOffhand();
                        if (offhandStack.getItem() == ModItems.familiar_receptacle) {
                            if (ModItems.familiar_receptacle.containSoul(offhandStack)) {
                                if (ModItems.familiar_receptacle.getDurabilityForDisplay(offhandStack) > 0d) {
                                    LangKey.MESSAGE_RECOVERING_RECEPTACLE.sendMessage(player, StyleType.COLOR_OFF);
                                } else if (ModItems.familiar_receptacle.revive(player, gravePos, offhandStack)) {
                                    CooldownHandler.INSTANCE.resetCooldown(player, CooldownType.NEXT_PRAY);
                                    cap.addKnowledgeAndSync(player, 5);
                                    Helper.damageItem(stack, 1, player, Hand.MAIN_HAND);
                                    ModTriggers.REVIVE_FAMILIAR.trigger(player);
                                    LangKey.MESSAGE_REVIVE_FAMILIAR.sendMessage(player, LangKey.MESSAGE_YOUR_FAMILIAR.getText());
                                    offhandStack.shrink(1);
                                } else {
                                    CompoundNBT tag = offhandStack.getTag();
                                    if (tag != null) {
                                        tag.remove("dead_pet");
                                    }
                                    LangKey.MESSAGE_CANT_REVIVE_FAMILIAR.sendMessage(player, StyleType.COLOR_OFF, LangKey.MESSAGE_YOUR_FAMILIAR.getText());
                                }
                            } else {
                                LangKey.MESSAGE_EMPTY_RECEPTACLE.sendMessage(player, StyleType.COLOR_OFF);
                            }
                        } else {
                            // TODO new results
                            CooldownHandler.INSTANCE.resetCooldown(player, CooldownType.NEXT_PRAY);
                            cap.addKnowledgeAndSync(player, 1);
                            Helper.damageItem(stack, 1, player, Hand.MAIN_HAND);
                            ModTriggers.FIRST_PRAY.trigger(player);
                            if (Helper.isAprilFoolsDay()) {
                                EffectHelper.addEffect(player, Effects.BAD_OMEN, TimeHelper.tickFromDay(1));
                            } else if (EffectHelper.clearEffect(player, Effects.BAD_OMEN)) {
                                LangKey.MESSAGE_DISPEL_BAD_OMEN.sendMessage(player, StyleType.MESSAGE_SPELL);
                            }
                            int res = Helper.getRandom(1, 100);
                            if (res == 100 || (Helper.isAprilFoolsDay() && res < 50)) {
                                CatEntity cat = EntityType.CAT.create(player.world);
                                if (cat != null) {
                                    cat.setPosition(gravePos.getX() + Helper.random.nextFloat() - 0.5d, gravePos.getY() + 0.5d, gravePos.getZ() + Helper.random.nextFloat() - 0.5d);
                                    cat.setTamed(true);
                                    cat.setOwnerId(player.getUniqueID());
                                    player.world.addEntity(cat);
                                }
                            }
                            if (Helper.isDateAroundHalloween() || res < ConfigTombstone.decorative_grave.chancePrayReward.get() + cap.getTotalPerkPoints()) {
                                EffectHelper.addRandomEffect(player, SharedConfigTombstone.decorative_grave.cooldownToPray.get() * 5000, true);
                            }
                        }
                    });
                } else {
                    LangKey.MESSAGE_CANT_PRAY.sendMessage(player, StyleType.COLOR_OFF);
                }
                EntityHelper.setCooldown(player, this, 10);
            }
        }
        return stack;
    }

    @Override
    public void onUsingTick(ItemStack stack, LivingEntity entity, int timeLeft) {
        if (entity != null && entity.world.isRemote && timeLeft == getUseDuration(stack)) {
            ModTombstone.PROXY.produceParticleCasting(entity, p -> !p.isHandActive());
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player == null || EntityHelper.hasCooldown(player, stack)) {
            return false;
        }
        int cooldownToPray = CooldownHandler.INSTANCE.getCooldown(player, CooldownType.NEXT_PRAY);
        if (cooldownToPray > 0) {
            int maxTime = CooldownType.NEXT_PRAY.getMaxCooldown(player);
            int elapsedTime = maxTime - cooldownToPray;
            CooldownTracker cd = player.getCooldownTracker();
            cd.ticks -= elapsedTime;
            cd.setCooldown(this, maxTime);
            cd.ticks += elapsedTime;
            return false;
        }
        return true;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 40;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public boolean isEnchanted(ItemStack stack) {
        return false;
    }

    @Override
    public boolean setEnchant(World world, BlockPos gravePos, PlayerEntity player, ItemStack stack) {
        if (!world.isRemote) {
            return player.getCapability(TBCapabilityProvider.TB_CAPABILITY, null).map(cap -> cap.resetPerks((ServerPlayerEntity) player)).orElse(false);
        }
        return false;
    }

    @Override
    public ITextComponent getEnchantSuccessMessage(PlayerEntity player) {
        return LangKey.MESSAGE_PERK_RESET_SUCCESS.getText();
    }

    @Override
    public ITextComponent getEnchantFailedMessage(PlayerEntity player) {
        if (!player.world.isRemote) {
            return player.getCapability(TBCapabilityProvider.TB_CAPABILITY, null).map(cap -> {
                int cd = CooldownHandler.INSTANCE.getCooldown(player, CooldownType.RESET_PERKS);
                if (cd > 0) {
                    int min = cd / 1200;
                    if (min > 0) {
                        cd -= min * 1200;
                    }
                    return LangKey.MESSAGE_PERK_RESET_IN_COOLDOWN.getText(min, cd / 20);
                }
                return LangKey.MESSAGE_PERK_RESET_FAILED.getText();
            }).orElse(LangKey.MESSAGE_PERK_RESET_FAILED.getText());
        }
        return LangKey.MESSAGE_PERK_RESET_FAILED.getText();
    }

    @Override
    public int getKnowledge() {
        return 0;
    }

    @Override
    @Nullable
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundNBT nbt) {
        return new TBSoulConsumerProvider(this);
    }
}

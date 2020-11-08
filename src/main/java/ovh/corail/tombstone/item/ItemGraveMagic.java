package ovh.corail.tombstone.item;

import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.UseAction;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import ovh.corail.tombstone.ModTombstone;
import ovh.corail.tombstone.api.magic.ISoulConsumer;
import ovh.corail.tombstone.api.magic.TBSoulConsumerProvider;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.NBTStackHelper;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.helper.TimeHelper;
import ovh.corail.tombstone.registry.ModSounds;
import ovh.corail.tombstone.registry.ModTabs;
import ovh.corail.tombstone.registry.ModTags;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

public abstract class ItemGraveMagic extends ItemGeneric implements ISoulConsumer {

    ItemGraveMagic(String name, Supplier<Boolean> supplierBoolean) {
        super(name, getBuilder(), supplierBoolean);
    }

    ItemGraveMagic(String name, Properties builder, Supplier<Boolean> supplierBoolean) {
        super(name, builder, supplierBoolean);
    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack) {
        ITextComponent name = super.getDisplayName(stack);
        return (isAncient(stack) ? LangKey.MESSAGE_ANCIENT_ITEM.getText(name) : isEnchanted(stack) ? LangKey.MESSAGE_ENCHANTED_ITEM.getText(name) : name.copyRaw()).setStyle(StyleType.MESSAGE_SPECIAL);
    }

    public boolean isAncient(ItemStack stack) {
        return stack.getItem() == this && NBTStackHelper.getBoolean(stack, ANCIENT_NBT_BOOL);
    }

    protected abstract boolean doEffects(World world, ServerPlayerEntity player, ItemStack stack);

    protected ItemStack onConsumeItem(PlayerEntity player, ItemStack stack) {
        // could change this for creative player
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canEnchant(World world, BlockPos gravePos, PlayerEntity player, ItemStack stack) {
        return isEnabled();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return isEnchanted(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        if (!Helper.canShowTooltip(world, stack)) {
            addInfoShowTooltip(list);
        }
        if (isEnchanted(stack)) {
            if (canConsumeOnUse()) {
                int uses = getUseCount(stack);
                if (uses > 0) {
                    addInfo(list, LangKey.MESSAGE_USE_LEFT, uses);
                }
            }
            int cd = getCooldown(world, stack);
            if (cd > 10) {
                addWarn(list, LangKey.MESSAGE_IN_COOLDOWN, TimeHelper.getTimeString(cd));
            }
        }
        super.addInformation(stack, world, list, flag);
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);
        /* start casting */
        if (isEnchanted(stack) && !isCooldown(world, stack)) {
            player.setActiveHand(hand);
            return new ActionResult<>(ActionResultType.SUCCESS, stack);
        }
        return super.onItemRightClick(world, player, hand);
    }

    @Override
    public ActionResultType itemInteractionForEntity(ItemStack stack, PlayerEntity player, LivingEntity target, Hand hand) {
        return ActionResultType.FAIL;
    }

    @Override
    public ActionResultType onItemUseFirst(ItemStack stack, ItemUseContext context) {
        if (canBlockInteractFirst(context.getWorld().getBlockState(context.getPos()), stack)) {
            return ActionResultType.PASS;
        }
        if (context.getPlayer() != null) {
            onItemRightClick(context.getWorld(), context.getPlayer(), context.getHand());
        }
        return ActionResultType.SUCCESS;
    }

    protected boolean canBlockInteractFirst(BlockState state, ItemStack stack) {
        return !isEnchanted(stack) || state.getBlock().isIn(ModTags.Blocks.decorative_graves);
    }

    @Override
    public void onUsingTick(ItemStack stack, LivingEntity entity, int timeLeft) {
        if (entity.world.isRemote && timeLeft == getUseDuration(stack) && isEnchanted(stack)) {
            ModTombstone.PROXY.produceParticleCasting(entity, p -> !p.isHandActive());
        }
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack stack, World world, LivingEntity entity) {
        /* end casting */
        if (EntityHelper.isValidPlayerMP(entity)) {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            if (isEnchanted(stack)) {
                int useCount = getUseCount(stack);
                if (useCount < 0) {
                    return ItemStack.EMPTY;
                }
                if (doEffects(world, player, stack)) {
                    ModSounds.playSoundAllAround(ModSounds.MAGIC_USE01, SoundCategory.PLAYERS, world, player.getPosition(), 0.5f, 0.5f);
                    if (!canConsumeOnUse() || --useCount > 0) {
                        setCooldown(world, stack, getCastingCooldown());
                        setUseCount(stack, useCount);
                        return stack;
                    } else {
                        return onConsumeItem(player, stack);
                    }
                } else {
                    EntityHelper.setCooldown(player, this, 10);
                    return stack;
                }
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 40;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    /**
     * cooldown
     */
    public abstract int getCastingCooldown();

    public boolean isCooldown(World world, ItemStack stack) {
        return getCooldown(world, stack) > 0;
    }

    public int getCooldown(@Nullable World world, ItemStack stack) {
        if (world != null && stack.getItem() == this) {
            long cooldown_time = NBTStackHelper.getLong(stack, COOLDOWN_TIME_NBT_LONG, 0L);
            if (cooldown_time > 0) {
                int cd = (int) (cooldown_time - TimeHelper.worldTicks(world));
                if (cd > getCastingCooldown()) { // invalid cooldown
                    setCooldown(world, stack, getCastingCooldown());
                    return getCastingCooldown();
                }
                return Math.max(cd, 0);
            }
        }
        return 0;
    }

    public void setCooldown(@Nullable World world, ItemStack stack, int time) {
        if (world != null && stack.getItem() == this) {
            NBTStackHelper.setLong(stack, COOLDOWN_TIME_NBT_LONG, TimeHelper.worldTicks(world) + time);
        }
    }

    /**
     * use count
     */
    public abstract int getUseMax();

    public abstract boolean canConsumeOnUse();

    public int getUseCount(ItemStack stack) {
        if (stack.getItem() != this) {
            return 0;
        }
        if (getUseMax() == 1 || !canConsumeOnUse()) {
            return 1;
        }
        return Math.max(0, NBTStackHelper.getInteger(stack, USE_COUNT_NBT_INT));
    }

    protected void setUseCount(ItemStack stack, int useCount) {
        if (canConsumeOnUse()) {
            NBTStackHelper.setInteger(stack, USE_COUNT_NBT_INT, useCount);
        }
    }

    @Override
    public ITextComponent getEnchantSuccessMessage(PlayerEntity player) {
        return LangKey.MESSAGE_ENCHANT_ITEM_SUCCESS.getText();
    }

    @Override
    public ITextComponent getEnchantFailedMessage(PlayerEntity player) {
        return LangKey.MESSAGE_ENCHANT_ITEM_FAILED.getText();
    }

    @Override
    @Nullable
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundNBT nbt) {
        return new TBSoulConsumerProvider(this);
    }

    protected static Properties getBuilder() {
        return new Properties().group(ModTabs.mainTab).maxStackSize(1).defaultMaxDamage(0);
    }

    protected static final String ANCIENT_NBT_BOOL = "ancient";
    protected static final String USE_COUNT_NBT_INT = "useCount";
    protected static final String COOLDOWN_TIME_NBT_LONG = "cooldown_time";
}

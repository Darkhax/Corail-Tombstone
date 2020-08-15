package ovh.corail.tombstone.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import ovh.corail.tombstone.api.magic.ISoulConsumer;
import ovh.corail.tombstone.api.magic.ModDamages;
import ovh.corail.tombstone.api.magic.TBSoulConsumerProvider;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.helper.EffectHelper;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.NBTStackHelper;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.helper.TimeHelper;
import ovh.corail.tombstone.registry.ModEffects;
import ovh.corail.tombstone.registry.ModPerks;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class ItemVoodooPoppet extends ItemGeneric implements ISoulConsumer {
    public enum PoppetProtections {
        SUFFOCATION(LangKey.MESSAGE_PREVENT_DEATH_SUFFOCATION, p -> p == DamageSource.DROWN || p == DamageSource.IN_WALL || p == DamageSource.CRAMMING),
        BURN(LangKey.MESSAGE_PREVENT_DEATH_BURN, DamageSource::isFireDamage),
        LIGHTNING(LangKey.MESSAGE_PREVENT_DEATH_LIGHTNING, p -> p == DamageSource.LIGHTNING_BOLT),
        FALL(LangKey.MESSAGE_PREVENT_DEATH_FALL, p -> p == DamageSource.FALL || p == DamageSource.FLY_INTO_WALL),
        DEGENERATION(LangKey.MESSAGE_PREVENT_DEATH_DEGENERATION, p -> p == DamageSource.WITHER || p == DamageSource.STARVE || p == ModDamages.BEYOND_THE_GRAVE);
        private final LangKey key;
        private final Predicate<DamageSource> preventDmg;

        PoppetProtections(LangKey key, Predicate<DamageSource> preventDmg) {
            this.key = key;
            this.preventDmg = preventDmg;
        }

        public String getName() {
            return name().toLowerCase();
        }

        public LangKey getLangKey() {
            return key;
        }

        public static PoppetProtections getRandomProtection() {
            return PoppetProtections.values()[Helper.getRandom(0, PoppetProtections.values().length - 1)];
        }
    }

    public ItemVoodooPoppet() {
        super("voodoo_poppet", getBuilder(true), SharedConfigTombstone.allowed_magic_items.allowVoodooPoppet::get);
    }

    @Override
    public ITextComponent getDisplayName(ItemStack stack) {
        ITextComponent name = super.getDisplayName(stack);
        return (isEnchanted(stack) ? LangKey.MESSAGE_ENCHANTED_ITEM.getText(name) : name.copyRaw()).setStyle(StyleType.MESSAGE_SPECIAL);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return isEnchanted(stack);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        if (Helper.canShowTooltip(world, stack)) {
            addItemDesc(list);
            boolean hasProt = false;
            for (PoppetProtections prot : PoppetProtections.values()) {
                if (hasProtection(stack, prot)) {
                    list.add(new TranslationTextComponent(getTranslationKey() + "." + prot.getName()).mergeStyle(StyleType.TOOLTIP_ITEM));
                    hasProt = true;
                }
            }
            addItemUse(list, isEnchanted(stack) ? "3" : hasProt ? "2" : "1");
            if (EntityHelper.getPerkLevelWithBonus(Minecraft.getInstance().player, ModPerks.voodoo_poppet) <= 0) {
                addWarn(list, LangKey.MESSAGE_PERK_REQUIRED, ModPerks.voodoo_poppet.getTranslation().copyRaw().mergeStyle(StyleType.TOOLTIP_ITEM));
            }
        } else {
            addInfoShowTooltip(list);
        }
        super.addInformation(stack, world, list, flag);
    }

    @Override
    public boolean isEnchanted(ItemStack stack) {
        return stack.getItem() == this && NBTStackHelper.getBoolean(stack, ENCHANT_NBT_BOOL);
    }

    @Override
    public ITextComponent getEnchantSuccessMessage(PlayerEntity player) {
        return LangKey.MESSAGE_ENCHANT_ITEM_SUCCESS.getText();
    }

    @Override
    public ITextComponent getEnchantFailedMessage(PlayerEntity player) {
        return LangKey.MESSAGE_NO_PROTECTION_TO_SEAL.getText();
    }

    @Override
    public boolean setEnchant(World world, BlockPos gravePos, PlayerEntity player, ItemStack stack) {
        // TODO custom messages
        if (!isEnchanted(stack) && Arrays.stream(PoppetProtections.values()).anyMatch(p -> hasProtection(stack, p))) {
            NBTStackHelper.setBoolean(stack, ENCHANT_NBT_BOOL, true);
            return true;
        }
        return false;
    }

    public ItemStack addProtection(ItemStack stack, PoppetProtections prot) {
        if (stack.getItem() == this && !isEnchanted(stack) && !hasProtection(stack, prot)) {
            NBTStackHelper.setBoolean(stack, POPPET_PROT_PREFIX + prot.ordinal(), true);
        }
        return stack;
    }

    public boolean hasProtection(ItemStack stack, PoppetProtections prot) {
        return stack.getItem() == this && NBTStackHelper.getBoolean(stack, POPPET_PROT_PREFIX + prot.ordinal());
    }

    private boolean removeProtection(ItemStack stack, PoppetProtections prot) {
        return NBTStackHelper.removeKeyName(stack, POPPET_PROT_PREFIX + prot.ordinal());
    }

    public boolean preventDeath(PlayerEntity player, ItemStack stack, PoppetProtections prot) {
        if (canPreventDeath(stack, prot) && removeProtection(stack, prot)) {
            NBTStackHelper.setBoolean(stack, ENCHANT_NBT_BOOL, false);
            int duration = TimeHelper.tickFromSecond(ConfigTombstone.decorative_grave.durationVoodooPoppetEffects.get());
            switch (prot) {
                case SUFFOCATION:
                    EffectHelper.addEffect(player, Effects.REGENERATION, duration, 9);
                    EffectHelper.addEffect(player, Effects.WATER_BREATHING, duration);
                    break;
                case BURN:
                    player.extinguish();
                    EffectHelper.addEffect(player, Effects.REGENERATION, duration, 9);
                    EffectHelper.addEffect(player, Effects.FIRE_RESISTANCE, duration);
                    break;
                case LIGHTNING:
                    EffectHelper.addEffect(player, Effects.REGENERATION, duration, 9);
                    EffectHelper.addEffect(player, ModEffects.lightning_resistance, duration);
                    break;
                case FALL:
                    EffectHelper.addEffect(player, Effects.REGENERATION, duration, 9);
                    EffectHelper.addEffect(player, ModEffects.feather_fall, duration);
                    break;
                case DEGENERATION:
                    EffectHelper.addEffect(player, Effects.REGENERATION, duration, 9);
                    EffectHelper.addEffect(player, Effects.SATURATION, duration);
                    EffectHelper.clearBadEffects(player);
                    break;
                default:
                    break;
            }
            return true;
        }
        return false;
    }

    public boolean canPreventDeath(ItemStack stack, PoppetProtections prot) {
        return isEnchanted(stack) && NBTStackHelper.getBoolean(stack, POPPET_PROT_PREFIX + prot.ordinal());
    }

    @Nullable
    public PoppetProtections getPoppetProtections(DamageSource srcDmg) {
        return Arrays.stream(PoppetProtections.values()).filter(p -> p.preventDmg.test(srcDmg)).findFirst().orElse(null);
    }

    @Override
    @Nullable
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundNBT nbt) {
        return new TBSoulConsumerProvider(this);
    }

    private static final String POPPET_PROT_PREFIX = "poppet_prot_";
}

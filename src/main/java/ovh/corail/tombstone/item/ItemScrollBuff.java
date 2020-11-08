package ovh.corail.tombstone.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.util.StringUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.helper.EffectHelper;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.NBTStackHelper;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.registry.ModEffects;
import ovh.corail.tombstone.registry.ModPerks;
import ovh.corail.tombstone.registry.ModTriggers;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

public class ItemScrollBuff extends ItemScroll {
    public enum SpellBuff {
        PRESERVATION(ModEffects.preservation, 0, SharedConfigTombstone.allowed_magic_items.allowScrollOfPreservation::get),
        UNSTABLE_INTANGIBLENESS(ModEffects.unstable_intangibleness, 0, SharedConfigTombstone.allowed_magic_items.allowScrollOfUnstableIntangibleness::get),
        FEATHER_FALL(ModEffects.feather_fall, 2, SharedConfigTombstone.allowed_magic_items.allowScrollOfFeatherFall::get),
        PURIFICATION(ModEffects.purification, 0, SharedConfigTombstone.allowed_magic_items.allowScrollOfPurification::get),
        TRUE_SIGHT(ModEffects.true_sight, 0, SharedConfigTombstone.allowed_magic_items.allowScrollOfTrueSight::get),
        REACH(ModEffects.reach, 4, SharedConfigTombstone.allowed_magic_items.allowScrollOfReach::get),
        LIGHTNING_RESISTANCE(ModEffects.lightning_resistance, 0, SharedConfigTombstone.allowed_magic_items.allowScrollOfLightningResistance::get);
        private final Effect potion;
        private final int amplifier;
        private final Supplier<Boolean> supplierEnabled;

        SpellBuff(Effect potion, int amplifier, Supplier<Boolean> supplierEnabled) {
            this.potion = potion;
            this.amplifier = amplifier;
            this.supplierEnabled = supplierEnabled;
        }

        public String getName() {
            return name().toLowerCase();
        }

        private boolean isEnable() {
            return this.supplierEnabled.get();
        }

        public static SpellBuff getRandomBuff() {
            return values()[Helper.getRandom(0, values().length - 1)];
        }
    }

    private final SpellBuff spellBuff;

    public ItemScrollBuff(SpellBuff spellBuff) {
        super("scroll_of_" + spellBuff.getName(), spellBuff::isEnable);
        this.spellBuff = spellBuff;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        if (Helper.canShowTooltip(world, stack)) {
            addItemDesc(list);
            addItemUse(list, isEnchanted(stack) ? LangKey.ITEM_SCROLL_BUFF_USE2 : LangKey.ITEM_SCROLL_BUFF_USE1);
            ClientPlayerEntity player = Minecraft.getInstance().player;
            list.add(new StringTextComponent("(" + StringUtils.ticksToElapsedTime(MathHelper.floor(SharedConfigTombstone.general.scrollDuration.get() * (1f + EntityHelper.getPerkLevelWithBonus(player, ModPerks.scribe) / 10f))) + ")").setStyle(StyleType.MESSAGE_SPELL));
        }
        super.addInformation(stack, world, list, flag);
    }

    @Override
    public boolean isEnchanted(ItemStack stack) {
        return NBTStackHelper.getBoolean(stack, ENCHANT_NBT_BOOL);
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
    protected boolean doEffects(World world, ServerPlayerEntity player, ItemStack stack) {
        EffectHelper.addEffect(player, this.spellBuff.potion, MathHelper.floor(SharedConfigTombstone.general.scrollDuration.get() * (1f + EntityHelper.getPerkLevelWithBonus(player, ModPerks.scribe) / 10f)), this.spellBuff.amplifier);
        LangKey.MESSAGE_SPELL_CAST_ON_YOU.sendMessage(player, StyleType.MESSAGE_SPELL, new TranslationTextComponent(this.spellBuff.potion.getName()).setStyle(StyleType.MESSAGE_SPECIAL));
        ModTriggers.SPELL_BUFF.get(this.spellBuff).trigger(player);
        return true;
    }
}

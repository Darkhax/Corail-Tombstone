package ovh.corail.tombstone.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.helper.EffectHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.TimeHelper;
import ovh.corail.tombstone.registry.ModEnchantments;

import java.util.List;
import java.util.stream.Collectors;

public class EnchantmentMagicSiphon extends WeaponEnchantment {
    public EnchantmentMagicSiphon() {
        super("magic_siphon", Rarity.RARE, EnchantmentType.WEAPON, new EquipmentSlotType[] { EquipmentSlotType.MAINHAND });
    }

    @Override
    protected boolean isEnabled() {
        return SharedConfigTombstone.enchantments.enableEnchantmentMagicSiphon.get();
    }

    @Override
    protected boolean canApplyTogether(Enchantment ench) {
        return super.canApplyTogether(ench) && (SharedConfigTombstone.enchantments.allowPlagueBringerCombiningMagicSiphon.get() || ench != ModEnchantments.plague_bringer);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return SharedConfigTombstone.enchantments.magicSiphonEnchantingTable.get() && super.canApplyAtEnchantingTable(stack);
    }

    @Override
    public int getMaxLevel() {
        return SharedConfigTombstone.enchantments.maxLevelMagicSiphon.get();
    }

    @Override
    public void onProc(LivingEntity attacker, LivingEntity target, int level) {
        List<EffectInstance> effects = target.getActivePotionEffects().stream().filter(EffectHelper::isAllowedEffect).collect(Collectors.toList());
        if (!effects.isEmpty()) {
            EffectInstance effectInstance = effects.get(Helper.random.nextInt(effects.size()));
            int maxDuration = TimeHelper.tickFromMinute(ConfigTombstone.enchantments.maxDurationMagicSiphon.get());
            EffectInstance effectCopy = new EffectInstance(effectInstance);
            if (effectCopy.duration > maxDuration) {
                effectCopy.duration = maxDuration;
            }
            attacker.addPotionEffect(effectCopy);
            EffectHelper.clearEffect(target, effectInstance.getPotion());
            if (effectInstance.hiddenEffects != null) {
                target.addPotionEffect(effectInstance.hiddenEffects);
            }
            if (target.getHealth() > target.getMaxHealth()) {
                target.setHealth(target.getMaxHealth());
            }
        }
    }
}

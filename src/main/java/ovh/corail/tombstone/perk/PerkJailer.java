package ovh.corail.tombstone.perk;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.api.capability.Perk;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.StyleType;

import javax.annotation.Nullable;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

public class PerkJailer extends Perk {

    public PerkJailer() {
        super("jailer", new ResourceLocation(MOD_ID, "textures/item/grave_key.png"));
    }

    @Override
    public int getLevelMax() {
        return 5 - (SharedConfigTombstone.general.chanceEnchantedGraveKey.get() / 20);
    }

    @Override
    public boolean isDisabled(@Nullable PlayerEntity player) {
        return SharedConfigTombstone.general.chanceEnchantedGraveKey.get() == -1 || SharedConfigTombstone.general.chanceEnchantedGraveKey.get() == 100;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public String getTooltip(int level, int actualLevel, int levelWithBonus) {
        if (level == actualLevel || (actualLevel == 0 && level == 1) || level == levelWithBonus) {
            return "+" + (level * 20) + "% " + I18n.format("tombstone.perk." + name + ".desc");
        } else if (level == actualLevel + 1) {
            return "+" + (level * 20) + "%";
        }
        return "";
    }

    @Override
    public int getCost(int level) {
        return level > 0 ? 1 : 0;
    }

    @Override
    public int getLevelBonus(PlayerEntity player) {
        return Helper.isDateAroundHalloween() ? 5 : 0;
    }

    @Override
    public ITextComponent getSpecialInfo(int levelWithBonus) {
        return LangKey.TOOLTIP_ACTUAL_BONUS.getText(StyleType.MESSAGE_SPECIAL, SharedConfigTombstone.general.chanceEnchantedGraveKey.get() + levelWithBonus * 20);
    }
}

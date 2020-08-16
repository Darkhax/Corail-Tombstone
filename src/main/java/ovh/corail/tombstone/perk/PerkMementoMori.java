package ovh.corail.tombstone.perk;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.api.capability.Perk;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.StyleType;

import javax.annotation.Nullable;

public class PerkMementoMori extends Perk {

    public PerkMementoMori() {
        super("memento_mori", new ResourceLocation("minecraft", "textures/item/experience_bottle.png"));
    }

    @Override
    public int getLevelMax() {
        return SharedConfigTombstone.player_death.xpLoss.get() / 20;
    }

    @Override
    public boolean isDisabled(@Nullable PlayerEntity player) {
        return !SharedConfigTombstone.player_death.handlePlayerXp.get() || SharedConfigTombstone.player_death.xpLoss.get() < 20;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public String getTooltip(int level, int actualLevel, int levelWithBonus) {
        if (level == actualLevel || (actualLevel == 0 && level == 1) || level == levelWithBonus) {
            return "+" + (level * 20) + "% " + I18n.format("tombstone.perk.memento_mori.desc");
        } else if (level == actualLevel + 1) {
            return "+" + (level * 20) + "%";
        }
        return "";
    }

    @Override
    public int getCost(int level) {
        return level > 0 ? level : 0;
    }

    @Override
    public ITextComponent getSpecialInfo(int levelWithBonus) {
        return LangKey.TOOLTIP_ACTUAL_BONUS.getText(StyleType.MESSAGE_SPECIAL, 100 - SharedConfigTombstone.player_death.xpLoss.get() + levelWithBonus * 20);
    }
}

package ovh.corail.tombstone.perk;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
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
    public ITextComponent getTooltip(int level, int actualLevel, int levelWithBonus) {
        if (level == actualLevel || (actualLevel == 0 && level == 1) || level == levelWithBonus) {
            return new StringTextComponent("+" + (level * 20) + "% ").append(new TranslationTextComponent("tombstone.perk.memento_mori.desc"));
        } else if (level == actualLevel + 1) {
            return new StringTextComponent("+" + (level * 20) + "%");
        }
        return StringTextComponent.EMPTY;
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

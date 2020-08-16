package ovh.corail.tombstone.perk;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import ovh.corail.tombstone.api.capability.Perk;

import javax.annotation.Nullable;

public class PerkAlchemist extends Perk {

    public PerkAlchemist() {
        super("alchemist", new ResourceLocation("minecraft", "textures/item/potion.png"));
    }

    @Override
    public int getLevelMax() {
        return 10;
    }

    @Override
    public boolean isDisabled(@Nullable PlayerEntity player) {
        return false;
    }

    @Override
    public String getTooltip(int level, int actualLevel, int levelWithBonus) {
        if (level == actualLevel || (actualLevel == 0 && level == 1) || level == levelWithBonus) {
            return "+" + (level * 10) + "% " + I18n.format("tombstone.perk.alchemist.desc");
        } else if (level == actualLevel + 1) {
            return "+" + (level * 10) + "%";
        }
        return "";
    }

    @Override
    public int getCost(int level) {
        return level > 0 ? 1 : 0;
    }
}

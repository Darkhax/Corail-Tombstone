package ovh.corail.tombstone.perk;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import ovh.corail.tombstone.api.capability.Perk;
import ovh.corail.tombstone.config.SharedConfigTombstone;

import javax.annotation.Nullable;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

public class PerkTreasureSeeker extends Perk {

    public PerkTreasureSeeker() {
        super("treasure_seeker", new ResourceLocation(MOD_ID, "textures/item/lost_tablet.png"));
    }

    @Override
    public int getLevelMax() {
        return 5;
    }

    @Override
    public boolean isDisabled(@Nullable PlayerEntity player) {
        return SharedConfigTombstone.loot.chanceLootLostTablet.get() <= 0;
    }

    @Override
    public ITextComponent getTooltip(int level, int actualLevel, int levelWithBonus) {
        if (level == actualLevel || (actualLevel == 0 && level == 1) || level == levelWithBonus) {
            return new StringTextComponent("+" + (level * 10) + "% ").append(new TranslationTextComponent("tombstone.perk." + name + ".desc"));
        } else if (level == actualLevel + 1) {
            return new StringTextComponent("+" + (level * 10) + "%");
        }
        return StringTextComponent.EMPTY;
    }

    @Override
    public int getCost(int level) {
        return level > 0 ? 1 : 0;
    }
}

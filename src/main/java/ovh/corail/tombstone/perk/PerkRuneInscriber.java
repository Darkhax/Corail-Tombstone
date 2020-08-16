package ovh.corail.tombstone.perk;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.api.capability.Perk;

import javax.annotation.Nullable;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

public class PerkRuneInscriber extends Perk {

    public PerkRuneInscriber() {
        super("rune_inscriber", new ResourceLocation(MOD_ID, "textures/item/tablet.png"));
    }

    @Override
    public int getLevelMax() {
        return 5;
    }

    @Override
    public boolean isDisabled(@Nullable PlayerEntity player) {
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public String getTooltip(int level, int actualLevel, int levelWithBonus) {
        if (level == actualLevel || (actualLevel == 0 && level == 1) || level == levelWithBonus) {
            return (level * 10) + "% " + I18n.format("tombstone.perk." + name + ".desc");
        } else if (level == actualLevel + 1) {
            return (level * 10) + "%";
        }
        return "";
    }

    @Override
    public int getCost(int level) {
        return level > 0 ? 1 : 0;
    }
}

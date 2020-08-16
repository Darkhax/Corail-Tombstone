package ovh.corail.tombstone.perk;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import ovh.corail.tombstone.api.capability.Perk;
import ovh.corail.tombstone.capability.TBCapabilityProvider;
import ovh.corail.tombstone.config.SharedConfigTombstone;

import javax.annotation.Nullable;

public class PerkTombRaider extends Perk {
    public PerkTombRaider() {
        super("tomb_raider", new ResourceLocation("textures/item/wooden_shovel.png"));
    }

    @Override
    public int getLevelMax() {
        return 1;
    }

    @Override
    public boolean isDisabled(@Nullable PlayerEntity player) {
        return !SharedConfigTombstone.player_death.allowTombRaiding.get() || (player != null && player.getCapability(TBCapabilityProvider.TB_CAPABILITY, null).map(cap -> cap.getAlignmentLevel() >= 0).orElse(true));
    }

    @Override
    public ITextComponent getTooltip(int level, int actualLevel, int levelWithBonus) {
        if (level == 1) {
            return new TranslationTextComponent("tombstone.perk.tomb_raider.desc");
        }
        return StringTextComponent.EMPTY;
    }

    @Override
    public int getCost(int level) {
        return 3;
    }
}

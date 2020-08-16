package ovh.corail.tombstone.perk;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import ovh.corail.tombstone.api.capability.Perk;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;

import javax.annotation.Nullable;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

public class PerkGhostlyShape extends Perk {

    public PerkGhostlyShape() {
        super("ghostly_shape", new ResourceLocation(MOD_ID, "textures/mob_effect/ghostly_shape.png"));
    }

    @Override
    public int getLevelMax() {
        return 5;
    }

    @Override
    public boolean isDisabled(@Nullable PlayerEntity player) {
        return SharedConfigTombstone.general.ghostlyShapeDuration.get() <= 0;
    }

    @Override
    public String getTranslationKey() {
        return "effect.tombstone.ghostly_shape";
    }

    @Override
    public ITextComponent getTooltip(int level, int actualLevel, int levelWithBonus) {
        if (level == 1) {
            return new TranslationTextComponent("effect.tombstone.feather_fall");
        } else if (level == 2) {
            return LangKey.MESSAGE_BREATHING.getText();
        } else if (level == 3) {
            return new TranslationTextComponent("effect.tombstone.purification");
        } else if (level == 4) {
            return new TranslationTextComponent("effect.tombstone.true_sight");
        } else if (level == 5) {
            return LangKey.MESSAGE_INVULNERABLE.getText();
        }
        return StringTextComponent.EMPTY;
    }

    @Override
    public int getCost(int level) {
        return level > 0 ? 1 : 0;
    }

    @Override
    public boolean isEncrypted() {
        return true;
    }

    @Override
    public int getLevelBonus(PlayerEntity player) {
        return Helper.isDateAroundHalloween() ? 5 : 0;
    }
}

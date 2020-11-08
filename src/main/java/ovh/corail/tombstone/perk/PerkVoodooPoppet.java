package ovh.corail.tombstone.perk;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import ovh.corail.tombstone.api.capability.Perk;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.helper.Helper;

import javax.annotation.Nullable;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

public class PerkVoodooPoppet extends Perk {

    public PerkVoodooPoppet() {
        super("voodoo_poppet", new ResourceLocation(MOD_ID, "textures/item/voodoo_poppet.png"));
    }

    @Override
    public int getLevelMax() {
        return 5;
    }

    @Override
    public boolean isDisabled(@Nullable PlayerEntity player) {
        return !SharedConfigTombstone.allowed_magic_items.allowVoodooPoppet.get();
    }

    @Override
    public String getTranslationKey() {
        return "tombstone.item.voodoo_poppet";
    }

    @Override
    public ITextComponent getTooltip(int level, int actualLevel, int levelWithBonus) {
        if (level == 1) {
            return new TranslationTextComponent("tombstone.item.voodoo_poppet.suffocation");
        } else if (level == 2) {
            return new TranslationTextComponent("tombstone.item.voodoo_poppet.burn");
        } else if (level == 3) {
            return new TranslationTextComponent("tombstone.item.voodoo_poppet.lightning");
        } else if (level == 4) {
            return new TranslationTextComponent("tombstone.item.voodoo_poppet.fall");
        } else if (level == 5) {
            return new TranslationTextComponent("tombstone.item.voodoo_poppet.degeneration");
        }
        return StringTextComponent.EMPTY;
    }

    @Override
    public int getCost(int level) {
        return level < 1 ? 0 : 1;
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

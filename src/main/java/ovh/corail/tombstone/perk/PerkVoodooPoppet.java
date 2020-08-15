package ovh.corail.tombstone.perk;

import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
    @OnlyIn(Dist.CLIENT)
    public String getTooltip(int level, int actualLevel, int levelWithBonus) {
        if (level == 1) {
            return I18n.format("tombstone.item.voodoo_poppet.suffocation");
        } else if (level == 2) {
            return I18n.format("tombstone.item.voodoo_poppet.burn");
        } else if (level == 3) {
            return I18n.format("tombstone.item.voodoo_poppet.lightning");
        } else if (level == 4) {
            return I18n.format("tombstone.item.voodoo_poppet.fall");
        } else if (level == 5) {
            return I18n.format("tombstone.item.voodoo_poppet.degeneration");
        }
        return "";
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

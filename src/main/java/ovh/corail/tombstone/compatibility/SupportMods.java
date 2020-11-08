package ovh.corail.tombstone.compatibility;

import net.minecraft.util.IStringSerializable;
import ovh.corail.tombstone.helper.Helper;

import javax.annotation.Nonnull;

public enum SupportMods implements IStringSerializable {
    CURIOS("curios"),
    COSMETIC_ARMOR("cosmeticarmorreworked", Helper.existClass("lain.mods.cos.api.event.CosArmorDeathDrops")),
    ENCH_DESC("enchdesc"),
    WAWLA("wawla"),
    ENCHANTING_PLUS("eplus"),
    GRAVESTONE("gravestone"),
    TOP("theoneprobe"),
    MINECOLONIES("minecolonies"),
    CORPSE("corpse"),
    TETRA("tetra"),
    TOOLBELT("toolbelt");

    private final String modid;
    private final boolean loaded;

    SupportMods(String modid, boolean test) {
        this.modid = modid;
        this.loaded = Helper.isModLoad(modid) && test;
    }

    SupportMods(String modid) {
        this.modid = modid;
        this.loaded = Helper.isModLoad(modid);
    }

    public boolean isLoaded() {
        return this.loaded;
    }

    @Override
    @Nonnull
    public String getString() {
        return this.modid;
    }

    public static boolean canDisplayTooltipOnEnchant() {
        return !ENCH_DESC.isLoaded() && !WAWLA.isLoaded() && !ENCHANTING_PLUS.isLoaded();
    }
}

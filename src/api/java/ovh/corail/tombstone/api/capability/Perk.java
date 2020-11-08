package ovh.corail.tombstone.api.capability;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.registries.ForgeRegistryEntry;
import ovh.corail.tombstone.api.TombstoneAPIProps;

import javax.annotation.Nullable;

public abstract class Perk extends ForgeRegistryEntry<Perk> implements Comparable<Perk>, IStringSerializable {
    protected final String name;
    protected final ResourceLocation icon;
    protected ITextComponent translation;

    public Perk(String name, @Nullable ResourceLocation icon) {
        this.name = name;
        this.icon = icon;
    }

    public abstract int getLevelMax();

    public boolean isDisabled(@Nullable PlayerEntity player) {
        return false;
    }

    public abstract ITextComponent getTooltip(int level, int actualLevel, int levelWithBonus);

    public abstract int getCost(int level);

    public boolean isEncrypted() {
        return false;
    }

    public int getLevelBonus(PlayerEntity player) {
        return 0;
    }

    @Nullable
    public ResourceLocation getIcon() {
        return this.icon;
    }

    public String getTranslationKey() {
        return TombstoneAPIProps.OWNER + ".perk." + this.name;
    }

    public ITextComponent getTranslation() {
        if (this.translation == null) {
            this.translation = new TranslationTextComponent(getTranslationKey());
        }
        return this.translation;
    }

    public ITextComponent getSpecialInfo(int levelWithBonus) {
        return StringTextComponent.EMPTY;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public String getString() {
        return this.name;
    }

    @Override
    public boolean equals(Object object) {
        ResourceLocation registryName = getRegistryName();
        return registryName != null && object instanceof Perk && registryName.equals(((Perk) object).getRegistryName());
    }

    @Override
    public int compareTo(Perk perk) {
        ResourceLocation registryName = getRegistryName();
        ResourceLocation otherRegistryName = perk.getRegistryName();
        if (registryName != null && otherRegistryName != null) {
            return registryName.compareTo(otherRegistryName);
        }
        return registryName == otherRegistryName ? 0 : registryName == null ? -1 : 1;
    }
}

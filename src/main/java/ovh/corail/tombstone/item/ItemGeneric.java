package ovh.corail.tombstone.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.api.recipe.IDisableable;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.Location;
import ovh.corail.tombstone.helper.NBTStackHelper;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.registry.ModTabs;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

@SuppressWarnings("WeakerAccess")
public class ItemGeneric extends Item implements IDisableable {
    protected final String name;
    private boolean hasEffect = false, isCraftingIngredient = false;
    private final Supplier<Boolean> supplierEnabled;

    public ItemGeneric(String name) {
        this(name, getBuilder(true), () -> true);
    }

    public ItemGeneric(String name, boolean hasTab) {
        this(name, getBuilder(hasTab), () -> true);
    }

    public ItemGeneric(String name, Properties builder) {
        this(name, builder, () -> true);
    }

    public ItemGeneric(String name, Properties builder, Supplier<Boolean> supplierEnabled) {
        super(builder);
        this.name = name;
        this.supplierEnabled = supplierEnabled;
    }

    public String getSimpleName() {
        return this.name;
    }

    @Override
    public boolean isEnabled() {
        return this.supplierEnabled.get();
    }

    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
        if (isInGroup(group) && isEnabled()) {
            items.add(new ItemStack(this));
        }
    }

    public ItemGeneric withEffect() {
        this.hasEffect = true;
        return this;
    }

    public ItemGeneric withCraftingInfo() {
        this.isCraftingIngredient = true;
        return this;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return this.hasEffect || NBTStackHelper.getBoolean(stack, ENCHANT_NBT_BOOL);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        if (this.isCraftingIngredient) {
            addInfo(list, LangKey.MESSAGE_CRAFTING_INGREDIENT);
        }
        if (!isEnabled()) {
            addWarn(list, LangKey.MESSAGE_DISABLED);
        }
    }

    @Override
    public String getTranslationKey() {
        return MOD_ID + ".item." + name;
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return getTranslationKey();
    }

    /**
     * translation
     */
    public void addItemDesc(List<ITextComponent> list) {
        addItemDesc(list, "");
    }

    public void addItemDesc(List<ITextComponent> list, String id, Object... format) {
        list.add(new TranslationTextComponent(getTranslationKey() + ".desc" + id, format).setStyle(StyleType.TOOLTIP_DESC));
    }

    public void addItemUse(List<ITextComponent> list) {
        addItemUse(list, "");
    }

    public void addItemUse(List<ITextComponent> list, String id, Object... format) {
        list.add(new TranslationTextComponent(getTranslationKey() + ".use" + id, format).setStyle(StyleType.TOOLTIP_USE));
    }

    public void addItemUse(List<ITextComponent> list, LangKey langKey, Object... format) {
        list.add(langKey.getText(StyleType.TOOLTIP_USE, format));
    }

    public void addInfo(List<ITextComponent> list, LangKey langKey, Object... format) {
        list.add(langKey.getText(StyleType.TOOLTIP_DESC, format));
    }

    public void addWarn(List<ITextComponent> list, LangKey langKey, Object... format) {
        list.add(langKey.getText(StyleType.COLOR_OFF, format));
    }

    public void addInfoInBeta(List<ITextComponent> list) {
        list.add(LangKey.TOOLTIP_BETA.getText(StyleType.TOOLTIP_IN_BETA));
    }

    public void addInfoShowTooltip(List<ITextComponent> list) {
        addInfo(list, LangKey.TOOLTIP_MORE_INFO);
    }

    public void addItemPosition(List<ITextComponent> list, Location location) {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player != null && !location.isOrigin()) {
            BlockPos pos = player.getPosition();
            list.add(LangKey.MESSAGE_DISTANCE.getText(TextFormatting.BLUE, (int) Helper.getDistance(location.getPos(), pos), location.x, location.y, location.z, location.dim.getLocation().toString()));
        }
    }

    @SuppressWarnings("all")
    static Properties getBuilder(boolean hasTab) {
        return new Properties().group(hasTab ? ModTabs.mainTab : null).maxStackSize(64);
    }

    protected static final String ENCHANT_NBT_BOOL = "enchant";
}

package ovh.corail.tombstone.item;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.NBTStackHelper;
import ovh.corail.tombstone.helper.StyleType;

import javax.annotation.Nullable;

public interface IImpregnable {
    default String getEntityType(ItemStack stack) {
        return stack.getItem() == this ? NBTStackHelper.getString(stack, ENTITY_TYPE_NBT_STRING) : "";
    }

    default ItemStack impregnate(ItemStack stack, String entityType) {
        NBTStackHelper.setString(stack, ENTITY_TYPE_NBT_STRING, entityType);
        return stack;
    }

    default boolean impregnate(ItemStack stack, @Nullable LivingEntity entity) {
        if (entity != null && getEntityType(stack).isEmpty()) {
            NBTStackHelper.setString(stack, ENTITY_TYPE_NBT_STRING, entity.getType().getRegistryName().toString());
            return true;
        }
        return false;
    }

    @Nullable
    default ITextComponent getTooltipDisplay(ItemStack stack) {
        String id = getEntityType(stack);
        if (!id.isEmpty()) {
            EntityType<?> entityType = EntityType.byKey(id).orElse(null);
            return LangKey.MESSAGE_IMPREGNATE.getText(StyleType.MESSAGE_SPECIAL, (entityType == null ? LangKey.MESSAGE_UNKNOWN.getText() : entityType.getName().copyRaw()).setStyle(StyleType.TOOLTIP_ITEM));
        }
        return null;
    }
    String ENTITY_TYPE_NBT_STRING = "entity_type";
}

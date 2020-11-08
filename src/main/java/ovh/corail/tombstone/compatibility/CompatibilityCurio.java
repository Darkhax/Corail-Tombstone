package ovh.corail.tombstone.compatibility;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.util.LazyOptional;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.Map;
import java.util.Set;

public class CompatibilityCurio implements IEquipableCompat {
    public static final CompatibilityCurio instance = new CompatibilityCurio();

    @CapabilityInject(ICuriosItemHandler.class)
    public static final Capability<ICuriosItemHandler> INVENTORY = null;
    @CapabilityInject(ICurio.class)
    public static final Capability<ICurio> ITEM = null;

    private CompatibilityCurio() {
    }

    @Override
    public boolean autoEquip(ItemStack stack, PlayerEntity player) {
        if (INVENTORY == null || ITEM == null) {
            return false;
        }
        boolean isToolbelt = isToolbelt(stack);
        LazyOptional<ICurio> lazyOptionalItem = LazyOptional.empty();
        if (!isToolbelt && !lazyOptionalItem.isPresent()) {
            return false;
        }
        Map<String, ICurioStacksHandler> map = player.getCapability(INVENTORY, null).map(ICuriosItemHandler::getCurios).orElse(null);
        if (map == null) {
            return false;
        }
        Set<String> tags = CuriosApi.getCuriosHelper().getCurioTags(stack.getItem());
        for (String tag : tags) {
            if (isToolbelt || lazyOptionalItem.map(capItem -> capItem.canEquip(tag, player)).orElse(false)) {
                ICurioStacksHandler stackHandler = map.get(tag);
                if (stackHandler != null) {
                    IDynamicStackHandler dynamicStacks = stackHandler.getStacks();
                    for (int i = 0; i < stackHandler.getSlots(); i++) {
                        if (dynamicStacks.getStackInSlot(i).isEmpty()) {
                            dynamicStacks.setStackInSlot(i, stack.copy());
                            lazyOptionalItem.ifPresent(capItem -> capItem.playRightClickEquipSound(player));
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isToolbelt(ItemStack stack) {
        ResourceLocation registryName;
        return SupportMods.TOOLBELT.isLoaded() && (registryName = stack.getItem().getRegistryName()) != null && registryName.getNamespace().equals(SupportMods.TOOLBELT.getString()) && registryName.getPath().equals("belt");
    }
}

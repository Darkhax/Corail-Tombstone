package ovh.corail.tombstone.compatibility;

import mcp.mobius.waila.api.IComponentProvider;
import mcp.mobius.waila.api.IDataAccessor;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IRegistrar;
import mcp.mobius.waila.api.IWailaPlugin;
import mcp.mobius.waila.api.TooltipPosition;
import mcp.mobius.waila.api.WailaPlugin;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import ovh.corail.tombstone.block.BlockGrave;

import java.util.List;

@WailaPlugin
public class IntegrationHwyla implements IWailaPlugin {
    @Override
    public void register(IRegistrar registrar) {
        IComponentProvider provider = new IComponentProvider() {
            @Override
            public ItemStack getStack(IDataAccessor accessor, IPluginConfig config) {
                return ((BlockGrave) accessor.getBlock()).asDecorativeStack();
            }

            @Override
            public void appendHead(List<ITextComponent> tooltip, IDataAccessor accessor, IPluginConfig config) {
                ITextComponent translation = accessor.getBlock().getTranslatedName().mergeStyle(TextFormatting.WHITE);
                if (tooltip.isEmpty()) {
                    tooltip.add(translation);
                } else {
                    tooltip.set(0, translation);
                }
            }
        };
        registrar.registerStackProvider(provider, BlockGrave.class);
        registrar.registerComponentProvider(provider, TooltipPosition.HEAD, BlockGrave.class);
    }
}

package ovh.corail.tombstone.compatibility;

import mcjty.theoneprobe.api.ElementAlignment;
import mcjty.theoneprobe.api.IElement;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.apiimpl.ProbeInfo;
import mcjty.theoneprobe.apiimpl.styles.LayoutStyle;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import ovh.corail.tombstone.ModTombstone;
import ovh.corail.tombstone.block.BlockGrave;
import ovh.corail.tombstone.registry.ModTags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static ovh.corail.tombstone.ModTombstone.LOGGER;

public class IntegrationTOP implements Function<ITheOneProbe, Void> {
    private static final Map<Block, List<IElement>> ELEMENTS = new HashMap<>();

    private static void setElements(BlockGrave grave, ProbeInfo probeInfo) {
        List<IElement> oldElements = probeInfo.getElements();
        List<IElement> cache = ELEMENTS.get(grave);
        if (cache == null) {
            ItemStack stack = grave.asDecorativeStack();
            if (!stack.isEmpty()) {
                List<IElement> toKeep = new ArrayList<>();
                for (int i = 1; i < oldElements.size(); i++) {
                    toKeep.add(oldElements.get(i));
                }
                oldElements.clear();
                probeInfo.horizontal(new LayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT).spacing(2)).item(stack).vertical(new LayoutStyle().alignment(ElementAlignment.ALIGN_TOPLEFT).spacing(2)).itemLabel(stack).text(new StringTextComponent(ModTombstone.MOD_NAME).mergeStyle(TextFormatting.BLUE, TextFormatting.ITALIC));
                cache = probeInfo.getElements();
                cache.addAll(toKeep);
                ELEMENTS.put(grave, new ArrayList<>(cache));
            }
        } else {
            oldElements.clear();
            oldElements.addAll(cache);
        }
    }

    @Override
    public Void apply(ITheOneProbe probe) {
        LOGGER.info("Integration TOP");
        probe.registerProvider(new IProbeInfoProvider() {
            @Override
            public String getID() {
                return "tombstone:top";
            }

            @Override
            public void addProbeInfo(ProbeMode probeMode, IProbeInfo iProbeInfo, PlayerEntity playerEntity, World world, BlockState blockState, IProbeHitData iProbeHitData) {
                if (blockState.isIn(ModTags.Blocks.player_graves)) {
                    setElements((BlockGrave) blockState.getBlock(), (ProbeInfo) iProbeInfo);
                }
            }
        });
        return null;
    }
}

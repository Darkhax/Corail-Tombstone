package ovh.corail.tombstone.registry;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag.INamedTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;

public class ModTags {
    public static class Blocks {
        public static final INamedTag<Block> graves = tag("graves");
        public static final INamedTag<Block> player_graves = tag("player_graves");
        public static final INamedTag<Block> decorative_graves = tag("decorative_graves");
        public static final INamedTag<Block> grave_marbles = tag("grave_marbles");

        private static INamedTag<Block> tag(String name) {
            return BlockTags.makeWrapperTag(new ResourceLocation(MOD_ID, name).toString());
        }
    }

    public static class Items {
        public static final INamedTag<Item> VOODOO_POPPET_INGREDIENTS = tag("voodoo_poppet_ingredients");

        private static INamedTag<Item> tag(String name) {
            return ItemTags.makeWrapperTag(new ResourceLocation(MOD_ID, name).toString());
        }
    }
}

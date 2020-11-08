package ovh.corail.tombstone.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ToolType;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.StyleType;

import javax.annotation.Nullable;
import java.util.List;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

public class BlockGraveMarble extends Block {
    public final String name;
    private final MarbleType type;

    public BlockGraveMarble(MarbleType type) {
    	
        super(getBuilder());
        this.name = type.getString() + "_marble";
        this.type = type;
    }

    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable IBlockReader world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(LangKey.MESSAGE_CRAFTING_INGREDIENT.getText(StyleType.TOOLTIP_DESC));
    }

    @Override
    @Nullable
    public ToolType getHarvestTool(BlockState state) {
        return ToolType.PICKAXE;
    }

    @Override
    public int getHarvestLevel(BlockState state) {
        return 0;
    }

    @Override
    public String getTranslationKey() {
        return MOD_ID + ".block." + name;
    }

    private static Properties getBuilder() {
        return Properties.create(Material.ROCK).hardnessAndResistance(4f, 18000000f).setLightLevel(s -> 0).sound(SoundType.STONE);
    }

    public enum MarbleType implements IStringSerializable {
        DARK, WHITE;

        public static MarbleType byId(int id) {
            return id >= 0 && id < values().length ? values()[id] : DARK;
        }

        public static MarbleType getDefault() {
            return DARK;
        }

		@Override
		public String getString() {

			return name().toLowerCase();
		}
    }
}

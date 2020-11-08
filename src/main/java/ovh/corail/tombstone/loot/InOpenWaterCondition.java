package ovh.corail.tombstone.loot;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.fluid.FluidState;
import net.minecraft.loot.ILootSerializer;
import net.minecraft.loot.LootConditionType;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import ovh.corail.tombstone.ModTombstone;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.helper.SpawnHelper;

public class InOpenWaterCondition implements ILootCondition {
    private static final ResourceLocation RL = new ResourceLocation(MOD_ID, "in_open_water");
    private static final InOpenWaterCondition INSTANCE = new InOpenWaterCondition();
    public static final Serializer SERIALIZER = new Serializer();

    private InOpenWaterCondition() {
    }

    @Override
    public boolean test(LootContext context) {
        if (!ConfigTombstone.general.fishingTreasureInOpenWater.get()) {
            return true;
        }
        Entity hook = context.get(LootParameters.THIS_ENTITY);
        return hook != null && isOpenWaterAround(hook.world, hook.getPosition());
    }

    @SuppressWarnings("deprecation")
    private boolean isOpenWater(World world, BlockPos pos, boolean inStillWater) {
        BlockState state = world.getBlockState(pos);
        FluidState fluidState = state.getFluidState();
        if (inStillWater) {
            return fluidState.isTagged(FluidTags.WATER) && fluidState.isSource() && (state.getBlock() != Blocks.BUBBLE_COLUMN && state.getCollisionShape(world, pos).isEmpty());
        }
        return state.isAir(world, pos) || state.getBlock() == Blocks.LILY_PAD || state.getCollisionShape(world, pos).isEmpty();
    }

    private boolean isOpenWaterAround(World world, BlockPos pos) {
        final int inStillWater = pos.getY();
        return SpawnHelper.getAllInBox(pos.add(-2, -2, -2), pos.add(2, 1, 2)).allMatch(aPos -> isOpenWater(world, aPos, aPos.getY() <= inStillWater));
    }

    public static ILootCondition.IBuilder builder() {
        return () -> INSTANCE;
    }

    private static class Serializer implements ILootSerializer<InOpenWaterCondition> {

		@Override
		public void serialize(JsonObject p_230424_1_, InOpenWaterCondition p_230424_2_, JsonSerializationContext p_230424_3_) {
		}

		@Override
		public InOpenWaterCondition deserialize(JsonObject p_230423_1_, JsonDeserializationContext p_230423_2_) {

			return INSTANCE;
		}
    }

	@Override
	public LootConditionType func_230419_b_() {

		return ModTombstone.OPEN_WATER;
	}
}

package ovh.corail.tombstone.block;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.IBucketPickupHandler;
import net.minecraft.block.ILiquidContainer;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import ovh.corail.tombstone.tileentity.TileEntityWritableGrave;

public abstract class BlockGraveBase<T extends TileEntityWritableGrave> extends Block implements IBucketPickupHandler, ILiquidContainer {
    
	public static final DirectionProperty FACING;
    public static final BooleanProperty IS_ENGRAVED, WATERLOGGED;
    public static final IntegerProperty MODEL_TEXTURE;

    static {
        FACING = BlockStateProperties.HORIZONTAL_FACING;
        IS_ENGRAVED = BooleanProperty.create("is_engraved");
        WATERLOGGED = BlockStateProperties.WATERLOGGED;
        MODEL_TEXTURE = IntegerProperty.create("model_texture", 0, 1);
    }

    protected final GraveModel graveModel;

    public BlockGraveBase(Properties builder, GraveModel graveModel) {
        super(builder);
        this.graveModel = graveModel;
        setDefaultState(getStateContainer().getBaseState().with(FACING, Direction.NORTH).with(MODEL_TEXTURE, 0).with(IS_ENGRAVED, false).with(WATERLOGGED, false));
    }

    @Override
    public T createTileEntity(BlockState state, IBlockReader world) {
        return createTileGrave(state, world);
    }

    protected abstract T createTileGrave(BlockState state, IBlockReader world);

    @Override
    public String getTranslationKey() {
        return MOD_ID + ".grave." + graveModel.getName();
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return getDefaultState()
                .with(FACING, context.getPlacementHorizontalFacing().getOpposite())
                .with(MODEL_TEXTURE, ItemBlockGrave.getModelTexture(context.getItem()))
                .with(WATERLOGGED, (context.getWorld().getBlockState(context.getPos()).getBlock() != this && (context.getWorld().getFluidState(context.getPos()).getFluid() == Fluids.WATER)))
                .with(IS_ENGRAVED, ItemBlockGrave.isEngraved(context.getItem()));
    }

    @Nullable
    @SuppressWarnings("unchecked")
    protected T getTileEntity(World world, BlockPos pos) {
        return (T) world.getTileEntity(pos);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING, MODEL_TEXTURE, IS_ENGRAVED, WATERLOGGED);
    }

    @Override
    public BlockState updatePostPlacement(BlockState olState, Direction facing, BlockState newState, IWorld world, BlockPos oldPos, BlockPos newPos) {
        if (olState.get(WATERLOGGED)) {
            world.getPendingFluidTicks().scheduleTick(oldPos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return super.updatePostPlacement(olState, facing, newState, world, oldPos, newPos);
    }

    @Override
    public Fluid pickupFluid(IWorld world, BlockPos pos, BlockState state) {
        if (state.get(WATERLOGGED)) {
            world.setBlockState(pos, state.with(WATERLOGGED, false), 3);
            return Fluids.WATER;
        } else {
            return Fluids.EMPTY;
        }
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStillFluidState(false) : Fluids.EMPTY.getDefaultState();
    }

    @Override
    public boolean canContainFluid(IBlockReader world, BlockPos pos, BlockState state, Fluid fluid) {
        return !state.get(WATERLOGGED) && fluid == Fluids.WATER;
    }

    @Override
    public boolean receiveFluid(IWorld world, BlockPos pos, BlockState state, FluidState fluidState) {
        if (!state.get(WATERLOGGED) && fluidState.getFluid() == Fluids.WATER) {
            if (!world.isRemote()) {
                world.setBlockState(pos, state.with(WATERLOGGED, true), 3);
                world.getPendingFluidTicks().scheduleTick(pos, fluidState.getFluid(), fluidState.getFluid().getTickRate(world));
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos) {
        return true;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return graveModel.getShape(state.get(FACING));
    }

    public GraveModel getGraveType() {
        return graveModel;
    }
}

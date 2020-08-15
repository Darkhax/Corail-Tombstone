package ovh.corail.tombstone.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameters;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.common.util.LazyOptional;
import ovh.corail.tombstone.api.magic.ISoulConsumer;
import ovh.corail.tombstone.api.magic.TBSoulConsumerProvider;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.NBTStackHelper;
import ovh.corail.tombstone.helper.TimeHelper;
import ovh.corail.tombstone.registry.ModItems;
import ovh.corail.tombstone.registry.ModSounds;
import ovh.corail.tombstone.registry.ModTriggers;
import ovh.corail.tombstone.tileentity.TileEntityDecorativeGrave;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

@SuppressWarnings("deprecation")
public class BlockDecorativeGrave extends BlockGraveBase<TileEntityDecorativeGrave> {
    public static final BooleanProperty HAS_SOUL;

    static {
        HAS_SOUL = BooleanProperty.create("has_soul");
    }

    public BlockDecorativeGrave(GraveModel graveModel) {
        super(getBuilder(), graveModel);
        setDefaultState(getDefaultState().with(HAS_SOUL, false));
    }

    @Override
    protected TileEntityDecorativeGrave createTileGrave(BlockState state, IBlockReader world) {
        return new TileEntityDecorativeGrave();
    }

    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> items) {
        IntStream.rangeClosed(0, ItemBlockGrave.MAX_MODEL_TEXTURE).mapToObj(textureId -> ItemBlockGrave.createDecorativeStack(this.graveModel, textureId)).forEach(items::add);
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(HAS_SOUL);
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

    // TODO disabled
    /*@Override
    public float getBlockHardness(BlockState state, IBlockReader world, BlockPos pos) {
        return SharedConfigTombstone.decorative_grave.unbreakableDecorativeGrave.get() ? -1f : blockHardness;
    }*/

    @Override
    public boolean canEntityDestroy(BlockState state, IBlockReader world, BlockPos pos, Entity entity) {
        return !SharedConfigTombstone.decorative_grave.unbreakableDecorativeGrave.get();
    }

    @Override
    public boolean isToolEffective(BlockState state, ToolType type) {
        return type == ToolType.PICKAXE || type == ToolType.SHOVEL;
    }

    /**
     * allow to harvest the grave with a shovel or a pickaxe
     */
    @Override
    public boolean canHarvestBlock(BlockState state, IBlockReader world, BlockPos pos, PlayerEntity player) {
        ItemStack stack = player.getHeldItemMainhand();
        if (stack.isEmpty() || (!(stack.getItem() instanceof ShovelItem) && !(stack.getItem() instanceof PickaxeItem))) {
            return super.canHarvestBlock(state, world, pos, player);
        }
        return true;
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity playerIn, Hand hand, BlockRayTraceResult rayTrace) {
        if (!EntityHelper.isValidPlayer(playerIn)) {
            return ActionResultType.FAIL;
        }
        if (hand != Hand.MAIN_HAND || playerIn.getHeldItemMainhand().isEmpty()) {
            return ActionResultType.FAIL;
        }
        ItemStack stack = playerIn.getHeldItemMainhand();
        if (playerIn.world.isRemote) {
            return !EntityHelper.hasCooldown(playerIn, stack) ? ActionResultType.SUCCESS : ActionResultType.FAIL;
        }
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) playerIn;
        boolean isSoulReceptacle = stack.getItem() == ModItems.soul_receptacle;
        LazyOptional<ISoulConsumer> soulConsumerHolder = isSoulReceptacle ? LazyOptional.empty() : stack.getCapability(TBSoulConsumerProvider.CAP_SOUL_CONSUMER);
        if (!isSoulReceptacle && !soulConsumerHolder.isPresent()) {
            return super.onBlockActivated(state, world, pos, serverPlayer, hand, rayTrace);
        }
        if (!EntityHelper.hasCooldown(serverPlayer, stack)) {
            if (isSoulReceptacle) {
                EntityHelper.setCooldown(serverPlayer, stack, 10);
                if (state.get(HAS_SOUL)) {
                	LangKey.MESSAGE_FREESOUL_FAILED.sendMessage(serverPlayer);
                    return super.onBlockActivated(state, world, pos, serverPlayer, hand, rayTrace);
                } else {
                    LightningBoltEntity lightningboltentity = EntityType.LIGHTNING_BOLT.create(world);
                    lightningboltentity.moveForced(Vector3d.copyCenteredHorizontally(pos));
                    lightningboltentity.setEffectOnly(true);
                    ((ServerWorld) world).addEntity(lightningboltentity);
                    
                    world.setBlockState(pos, state.with(HAS_SOUL, true), 3);
                    LangKey.MESSAGE_FREESOUL_SUCCESS.sendMessage(serverPlayer);
                    if (!serverPlayer.isCreative()) {
                        serverPlayer.setHeldItem(Hand.MAIN_HAND, ItemStack.EMPTY);
                    }
                    EntityHelper.addKnowledge(serverPlayer, 10);
                    EntityHelper.addAlignment(serverPlayer, ConfigTombstone.alignment.pointsFreeSoulReceptacle.get());
                    return ActionResultType.SUCCESS;
                }
            }
            soulConsumerHolder.ifPresent(soulConsumer -> {
                if (soulConsumer.isEnchanted(stack)) {
                	LangKey.MESSAGE_ENCHANT_ITEM_ALREADY_ENCHANTED.sendMessage(serverPlayer);
                } else {
                    if (state.get(HAS_SOUL)) {
                        if (soulConsumer.canEnchant(world, pos, serverPlayer, stack)) {
                            if (soulConsumer.setEnchant(world, pos, serverPlayer, stack)) {
                                world.setBlockState(pos, state.with(HAS_SOUL, false), 3);
                                TileEntityDecorativeGrave tile = getTileEntity(world, pos);
                                if (tile != null) {
                                    tile.resetCheckSoul();
                                }
                                // TODO animation effect on consume soul
                                ModSounds.playSoundAllAround(ModSounds.MAGIC_USE01, SoundCategory.PLAYERS, world, serverPlayer.getPosition(), 0.5f, 0.5f);
                                serverPlayer.sendMessage(soulConsumer.getEnchantSuccessMessage(serverPlayer), Util.DUMMY_UUID);
                                ModTriggers.ACTIVATE_MAGIC_ITEM.trigger(serverPlayer);
                                EntityHelper.addKnowledge(serverPlayer, soulConsumer.getKnowledge());
                            } else {
                                serverPlayer.sendMessage(soulConsumer.getEnchantFailedMessage(serverPlayer), Util.DUMMY_UUID);
                            }
                        } else {
                        	LangKey.MESSAGE_ENCHANT_ITEM_NOT_ALLOWED.sendMessage(serverPlayer);
                        }
                    } else {
                    	LangKey.MESSAGE_ENCHANT_ITEM_NO_SOUL.sendMessage(serverPlayer);
                    }
                }
            });
            EntityHelper.setCooldown(serverPlayer, stack, 10);
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (entity.isSneaking() && !entity.removed && TimeHelper.atInterval(entity.ticksExisted, 20) && EntityHelper.isValidPlayer(entity)) {
            ItemStack stack = ((PlayerEntity) entity).getHeldItemMainhand();
            if (!stack.isEmpty()) {
                stack.getCapability(TBSoulConsumerProvider.CAP_SOUL_CONSUMER).ifPresent(soulConsumer -> {
                    soulConsumer.onSneakGrave(world, pos, (PlayerEntity) entity, stack);
                });
            }
        }
    }

    /**
     * ABOUT engraved name
     */

    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder) {
        List<ItemStack> drops = new ArrayList<>();
        String engravedName = "";
        TileEntity tile = builder.get(LootParameters.BLOCK_ENTITY);
        if (tile instanceof TileEntityDecorativeGrave) {
            engravedName = ((TileEntityDecorativeGrave) tile).getOwnerName();
        }
        drops.add(ItemBlockGrave.createDecorativeStack(this.graveModel, state.get(MODEL_TEXTURE), engravedName));
        return drops;
    }

    @Override
    public boolean removedByPlayer(BlockState state, World world, BlockPos pos, PlayerEntity player, boolean willHarvest, FluidState fluid) {
        return willHarvest || super.removedByPlayer(state, world, pos, player, willHarvest, fluid);
    }

    @Override
    public void harvestBlock(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable TileEntity te, ItemStack tool) {
        super.harvestBlock(world, player, pos, state, te, tool);
        Helper.removeNoEvent(world, pos);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!world.isRemote) {
            String engraved_name = NBTStackHelper.getString(stack, "engraved_name");
            if (!engraved_name.isEmpty()) {
                TileEntityDecorativeGrave tile = getTileEntity(world, pos);
                if (tile != null) {
                    tile.setOwner(engraved_name, new Date().getTime());
                }
            }
        }
    }

    private static Properties getBuilder() {
        return Properties.create(Material.ROCK).hardnessAndResistance(4f, 18000000f).setLightLevel(s -> 3).sound(SoundType.STONE);
    }
}

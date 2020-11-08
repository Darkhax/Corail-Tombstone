package ovh.corail.tombstone.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.registries.ForgeRegistries;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.helper.DeathHandler;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.Location;
import ovh.corail.tombstone.helper.SpawnHelper;
import ovh.corail.tombstone.helper.TimeHelper;
import ovh.corail.tombstone.registry.ModItems;
import ovh.corail.tombstone.tileentity.TileEntityGrave;

import java.util.stream.IntStream;

@SuppressWarnings("deprecation")
public class BlockGrave extends BlockGraveBase<TileEntityGrave> {

    public BlockGrave(GraveModel graveModel) {
        super(getBuilder(), graveModel);
    }

    @Override
    protected TileEntityGrave createTileGrave(BlockState state, IBlockReader world) {
        return new TileEntityGrave();
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTrace) {
        if (!world.isRemote()) {
            return activatePlayerGrave(world, pos, state, (ServerPlayerEntity) player) ? ActionResultType.SUCCESS : ActionResultType.FAIL;
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public boolean canEntityDestroy(BlockState state, IBlockReader world, BlockPos pos, Entity entity) {
        return false;
    }

    @Override
    public boolean isToolEffective(BlockState state, ToolType type) {
        return false;
    }

    private boolean activatePlayerGrave(World world, BlockPos pos, BlockState state, ServerPlayerEntity player) {
        if (world.isRemote) {
            return true;
        }
        TileEntityGrave tile = getTileEntity(world, pos);
        if (tile == null || tile.countTicks < 30 || !player.isAlive()) {
            return false;
        }
        // look for grave's key
        boolean hasKey = ModItems.grave_key.removeKeyForGraveInInventory(player, new Location(pos, world));
        // no need of access
        boolean valid = hasKey || tile.isOwner(player) || !tile.getNeedAccess();
        if (valid) {
            tile.giveInventory(player);
            if (world.getWorldInfo().getDifficulty() != Difficulty.PEACEFUL && ConfigTombstone.player_death.chanceMobOnGraveRecovery.get() > 0 && Helper.getRandom(1, 100) <= ConfigTombstone.player_death.chanceMobOnGraveRecovery.get()) {
                IntStream.range(0, Helper.getRandom(1, 3)).forEach(i -> spawnRandomMob((ServerWorld) world, pos));
            }
        } else if (tile.canPlunder(player)) {
            if (!tile.isAbandoned(player)) {
            	LangKey.MESSAGE_TOMB_RAIDER_NOT_ABANDONED.sendMessage(player);
            } else if (tile.wasPlunderedBy(player)) {
            	LangKey.MESSAGE_TOMB_RAIDER_VISITED.sendMessage(player);
            } else if (tile.plunder(player)) {
            	LangKey.MESSAGE_TOMB_RAIDER_SUCCESS.sendMessage(player);
                return true;
            } else {
            	LangKey.MESSAGE_TOMB_RAIDER_FAILED.sendMessage(player);
            }
        } else if (ModItems.grave_key.countKeyInInventory(player) > 0) {
        	LangKey.MESSAGE_OPEN_GRAVE_WRONG_KEY.sendMessage(player);
        } else {
        	LangKey.MESSAGE_OPEN_GRAVE_NEED_KEY.sendMessage(player);
        }
        return valid;
    }

    private void spawnRandomMob(ServerWorld world, BlockPos pos) {
        Location spawnPos = new SpawnHelper(world, new BlockPos(pos.getX() + Helper.getRandom(-9, 9), pos.getY(), pos.getZ() + Helper.getRandom(-9, 9))).findSafePlace(2, true);
        if (spawnPos.isOrigin()) {
            return;
        }
        final ZombieEntity mob;
        try {
            mob = EntityType.ZOMBIE.create(world);
            if (mob == null) {
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        mob.setChild(true);
        if (Helper.isDateAroundHalloween()) {
            mob.setItemStackToSlot(EquipmentSlotType.HEAD, new ItemStack(Blocks.PUMPKIN));
        }
        mob.onInitialSpawn(world, world.getDifficultyForLocation(new BlockPos(mob.getPositionVec())), SpawnReason.TRIGGERED, null, null);
        mob.setLocationAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, world.rand.nextFloat() * 360f, 0f);
        world.addEntity(mob);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!world.isRemote && entity.isSneaking() && entity.isAlive() && TimeHelper.atInterval(entity.ticksExisted, 20) && EntityHelper.isValidPlayer(entity) && DeathHandler.INSTANCE.getOptionActivateGraveBySneaking(entity.getUniqueID())) {
            activatePlayerGrave(world, pos, state, (ServerPlayerEntity) entity);
        }
    }

    @Override
    public void onBlockExploded(BlockState state, World world, BlockPos pos, Explosion explosion) {
    }

    @Override
    public boolean canDropFromExplosion(Explosion explosionIn) {
        return false;
    }

    private static Properties getBuilder() {
        return Properties.create(Material.ROCK).hardnessAndResistance(-1f, 18000000f).setLightLevel(l -> 6).sound(SoundType.STONE);
    }

    public ItemStack asDecorativeStack() {
        ResourceLocation registryName = getRegistryName();
        if (registryName != null) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(registryName.getNamespace(), "decorative_" + registryName.getPath()));
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
        }
        return ItemStack.EMPTY;
    }
}

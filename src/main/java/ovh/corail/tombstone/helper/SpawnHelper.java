package ovh.corail.tombstone.helper;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.fluid.FluidState;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.Tags;
import org.apache.commons.lang3.tuple.Pair;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.registry.ModTags;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a way to spawn safely players without using the vanilla code
 *
 * @author Corail31
 */
public class SpawnHelper {
    public enum EnumSpawnPlace {
        SAFE, GROUND, UNSAFE, WATER;

        public boolean isSafe() {
            return this == SAFE;
        }

        public boolean isGround() {
            return this == GROUND;
        }

        public boolean isWater() {
            return this == WATER;
        }
    }

    public enum EnumSpawnType {NONE, MINIMAL, NORMAL, FIT, IDEAL}

    private final World world;
    private final RegistryKey<World> dimId;
    private final int actualHeight;
    private final BlockPos initPos;
    private List<BlockPos> positions;
    private final Map<BlockPos, EnumSpawnPlace> spawnPlaces = new HashMap<>();
    private BlockPos spawnPos = null;
    private EnumSpawnType spawnType = EnumSpawnType.NONE;

    public SpawnHelper(ServerWorld world, BlockPos initPos) {
        this.world = world;
        this.dimId = world.getDimensionKey();
        this.actualHeight = world.getDimensionType().getLogicalHeight();
        this.initPos = Helper.getCloserValidPos(world, initPos);
    }

    public Location findSpawnPlace() {
        return findSpawnPlace(false);
    }

    /**
     * find a safe place (short range if withHeight)
     */
    public Location findSafePlace(int range, boolean withHeight) {
        initPositions(this.initPos.add(-range, (withHeight ? -range : 0), -range), this.initPos.add(range, (withHeight ? range : 0), range));
        for (BlockPos currentPos : this.positions) {
            if (isValidSpawnPlace(currentPos)) {
                return new Location(currentPos, this.world);
            }
        }
        return this.spawnType.ordinal() >= EnumSpawnType.NORMAL.ordinal() ? new Location(this.spawnPos, this.dimId) : Location.ORIGIN;
    }

    public Location findPlaceInStructure(ResourceLocation structureRL) {
        return findPlaceInStructure(structureRL.getNamespace(), structureRL.getPath());
    }

    public Location findPlaceInStructure(String structureRL) {
        Pair<String, String> rl = Helper.parseRLString(structureRL);
        return findPlaceInStructure(rl.getLeft(), rl.getRight());
    }

    public Location findPlaceInStructure(String domain, String path) {
        if (SupportStructures.BURIED_TREASURE.is(domain, path)) {
            // ensure to stay on the pos of the buried treasure
            this.positions = ImmutableList.of(this.initPos);
        } else {
            initChunkPositions();
        }
        int y = SupportStructures.getY(domain, path);
        int maxY = Math.min(y + 16, actualHeight);
        int minY = Math.max(y - 16, 0);
        int yUp = this.initPos.getY();
        int yDown = yUp - 1;
        boolean canGoDown = true, canGoUp = true;
        while (canGoUp || canGoDown) {
            canGoUp = yUp < maxY;
            canGoDown = yDown > minY;
            for (final BlockPos pos : this.positions) {
                /* check up */
                if (canGoUp) {
                    BlockPos currentPos = new BlockPos(pos.getX(), yUp, pos.getZ());
                    if (isValidSpawnPlace(currentPos)) {
                        return new Location(currentPos, this.dimId);
                    }
                }
                /* check down */
                if (canGoDown) {
                    BlockPos currentPos = new BlockPos(pos.getX(), yDown, pos.getZ());
                    if (isValidSpawnPlace(currentPos)) {
                        return new Location(currentPos, this.dimId);
                    }
                }
            }
            yUp++;
            yDown--;
        }
        return this.spawnType.ordinal() >= EnumSpawnType.NORMAL.ordinal() ? new Location(this.spawnPos, this.dimId) : Location.ORIGIN;
    }

    private boolean containLiquid(BlockState state) {
        return !state.getFluidState().isEmpty();
    }

    public Location findSpawnPlace(boolean isGrave) {
        if ((isGrave && (isValidGravePlace(initPos))) || isValidSpawnPlace(initPos)) {
            return new Location(this.initPos, dimId);
        }
        initChunkPositions();
        int minY, maxY;
        BlockPos adjustedPos = this.initPos;
        /* check if dead in liquid (also check the block above to avoid others graves or bags) */
        boolean wasInLiquid = !ConfigTombstone.player_death.allowGraveInWater.get() && (containLiquid(this.world.getBlockState(adjustedPos)) || (World.isValid(adjustedPos.up()) && containLiquid(this.world.getBlockState(adjustedPos.up()))));
        /* move up until not in liquid */
        if (wasInLiquid) {
            adjustedPos = adjustedPos.up();
            while (containLiquid(this.world.getBlockState(adjustedPos))) {
                adjustedPos = adjustedPos.up();
                /* security for the loop */
                if (!World.isValid(adjustedPos) || (adjustedPos.getY() - this.initPos.getY()) > 300) { /* 300 for cubicChunk */
                    adjustedPos = this.initPos;
                    wasInLiquid = false;
                    break;
                }
            }
        }
        if (!isGrave) {
            maxY = Math.min(adjustedPos.getY() + 70, actualHeight);
            minY = Math.max(adjustedPos.getY() - 70, 0);
        } else {
            maxY = actualHeight;
            minY = 0;
        }
        int yUp = adjustedPos.getY();
        int yDown = yUp - 1;
        boolean canGoDown = true, canGoUp = true;
        while (canGoUp || canGoDown) {
            canGoUp = (yUp < maxY);
            canGoDown = (!wasInLiquid && yDown > minY);
            for (final BlockPos pos : this.positions) {
                /* check up */
                if (canGoUp) {
                    BlockPos currentPos = new BlockPos(pos.getX(), yUp, pos.getZ());
                    boolean valid = !isGrave ? isValidSpawnPlace(currentPos) : isValidGravePlace(currentPos);
                    if (valid) {
                        return new Location(currentPos, this.dimId);
                    }
                }
                /* check down */
                if (canGoDown) {
                    BlockPos currentPos = new BlockPos(pos.getX(), yDown, pos.getZ());
                    boolean valid = !isGrave ? isValidSpawnPlace(currentPos) : isValidGravePlace(currentPos);
                    if (valid) {
                        return new Location(currentPos, this.dimId);
                    }
                }
            }
            yUp++;
            yDown--;
        }
        /* return the place if convenient */
        boolean valid = false;
        if (isGrave) {
            if (this.spawnType != EnumSpawnType.NONE) {
                valid = true;
            }
        } else {
            if (this.spawnType.ordinal() >= EnumSpawnType.NORMAL.ordinal()) {
                valid = true;
            }
        }
        return valid ? new Location(this.spawnPos, this.world) : Location.ORIGIN;
    }

    private boolean isValidSpawnPlace(BlockPos pos) {
        if (isSafeGround(pos.down()) && isSafePlace(pos) && isSafePlace(pos.up())) {
            setTypeAndPosition(EnumSpawnType.IDEAL, pos);
            return true;
        }
        return false;
    }

    private boolean isValidGravePlace(BlockPos pos) {
        if (isGravePlace(pos)) {
            setTypeAndPosition(EnumSpawnType.MINIMAL, pos);
            if (isSafeGround(pos.down())) {
                setTypeAndPosition(EnumSpawnType.NORMAL, pos);
                if (isSafePlace(pos.up()) && isSafePlace(pos.up(2))) {
                    setTypeAndPosition(EnumSpawnType.IDEAL, pos);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isGravePlace(BlockPos pos) {
        return isSafePlace(pos) && this.world.getTileEntity(pos) == null;
    }

    private boolean isSafeGround(BlockPos pos) {
        EnumSpawnPlace placeType = getPlaceType(pos);
        return placeType.isGround() || (ConfigTombstone.player_death.allowGraveInWater.get() && placeType.isWater());
    }

    private boolean isSafePlace(BlockPos pos) {
        EnumSpawnPlace placeType = getPlaceType(pos);
        return placeType.isSafe() || (ConfigTombstone.player_death.allowGraveInWater.get() && placeType.isWater());
    }

    private void setTypeAndPosition(EnumSpawnType type, BlockPos pos) {
        if (type.ordinal() > this.spawnType.ordinal()) {
            this.spawnType = type;
            this.spawnPos = pos;
        }
    }

    private EnumSpawnPlace getPlaceType(BlockPos pos) {
        return this.spawnPlaces.computeIfAbsent(pos, p -> getPathNodeTypeRaw(this.world, p));
    }

    private void initChunkPositions() {
        /* sort all blockpos at Y=0 of the current chunk */
        ChunkPos chunkPos = new ChunkPos(this.initPos);
        initPositions(new BlockPos(chunkPos.getXStart(), 0, chunkPos.getZStart()), new BlockPos(chunkPos.getXEnd(), 0, chunkPos.getZEnd()));
    }

    private void initPositions(BlockPos startPos, BlockPos endPos) {
        this.spawnPos = null;
        this.spawnType = EnumSpawnType.NONE;
        this.spawnPlaces.clear();
        this.positions = getAllInBox(startPos, endPos).sorted(Comparator.comparingDouble(pos -> Helper.getDistanceSq(pos, initPos.getX(), 0, initPos.getZ()))).collect(Collectors.toList());
    }

    public static Stream<BlockPos> getAllInBox(BlockPos startPos, BlockPos endPos) {
        int minX = Math.min(startPos.getX(), endPos.getX());
        int maxX = Math.max(startPos.getX(), endPos.getX());
        int minY = Math.min(startPos.getY(), endPos.getY());
        int maxY = Math.max(startPos.getY(), endPos.getY());
        int minZ = Math.min(startPos.getZ(), endPos.getZ());
        int maxZ = Math.max(startPos.getZ(), endPos.getZ());
        Stream.Builder<BlockPos> list = Stream.builder();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    list.accept(new BlockPos(x, y, z));
                }
            }
        }
        return list.build();
    }

    @SuppressWarnings("deprecation")
    private static <T extends World> EnumSpawnPlace getPathNodeTypeRaw(T world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (!Helper.isValidPos(world, pos) || !world.isBlockLoaded(pos)) {
            return EnumSpawnPlace.UNSAFE;
        }

        PathNodeType type = block.getAiPathNodeType(state, world, pos, null);
        if (type != null) {
            if (type == PathNodeType.OPEN || type == PathNodeType.BREACH) {
                return EnumSpawnPlace.SAFE;
            } else if (type == PathNodeType.BLOCKED || type == PathNodeType.WALKABLE || type == PathNodeType.LEAVES) {
                return EnumSpawnPlace.GROUND;
            }
            return EnumSpawnPlace.UNSAFE;
        }

        Material mat = state.getMaterial();
        if (state.isAir(world, pos)) {
            return EnumSpawnPlace.SAFE;
        }
        if (mat == Material.LAVA || mat == Material.FIRE || mat == Material.PORTAL || mat == Material.CACTUS) {
            return EnumSpawnPlace.UNSAFE;
        }

        FluidState fluidState = state.getFluidState();
        if (!fluidState.isEmpty() && fluidState.getLevel() >= 8) {
            return fluidState.isTagged(FluidTags.WATER) && block != Blocks.BUBBLE_COLUMN && state.getCollisionShape(world, pos).isEmpty() ? EnumSpawnPlace.WATER : EnumSpawnPlace.UNSAFE;
        }

        if (block == Blocks.SWEET_BERRY_BUSH || block == Blocks.MAGMA_BLOCK) {
            return EnumSpawnPlace.UNSAFE;
        }
        if (state.getBlock() == Blocks.LILY_PAD) {
            return EnumSpawnPlace.SAFE;
        }

        VoxelShape collisionShape = state.getCollisionShape(world, pos);
        if (collisionShape.isEmpty()) {
            return EnumSpawnPlace.SAFE;
        }

        // TODO
        if (block.isIn(ModTags.Blocks.graves) || block == Blocks.GLOWSTONE || block == Blocks.FROSTED_ICE || block == Blocks.GRASS_PATH || block == Blocks.FARMLAND || block.isIn(BlockTags.SLABS) || block.isIn(BlockTags.STAIRS) || block.isIn(BlockTags.WALLS) || block.isIn(BlockTags.CARPETS) || block.isIn(BlockTags.BEDS) || block.isIn(BlockTags.LEAVES)) {
            return EnumSpawnPlace.GROUND;
        } else if (block.isIn(BlockTags.TRAPDOORS) || block.isIn(BlockTags.DOORS) || block.isIn(BlockTags.FENCES) || block.isIn(Tags.Blocks.FENCE_GATES)) {
            return EnumSpawnPlace.UNSAFE;
        } else {
            return !mat.blocksMovement() ? EnumSpawnPlace.SAFE : (state.isNormalCube(world, pos) ? EnumSpawnPlace.GROUND : EnumSpawnPlace.UNSAFE);
        }
    }
}

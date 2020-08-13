package ovh.corail.tombstone.helper;

import com.google.common.base.MoreObjects;
import net.minecraft.entity.Entity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class Location {
    public int x, y, z;
    public RegistryKey<World> dim;
    static final BlockPos ORIGIN_POS = new BlockPos(0, Integer.MIN_VALUE, 0);
    public static final Location ORIGIN = new Location(ORIGIN_POS, Integer.MIN_VALUE);

    public Location(BlockPos pos, int dim) {
        this(pos.getX(), pos.getY(), pos.getZ(), dim);
    }

    public Location(BlockPos pos, IWorld world) {
        this(pos.getX(), pos.getY(), pos.getZ(), world);
    }

    public Location(int x, int y, int z, IWorld world) {
        this(x, y, z, Helper.getDimensionId(world));
    }

    public Location(int x, int y, int z, RegistryKey<World> dim) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dim = dim;
    }

    public Location(Entity entity) {
        this(entity.getPosition(), entity.world);
    }

    public BlockPos getPos() {
        return new BlockPos(x, y, z);
    }

    @Nullable
    public DimensionType getDimensionType() {
        return DimensionType.getById(dim);
    }

    public boolean equals(Location pos) {
        return pos.x == x && pos.y == y && pos.z == z && pos.dim == dim;
    }

    public boolean isOrigin() {
        return this.equals(ORIGIN);
    }

    public boolean isSameDimension(World world) {
        return this.dim == Helper.getDimensionId(world);
    }

    public double getDistanceSq(Location location) {
        return getDistanceSq(location.getPos());
    }

    public double getDistanceSq(BlockPos pos) {
        double d0 = this.x - pos.getX();
        double d1 = this.y - pos.getY();
        double d2 = this.z - pos.getZ();
        return d0 * d0 + d1 * d1 + d2 * d2;
    }

    public boolean isInRangeAndDimension(Location loc, int range) {
        return isInRange(loc, range) && this.dim == loc.dim;
    }

    public boolean isInRange(int x, int y, int z, int range) {
        return isInRange(new Location(x, y, z, this.dim), range);
    }

    public boolean isInRange(BlockPos position, int range) {
        return getDistanceSq(position) <= range * range;
    }

    public boolean isInRange(Location location, int range) {
        return getDistanceSq(location) <= range * range;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("x", x).add("y", y).add("z", z).add("dim", dim).toString();
    }
}

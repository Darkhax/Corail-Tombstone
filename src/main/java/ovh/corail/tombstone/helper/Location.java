package ovh.corail.tombstone.helper;

import com.google.common.base.MoreObjects;
import net.minecraft.entity.Entity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Location {
    public int x, y, z;
    public RegistryKey<World> dim;
    static final BlockPos ORIGIN_POS = new BlockPos(0, Integer.MIN_VALUE, 0);
    public static final Location ORIGIN = new Location(ORIGIN_POS, World.OVERWORLD);

    public Location(BlockPos pos, RegistryKey<World> dim) {
        this(pos.getX(), pos.getY(), pos.getZ(), dim);
    }

    public Location(BlockPos pos, World world) {
        this(pos.getX(), pos.getY(), pos.getZ(), world);
    }

    public Location(int x, int y, int z, World world) {
        this(x, y, z, world.getDimensionKey());
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

    public boolean equals(Location location) {
        return location.x == x && location.y == y && location.z == z && location.dim.equals(dim);
    }

    public boolean isOrigin() {
        return this.equals(ORIGIN);
    }

    public boolean isSameDimension(World world) {
        return this.dim.equals(world.getDimensionKey());
    }

    public boolean isSameDimension(String worldRL) {
        return this.dim.getLocation().toString().equals(worldRL);
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
        return isInRange(loc, range) && this.dim.equals(loc.dim);
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

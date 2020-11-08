package ovh.corail.tombstone.block;

import net.minecraft.util.Direction;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import ovh.corail.tombstone.helper.Helper;

import java.util.HashMap;
import java.util.Map;

public enum GraveModel implements IStringSerializable {
    GRAVE_SIMPLE("grave_simple"),
    GRAVE_NORMAL("grave_normal"),
    GRAVE_CROSS("grave_cross"),
    TOMBSTONE("tombstone"),
    SUBARAKI_GRAVE("subaraki_grave", true),
    GRAVE_ORIGINAL("grave_original");

    private final String name;
    private final boolean onlyContributor;
    private final Map<Direction, VoxelShape> collisions = new HashMap<>();
    private static final VoxelShape ground_bounds = VoxelShapes.create(0d, 0d, 0d, 1d, 0.0625d, 1d);
    private static final VoxelShape subaraki_ground_bounds = VoxelShapes.or(
            VoxelShapes.create(0.0625d, 0d, 0.0625d, 0.9375d, 0.0625d, 0.9375d),
            VoxelShapes.create(0.125d, 0.0625d, 0.125d, 0.875d, 0.125d, 0.875d),
            VoxelShapes.create(0.25d, 0.125d, 0.25d, 0.75d, 0.375d, 0.75d),
            VoxelShapes.create(0.1875d, 0.375d, 0.1875d, 0.8125d, 0.4375d, 0.8125d),
            VoxelShapes.create(0d, 0d, 0d, 0.125d, 0.1875d, 0.125d),
            VoxelShapes.create(0.875d, 0d, 0.875d, 1d, 0.1875d, 1d),
            VoxelShapes.create(0d, 0d, 0.875d, 0.125d, 0.1875d, 1d),
            VoxelShapes.create(0.875d, 0d, 0d, 1d, 0.1875d, 0.125d)
    );

    static {
        for (GraveModel model : values()) {
            model.createShape();
        }
    }

    GraveModel(String name) {
        this(name, false);
    }

    GraveModel(String name, boolean onlyContributor) {
        this.name = name;
        this.onlyContributor = onlyContributor;
    }

    public GraveModel getPrevious() {
        return byId(this.ordinal() > 0 ? this.ordinal() - 1 : values().length - 1);
    }

    public GraveModel getNext() {
        return byId(this.ordinal() == values().length - 1 ? 0 : this.ordinal() + 1);
    }

    public boolean isOnlyContributor() {
        return this.onlyContributor;
    }

    public final VoxelShape getShape(Direction facing) {
        return collisions.get(facing);
    }

    public static GraveModel byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : getDefault();
    }

    public static GraveModel getDefault() {
        return GRAVE_SIMPLE;
    }

    public static GraveModel getRandom() {
        return values()[Helper.random.nextInt(values().length)];
    }

    @Override
    public String toString() {
        return this.name;
    }

    public String getName() {
        return this.name;
    }

    private VoxelShape getVoxelForDirection(Direction direction, double x1, double y1, double z1, double x2, double y2, double z2) {
        switch (direction) {
            case SOUTH:
                return VoxelShapes.create(x1, y1, 1d - z2, x2, y2, 1d - z1);
            case WEST:
                return VoxelShapes.create(z1, y1, x1, z2, y2, x2);
            case EAST:
                return VoxelShapes.create(1d - z2, y1, x1, 1d - z1, y2, x2);
            case NORTH:
            default:
                return VoxelShapes.create(x1, y1, z1, x2, y2, z2);
        }
    }

    private void createShape() {
        switch (this) {
            case GRAVE_NORMAL:
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    this.collisions.put(direction, VoxelShapes.or(ground_bounds,
                            getVoxelForDirection(direction, 0.1875d, 0.0625d, 0d, 0.8125d, 0.25d, 1d),
                            getVoxelForDirection(direction, 0.25d, 0.25d, 0.0625d, 0.75d, 0.3125d, 0.875d),
                            getVoxelForDirection(direction, 0.1875d, 0.25d, 0.875d, 0.8125d, 0.8125d, 1d),
                            getVoxelForDirection(direction, 0.25d, 0.8125d, 0.875d, 0.75d, 0.875d, 1d)
                    ));
                }
                break;
            case GRAVE_CROSS:
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    this.collisions.put(direction, VoxelShapes.or(ground_bounds,
                            getVoxelForDirection(direction, 0.40625d, 0.125d, 0.6875d, 0.59375d, 0.9375d, 0.875d),
                            getVoxelForDirection(direction, 0.21875d, 0.5625d, 0.6875d, 0.78125d, 0.75d, 0.875d),
                            getVoxelForDirection(direction, 0.28125d, 0.0625d, 0.5625d, 0.71875d, 0.125d, 1.0d),
                            getVoxelForDirection(direction, 0.34375d, 0.125d, 0.625d, 0.65625d, 0.1875d, 0.9375d)
                    ));
                }
                break;
            case TOMBSTONE:
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    this.collisions.put(direction, VoxelShapes.or(ground_bounds,
                            getVoxelForDirection(direction, 0.0625d, 0.0625d, 0.0625d, 0.9375d, 0.09375d, 0.4375d),
                            getVoxelForDirection(direction, 0.0625d, 0.09375d, 0.1875d, 0.9375d, 0.125d, 0.4375d),
                            getVoxelForDirection(direction, 0.84375d, 0.125d, 0.28125d, 0.90625d, 0.59375d, 0.34375d),
                            getVoxelForDirection(direction, 0.09375d, 0.125d, 0.28125d, 0.15625d, 0.59375d, 0.34375d),
                            getVoxelForDirection(direction, 0.0625d, 0.0625d, 0.4375d, 0.9375d, 0.59375d, 0.9375d),
                            getVoxelForDirection(direction, 0.03125d, 0.59375d, 0.21875d, 0.96875d, 0.625d, 0.96875d),
                            getVoxelForDirection(direction, 0.0625d, 0.625d, 0.25d, 0.9375d, 0.64375d, 0.9375d)
                    ));
                }
                break;
            case SUBARAKI_GRAVE:
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    this.collisions.put(direction, VoxelShapes.or(subaraki_ground_bounds,
                            getVoxelForDirection(direction, 0.25d, 0.4375d, 0.375d, 0.75d, 0.9375, 0.625d)
                    ));
                }
                break;
            case GRAVE_ORIGINAL:
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    this.collisions.put(direction, VoxelShapes.or(ground_bounds,
                            getVoxelForDirection(direction, 0.15625d, 0.0625d, 0d, 0.84375, 0.09375d, 1d),
                            getVoxelForDirection(direction, 0.28125d, 0.09375d, 0.03125d, 0.71875d, 0.46875d, 0.96875d),
                            getVoxelForDirection(direction, 0.40625d, 0.46875d, 0.71875d, 0.59375d, 0.5d, 0.90625d),
                            getVoxelForDirection(direction, 0.453125d, 0.5d, 0.765625d, 0.546875d, 0.625d, 0.859375d),
                            getVoxelForDirection(direction, 0.484375d, 0.625d, 0.796875d, 0.515625d, 0.875d, 0.828125d),
                            getVoxelForDirection(direction, 0.421875d, 0.78125d, 0.796875d, 0.578125d, 0.8125d, 0.828125d),
                            getVoxelForDirection(direction, 0.453125d, 0.75d, 0.796875d, 0.546875d, 0.84375d, 0.828125d)
                    ));
                }
                break;
            default:
            case GRAVE_SIMPLE:
                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    this.collisions.put(direction, VoxelShapes.or(ground_bounds,
                            getVoxelForDirection(direction, 0.1875d, 0.0625d, 0.8125d, 0.8125d, 0.8125d, 1.0d),
                            getVoxelForDirection(direction, 0.25d, 0.8125d, 0.8125d, 0.75d, 0.875d, 1.0d)
                    ));
                }
        }
    }

	@Override
	public String getString() {

		return this.name;
	}
}

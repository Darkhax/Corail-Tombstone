package ovh.corail.tombstone.helper;

import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class SpawnProtectionHandler {
    private static final SpawnProtectionHandler instance = new SpawnProtectionHandler();
    private boolean isActive = false;
    private BlockPos spawnPos;
    private int range;

    private SpawnProtectionHandler() {
    }

    public static SpawnProtectionHandler getInstance() {
        return instance;
    }

    public void setSpawnProtection(BlockPos spawnPos, int range) {
        isActive = range > 0;
        this.spawnPos = spawnPos;
        this.range = range;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isBlockProtected(RegistryKey<World> dimension, BlockPos currentPos) {
        if (!isActive || !dimension.equals(World.OVERWORLD)) {
            return false;
        }
        int i = MathHelper.abs(currentPos.getX() - spawnPos.getX());
        int j = MathHelper.abs(currentPos.getZ() - spawnPos.getZ());
        int k = Math.max(i, j);
        return k <= range;
    }
}

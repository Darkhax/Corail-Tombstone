package ovh.corail.tombstone.tileentity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.vector.Vector3d;
import ovh.corail.tombstone.ModTombstone;
import ovh.corail.tombstone.block.BlockDecorativeGrave;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.helper.TimeHelper;
import ovh.corail.tombstone.registry.ModBlocks;

public class TileEntityDecorativeGrave extends TileEntityWritableGrave {
    private long lastCheckSoul = -1L;

    public TileEntityDecorativeGrave() {
        super(ModBlocks.tile_decorative_grave);
    }

    @Override
    public boolean canShowFog() {
        return this.world != null && ConfigTombstone.client.fogPeriod.get().test(this.world);
    }

    @Override
    public void tick() {
        if (this.world == null || !(getBlockState().getBlock() instanceof BlockDecorativeGrave)) {
            return;
        }
        super.tick();
        if (this.world.isRemote) {
            // client side
            if (TimeHelper.atInterval(this.countTicks, 100)) {
                if (getBlockState().get(BlockDecorativeGrave.HAS_SOUL)) {
                    ModTombstone.PROXY.produceGraveSoul(this.world, this.pos);
                }
            }
        } else {
            // server side
            long worldTicks = TimeHelper.worldTicks(this.world);
            if (this.lastCheckSoul <= 0L || this.lastCheckSoul > worldTicks) {
                this.lastCheckSoul = worldTicks;
                return;
            }
            long elapsedMinutes = TimeHelper.minuteElapsed(this.world, this.lastCheckSoul);
            if (elapsedMinutes >= ConfigTombstone.decorative_grave.timeSoul.get()) {
                if (getBlockState().get(BlockDecorativeGrave.HAS_SOUL)) {
                    resetCheckSoul();
                    return;
                }
                long count = elapsedMinutes / ConfigTombstone.decorative_grave.timeSoul.get();
                this.lastCheckSoul += (count * ConfigTombstone.decorative_grave.timeSoul.get());
                double chance = 1d - (Math.pow(1d - (ConfigTombstone.decorative_grave.chanceSoul.get() / 1000d), (double) count));
                if (chance >= 1d || this.world.rand.nextDouble() <= chance) {
                    if (elapsedMinutes == ConfigTombstone.decorative_grave.timeSoul.get()) {
                    	
                    	LightningBoltEntity lightning = EntityType.LIGHTNING_BOLT.create(this.world);
                    	lightning.moveForced(Vector3d.copyCenteredHorizontally(this.pos));
                    	lightning.setEffectOnly(true);
                    	this.world.addEntity(lightning);
                    }
                    this.world.setBlockState(this.pos, getBlockState().with(BlockDecorativeGrave.HAS_SOUL, true), 3);
                }
            }
        }
    }

    public void resetCheckSoul() {
        this.lastCheckSoul = -1L;
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        return super.write(compound);
    }

    @Override
    protected CompoundNBT writeShared(CompoundNBT compound) {
        super.writeShared(compound);
        if (this.lastCheckSoul > 0L) {
            compound.putLong("lastCheckSoul", this.lastCheckSoul);
        }
        return compound;
    }

    @Override
    public void read(BlockState state, CompoundNBT compound) {
        super.read(state, compound);
        if (compound.contains("lastCheckSoul")) {
            this.lastCheckSoul = compound.getLong("lastCheckSoul");
        }
    }
}

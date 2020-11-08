package ovh.corail.tombstone.tileentity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.AxisAlignedBB;
import ovh.corail.tombstone.ModTombstone;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.TimeHelper;

@SuppressWarnings("WeakerAccess")
public abstract class TileEntityWritableGrave extends TileEntity implements ITickableTileEntity {
    protected String ownerName = "";
    protected long deathDate;
    public int countTicks = 0;

    public TileEntityWritableGrave(TileEntityType<?> tileType) {
        super(tileType);
    }

    public abstract boolean canShowFog();

    @Override
    public void tick() {
        if (this.world != null) {
            this.countTicks++;
            if (this.world.isRemote) {
                if (canShowFog()) {
                    ModTombstone.PROXY.produceGraveSmoke(this.world, this.pos.getX(), this.pos.getY(), this.pos.getZ());
                }
            } else if (Helper.isAprilFoolsDay() && TimeHelper.atInterval(this.countTicks, 600) && Helper.random.nextFloat() < 0.3f) {
                Helper.handleAprilFoolsDayGrave(this.world, this.pos);
            }
        }
    }

    public void setOwner(Entity owner, long deathDate) {
        setOwner(owner.getName().getUnformattedComponentText(), deathDate);
    }

    public void setOwner(String ownerName, long deathDate) {
        this.ownerName = ownerName;
        this.deathDate = deathDate;
    }

    public String getOwnerName() {
        return this.ownerName;
    }

    public boolean hasOwner() {
        return this.ownerName.length() > 0;
    }

    public void resetDeathTime() {
        this.deathDate = TimeHelper.systemTime();
        this.countTicks = 0;
    }

    public long getOwnerDeathTime() {
        return this.deathDate;
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        return writeShared(compound);
    }

    protected CompoundNBT writeShared(CompoundNBT compound) {
        super.write(compound);
        compound.putString("ownerName", this.ownerName);
        compound.putLong("deathDate", this.deathDate);
        compound.putInt("countTicks", this.countTicks);
        return compound;
    }

    @Override
    public void read(BlockState state, CompoundNBT compound) {
    	
        super.read(state, compound);
        if (compound.contains("ownerName")) {
            this.ownerName = compound.getString("ownerName");
        }
        if (compound.contains("deathDate")) {
            this.deathDate = compound.getLong("deathDate");
        }
        if (compound.contains("countTicks")) {
            this.countTicks = compound.getInt("countTicks");
        }
    }

    @Override
    public CompoundNBT getUpdateTag() {
        return writeShared(new CompoundNBT());
    }

    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(this.pos, 1, getUpdateTag());
    }

    @Override
    public boolean receiveClientEvent(int id, int type) {
        return true;
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket pkt) {
        read(this.world.getBlockState(pkt.getPos()), pkt.getNbtCompound());
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        double renderExtension = 1.0d;
        return new AxisAlignedBB(this.pos.getX() - renderExtension, this.pos.getY() - renderExtension, this.pos.getZ() - renderExtension, this.pos.getX() + 1 + renderExtension, this.pos.getY() + 1 + renderExtension, this.pos.getZ() + 1 + renderExtension);
    }

    @Override
    public void warnInvalidBlock() {
    }
}

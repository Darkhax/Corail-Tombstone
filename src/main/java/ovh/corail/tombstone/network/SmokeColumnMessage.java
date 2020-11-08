package ovh.corail.tombstone.network;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.fml.network.NetworkEvent;
import ovh.corail.tombstone.ModTombstone;
import ovh.corail.tombstone.helper.Helper;

import java.util.function.Supplier;

public class SmokeColumnMessage {
    private int id;

    public SmokeColumnMessage(int id) {
        this.id = id;
    }

    static SmokeColumnMessage fromBytes(PacketBuffer buf) {
        return new SmokeColumnMessage(buf.readInt());
    }

    static void toBytes(SmokeColumnMessage msg, PacketBuffer buf) {
        buf.writeInt(msg.id);
    }

    public static class Handler {
        static void handle(final SmokeColumnMessage msg, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context ctx = contextSupplier.get();
            if (Helper.isPacketToClient(ctx)) {
                ctx.enqueueWork(new Runnable() {
                    @Override
                    public void run() {
                        Entity entity = Minecraft.getInstance().world != null ? Minecraft.getInstance().world.getEntityByID(msg.id) : null;
                        if (entity != null) {
                            Vector3d pVec = entity.getPositionVec();
                            ModTombstone.PROXY.produceSmokeColumn(Minecraft.getInstance().world, pVec.x, pVec.y, pVec.z);
                        }
                    }
                });
            }
            ctx.setPacketHandled(true);
        }
    }
}

package ovh.corail.tombstone.network;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkEvent;
import ovh.corail.tombstone.block.BlockGrave;
import ovh.corail.tombstone.helper.Helper;

import java.util.function.Supplier;

public class TombstoneActivatedMessage {
    private BlockPos currentPos;

    public TombstoneActivatedMessage(BlockPos currentPos) {
        this.currentPos = currentPos;
    }

    static TombstoneActivatedMessage fromBytes(PacketBuffer buf) {
        return new TombstoneActivatedMessage(BlockPos.fromLong(buf.readLong()));
    }

    static void toBytes(TombstoneActivatedMessage msg, PacketBuffer buf) {
        buf.writeLong(msg.currentPos.toLong());
    }

    @SuppressWarnings("deprecation")
    public static class Handler {
        static void handle(final TombstoneActivatedMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context ctx = contextSupplier.get();
            if (Helper.isPacketToServer(ctx)) {
                ctx.enqueueWork(() -> {
                    ServerPlayerEntity player = ctx.getSender();
                    if (player != null) {
                        ServerWorld world = player.getServerWorld();
                        BlockState state = world.getBlockState(message.currentPos);
                        Block block = state.getBlock();
                        if (block instanceof BlockGrave) {
                            MinecraftServer server = player.getServer();
                            if (server != null && server.isDedicatedServer() && server.isBlockProtected(world, message.currentPos, player)) {
                                block.onBlockActivated(state, world, message.currentPos, player, Hand.MAIN_HAND, new BlockRayTraceResult(new Vector3d(message.currentPos.getX(), message.currentPos.getY(), message.currentPos.getZ()), Direction.DOWN, message.currentPos, true));
                            }
                        }
                    }
                });
            }
            ctx.setPacketHandled(true);
        }
    }
}

package ovh.corail.tombstone.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.Pair;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.Location;
import ovh.corail.tombstone.helper.SpawnHelper;
import ovh.corail.tombstone.helper.StyleType;

public class CommandTBTeleportHome extends TombstoneCommand {

    public CommandTBTeleportHome(CommandDispatcher<CommandSource> commandDispatcher) {
        super(commandDispatcher);
    }

    @Override
    String getName() {
        return "tbteleporthome";
    }

    @Override
    LiteralArgumentBuilder<CommandSource> getBuilder(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(c -> teleportHome(c.getSource(), c.getSource().asPlayer(), c.getSource().asPlayer()));
        builder.then(Commands.argument(PLAYER_PARAM, EntityArgument.player())
                .executes(c -> teleportHome(c.getSource(), c.getSource().asPlayer(), EntityArgument.getPlayer(c, PLAYER_PARAM)))
                .then(Commands.argument(TARGET_PARAM, EntityArgument.player())
                        .executes(c -> teleportHome(c.getSource(), EntityArgument.getPlayer(c, PLAYER_PARAM), EntityArgument.getPlayer(c, TARGET_PARAM)))
                )
        );
        return builder;
    }

    private int teleportHome(CommandSource sender, ServerPlayerEntity player, ServerPlayerEntity target) {
        checkAlive(player);
        checkNotSpectator(player);

        Pair<ServerWorld, BlockPos> spawnPoint = getRespawnPoint(sender.getServer(), target);
        Location location = new SpawnHelper(spawnPoint.getLeft(), spawnPoint.getRight()).findSpawnPlace(false);
        checkValidPos(spawnPoint.getLeft(), location.getPos());
        Entity newEntity = Helper.teleportToGrave(player, location);
        if (EntityHelper.isValidPlayer(newEntity)) {
            LangKey.MESSAGE_TELEPORT_SUCCESS.sendMessage((PlayerEntity) newEntity, StyleType.MESSAGE_SPELL);
        }
        sendMessage(sender, LangKey.MESSAGE_TELEPORT_TARGET_TO_LOCATION.getText(newEntity.getName(), LangKey.MESSAGE_HERE.getText(), location.x, location.y, location.z, location.getDimString()), false);
        return 1;
    }

    public static Pair<ServerWorld, BlockPos> getRespawnPoint(MinecraftServer server, ServerPlayerEntity player) {
        // individual spawn position
        BlockPos spawnPos = player.func_241140_K_();
        if (spawnPos != null) {
            ServerWorld world = server.getWorld(player.func_241141_L_());
            if (world != null) {
                return Pair.of(world, spawnPos);
            }
        }
        // overworld
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        assert overworld != null;
        // default spawn in overworld
        return Pair.of(overworld, overworld.getSpawnPoint());
    }
}

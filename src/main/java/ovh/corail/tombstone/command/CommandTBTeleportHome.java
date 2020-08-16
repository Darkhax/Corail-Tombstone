package ovh.corail.tombstone.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
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

        Pair<World, BlockPos> spawnPoint = getRespawnPoint(target);
        Location location = new SpawnHelper((ServerWorld) spawnPoint.getLeft(), spawnPoint.getRight()).findSpawnPlace(false);
        checkValidPos(spawnPoint.getLeft(), spawnPoint.getRight());
        Entity newEntity = Helper.teleportToGrave(player, location);
        if (EntityHelper.isValidPlayer(newEntity)) {
            LangKey.MESSAGE_TELEPORT_SUCCESS.sendMessage((PlayerEntity) newEntity, StyleType.MESSAGE_SPELL);
        }
        sendMessage(sender, LangKey.MESSAGE_TELEPORT_TARGET_TO_LOCATION.getText(newEntity.getName(), LangKey.MESSAGE_HERE.getText(), location.x, location.y, location.z, location.dim.func_240901_a_().toString()), false);
        return 1;
    }

    public static Pair<World, BlockPos> getRespawnPoint(PlayerEntity player) {
        // TODO disabled
        return Pair.of(player.world, player.getBedPosition().orElse(player.getPosition()));
        /*BlockPos pos = player.getBedLocation(player.world.func_234923_W_());
        if (pos != null) {
            return Pair.of(player.world, pos);
        }
        World spawnWorld = player.getServer().getWorld(player.getSpawnDimension());
        pos = player.getBedLocation(player.getSpawnDimension());
        if (pos == null) {
            pos = spawnWorld.getSpawnPoint();
        }
        return Pair.of(spawnWorld, pos);*/
    }
}

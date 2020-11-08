package ovh.corail.tombstone.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.Location;
import ovh.corail.tombstone.helper.SpawnHelper;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.helper.TimeHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CommandTBAcceptTeleport extends TombstoneCommand {
    private static final List<Ticket> ticketList = new ArrayList<>();

    public CommandTBAcceptTeleport(CommandDispatcher<CommandSource> commandDispatcher) {
        super(commandDispatcher);
    }

    @Override
    public String getName() {
        return "tbacceptteleport";
    }

    @Override
    public int getPermissionLevel() {
        return 0;
    }

    @Override
    LiteralArgumentBuilder<CommandSource> getBuilder(LiteralArgumentBuilder<CommandSource> builder) {
        return builder.executes(c -> showUsage(c.getSource()))
                .then(Commands.argument(TARGET_PARAM, StringArgumentType.word())
                        .executes(c -> acceptTeleport(c.getSource(), StringArgumentType.getString(c, TARGET_PARAM))));
    }

    private int acceptTeleport(CommandSource sender, String targetString) throws CommandSyntaxException {
        ServerPlayerEntity player = sender.asPlayer();
        ServerPlayerEntity target = sender.getServer().getPlayerList().getPlayerByUUID(UUID.fromString(targetString));
        if (target == null) {
            throw LangKey.MESSAGE_PLAYER_INVALID.asCommandException();
        }
        boolean valid = ticketList.removeIf(p -> p.playerUUID.equals(player.getUniqueID()) && p.targetUUID.equals(target.getUniqueID()) && p.expiredTime >= TimeHelper.systemTime());
        if (valid) {
            Location spawnLoc = new SpawnHelper(player.getServerWorld(), player.getPosition()).findSafePlace(3, true);
            ServerPlayerEntity newPlayer = Helper.teleportEntity(target, spawnLoc.isOrigin() ? new Location(player) : spawnLoc);
            LangKey.MESSAGE_TELEPORT_SUCCESS.sendMessage(newPlayer, StyleType.MESSAGE_SPELL);
            LangKey.MESSAGE_JOIN_YOU.sendMessage(player, StyleType.MESSAGE_SPELL, newPlayer.getName());
            return 1;
        } else {
            throw LangKey.MESSAGE_NO_TICKET.asCommandException();
        }
    }

    public static void addTicket(PlayerEntity player, PlayerEntity target, int duration) {
        ticketList.add(new Ticket(player.getUniqueID(), target.getUniqueID(), TimeHelper.systemTime() + TimeUnit.SECONDS.toMillis(duration)));
    }

    public static void cleanTickets(long time) {
        ticketList.removeIf(p -> p.expiredTime < time);
    }

    public static class Ticket {
        private final UUID playerUUID;
        private final UUID targetUUID;
        private long expiredTime;

        Ticket(UUID playerUUID, UUID targetUUID, long expiredTime) {
            this.playerUUID = playerUUID;
            this.targetUUID = targetUUID;
            this.expiredTime = expiredTime;
        }
    }
}

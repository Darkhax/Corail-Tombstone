package ovh.corail.tombstone.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ServerWorld;
import ovh.corail.tombstone.api.cooldown.CooldownType;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.helper.CooldownHandler;
import ovh.corail.tombstone.helper.DeathHandler;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.Location;
import ovh.corail.tombstone.helper.SpawnHelper;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.helper.TimeHelper;

public class CommandTBTeleportDeath extends TombstoneCommand {

    public CommandTBTeleportDeath(CommandDispatcher<CommandSource> commandDispatcher) {
        super(commandDispatcher);
    }

    @Override
    String getName() {
        return "tbteleportdeath";
    }

    @Override
    public int getPermissionLevel() {
        return 0;
    }

    @Override
    LiteralArgumentBuilder<CommandSource> getBuilder(LiteralArgumentBuilder<CommandSource> builder) {
        return builder.executes(c -> teleportDeath(c.getSource()));
    }

    private int teleportDeath(CommandSource sender) throws CommandSyntaxException {
        if (ConfigTombstone.general.cooldownTeleportDeath.get() < 0) {
            throw LangKey.MESSAGE_DISABLED_COMMAND.asCommandException();
        }
        final ServerPlayerEntity player = sender.asPlayer();
        checkAlive(player);
        checkNotSpectator(player);
        Location lastDeathLocation = DeathHandler.INSTANCE.getLastDeathLocation(player);
        if (lastDeathLocation.isOrigin()) {
            throw LangKey.MESSAGE_NO_DEATH_LOCATION.asCommandException();
        }
        int cd = CooldownHandler.INSTANCE.getCooldown(player, CooldownType.TELEPORT_DEATH);
        if (cd > 0) {
            int[] timeArray = TimeHelper.getTimeArray(cd);
            LangKey.MESSAGE_COMMAND_IN_COOLDOWN.sendMessage(player, timeArray[0], timeArray[1], timeArray[2]);
            return 0;
        }
        ServerWorld world = getOrThrowWorld(sender.getServer(), lastDeathLocation.dim);
        Location location = new SpawnHelper(world, lastDeathLocation.getPos()).findSpawnPlace(false);
        if (location.isOrigin()) {
            throw LangKey.MESSAGE_NO_SPAWN.asCommandException();
        }
        CooldownHandler.INSTANCE.resetCooldown(player, CooldownType.TELEPORT_DEATH);
        Entity newEntity = Helper.teleportEntity(player, location);
        if (EntityHelper.isValidPlayer(newEntity)) {
            LangKey.MESSAGE_TELEPORT_SUCCESS.sendMessage((PlayerEntity) newEntity, StyleType.MESSAGE_SPELL);
        }
        sendMessage(sender, LangKey.MESSAGE_TELEPORT_TARGET_TO_LOCATION.getText(newEntity.getName(), LangKey.MESSAGE_HERE.getText(), location.x, location.y, location.z, location.dim.getLocation().toString()), false);
        return 1;
    }
}

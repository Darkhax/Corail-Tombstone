package ovh.corail.tombstone.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.ITextComponent;
import ovh.corail.tombstone.api.cooldown.CooldownType;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.helper.CooldownHandler;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.helper.TimeHelper;

public class CommandTBRequestTeleport extends TombstoneCommand {

    public CommandTBRequestTeleport(CommandDispatcher<CommandSource> commandDispatcher) {
        super(commandDispatcher);
    }

    @Override
    String getName() {
        return "tbrequestteleport";
    }

    @Override
    public int getPermissionLevel() {
        return 0;
    }

    @Override
    LiteralArgumentBuilder<CommandSource> getBuilder(LiteralArgumentBuilder<CommandSource> builder) {
        return builder.executes(c -> showUsage(c.getSource()))
                .then(Commands.argument(TARGET_PARAM, EntityArgument.player())
                        .executes(c -> requestTeleport(c.getSource(), EntityArgument.getPlayer(c, TARGET_PARAM))));
    }

    private int requestTeleport(CommandSource sender, ServerPlayerEntity target) throws CommandSyntaxException {
        if (ConfigTombstone.general.cooldownRequestTeleport.get() < 0) {
            throw LangKey.MESSAGE_DISABLED_COMMAND.asCommandException();
        }
        final ServerPlayerEntity player = sender.asPlayer();
        if (player.equals(target)) {
            throw LangKey.MESSAGE_TELEPORT_SAME_PLAYER.asCommandException();
        }
        checkAlive(target);
        checkNotSpectator(target);
        int cd = CooldownHandler.INSTANCE.getCooldown(player, CooldownType.REQUEST_TELEPORT);
        if (cd > 0) {
            int[] timeArray = TimeHelper.getTimeArray(cd);
            LangKey.MESSAGE_COMMAND_IN_COOLDOWN.sendMessage(player, timeArray[0], timeArray[1], timeArray[2]);
            return 0;
        }
        CooldownHandler.INSTANCE.resetCooldown(player, CooldownType.REQUEST_TELEPORT);
        CommandTBAcceptTeleport.addTicket(target, player, Math.max(120, ConfigTombstone.general.cooldownRequestTeleport.get() * 60));
        ITextComponent hereClick = LangKey.createComponentCommand(target, "/tbacceptteleport " + player.getUniqueID(), LangKey.MESSAGE_HERE);
        LangKey.MESSAGE_REQUEST_TO_JOIN_RECEIVER.sendMessage(target, StyleType.MESSAGE_SPECIAL, hereClick, player.getName());
        LangKey.MESSAGE_REQUEST_TO_JOIN_SENDER.sendMessage(player, target.getName());
        return 1;
    }
}

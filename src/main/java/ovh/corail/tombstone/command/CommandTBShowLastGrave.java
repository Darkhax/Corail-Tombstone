package ovh.corail.tombstone.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import ovh.corail.tombstone.helper.DeathHandler;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.Location;
import ovh.corail.tombstone.registry.ModItems;

public class CommandTBShowLastGrave extends TombstoneCommand {

    public CommandTBShowLastGrave(CommandDispatcher<CommandSource> commandDispatcher) {
        super(commandDispatcher);
    }

    @Override
    public String getName() {
        return "tbshowlastgrave";
    }

    @Override
    LiteralArgumentBuilder<CommandSource> getBuilder(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(c -> getLastGrave(c.getSource(), c.getSource().asPlayer()));
        builder.then(Commands.argument(PLAYER_PARAM, EntityArgument.player())
                .executes(c -> getLastGrave(c.getSource(), EntityArgument.getPlayer(c, PLAYER_PARAM)))
        );
        return builder;
    }

    private int getLastGrave(CommandSource sender, ServerPlayerEntity target) {
        String targetName = target.getName().getUnformattedComponentText();
        Location lastGrave = DeathHandler.INSTANCE.getLastGrave(targetName);
        // check for grave's key on the player if no grave is found
        if (lastGrave.isOrigin()) {
            lastGrave = ModItems.grave_key.getTombPos(ModItems.grave_key.findFirstKeyInInventory(target));
        }
        if (lastGrave.isOrigin()) {
            throw LangKey.MESSAGE_NO_GRAVE.asCommandException();
        }

        sendMessage(sender, LangKey.MESSAGE_LAST_GRAVE_PLACE.getText(targetName, LangKey.MESSAGE_LAST_GRAVE.getText(), lastGrave.x, lastGrave.y, lastGrave.z, lastGrave.dim), false);
        return 1;
    }
}

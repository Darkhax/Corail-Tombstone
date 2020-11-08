package ovh.corail.tombstone.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.spawner.CustomVillageSiege;

import static ovh.corail.tombstone.spawner.CustomVillageSiege.SiegeState.SIEGE_START;

public class CommandTBSiege extends TombstoneCommand {

    public CommandTBSiege(CommandDispatcher<CommandSource> commandDispatcher) {
        super(commandDispatcher);
    }

    @Override
    public String getName() {
        return "tbsiege";
    }

    @Override
    LiteralArgumentBuilder<CommandSource> getBuilder(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(c -> launchSiege(c.getSource()));
        return builder;
    }

    private int launchSiege(CommandSource sender) {
        if (!ConfigTombstone.village_siege.handleVillageSiege.get()) {
            throw LangKey.MESSAGE_DISABLED_COMMAND.asCommandException();
        }
        ServerWorld world = sender.getServer().getWorld(World.OVERWORLD);
        if (world == null) {
            // should never happen
            throw LangKey.MESSAGE_UNLOADED_DIMENSION.asCommandException();
        }
        if (world.getWorldInfo().getDifficulty() == Difficulty.PEACEFUL) {
            throw LangKey.MESSAGE_DIFFICULTY_PEACEFUL.asCommandException();
        }
        if (world.isDaytime()) {
            throw LangKey.MESSAGE_ONLY_AT_NIGHT.asCommandException();
        }
        CustomVillageSiege villageSiege = (CustomVillageSiege) world.field_241104_N_.stream().filter(spawner -> spawner instanceof CustomVillageSiege).findFirst().orElseThrow(() -> new CommandException(new StringTextComponent("The Overworld chunk generator is not the vanilla one")));
        switch (villageSiege.siegeState) {
            case SIEGE_START:
            case SPAWN_MOBS:
                throw LangKey.MESSAGE_START_SIEGE_FAILED.asCommandException();
            case SIEGE_END:
                villageSiege.hasFailedTrySiege = false;
                villageSiege.siegeState = SIEGE_START;
                sendMessage(sender, LangKey.MESSAGE_START_SIEGE_SUCCESS.getText(), false);
        }
        return 1;
    }
}

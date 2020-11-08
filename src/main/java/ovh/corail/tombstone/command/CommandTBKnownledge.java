package ovh.corail.tombstone.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import ovh.corail.tombstone.capability.TBCapabilityProvider;
import ovh.corail.tombstone.helper.DeathHandler;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.StyleType;

public class CommandTBKnownledge extends TombstoneCommand {

    public CommandTBKnownledge(CommandDispatcher<CommandSource> commandDispatcher) {
        super(commandDispatcher);
    }

    @Override
    public String getName() {
        return "tbknowledge";
    }

    @Override
    LiteralArgumentBuilder<CommandSource> getBuilder(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(c -> showUsage(c.getSource()));
        builder.then(Commands.argument(PLAYER_PARAM, EntityArgument.player())
                .executes(c -> showKnowledge(c.getSource(), EntityArgument.getPlayer(c, PLAYER_PARAM)))
                .then(SubCommand.SHOW.literal()
                        .executes(c -> showKnowledge(c.getSource(), EntityArgument.getPlayer(c, PLAYER_PARAM)))
                )
                .then(SubCommand.SET.literal()
                        .executes(c -> showUsage(c.getSource()))
                        .then(Commands.argument(AMOUNT_PARAM, IntegerArgumentType.integer()).suggests(AMOUNT_SUGGESTION)
                                .executes(c -> setKnowledge(c.getSource(), EntityArgument.getPlayer(c, PLAYER_PARAM), IntegerArgumentType.getInteger(c, AMOUNT_PARAM)))
                        )
                )
                .then(SubCommand.GIVE.literal()
                        .executes(c -> showUsage(c.getSource()))
                        .then(Commands.argument(AMOUNT_PARAM, IntegerArgumentType.integer()).suggests(AMOUNT_SUGGESTION)
                                .executes(c -> giveKnowledge(c.getSource(), EntityArgument.getPlayer(c, PLAYER_PARAM), IntegerArgumentType.getInteger(c, AMOUNT_PARAM)))
                        )
                )
                .then(SubCommand.REMOVE.literal()
                        .executes(c -> showUsage(c.getSource()))
                        .then(Commands.argument(AMOUNT_PARAM, IntegerArgumentType.integer()).suggests(AMOUNT_SUGGESTION)
                                .executes(c -> removeKnowledge(c.getSource(), EntityArgument.getPlayer(c, PLAYER_PARAM), IntegerArgumentType.getInteger(c, AMOUNT_PARAM)))
                        )
                )
        );
        return builder;
    }

    private int showKnowledge(CommandSource sender, ServerPlayerEntity target) {
        target.getCapability(TBCapabilityProvider.TB_CAPABILITY, null).ifPresent(cap -> sendMessage(sender, LangKey.MESSAGE_SHOW_KNOWLEDGE.getText(StyleType.MESSAGE_SPELL, target.getName(), cap.getKnowledge()), false));
        return 1;
    }

    private int setKnowledge(CommandSource sender, ServerPlayerEntity target, long newKnowledge) {
        if (newKnowledge < 0) {
            throw LangKey.MESSAGE_POSITIVE_INTEGER.asCommandException();
        }
        int adjust = (int) (newKnowledge - EntityHelper.getKnowledge(target));
        if (adjust >= 0) {
            giveKnowledge(sender, target, adjust);
        } else {
            removeKnowledge(sender, target, -adjust);
        }
        return 1;
    }

    private int giveKnowledge(CommandSource sender, ServerPlayerEntity target, int amount) {
        if (amount <= 0) {
            throw LangKey.MESSAGE_POSITIVE_INTEGER.asCommandException();
        }
        EntityHelper.addKnowledge(target, amount);
        if (!target.equals(sender.getEntity()) || !DeathHandler.INSTANCE.getOptionKnowledgeMessage(target.getUniqueID())) {
            sendMessage(sender, LangKey.MESSAGE_EARN_KNOWLEDGE.getText(LangKey.MESSAGE_PLAYER_KNOWLEDGE.getText(target.getName()), amount), false);
        }
        return 1;
    }

    private int removeKnowledge(CommandSource sender, ServerPlayerEntity target, int amount) {
        if (amount <= 0) {
            throw LangKey.MESSAGE_POSITIVE_INTEGER.asCommandException();
        }
        EntityHelper.removeKnowledge(target, amount);
        if (!target.equals(sender.getEntity()) || !DeathHandler.INSTANCE.getOptionKnowledgeMessage(target.getUniqueID())) {
            sendMessage(sender, LangKey.MESSAGE_LOSE_KNOWLEDGE.getText(StyleType.MESSAGE_SPELL, LangKey.MESSAGE_PLAYER_KNOWLEDGE.getText(target.getName()), amount), false);
        }
        return 1;
    }

    private enum SubCommand implements ISubCommand {SHOW, SET, GIVE, REMOVE}
}

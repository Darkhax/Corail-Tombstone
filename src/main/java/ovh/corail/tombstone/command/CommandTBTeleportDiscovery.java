package ovh.corail.tombstone.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.ResourceLocationArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.registries.ForgeRegistries;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.Location;
import ovh.corail.tombstone.helper.SpawnHelper;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.helper.SupportStructures;

public class CommandTBTeleportDiscovery extends TombstoneCommand {

    public CommandTBTeleportDiscovery(CommandDispatcher<CommandSource> commandDispatcher) {
        super(commandDispatcher);
    }

    @Override
    public String getName() {
        return "tbteleportdiscovery";
    }

    @Override
    LiteralArgumentBuilder<CommandSource> getBuilder(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(c -> showUsage(c.getSource()))
                .then(Commands.argument(TARGET_PARAM, EntityArgument.entity())
                        .executes(c -> teleportDiscovery(c.getSource(), EntityArgument.getEntity(c, TARGET_PARAM), DEFAULT_STRUCTURE))
                        .then(Commands.argument(STRUCTURE_PARAM, ResourceLocationArgument.resourceLocation()).suggests(SUGGESTION_STRUCTURE)
                                .executes(c -> teleportDiscovery(c.getSource(), EntityArgument.getEntity(c, TARGET_PARAM), ResourceLocationArgument.getResourceLocation(c, STRUCTURE_PARAM)))
                                .then(Commands.argument(DIM_PARAM, DimensionArgument.getDimension())
                                        .executes(c -> teleportDiscovery(c.getSource(), EntityArgument.getEntity(c, TARGET_PARAM), ResourceLocationArgument.getResourceLocation(c, STRUCTURE_PARAM), DimensionArgument.getDimensionArgument(c, DIM_PARAM)))
                                )
                        )
                );
        return builder;
    }

    private int teleportDiscovery(CommandSource sender, Entity target, ResourceLocation structureRL) {
        return teleportDiscovery(sender, target, structureRL, (ServerWorld) target.world);
    }

    private int teleportDiscovery(CommandSource sender, Entity target, ResourceLocation structureRL, ServerWorld world) {
        checkAlive(target);
        checkNotSpectator(target);
        if (!ForgeRegistries.STRUCTURE_FEATURES.getKeys().contains(structureRL)) {
            throw LangKey.MESSAGE_INVALID_STRUCTURE.asCommandException();
        }
        int y = SupportStructures.getY(structureRL);
        final BlockPos targetPos = new BlockPos(target.getPosX(), y, target.getPosZ());
        Location structureLoc = Helper.findNearestStructure(world, targetPos, structureRL, y, true);
        if (structureLoc.isOrigin()) {
            throw LangKey.MESSAGE_NO_STRUCTURE.asCommandException();
        }
        final Location spawnLoc = new SpawnHelper(world, structureLoc.getPos()).findPlaceInStructure(structureRL);
        if (spawnLoc.isOrigin()) {
            throw LangKey.MESSAGE_NO_SPAWN.asCommandException();
        }
        runNextTick(() -> {
            Entity newEntity = Helper.teleportEntity(target, spawnLoc);
            sendMessage(sender, LangKey.MESSAGE_TELEPORT_TARGET_TO_LOCATION.getText(newEntity.getName(), LangKey.MESSAGE_HERE.getText(), spawnLoc.x, spawnLoc.y, spawnLoc.z, spawnLoc.getDimString()), false);
            if (EntityHelper.isValidPlayer(newEntity)) {
                LangKey.MESSAGE_TELEPORT_SUCCESS.sendMessage((PlayerEntity) newEntity, StyleType.MESSAGE_SPELL);
            }
        });
        return 1;
    }

    private static final ResourceLocation DEFAULT_STRUCTURE = new ResourceLocation("minecraft", "village");
}

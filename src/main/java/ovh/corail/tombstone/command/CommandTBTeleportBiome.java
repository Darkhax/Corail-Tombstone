package ovh.corail.tombstone.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.ResourceLocationArgument;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.provider.BiomeProvider;
import net.minecraft.world.server.ServerWorld;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.Location;
import ovh.corail.tombstone.helper.SpawnHelper;
import ovh.corail.tombstone.helper.StyleType;

import java.util.Random;

public class CommandTBTeleportBiome extends TombstoneCommand {

    public CommandTBTeleportBiome(CommandDispatcher<CommandSource> commandDispatcher) {
        super(commandDispatcher);
    }

    @Override
    String getName() {
        return "tbteleportbiome";
    }

    @Override
    LiteralArgumentBuilder<CommandSource> getBuilder(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(c -> showUsage(c.getSource()))
                .then(Commands.argument(TARGET_PARAM, EntityArgument.entity())
                        .executes(c -> showUsage(c.getSource()))
                        .then(Commands.argument(BIOME_PARAM, ResourceLocationArgument.resourceLocation()).suggests(SUGGESTION_BIOME)
                                .executes(c -> teleportBiome(c.getSource(), EntityArgument.getEntity(c, TARGET_PARAM), getOrThrowBiome(c, BIOME_PARAM)))
                                .then(Commands.argument(DIM_PARAM, IntegerArgumentType.integer()).suggests(SUGGESTION_DIM_IDS)
                                        .executes(c -> teleportBiome(c.getSource(), EntityArgument.getEntity(c, TARGET_PARAM), getOrThrowBiome(c, BIOME_PARAM), c.getSource().getServer().getWorld(getOrThrowDimensionType(IntegerArgumentType.getInteger(c, DIM_PARAM)))))
                                )));
        return builder;
    }

    private int teleportBiome(CommandSource sender, Entity target, Biome biome) {
        return teleportBiome(sender, target, biome, (ServerWorld) target.world);
    }

    private int teleportBiome(CommandSource sender, Entity target, Biome biome, ServerWorld world) {
        checkAlive(target);
        checkNotSpectator(target);

        if (biome == Biomes.MOUNTAIN_EDGE) {
            throw LangKey.MESSAGE_NO_BIOME_FOR_DIMENSION.asCommandException();
        }

        BlockPos startingPos = target.getPosition();
        Location biomePos = Location.ORIGIN;
        int nbTry = 0;
        while (biomePos.isOrigin() && nbTry < 3) {
            startingPos = Helper.getCloserValidPos(world, startingPos.add(nbTry * Helper.random.nextGaussian() * 5000, 0d, nbTry * Helper.random.nextGaussian() * 5000));
            biomePos = findNearestBiome(world, startingPos.getX(), startingPos.getY(), startingPos.getZ(), 6400, 8, biome, Helper.random, true);
            nbTry++;
        }

        if (biomePos.isOrigin()) {
            throw LangKey.MESSAGE_NO_BIOME.asCommandException();
        }
        final Location spawnLoc = new SpawnHelper(world, biomePos.getPos()).findSpawnPlace(false);
        if (spawnLoc.isOrigin()) {
            throw LangKey.MESSAGE_NO_SPAWN.asCommandException();
        }
        runNextTick(() -> {
            Entity newEntity = Helper.teleportEntity(target, spawnLoc);
            sendMessage(sender, LangKey.MESSAGE_TELEPORT_TARGET_TO_LOCATION.getTranslation(newEntity.getName(), LangKey.MESSAGE_HERE.getTranslation(), spawnLoc.x, spawnLoc.y, spawnLoc.z, spawnLoc.dim), false);
            if (EntityHelper.isValidPlayer(newEntity)) {
                newEntity.sendMessage(LangKey.MESSAGE_TELEPORT_SUCCESS.getTranslationWithStyle(StyleType.MESSAGE_SPELL));
            }
        });
        return 1;
    }

    private Location findNearestBiome(ServerWorld world, int x, int y, int z, int radius, int radiusStep, Biome biome, Random random, boolean onionSearch) {
        BiomeProvider biomeProvider = world.getChunkProvider().getChunkGenerator().getBiomeProvider();
        int dimId = Helper.getDimensionId(world);
        int xMin = x >> 2;
        int zMin = z >> 2;
        int radiusMax = radius >> 2;
        int yBiomeChunk = y >> 2;
        Location biomePos = Location.ORIGIN;
        int count = 0;
        int radiusMin = onionSearch ? 0 : radiusMax;
        for (int actualRadius = radiusMin; actualRadius <= radiusMax; actualRadius += radiusStep) {
            for (int biomeStepZ = -actualRadius; biomeStepZ <= actualRadius; biomeStepZ += radiusStep) {
                boolean onEdgeZ = Math.abs(biomeStepZ) == actualRadius;
                for (int biomeStepX = -actualRadius; biomeStepX <= actualRadius; biomeStepX += radiusStep) {
                    if (onionSearch) {
                        boolean onEdgeX = Math.abs(biomeStepX) == actualRadius;
                        if (!onEdgeX && !onEdgeZ) {
                            continue;
                        }
                    }
                    int xBiomeChunk = xMin + biomeStepX;
                    int zBiomeChunk = zMin + biomeStepZ;
                    if (biome.equals(biomeProvider.getNoiseBiome(xBiomeChunk, yBiomeChunk, zBiomeChunk))) {
                        if (biomePos.isOrigin() || random.nextInt(count + 1) == 0) {
                            biomePos = new Location(xBiomeChunk << 2, y, zBiomeChunk << 2, dimId);
                            if (onionSearch) {
                                return biomePos;
                            }
                        }
                        ++count;
                    }
                }
            }
        }
        return biomePos;
    }
}

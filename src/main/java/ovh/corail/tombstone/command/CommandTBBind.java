package ovh.corail.tombstone.command;

import java.util.Iterator;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.NonNullList;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.Constants;
import ovh.corail.tombstone.api.cooldown.CooldownType;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.helper.CooldownHandler;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.Location;
import ovh.corail.tombstone.helper.NBTStackHelper;
import ovh.corail.tombstone.helper.SpawnHelper;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.helper.TimeHelper;

public class CommandTBBind extends TombstoneCommand {

    public CommandTBBind(CommandDispatcher<CommandSource> commandDispatcher) {
        super(commandDispatcher);
    }

    @Override
    String getName() {
        return "tbbind";
    }

    @Override
    LiteralArgumentBuilder<CommandSource> getBuilder(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(c -> showUsage(c.getSource()));
        builder.then(SubCommand.SHOW.literal()
                .executes(c -> showBinds(c.getSource()))
        );
        builder.then(SubCommand.SET.literal()
                .executes(c -> setBind(c.getSource(), 0))
                .then(Commands.argument(BIND_LOCATION_ID_NBT_BYTE, IntegerArgumentType.integer(1, 5)).suggests(SUGGESTION_BIND_IDS)
                        .executes(c -> setBind(c.getSource(), IntegerArgumentType.getInteger(c, BIND_LOCATION_ID_NBT_BYTE)))
                )
        );
        builder.then(SubCommand.TELEPORT.literal()
                .executes(c -> {
                    ServerPlayerEntity player = c.getSource().asPlayer();
                    return teleportBind(c.getSource(), getOrThrowLocation(player, 0));
                })
                .then(Commands.argument(BIND_LOCATION_ID_NBT_BYTE, IntegerArgumentType.integer(1, 5)).suggests(SUGGESTION_BIND_IDS)
                        .executes(c -> teleportBind(c.getSource(), getOrThrowLocation(c.getSource().asPlayer(), IntegerArgumentType.getInteger(c, BIND_LOCATION_ID_NBT_BYTE))))
                )
        );
        return builder;
    }

    private int showBinds(CommandSource sender) throws CommandSyntaxException {
        if (ConfigTombstone.general.cooldownTeleportBind.get() < 0) {
            throw LangKey.MESSAGE_DISABLED_COMMAND.asCommandException();
        }
        final ServerPlayerEntity player = sender.asPlayer();
        final NonNullList<Location> locations = getLocationList(player);
        if (locations.stream().allMatch(Location::isOrigin)) {
            throw LangKey.MESSAGE_NO_BIND_LOCATION.asCommandException();
        }
        IntStream.range(0, locations.size()).forEach(i -> {
            Location location = locations.get(i);
            if (!location.isOrigin()) {
                player.sendMessage(new StringTextComponent((i + 1) + " -> {" + location.x + ", " + location.y + ", " + location.z + ", " + location.getDimString() + "}"), Util.DUMMY_UUID);
            }
        });
        return 1;
    }

    private int teleportBind(CommandSource sender, Location location) throws CommandSyntaxException {
        if (ConfigTombstone.general.cooldownTeleportBind.get() < 0) {
            throw LangKey.MESSAGE_DISABLED_COMMAND.asCommandException();
        }
        final ServerPlayerEntity player = sender.asPlayer();
        checkAlive(player);
        checkNotSpectator(player);
        int cd = CooldownHandler.INSTANCE.getCooldown(player, CooldownType.TELEPORT_BIND);
        if (cd > 0) {
            int[] timeArray = TimeHelper.getTimeArray(cd);
            LangKey.MESSAGE_COMMAND_IN_COOLDOWN.sendMessage(player, String.format("%02d", timeArray[0]), String.format("%02d", timeArray[1]), String.format("%02d", timeArray[2]));
            return 0;
        }

        ServerWorld world = sender.getServer().getWorld(location.dim);
        Location spawnPlace = new SpawnHelper(world, location.getPos()).findSpawnPlace(false);
        if (spawnPlace.isOrigin()) {
            throw LangKey.MESSAGE_NO_SPAWN.asCommandException();
        }
        CooldownHandler.INSTANCE.resetCooldown(player, CooldownType.TELEPORT_BIND);
        Entity newEntity = Helper.teleportEntity(player, spawnPlace);
        if (EntityHelper.isValidPlayer(newEntity)) {
            LangKey.MESSAGE_TELEPORT_SUCCESS.sendMessage((PlayerEntity) newEntity, StyleType.MESSAGE_SPELL);
        }
        sendMessage(sender, LangKey.MESSAGE_TELEPORT_TARGET_TO_LOCATION.getText(newEntity.getName(), LangKey.MESSAGE_HERE.getText(), spawnPlace.x, spawnPlace.y, spawnPlace.z, spawnPlace.getDimString()), false);
        return 1;
    }

    private int setBind(CommandSource sender, int bindId) throws CommandSyntaxException {
        if (ConfigTombstone.general.cooldownTeleportBind.get() < 0) {
            throw LangKey.MESSAGE_DISABLED_COMMAND.asCommandException();
        }
        ServerPlayerEntity player = sender.asPlayer();
        CompoundNBT persistentTag = EntityHelper.getPersistentTag(player);
        ListNBT locationList = persistentTag.getList(BIND_LOCATIONS_NBT_LIST, Constants.NBT.TAG_COMPOUND);
        Location location = new Location(player);
        if (!findFirstLocationInListNBT(locationList, nbt -> NBTStackHelper.getLocation((CompoundNBT) nbt, BIND_LOCATION_NBT_TAG).equals(location)).isOrigin()) {
            throw LangKey.MESSAGE_EXISTING_BIND_LOCATION.asCommandException();
        }
        if (bindId == 0) {
            byte id = getFirstEmptyIdInListNBT(locationList);
            if (id == -1) {
                setOrReplaceLocationInListNBT(locationList, location, bindId).putByte(BIND_LOCATION_ID_NBT_BYTE, (byte) BIND_ID_MIN);
            } else {
                CompoundNBT nbt = NBTStackHelper.setLocation(new CompoundNBT(), BIND_LOCATION_NBT_TAG, location);
                nbt.putByte(BIND_LOCATION_ID_NBT_BYTE, id);
                locationList.add(nbt);
            }
        } else {
            setOrReplaceLocationInListNBT(locationList, location, bindId).putByte(BIND_LOCATION_ID_NBT_BYTE, (byte) bindId);
        }
        persistentTag.put(BIND_LOCATIONS_NBT_LIST, locationList);
        LangKey.MESSAGE_BIND_LOCATION.sendMessage(player);
        return 1;
    }

    private Byte getFirstEmptyIdInListNBT(ListNBT locationList) {
        return locationList.stream().map(nbt -> ((CompoundNBT) nbt).getByte(BIND_LOCATION_ID_NBT_BYTE)).filter(id -> id >= BIND_ID_MIN && id <= BIND_ID_MAX).min(Byte::compareTo).orElse((byte) -1);
    }

    private CompoundNBT setOrReplaceLocationInListNBT(ListNBT locationList, Location location, int bindId) {
        return NBTStackHelper.setLocation(locationList.stream().map(CompoundNBT.class::cast).filter(nbt -> IS_BIND_ID.test(nbt, bindId)).findFirst().orElseGet(() -> {
            CompoundNBT nbt = new CompoundNBT();
            locationList.add(nbt);
            return nbt;
        }), BIND_LOCATION_NBT_TAG, location);
    }

    private Location getOrThrowLocation(ServerPlayerEntity player, int bindId) {
        final CompoundNBT persistentTag;
        if (bindId < 0 || bindId > 5 || !(persistentTag = EntityHelper.getPersistentTag(player)).contains(BIND_LOCATIONS_NBT_LIST, Constants.NBT.TAG_LIST)) {
            throw LangKey.MESSAGE_INVALID_LOCATION.asCommandException();
        }
        ListNBT locationList = persistentTag.getList(BIND_LOCATIONS_NBT_LIST, Constants.NBT.TAG_COMPOUND);
        Location location = findFirstLocationInListNBT(locationList, bindId == 0 ? nbt -> true : nbt -> IS_BIND_ID.test(nbt, bindId));
        if (location.isOrigin()) {
            throw LangKey.MESSAGE_NO_BIND_LOCATION.asCommandException();
        }
        return location;
    }

    private Location findFirstLocationInListNBT(ListNBT locationList, Predicate<INBT> test) {
        return locationList.stream().filter(test).map(nbt -> NBTStackHelper.getLocation((CompoundNBT) nbt, BIND_LOCATION_NBT_TAG)).findFirst().orElse(Location.ORIGIN);
    }

    private NonNullList<Location> getLocationList(ServerPlayerEntity player) {
    	
        CompoundNBT persistentTag = EntityHelper.getPersistentTag(player);
        NonNullList<Location> locationInstances = NonNullList.withSize(5, Location.ORIGIN);
        final MinecraftServer server = player.getServer();
        
        if (persistentTag.contains(BIND_LOCATIONS_NBT_LIST, Constants.NBT.TAG_LIST)) {
        	
            ListNBT locationList = persistentTag.getList(BIND_LOCATIONS_NBT_LIST, Constants.NBT.TAG_COMPOUND);
            Iterator<INBT> it = locationList.iterator();
            
            while (it.hasNext()) {
            	
                CompoundNBT data = (CompoundNBT) it.next();
                Location location = NBTStackHelper.getLocation(data, BIND_LOCATION_NBT_TAG);
                
                if (!location.isOrigin() && !Helper.isInvalidDimension(server, location.dim)) {
                    locationInstances.set(data.getByte(BIND_LOCATION_ID_NBT_BYTE) - 1, location);
                } 
                
                else {
                    it.remove();
                }
            }
        }
        return locationInstances;
    }

    private enum SubCommand implements ISubCommand {SHOW, SET, TELEPORT}

    private static final String BIND_LOCATIONS_NBT_LIST = "tb_bind_locations";
    private static final String BIND_LOCATION_NBT_TAG = "location";
    private static final String BIND_LOCATION_ID_NBT_BYTE = "bind_id";
    private static final int BIND_ID_MIN = 1, BIND_ID_MAX = 5;
    private static final SuggestionProvider<CommandSource> SUGGESTION_BIND_IDS = (ctx, build) -> ISuggestionProvider.suggest(IntStream.rangeClosed(BIND_ID_MIN, BIND_ID_MAX).mapToObj(String::valueOf), build);
    private static final BiPredicate<INBT, Integer> IS_BIND_ID = (nbt, bindId) -> bindId == ((CompoundNBT) nbt).getByte(BIND_LOCATION_ID_NBT_BYTE);
}

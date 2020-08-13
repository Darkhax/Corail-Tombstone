package ovh.corail.tombstone.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.play.server.SPlayEntityEffectPacket;
import net.minecraft.network.play.server.SPlayerAbilitiesPacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.concurrent.ThreadTaskExecutor;
import net.minecraft.world.storage.FolderName;
import org.apache.commons.lang3.tuple.Pair;
import ovh.corail.tombstone.ModTombstone;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.helper.ThreadedBackup;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

public class CommandTBRecovery extends TombstoneCommand {

    public CommandTBRecovery(CommandDispatcher<CommandSource> commandDispatcher) {
        super(commandDispatcher);
    }

    @Override
    public String getName() {
        return "tbrecovery";
    }

    @Override
    LiteralArgumentBuilder<CommandSource> getBuilder(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(c -> showUsage(c.getSource()));
        builder.then(SubCommand.SAVE_ALL_PLAYERS.literal()
                .executes(c -> saveAllPlayers(c.getSource().getServer(), success -> sendMessage(c.getSource(), (success ? LangKey.MESSAGE_RECOVERY_SAVE_ALL_PLAYERS_SUCCESS : LangKey.MESSAGE_RECOVERY_SAVE_ALL_PLAYERS_FAILED).getText(), true))));
        builder.then(SubCommand.SAVE_PLAYER.literal()
                .executes(c -> showUsage(c.getSource()))
                .then(Commands.argument(PLAYER_PARAM, EntityArgument.player())
                        .executes(c -> {
                            ServerPlayerEntity player = EntityArgument.getPlayer(c, PLAYER_PARAM);
                            return savePlayer(player, success -> sendMessage(c.getSource(), (success ? LangKey.MESSAGE_RECOVERY_SAVE_PLAYER_SUCCESS : LangKey.MESSAGE_RECOVERY_SAVE_PLAYER_FAILED).getText(player.getName()), false));
                        })
                )
        );
        builder.then(SubCommand.LOAD_OFFLINE.literal()
                .executes(c -> showUsage(c.getSource()))
                .then(Commands.argument(UUID_PARAM, StringArgumentType.word()).suggests(SUGGESTION_UUID)
                        .executes(c -> {
                            UUID uuid = UUID.fromString(StringArgumentType.getString(c, UUID_PARAM));
                            GameProfile profil = c.getSource().getServer().getPlayerProfileCache().getProfileByUUID(uuid);
                            if (profil == null) {
                                throw EntityArgument.ONLY_PLAYERS_ALLOWED.create();
                            }
                            if (c.getSource().getServer().getPlayerList().getPlayerByUUID(uuid) != null) {
                                throw LangKey.MESSAGE_PLAYER_ONLINE.asCommandException();
                            }
                            return recoverPlayerOffline(c.getSource(), profil, ".latest");
                        })
                        .then(Commands.argument("file_string", StringArgumentType.word()).suggests(SUGGESTION_LOAD_OFFLINE)
                                .executes(c -> {
                                    UUID uuid = UUID.fromString(StringArgumentType.getString(c, UUID_PARAM));
                                    GameProfile profil = c.getSource().getServer().getPlayerProfileCache().getProfileByUUID(uuid);
                                    if (profil == null) {
                                        throw EntityArgument.ONLY_PLAYERS_ALLOWED.create();
                                    }
                                    return recoverPlayerOffline(c.getSource(), profil, StringArgumentType.getString(c, "file_string"));
                                })
                        )
                )
        );
        builder.then(SubCommand.LOAD_PLAYER.literal()
                .executes(c -> showUsage(c.getSource()))
                .then(Commands.argument(PLAYER_PARAM, EntityArgument.player())
                        .executes(c -> loadPlayer(c.getSource(), EntityArgument.getPlayer(c, PLAYER_PARAM), ".latest"))
                        .then(Commands.argument("file_string", StringArgumentType.word()).suggests(SUGGESTION_LOAD_PLAYER)
                                .executes(c -> loadPlayer(c.getSource(), EntityArgument.getPlayer(c, PLAYER_PARAM), StringArgumentType.getString(c, "file_string")))
                        )
                )
        );
        return builder;
    }

    public static int saveAllPlayers(MinecraftServer server, Consumer<Boolean> consumer) {
        File baseFolder = server.func_240776_a_(SAVE_FOLDER).toFile();
        if (!baseFolder.exists() && !baseFolder.mkdirs()) {
            ModTombstone.LOGGER.info("The backup folder for players cannot be created");
            consumer.accept(false);
            return 1;
        }
        List<Pair<CompoundNBT, File>> pairs = new ArrayList<>();
        boolean hasPlayerSkipped = false;
        for (ServerPlayerEntity player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            File playerFolder = getPlayerFolder(player);
            if (playerFolder == null) {
                hasPlayerSkipped = true;
                continue;
            }
            pairs.add(Pair.of(player.writeWithoutTypeId(new CompoundNBT()), playerFolder));
        }
        if (pairs.size() == 0) {
            consumer.accept(true);
            return 1;
        }
        final boolean skipped = hasPlayerSkipped;
        ThreadedBackup.INSTANCE.queueBackup(() -> {
            boolean wasSuccessful = !skipped;
            for (Pair<CompoundNBT, File> pair : pairs) {
                boolean success = savePlayerData(pair.getLeft(), pair.getRight());
                if (!success) {
                    wasSuccessful = false;
                }
            }
            final boolean isSuccessful = wasSuccessful;
            server.deferTask(() -> consumer.accept(isSuccessful));
            return false;
        });
        return 1;
    }

    public static int savePlayer(ServerPlayerEntity player, Consumer<Boolean> consumer) {
        checkAlive(player);
        checkNotSpectator(player);

        File playerFolder = getPlayerFolder(player);
        if (playerFolder == null) {
            consumer.accept(false);
            return 1;
        }
        ThreadTaskExecutor listener = player.server;
        CompoundNBT tag = player.writeWithoutTypeId(new CompoundNBT());
        ThreadedBackup.INSTANCE.queueBackup(() -> {
            boolean result = savePlayerData(tag, playerFolder);
            listener.deferTask(() -> consumer.accept(result));
            return false;
        });
        return 1;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static boolean savePlayerData(CompoundNBT tag, File saveFolder) {
        String dateString = new SimpleDateFormat("yyyyMMdd-hhmmss", Locale.US).format(new Date());
        try (FileWriter writer = new FileWriter(new File(saveFolder, dateString + ".save"))) {
            writer.write(tag.toString());
            writer.close();
            /* delete the oldest ones */
            File[] matchingFiles = saveFolder.listFiles((file, name) -> name.endsWith(".save"));
            if (matchingFiles != null && matchingFiles.length > ConfigTombstone.recovery.recoveryPlayerMaxSaves.get()) {
                int diff = matchingFiles.length - ConfigTombstone.recovery.recoveryPlayerMaxSaves.get();
                Arrays.sort(matchingFiles, Comparator.comparingLong(File::lastModified));
                int num = 0;
                for (File file : matchingFiles) {
                    if (num >= diff) {
                        break;
                    }
                    file.delete();
                    num++;
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static @Nullable
    File getPlayerFolder(ServerPlayerEntity player) {
        File saveFolder = new File(player.server.func_240776_a_(SAVE_FOLDER).toFile(), player.getUniqueID() + "");
        if (!saveFolder.exists() && !saveFolder.mkdirs()) {
            ModTombstone.LOGGER.info("The backup folder cannot be created");
            return null;
        }
        return saveFolder;
    }

    private int recoverPlayerOffline(CommandSource sender, GameProfile profil, String fileString) {
        PlayerList playerList = sender.getServer().getPlayerList();
        ServerPlayerEntity player = playerList.createPlayerForUser(profil);
        try (BufferedReader reader = new BufferedReader(new FileReader(getBackupFile(sender, profil.getId(), fileString)))) {
            CompoundNBT nbt = JsonToNBT.getTagFromJson(reader.readLine());
            reader.close();
            if (!nbt.keySet().isEmpty()) {
                player.read(nbt);
                player.server.getPlayerList().writePlayerData(player);
                sendMessage(sender, LangKey.MESSAGE_RECOVERY_LOAD_PLAYER_SUCCESS.getText(player.getName()), false);
            }
        } catch (Exception e) {
            throw LangKey.MESSAGE_RECOVERY_LOAD_PLAYER_FAILED.asCommandException(player.getName());
        }
        return 1;
    }

    private int loadPlayer(CommandSource sender, ServerPlayerEntity player, String fileString) {
        checkAlive(player);
        checkNotSpectator(player);

        File file = getBackupFile(sender, player.getUniqueID(), fileString);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            CompoundNBT nbt = JsonToNBT.getTagFromJson(reader.readLine());
            reader.close();
            if (!nbt.keySet().isEmpty()) {
                DimensionType sourceDim = player.world.dimension.getType();
                DimensionType targetDim = DimensionType.getById(nbt.getInt("Dimension"));
                if (targetDim == null) {
                    targetDim = DimensionType.OVERWORLD;
                }
                ListNBT pos = nbt.getList("Pos", 6);
                double x = pos.getDouble(0);
                double y = pos.getDouble(1);
                double z = pos.getDouble(2);
                ListNBT rot = nbt.getList("Rotation", 5);
                float yaw = rot.getFloat(0);
                float pitch = rot.getFloat(1);
                nbt.remove("Dimension");
                nbt.remove("Pos");
                nbt.remove("Rotation");
                player.deserializeNBT(nbt);
                player.teleport(sender.getServer().getWorld(targetDim), x, y, z, yaw, pitch);
                if (sourceDim == targetDim) {
                    player.connection.sendPacket(new SPlayerAbilitiesPacket(player.abilities));
                    for (EffectInstance potioneffect : player.getActivePotionEffects()) {
                        player.connection.sendPacket(new SPlayEntityEffectPacket(player.getEntityId(), potioneffect));
                    }
                }
                LangKey.MESSAGE_RECOVERY_LOAD_PLAYER_TARGET_SUCCESS.sendMessage(player, StyleType.MESSAGE_SPELL);
                sendMessage(sender, LangKey.MESSAGE_RECOVERY_LOAD_PLAYER_SUCCESS.getText(player.getName()), false);
            } else {
                throw LangKey.MESSAGE_RECOVERY_LOAD_PLAYER_FAILED.asCommandException(player.getName());
            }
        } catch (Exception e) {
            //e.printStackTrace();
            throw LangKey.MESSAGE_RECOVERY_LOAD_PLAYER_FAILED.asCommandException(player.getName());
        }
        return 1;
    }

    private File getBackupFile(CommandSource sender, UUID id, String fileString) {
        File saveFolder = new File (sender.getServer().func_240776_a_(SAVE_FOLDER).toFile(), id + "");
        if (!saveFolder.exists()) {
            throw LangKey.MESSAGE_RECOVERY_NO_FOLDER.asCommandException(saveFolder.getAbsolutePath());
        }
        String fileName;
        if (fileString.equals(".latest") || fileString.equals(".oldest")) {
            File[] saveFiles = saveFolder.listFiles(p -> p.isFile() && p.getName().endsWith(".save"));
            if (saveFiles == null || saveFiles.length == 0) {
                throw LangKey.MESSAGE_RECOVERY_NO_FILE.asCommandException(fileString);
            }
            Optional<File> res = fileString.equals(".latest") ? Stream.of(saveFiles).max(File::compareTo) : Stream.of(saveFiles).min(File::compareTo);
            fileName = res.get().getName();
        } else {
            fileName = fileString + ".save";
            File[] files = saveFolder.listFiles(p -> p.isFile() && p.getName().equals(fileName));
            if (files == null || files.length == 0) {
                throw LangKey.MESSAGE_RECOVERY_NO_FILE.asCommandException(fileName);
            }
        }
        return new File(saveFolder, fileName);
    }

    private SuggestionProvider<CommandSource> SUGGESTION_UUID = (ctx, build) -> {
        File checkedDir = ctx.getSource().getServer().func_240776_a_(SAVE_FOLDER).toFile();
        File[] uuidDirs = checkedDir.listFiles((file, name) -> {
            boolean valid = file.isDirectory() && name.contains("-");
            if (valid) {
                UUID uuid = UUID.fromString(name);
                if (ctx.getSource().getServer().getPlayerList().getPlayerByUUID(uuid) == null) {
                    return ctx.getSource().getServer().getPlayerProfileCache().getProfileByUUID(uuid) != null;
                }
            }
            return false;
        });
        return ISuggestionProvider.suggest(uuidDirs == null ? new ArrayList<>() : Arrays.stream(uuidDirs).map(File::getName).collect(Collectors.toList()), build);
    };

    private SuggestionProvider<CommandSource> SUGGESTION_LOAD_OFFLINE = (ctx, build) -> {
        List<String> list = new ArrayList<>();
        File checkedFile = new File(ctx.getSource().getServer().func_240776_a_(SAVE_FOLDER).toFile(), UUID.fromString(StringArgumentType.getString(ctx, "uuid")) + "");
        if (checkedFile.exists()) {
            File[] matchingFiles = checkedFile.listFiles((file, name) -> name.endsWith(".save"));
            if (matchingFiles != null) {
                list = Arrays.stream(matchingFiles).map(p -> p.getName().replace(".save", "")).collect(Collectors.toList());
                list.add(".latest");
                list.add(".oldest");
            }
        }
        return ISuggestionProvider.suggest(list, build);
    };

    private SuggestionProvider<CommandSource> SUGGESTION_LOAD_PLAYER = (ctx, build) -> {
        ServerPlayerEntity player = EntityArgument.getPlayer(ctx, "player");
        List<String> list = new ArrayList<>();
        File checkedFile = new File(ctx.getSource().getServer().func_240776_a_(SAVE_FOLDER).toFile(), player.getUniqueID() + "");
        if (checkedFile.exists()) {
            File[] matchingFiles = checkedFile.listFiles((file, name) -> name.endsWith(".save"));
            if (matchingFiles != null) {
                list = Arrays.stream(matchingFiles).map(p -> p.getName().replace(".save", "")).collect(Collectors.toList());
                list.add(".latest");
                list.add(".oldest");
            }
        }
        return ISuggestionProvider.suggest(list, build);
    };

    private enum SubCommand implements ISubCommand {SAVE_ALL_PLAYERS, SAVE_PLAYER, LOAD_PLAYER, LOAD_OFFLINE}
    private static final FolderName SAVE_FOLDER = new FolderName(MOD_ID + "/saved_players");
}

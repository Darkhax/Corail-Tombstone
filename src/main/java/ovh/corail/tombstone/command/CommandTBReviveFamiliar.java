package ovh.corail.tombstone.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.items.CapabilityItemHandler;
import ovh.corail.tombstone.ModTombstone;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.helper.ThreadedBackup;
import ovh.corail.tombstone.registry.ModItems;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class CommandTBReviveFamiliar extends TombstoneCommand {

    public CommandTBReviveFamiliar(CommandDispatcher<CommandSource> commandDispatcher) {
        super(commandDispatcher);
    }

    @Override
    String getName() {
        return "tbrevivefamiliar";
    }

    @Override
    LiteralArgumentBuilder<CommandSource> getBuilder(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(c -> showUsage(c.getSource()));
        builder.then(Commands.argument(PLAYER_PARAM, EntityArgument.player())
                .executes(c -> reviveFamiliar(c.getSource(), EntityArgument.getPlayer(c, PLAYER_PARAM)))
        );
        return builder;
    }

    private int reviveFamiliar(CommandSource sender, ServerPlayerEntity player) {
        checkAlive(player);
        checkNotSpectator(player);

        CompoundNBT petTag = getLastSave(sender, player);
        ItemStack receptacle;
        if (petTag != null) {
            receptacle = new ItemStack(ModItems.familiar_receptacle);
            receptacle.getOrCreateTag().put("dead_pet", petTag);
        } else {
            // checks the player's inventory for receptacle
            receptacle = player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).map(invent -> IntStream.range(0, invent.getSlots()).mapToObj(invent::getStackInSlot)
                    .filter(ModItems.familiar_receptacle::containSoul).findFirst().orElse(ItemStack.EMPTY)).orElse(ItemStack.EMPTY);
        }
        if (receptacle.isEmpty()) {
            throw LangKey.MESSAGE_NO_SAVE_TO_RESTORE.asCommandException();
        }
        boolean success = ModItems.familiar_receptacle.revive(player, player.getPosition(), receptacle);
        LangKey langKey = success ? LangKey.MESSAGE_BRING_BACK_TO_LIFE : LangKey.MESSAGE_CANT_REVIVE_FAMILIAR;
        player.sendMessage(langKey.getText(success ? StyleType.MESSAGE_SPELL : StyleType.COLOR_OFF, LangKey.MESSAGE_YOUR_FAMILIAR.getText()), Util.DUMMY_UUID);
        if (!player.equals(sender.getEntity())) {
            sendMessage(sender, langKey.getText(success ? StyleType.MESSAGE_SPELL : StyleType.COLOR_OFF, LangKey.MESSAGE_FAMILIAR_OF.getText(player.getName())), false);
        }
        receptacle.shrink(1);
        player.container.detectAndSendChanges();
        return 1;
    }

    @Nullable
    private CompoundNBT getLastSave(CommandSource sender, PlayerEntity player) {
        File saveFolder = new File(sender.getServer().func_240776_a_(SAVE_FOLDER).toFile(), player.getUniqueID().toString());
        if (!saveFolder.exists()) {
            return null;
        }
        File[] saveFiles = saveFolder.listFiles(p -> p.isFile() && p.getName().endsWith(".save"));
        if (saveFiles == null || saveFiles.length == 0) {
            return null;
        }
        File file = Stream.of(saveFiles).max(File::compareTo).orElse(null);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            CompoundNBT nbt = JsonToNBT.getTagFromJson(reader.readLine());
            reader.close();
            if (!nbt.keySet().isEmpty()) {
                file.delete();
                return nbt;
            }
        } catch (CommandSyntaxException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void saveFamiliar(MinecraftServer server, UUID ownerId, CompoundNBT tag, String saveName) {
        File saveFolder = new File(server.func_240776_a_(SAVE_FOLDER).toFile(), ownerId.toString());
        if (!saveFolder.exists() && !saveFolder.mkdirs()) {
            ModTombstone.LOGGER.info("The backup folder for familiars cannot be created");
            return;
        }
        final String dateString = new SimpleDateFormat("yyyyMMdd-hhmmss", Locale.US).format(new Date());
        final File accessedFile = new File(saveFolder, dateString + "_" + saveName + ".save");
        ThreadedBackup.INSTANCE.queueBackup(() -> {
            try (FileWriter writer = new FileWriter(accessedFile)) {
                writer.write(tag.toString());
                writer.close();
                /* delete the oldest ones */
                File[] matchingFiles = saveFolder.listFiles((file, name) -> name.endsWith(".save"));
                if (matchingFiles != null && matchingFiles.length > 5) {
                    int diff = matchingFiles.length - 5;
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
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        });
    }

    private static final FolderName SAVE_FOLDER = new FolderName(MOD_ID + "/saved_familiars");
}

package ovh.corail.tombstone.item;

import net.minecraft.block.BlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.command.CommandTBAcceptTeleport;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.NBTStackHelper;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.registry.ModItems;
import ovh.corail.tombstone.registry.ModTriggers;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Stream;

public class ItemTabletOfAssistance extends ItemTablet {

    public ItemTabletOfAssistance() {
        super("tablet_of_assistance", SharedConfigTombstone.allowed_magic_items.allowTabletOfAssistance::get);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        if (list.size() > 0) {
            list.get(0).getStyle().mergeStyle(StyleType.MESSAGE_SPECIAL);
        }
        if (Helper.canShowTooltip(world, stack)) {
            if (!isEnchanted(stack)) {
                addItemDesc(list);
                addItemUse(list, "1");
            } else {
                String engraved_name = getEngravedName(stack);
                if (engraved_name.isEmpty()) {
                    addItemDesc(list);
                    addItemUse(list, "2", Helper.getFormattingCode(StyleType.TOOLTIP_ITEM) + "[" + I18n.format(ModItems.grave_dust.getTranslationKey(stack)).toLowerCase() + "]");
                } else {
                    addInfo(list, LangKey.MESSAGE_ENGRAVED, '"' + engraved_name + '"');
                    addItemUse(list, "3");
                }
            }
        }
        super.addInformation(stack, world, list, flag);
    }

    @Override
    public boolean isEnchanted(ItemStack stack) {
        return stack.getItem() == this && NBTStackHelper.getBoolean(stack, ENCHANT_NBT_BOOL);
    }

    @Override
    public boolean setEnchant(World world, BlockPos gravePos, PlayerEntity player, ItemStack stack) {
        if (stack.getItem() != this) {
            return false;
        }
        NBTStackHelper.setBoolean(stack, ENCHANT_NBT_BOOL, true);
        setUseCount(stack, getUseMax());
        return true;
    }

    public boolean setEngravedName(ItemStack stack, String engraved_name) {
        if (isEnchanted(stack)) {
            NBTStackHelper.setString(stack, "engraved_name", engraved_name);
            return true;
        }
        return false;
    }

    public String getEngravedName(ItemStack stack) {
        return isEnchanted(stack) ? NBTStackHelper.getString(stack, "engraved_name") : "";
    }

    @Override
    protected boolean doEffects(World world, ServerPlayerEntity player, ItemStack stack) {
        String engraved_name = NBTStackHelper.getString(stack, "engraved_name");
        if (engraved_name.equals(player.getName().getUnformattedComponentText())) {
            LangKey.MESSAGE_TELEPORT_SAME_PLAYER.sendMessage(player);
            return false;
        }
        assert world.getServer() != null;
        if (Stream.of(world.getServer().getOnlinePlayerNames()).noneMatch(p -> p.equals(engraved_name))) {
            if (world.getServer().getPlayerProfileCache().usernameToProfileEntryMap.keySet().contains(engraved_name)) {
                LangKey.MESSAGE_PLAYER_OFFLINE.sendMessage(player);
            } else {
                LangKey.MESSAGE_PLAYER_INVALID.sendMessage(player);
            }
            return false;
        }
        ServerPlayerEntity receiver = world.getServer().getPlayerList().getPlayerByUsername(engraved_name);
        if (receiver == null) {
            LangKey.MESSAGE_TELEPORT_FAILED.sendMessage(player);
            return false;
        }
        if (!receiver.world.getDimensionKey().equals(player.world.getDimensionKey()) && !ConfigTombstone.general.teleportDim.get()) {
            LangKey.MESSAGE_TELEPORT_SAME_DIMENSION.sendMessage(player);
            return false;
        }
        ITextComponent hereClick = LangKey.createComponentCommand("/tbacceptteleport " + player.getUniqueID(), LangKey.MESSAGE_HERE);
        LangKey.MESSAGE_REQUEST_TO_JOIN_RECEIVER.sendMessage(receiver, StyleType.MESSAGE_SPECIAL, hereClick, player.getName());
        CommandTBAcceptTeleport.addTicket(receiver, player, 120);
        LangKey.MESSAGE_REQUEST_TO_JOIN_SENDER.sendMessage(player, receiver.getName());
        ModTriggers.USE_ASSISTANCE.trigger(player);
        return true;
    }

    @Override
    protected boolean canBlockInteractFirst(BlockState state, ItemStack stack) {
        return super.canBlockInteractFirst(state, stack) || state.getBlock().isIn(BlockTags.ANVIL);
    }
}

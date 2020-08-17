package ovh.corail.tombstone.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import ovh.corail.tombstone.command.CommandTBTeleportHome;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.helper.CallbackHandler;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.Location;
import ovh.corail.tombstone.helper.NBTStackHelper;
import ovh.corail.tombstone.helper.SpawnHelper;
import ovh.corail.tombstone.registry.ModTriggers;

import javax.annotation.Nullable;
import java.util.List;

public class ItemTabletOfHome extends ItemTablet {

    public ItemTabletOfHome() {
        super("tablet_of_home", SharedConfigTombstone.allowed_magic_items.allowTabletOfHome::get);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        if (Helper.canShowTooltip(world, stack)) {
            addItemDesc(list);
            boolean isAncient = isAncient(stack);
            if (isAncient) {
                addItemDesc(list, "_ancient");
            }
            addItemUse(list, !isEnchanted(stack) ? "1" : "2");
            addInfoInBeta(list);
        }
        super.addInformation(stack, world, list, flag);
    }

    @Override
    protected boolean doEffects(World world, ServerPlayerEntity player, ItemStack stack) {
        MinecraftServer server = player.getServer();
        assert server != null;
        Pair<ServerWorld, BlockPos> respawnPoint = CommandTBTeleportHome.getRespawnPoint(server, player);
        if (!ConfigTombstone.general.teleportDim.get() && !respawnPoint.getLeft().func_234923_W_().equals(world.func_234923_W_())) {
            LangKey.MESSAGE_TELEPORT_SAME_DIMENSION.sendMessage(player);
            return false;
        }
        Location location = new SpawnHelper(respawnPoint.getLeft(), respawnPoint.getRight()).findSpawnPlace(false);
        if (location.isOrigin()) {
            LangKey.MESSAGE_TELEPORT_FAILED.sendMessage(player);
            return false;
        }
        CallbackHandler.addCallback(1, () -> {
            boolean isAncient = isAncient(stack);
            AxisAlignedBB area = isAncient ? player.getBoundingBox().grow(3d, 0d, 3d) : null;
            ServerPlayerEntity newPlayer = Helper.teleportEntity(player, location);
            if (isAncient) {
                List<Entity> entities = world.getEntitiesWithinAABBExcludingEntity(player, area);
                entities.forEach(entity -> Helper.teleportEntity(entity, new Location(newPlayer)));
            }
            LangKey.MESSAGE_TELEPORT_SUCCESS.sendMessage(newPlayer);
            ModTriggers.USE_HOME.trigger(player);
        });
        return true;
    }

    @Override
    public boolean isEnchanted(ItemStack stack) {
        return NBTStackHelper.getBoolean(stack, ENCHANT_NBT_BOOL);
    }

    @Override
    public boolean setEnchant(World world, BlockPos gravePos, PlayerEntity player, ItemStack stack) {
        NBTStackHelper.setBoolean(stack, ENCHANT_NBT_BOOL, true);
        setUseCount(stack, getUseMax());
        return true;
    }
}

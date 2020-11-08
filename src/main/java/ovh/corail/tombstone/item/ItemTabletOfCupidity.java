package ovh.corail.tombstone.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.LockableLootTileEntity;
import net.minecraft.tileentity.MobSpawnerTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.helper.CallbackHandler;
import ovh.corail.tombstone.helper.EffectHelper;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.Location;
import ovh.corail.tombstone.helper.NBTStackHelper;
import ovh.corail.tombstone.helper.SpawnHelper;
import ovh.corail.tombstone.registry.ModTriggers;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class ItemTabletOfCupidity extends ItemTablet {
    public ItemTabletOfCupidity() {
        super("tablet_of_cupidity", SharedConfigTombstone.allowed_magic_items.allowTabletOfCupidity::get);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        if (Helper.canShowTooltip(world, stack)) {
            addItemDesc(list);
            addItemUse(list, isEnchanted(stack) ? "2" : "1");
            addInfoInBeta(list);
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
        setUseCount(stack, getUseMax());
        NBTStackHelper.setBoolean(stack, ENCHANT_NBT_BOOL, true);
        return true;
    }

    @Override
    protected boolean doEffects(World world, ServerPlayerEntity player, ItemStack stack) {
        TileEntity tile = Helper.getRandomInList(((ServerWorld) world).loadedTileEntityList.stream().filter(aTile -> aTile instanceof MobSpawnerTileEntity || (aTile instanceof ChestTileEntity && ((LockableLootTileEntity) aTile).lootTable != null)).collect(Collectors.toList()));
        if (tile != null) {
            // ensure the position can be accessed
            if (Helper.isValidPos(world, tile.getPos())) {
                Location spawnPos = new SpawnHelper((ServerWorld) world, tile.getPos()).findSpawnPlace(false);
                if (!spawnPos.isOrigin()) {
                    CallbackHandler.addCallback(1, () -> {
                        ServerPlayerEntity newPlayer = Helper.teleportEntity(player, spawnPos);
                        LangKey.MESSAGE_TELEPORT_SUCCESS.sendMessage(newPlayer);
                        EntityHelper.addAlignment(newPlayer, ConfigTombstone.alignment.pointsTabletOfCupidity.get(), ConfigTombstone.alignment.chanceTabletOfCupidity.get());
                        EffectHelper.addRandomEffect(newPlayer, 1200, true, true);
                        ModTriggers.USE_CUPIDITY.trigger(newPlayer);
                    });
                    return true;
                }
            }
        }
        LangKey.MESSAGE_TABLET_SEARCH_FAILED.sendMessage(player);
        return true; // to consume uses
    }

    @Override
    public int getCastingCooldown() {
        return 18000;
    }
}

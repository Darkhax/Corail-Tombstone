package ovh.corail.tombstone.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.ItemHandlerHelper;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.helper.CallbackHandler;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.Location;
import ovh.corail.tombstone.helper.NBTStackHelper;
import ovh.corail.tombstone.helper.SpawnHelper;
import ovh.corail.tombstone.helper.StyleType;
import ovh.corail.tombstone.helper.SupportStructures;
import ovh.corail.tombstone.helper.TimeHelper;
import ovh.corail.tombstone.registry.ModItems;
import ovh.corail.tombstone.registry.ModPerks;
import ovh.corail.tombstone.registry.ModTriggers;

import javax.annotation.Nullable;
import java.util.List;

public class ItemLostTablet extends ItemGraveMagic {
    public ItemLostTablet() {
        super("lost_tablet", SharedConfigTombstone.allowed_magic_items.allowLostTablet::get);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag) {
        if (Helper.canShowTooltip(world, stack)) {
            Location location = getStructurePos(stack);
            String id = location.isOrigin() ? "1" : (isEnchanted(stack) ? "3" : "2");
            addItemDesc(list, id);
            if (!location.isOrigin()) {
                String structureRL = getStructureId(stack);
                if (structureRL != null) {
                    list.add(new TranslationTextComponent(SupportStructures.getStructureName(structureRL)));
                }
                addItemPosition(list, location);
            }
            addItemUse(list, id);
        }
        super.addInformation(stack, world, list, flag);
    }

    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slotId, boolean isSelected) {
        if (!world.isRemote && stack.getItem() == this && EntityHelper.isValidPlayer(entity) && !isWakeUp(stack)) {
            int cd = getCooldown(world, stack);
            if (cd <= 0) {
                setCooldown(world, stack, TimeHelper.tickFromSecond(Helper.getRandom(500, 800)));
            } else if (cd == 1) {
                ServerPlayerEntity player = (ServerPlayerEntity) entity;
                boolean success = wakeUpMagic(player, stack);
                if (success) {
                    LangKey.MESSAGE_LOST_TABLET_WAKE_UP_SUCCESS.sendMessage(player, StyleType.MESSAGE_SPELL);
                } else {
                    if (Helper.random.nextBoolean()) {
                        setCooldown(world, stack, TimeHelper.tickFromSecond(Helper.getRandom(1500, 1800)));
                    } else {
                        LangKey.MESSAGE_LOST_TABLET_WAKE_UP_FAILED.sendMessage(player, StyleType.MESSAGE_SPELL);
                        stack.shrink(1);
                        ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModItems.grave_dust, Helper.getRandom(3, 5)));
                        player.container.detectAndSendChanges();
                    }
                }
            }
        }
    }

    @Override
    public int getCooldown(@Nullable World world, ItemStack stack) {
        if (world != null && stack.getItem() == this) {
            long cooldown_time = NBTStackHelper.getLong(stack, COOLDOWN_TIME_NBT_LONG, 0L);
            if (cooldown_time > 0) {
                int cd = (int) (cooldown_time - TimeHelper.worldTicks(world));
                if (cd > 1800) { // invalid cooldown
                    setCooldown(world, stack, 1800);
                    return 1800;
                }
                return Math.max(cd, 0);
            }
        }
        return 0;
    }

    private boolean wakeUpMagic(ServerPlayerEntity player, ItemStack stack) {
        String structureRLString;
        if (Helper.getRandom(1, 100) < (60 - (EntityHelper.getPerkLevelWithBonus(player, ModPerks.treasure_seeker) * 10))) {
            structureRLString = SupportStructures.VILLAGE.getId();
        } else {
            if (ConfigTombstone.decorative_grave.lostTabletSearchModdedStructure.get()) {
                structureRLString = SupportStructures.getRandomStructure(p -> !SupportStructures.VILLAGE.is(p) && !Helper.containRL(ConfigTombstone.decorative_grave.lostTabletDeniedStructures.get(), p));
            } else {
                structureRLString = SupportStructures.getRandomVanillaStructure(p -> !SupportStructures.VILLAGE.is(p));
            }
        }
        if (structureRLString == null) {
            structureRLString = SupportStructures.VILLAGE.getId();
        }
        ServerWorld world = player.getServerWorld();

        Structure<?> structure = SupportStructures.getStructure(structureRLString);
        boolean noStructureInCurrentWorld = !SupportStructures.hasStructureInWorld(world, structure);
        if (noStructureInCurrentWorld || Helper.random.nextFloat() < 0.3f) {
            if (ConfigTombstone.decorative_grave.lostTabletSearchOutsideWorld.get()) {
                // search in a different world
                RegistryKey<World> dimension = Helper.getRandomInList(SupportStructures.getDimensionTypesForStructure(world.getServer(), structure));
                if (dimension != null) {
                    world = world.getServer().getWorld(dimension);
                } else if (noStructureInCurrentWorld) {
                    return false;
                }
            } else if (noStructureInCurrentWorld) {
                return false;
            }
        }
        int radius = 5000;
        Location location = Helper.findNearestStructure(world, new BlockPos(player.getPosX() + Helper.random.nextGaussian() * radius, SupportStructures.getY(structureRLString), player.getPosZ() + Helper.random.nextGaussian() * radius), structureRLString, true);
        if (location.isOrigin() || !Helper.isValidPos(world, location.getPos())) {
            return false;
        }
        NBTStackHelper.setLocation(stack, STRUCTURE_POS_NBT_LOCATION, location);
        NBTStackHelper.setString(stack, STRUCTURE_ID_NBT_STRING, structureRLString);
        ModTriggers.FIND_LOST_TABLET.trigger(player);
        return true;
    }

    @Nullable
    public String getStructureId(ItemStack stack) {
        if (stack.getItem() == this) {
            CompoundNBT tag = stack.getTag();
            if (tag != null) {
                String structureId = NBTStackHelper.getString(stack, STRUCTURE_ID_NBT_STRING);
                if (!structureId.isEmpty()) {
                    return structureId;
                } else if (tag.contains("structureType", Constants.NBT.TAG_INT)) {
                    // TODO retrocompat to remove later
                    int ordinal = NBTStackHelper.getInteger(stack, "structureType");
                    if (ordinal < SupportStructures.values().length) {
                        structureId = SupportStructures.values()[ordinal].getId();
                        NBTStackHelper.setString(stack, STRUCTURE_ID_NBT_STRING, structureId);
                        return structureId;
                    }
                }
            }
        }
        return null;
    }

    public boolean isWakeUp(ItemStack stack) {
        return getStructureId(stack) != null;
    }

    @Override
    public boolean canEnchant(World world, BlockPos gravePos, PlayerEntity player, ItemStack stack) {
        return super.canEnchant(world, gravePos, player, stack) && isWakeUp(stack);
    }

    @Override
    public boolean isEnchanted(ItemStack stack) {
        return isWakeUp(stack) && NBTStackHelper.getBoolean(stack, ENCHANT_NBT_BOOL);
    }

    @Override
    public boolean setEnchant(World world, BlockPos gravePos, PlayerEntity player, ItemStack stack) {
        if (!isWakeUp(stack)) {
            return false;
        }
        NBTStackHelper.setBoolean(stack, ENCHANT_NBT_BOOL, true);
        return true;
    }

    @Override
    protected boolean doEffects(World world, ServerPlayerEntity player, ItemStack stack) {
        Location location = getStructurePos(stack);
        String structureId = getStructureId(stack);
        if (structureId == null || location.isOrigin() || Helper.isInvalidDimension(location.dim)) {
            LangKey.MESSAGE_TELEPORT_FAILED.sendMessage(player);
            resetStack(world, stack);
            return false;
        }
        if (!ConfigTombstone.general.teleportDim.get() && !location.isSameDimension(world)) {
            LangKey.MESSAGE_TELEPORT_SAME_DIMENSION.sendMessage(player);
            return false;
        }
        DimensionType dimType = DimensionType.getById(location.dim);
        assert player.getServer() != null && dimType != null;
        ServerWorld targetWorld = player.getServer().getWorld(dimType);
        // TODO break tablets instead
        if (!Helper.isValidPos(targetWorld, location.getPos())) {
            LangKey.MESSAGE_TELEPORT_FAILED.sendMessage(player);
            resetStack(world, stack);
            return false;
        }
        Location spawnLoc = new SpawnHelper(targetWorld, location.getPos()).findPlaceInStructure(structureId);
        if (spawnLoc.isOrigin()) {
            LangKey.MESSAGE_NO_SPAWN.sendMessage(player);
            resetStack(world, stack);
            return false;
        }
        player.getCooldownTracker().setCooldown(this, 10);
        NBTStackHelper.removeKeyName(stack, ENCHANT_NBT_BOOL);
        NBTStackHelper.setLocation(stack, STRUCTURE_POS_NBT_LOCATION, spawnLoc);
        CallbackHandler.addCallback(1, () -> {
            PlayerEntity newPlayer = Helper.teleportEntity(player, spawnLoc);
            LangKey.MESSAGE_TELEPORT_SUCCESS.sendMessage(newPlayer);
            ModTriggers.USE_LOST_TABLET.trigger(player);
        });
        return true;
    }

    private void resetStack(World world, ItemStack stack) {
        setCooldown(world, stack, TimeHelper.tickFromMinute(10));
        NBTStackHelper.removeKeyName(stack, ENCHANT_NBT_BOOL);
        NBTStackHelper.removeLocation(stack, STRUCTURE_POS_NBT_LOCATION);
    }

    public Location getStructurePos(ItemStack stack) {
        if (stack.getItem() == this) {
            return NBTStackHelper.getLocation(stack, STRUCTURE_POS_NBT_LOCATION);
        }
        return Location.ORIGIN;
    }

    @Override
    public int getCastingCooldown() {
        return 0;
    }

    @Override
    public int getUseMax() {
        return 1;
    }

    @Override
    public boolean canConsumeOnUse() {
        return false;
    }

    private static final String STRUCTURE_ID_NBT_STRING = "structure_id";
    private static final String STRUCTURE_POS_NBT_LOCATION = "structurePos";
}

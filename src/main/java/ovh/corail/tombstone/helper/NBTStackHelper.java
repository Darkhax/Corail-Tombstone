package ovh.corail.tombstone.helper;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.Constants.NBT;
import ovh.corail.tombstone.ModTombstone;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

@SuppressWarnings({ "UnusedReturnValue", "WeakerAccess" })
public class NBTStackHelper {

    public static ItemStack setString(ItemStack stack, String keyName, String keyValue) {
        stack.getOrCreateTag().putString(keyName, keyValue);
        return stack;
    }

    public static String getString(ItemStack stack, String keyName) {
        CompoundNBT tag = stack.getTag();
        if (tag != null && tag.contains(keyName, NBT.TAG_STRING)) {
            return tag.getString(keyName);
        }
        return "";
    }

    public static ItemStack setBoolean(ItemStack stack, String keyName, boolean keyValue) {
        stack.getOrCreateTag().putBoolean(keyName, keyValue);
        return stack;
    }

    public static boolean getBoolean(ItemStack stack, String keyName) {
        CompoundNBT tag = stack.getTag();
        if (tag != null && tag.contains(keyName, NBT.TAG_BYTE)) {
            return tag.getBoolean(keyName);
        }
        return false;
    }

    public static ItemStack setInteger(ItemStack stack, String keyName, int keyValue) {
        stack.getOrCreateTag().putInt(keyName, keyValue);
        return stack;
    }

    public static int getInteger(ItemStack stack, String keyName) {
        CompoundNBT tag = stack.getTag();
        if (tag != null && tag.contains(keyName, NBT.TAG_INT)) {
            return tag.getInt(keyName);
        }
        return Integer.MIN_VALUE;
    }

    public static ItemStack setLong(ItemStack stack, String keyName, long keyValue) {
        stack.getOrCreateTag().putLong(keyName, keyValue);
        return stack;
    }

    @Deprecated
    public static long getLong(ItemStack stack, String keyName) {
        return getLong(stack, keyName, Long.MIN_VALUE);
    }

    public static long getLong(ItemStack stack, String keyName, long fallback) {
        CompoundNBT tag = stack.getTag();
        if (tag != null && tag.contains(keyName, NBT.TAG_LONG)) {
            return tag.getLong(keyName);
        }
        return fallback;
    }

    public static ItemStack setBlockPos(ItemStack stack, String keyName, BlockPos keyValue) {
        setBlockPos(stack.getOrCreateTag(), keyName, keyValue);
        return stack;
    }

    public static void setBlockPos(CompoundNBT tag, String keyName, BlockPos keyValue) {
        tag.putInt(keyName + "X", keyValue.getX());
        tag.putInt(keyName + "Y", keyValue.getY());
        tag.putInt(keyName + "Z", keyValue.getZ());
    }

    public static BlockPos getBlockPos(ItemStack stack, String keyName) {
        return getBlockPos(stack.getTag(), keyName);
    }

    public static BlockPos getBlockPos(@Nullable CompoundNBT tag, String keyName) {
        if (tag != null && tag.contains(keyName + "X", NBT.TAG_INT) && tag.contains(keyName + "Y", NBT.TAG_INT) && tag.contains(keyName + "Z", NBT.TAG_INT)) {
            return new BlockPos(tag.getInt(keyName + "X"), tag.getInt(keyName + "Y"), tag.getInt(keyName + "Z"));
        }
        return Location.ORIGIN_POS;
    }

    public static ItemStack setLocation(ItemStack stack, String keyName, Location location) {
        setLocation(stack.getOrCreateTag(), keyName, location);
        return stack;
    }

    public static CompoundNBT setLocation(CompoundNBT tag, String keyName, Location location) {
        setBlockPos(tag, keyName, location.getPos());
        setWorldKey(tag, keyName + "D", location.dim);
        return tag;
    }

    public static Location getLocation(ItemStack stack, String keyName) {
        return getLocation(stack.getTag(), keyName);
    }

    public static Location getLocation(@Nullable CompoundNBT tag, String keyName) {
        if (tag != null && tag.contains(keyName + "D")) {
            BlockPos pos = getBlockPos(tag, keyName);
            RegistryKey<World> worldKey = getWorldKey(tag, keyName + "D");
            if (!pos.equals(Location.ORIGIN_POS) && worldKey != null) {
                return new Location(pos, worldKey);
            }
        }
        return Location.ORIGIN;
    }

    public static ItemStack setWorldKey(ItemStack stack, String keyName, RegistryKey<World> worldKey) {
        setWorldKey(stack.getOrCreateTag(), keyName, worldKey);
        return stack;
    }

    public static CompoundNBT setWorldKey(CompoundNBT tag, String keyName, RegistryKey<World> worldKey) {
        World.field_234917_f_.encodeStart(NBTDynamicOps.INSTANCE, worldKey).resultOrPartial(ModTombstone.LOGGER::error).ifPresent(rl -> {
            tag.put(keyName, rl);
        });
        return tag;
    }

    @Nullable
    public static RegistryKey<World> getWorldKey(ItemStack stack, String keyName) {
        return getWorldKey(stack.getTag(), keyName);
    }

    @Nullable
    public static RegistryKey<World> getWorldKey(@Nullable CompoundNBT tag, String keyName) {
        return tag != null && tag.contains(keyName) ? World.field_234917_f_.parse(NBTDynamicOps.INSTANCE, tag.get(keyName)).result().orElse(null) : null;
    }

    public static boolean removeLocation(ItemStack stack, String keyName) {
        // TODO clean remove method
        boolean removed = NBTStackHelper.removeKeyName(stack, keyName + "X");
        removed = NBTStackHelper.removeKeyName(stack, keyName + "Y") || removed;
        removed = NBTStackHelper.removeKeyName(stack, keyName + "Z") || removed;
        return NBTStackHelper.removeKeyName(stack, keyName + "D") || removed;
    }

    public static boolean removeKeyName(ItemStack stack, String keyName) {
        CompoundNBT tag = stack.getTag();
        boolean removed = false;
        if (tag != null && tag.contains(keyName)) {
            removed = true;
            tag.remove(keyName);
            if (tag.isEmpty()) {
                stack.setTag(null);
            }
        }
        return removed;
    }

    public static List<EffectInstance> getEffectList(CompoundNBT tag, String keyName, Predicate<Effect> predic) {
        List<EffectInstance> effectInstances = new ArrayList<>();
        if (tag.contains(keyName, Constants.NBT.TAG_LIST)) {
            ListNBT effectList = tag.getList(keyName, NBT.TAG_COMPOUND);
            for (int i = 0; i < effectList.size(); i++) {
                EffectInstance effectInstance = getEffect(effectList.getCompound(i), predic);
                if (effectInstance != null) {
                    effectInstances.add(effectInstance);
                }
            }
        }
        return effectInstances;
    }

    public static void setEffectlist(CompoundNBT tag, String keyName, Stream<EffectInstance> effectInstances) {
        ListNBT effectList = new ListNBT();
        effectInstances.forEach(effectInstance -> effectList.add(setEffect(new CompoundNBT(), effectInstance)));
        tag.put(keyName, effectList);
    }

    public static CompoundNBT setEffect(CompoundNBT tag, EffectInstance effectInstance) {
        effectInstance.write(tag);
        return tag;
    }

    @Nullable
    public static EffectInstance getEffect(CompoundNBT tag, Predicate<Effect> predic) {
        EffectInstance effectInstance = null;
        if (tag.contains("ShowIcon", NBT.TAG_BYTE)) {
            effectInstance = EffectInstance.read(tag);
            if (predic.test(effectInstance.getPotion())) {
                return effectInstance;
            }
        }
        return null;
    }
}

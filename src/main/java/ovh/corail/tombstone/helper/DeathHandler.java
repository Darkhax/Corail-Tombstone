package ovh.corail.tombstone.helper;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import org.apache.commons.lang3.tuple.Pair;
import ovh.corail.tombstone.block.BlockGraveMarble.MarbleType;
import ovh.corail.tombstone.block.GraveModel;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.registry.ModEffects;
import ovh.corail.tombstone.registry.ModPerks;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Predicate;

import static ovh.corail.tombstone.ModTombstone.LOGGER;

@SuppressWarnings("WeakerAccess")
public class DeathHandler {
    public static final DeathHandler INSTANCE = new DeathHandler();
    public final String IS_PLAYER_DEAD_NBT_BOOL = "tb_is_player_dead";
    public final String PRESERVED_EFFECTS_NBT_LIST = "tb_preserved_effects";
    public final String LAST_DEATH_LOCATION_NBT_TAG = "tb_last_death_location";
    private final Map<UUID, Pair<GraveModel, MarbleType>> optionFavoriteGrave = new HashMap<>();
    private final Map<UUID, Boolean> optionEquipElytraInPriority = new HashMap<>();
    private final Map<UUID, Boolean> optionKnowledgeMessage = new HashMap<>();
    private final Map<UUID, Boolean> optionPriorizeToolOnHotbar = new HashMap<>();
    private final Map<UUID, Boolean> optionActivateGraveBySneaking = new HashMap<>();
    private final Map<String, Location> lastGraveList = new HashMap<>();
    private Predicate<Location> no_grave_locations = l -> false;

    private DeathHandler() {
    }

    public Location getLastGrave(String playerName) {
        return lastGraveList.getOrDefault(playerName, Location.ORIGIN);
    }

    public void removeGrave(Location pos) {
        Iterator<Entry<String, Location>> it = lastGraveList.entrySet().iterator();
        while (it.hasNext()) {
            Location gravePos = it.next().getValue();
            if (gravePos.equals(pos)) {
                it.remove();
            }
        }
    }

    public void logLastGrave(@Nullable PlayerEntity player, Location loc) {
        if (player == null) {
            return;
        }
        lastGraveList.put(player.getGameProfile().getName(), loc);
        if (ConfigTombstone.player_death.logPlayerGrave.get()) {
            LOGGER.info("A new grave of the player " + player.getGameProfile().getName() + " was created at position [x:" + loc.x + ", y:" + loc.y + ", z:" + loc.z + ", dim:" + loc.getDimString() + "]");
        }
    }

    public boolean isNoGraveLocation(Location location) {
        return this.no_grave_locations.test(location);
    }

    public void setLastDeathLocation(PlayerEntity player, Location location) {
        NBTStackHelper.setLocation(EntityHelper.getPersistentTag(player), LAST_DEATH_LOCATION_NBT_TAG, location);
    }

    public Location getLastDeathLocation(PlayerEntity player) {
        return NBTStackHelper.getLocation(EntityHelper.getPersistentTag(player), LAST_DEATH_LOCATION_NBT_TAG);
    }

    public void addPlayerDead(PlayerEntity player) {
        CompoundNBT persistentTag = EntityHelper.getPersistentTag(player);
        persistentTag.putBoolean(IS_PLAYER_DEAD_NBT_BOOL, true);
        setLastDeathLocation(player, new Location(player));
        if (ConfigTombstone.player_death.restoreEffectsOnDeath.get() || EffectHelper.isPotionActive(player, ModEffects.preservation)) {
            NBTStackHelper.setEffectlist(persistentTag, PRESERVED_EFFECTS_NBT_LIST, player.getActivePotionEffects().stream().filter(EffectHelper::isAllowedEffect));
        }
        persistentTag.putInt("tb_experience_total", EntityHelper.getPlayerTotalXp(player));
        persistentTag.putInt("tb_experience_level", player.experienceLevel);
        persistentTag.putFloat("tb_experience_bar", player.experience);
    }

    public void restorePlayerDead(PlayerEntity player) {
        if (!isPlayerDead(player)) {
            return;
        }
        if (!Helper.isDisabledPerk(ModPerks.ghostly_shape, player)) {
            EffectHelper.addEffect(player, ModEffects.ghostly_shape, SharedConfigTombstone.general.ghostlyShapeDuration.get() * 20, EntityHelper.getPerkLevelWithBonus(player, ModPerks.ghostly_shape));
        }
        CompoundNBT persistentTag = EntityHelper.getPersistentTag(player);
        persistentTag.remove(IS_PLAYER_DEAD_NBT_BOOL);
        List<EffectInstance> effectInstances = NBTStackHelper.getEffectList(persistentTag, PRESERVED_EFFECTS_NBT_LIST, EffectHelper::isAllowedEffect);
        if (!effectInstances.isEmpty()) {
            effectInstances.forEach(effectInstance -> EffectHelper.addEffect(player, effectInstance));
            persistentTag.remove(PRESERVED_EFFECTS_NBT_LIST);
        }
    }

    public boolean isPlayerDead(PlayerEntity player) {
        return EntityHelper.getPersistentTag(player).getBoolean(IS_PLAYER_DEAD_NBT_BOOL);
    }

    public DeathHandler setFavoriteGrave(UUID id, @Nullable GraveModel graveType, @Nullable MarbleType marbleType) {
        this.optionFavoriteGrave.put(id, Pair.of(graveType != null ? graveType : GraveModel.getDefault(), marbleType != null ? marbleType : MarbleType.getDefault()));
        return this;
    }

    public Pair<GraveModel, MarbleType> getFavoriteGrave(ServerPlayerEntity player) {
        return this.optionFavoriteGrave.getOrDefault(player.getUniqueID(), Pair.of(GraveModel.getDefault(), MarbleType.getDefault()));
    }

    public DeathHandler setOptionEquipElytraInPriority(UUID id, boolean value) {
        this.optionEquipElytraInPriority.put(id, value);
        return this;
    }

    public boolean getOptionEquipElytraInPriority(UUID id) {
        return this.optionEquipElytraInPriority.getOrDefault(id, false);
    }

    public DeathHandler setOptionKnowledgeMessage(UUID id, boolean value) {
        this.optionKnowledgeMessage.put(id, value);
        return this;
    }

    public boolean getOptionKnowledgeMessage(UUID id) {
        return this.optionKnowledgeMessage.getOrDefault(id, true);
    }

    public DeathHandler setOptionPriorizeToolOnHotbar(UUID id, boolean value) {
        this.optionPriorizeToolOnHotbar.put(id, value);
        return this;
    }

    public boolean getOptionPriorizeToolOnHotbar(UUID id) {
        return this.optionPriorizeToolOnHotbar.getOrDefault(id, false);
    }

    public DeathHandler setOptionActivateGraveBySneaking(UUID id, boolean value) {
        this.optionActivateGraveBySneaking.put(id, value);
        return this;
    }

    public boolean getOptionActivateGraveBySneaking(UUID id) {
        return this.optionActivateGraveBySneaking.getOrDefault(id, false);
    }

    public void updateNoGraveLocations() {
        List<Predicate<Location>> list = new ArrayList<>();
        for (String s : ConfigTombstone.player_death.noGraveLocation.get()) {
            if (!s.isEmpty()) {
                String[] res = s.split(",");
                if (res.length == 1) {
                    list.add(l -> l.isSameDimension(res[0].trim()));
                } else if (res.length == 5) {
                    int x, y, z, range;
                    try {
                        x = Integer.valueOf(res[0].trim());
                        y = Integer.valueOf(res[1].trim());
                        z = Integer.valueOf(res[2].trim());
                        range = Integer.valueOf(res[4].trim());
                    } catch (NumberFormatException e) {
                        LOGGER.warn("invalid number in noGraveLocations with provided string: " + s);
                        continue;
                    }
                    list.add(l -> l.isSameDimension(res[3].trim()) && l.isInRange(x, y, z, range));

                }
            }
        }
        this.no_grave_locations = list.stream().reduce(Predicate::or).orElse(l -> false);
    }

    public void clear() {
        this.optionFavoriteGrave.clear();
        this.optionEquipElytraInPriority.clear();
        this.optionKnowledgeMessage.clear();
        this.optionPriorizeToolOnHotbar.clear();
        this.optionActivateGraveBySneaking.clear();
        this.lastGraveList.clear();
        CooldownHandler.INSTANCE.clear();
    }
}
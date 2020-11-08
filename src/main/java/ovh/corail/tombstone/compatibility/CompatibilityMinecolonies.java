package ovh.corail.tombstone.compatibility;

import com.google.common.collect.ImmutableList;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.helper.EntityHelper;

import java.util.List;

public class CompatibilityMinecolonies {
    public static final CompatibilityMinecolonies instance = new CompatibilityMinecolonies();

    private CompatibilityMinecolonies() {
    }

    private static final List<String> RAIDERS = ImmutableList.of("barbarian", "archerbarbarian", "pirate", "archerpirate", "amazon", "shieldmaiden", "norsemenarcher", "archermummy", "pharao", "mummy", "chiefbarbarian", "chiefpirate", "amazonchief", "norsemenchief");

    public boolean applyKillResult(ServerPlayerEntity player, EntityType<?> type) {
        ResourceLocation typeRL = type.getRegistryName();
        if (SupportMods.MINECOLONIES.isLoaded() && typeRL != null && typeRL.getNamespace().equals(SupportMods.MINECOLONIES.getString())) {
            if (typeRL.getPath().equals("citizen")) {
                EntityHelper.addAlignment(player, ConfigTombstone.alignment.pointsKillVillager.get());
            } else if (RAIDERS.contains(typeRL.getPath())) {
                EntityHelper.addAlignment(player, ConfigTombstone.alignment.pointsKillRaider.get(), ConfigTombstone.alignment.chanceKillRaider.get());
            }
        }
        return false;
    }
}

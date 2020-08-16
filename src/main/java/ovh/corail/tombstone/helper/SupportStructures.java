package ovh.corail.tombstone.helper;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Streams;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.registries.ForgeRegistries;

public enum SupportStructures {
    VILLAGE("village", 68),
    DESERT_TEMPLE("desert_pyramid", 68),
    IGLOO_TEMPLE("igloo", 68),
    JUNGLE_TEMPLE("jungle_pyramid", 68),
    MANSION("mansion", 68),
    MONUMENT("monument", 68),
    STRONGHOLD("stronghold", 35),
    END_CITY("endcity", 68),
    MINESHAFT("mineshaft", 37),
    FORTRESS("fortress", 35),
    SWAMP_HUT("swamp_hut", 68),
    OCEAN_RUIN("ocean_ruin", 68),
    BURIED_TREASURE("buried_treasure", 68),
    SHIPWRECK("shipwreck", 68),
    PILLAGER_OUTPOST("pillager_outpost", 68);

    private final String structureName;
    private final int y;

    SupportStructures(String structureName, int y) {
        this.structureName = structureName;
        this.y = y;
    }

    public String getId() {
        return "minecraft:" + this.structureName;
    }

    public boolean is(String structureId) {
        Pair<String, String> rl = Helper.parseRLString(structureId);
        return "minecraft".equals(rl.getLeft()) && this.structureName.equals(rl.getRight());
    }

    public boolean is(ResourceLocation structureId) {
        return is(structureId.getNamespace(), structureId.getPath());
    }

    public boolean is(String domain, String path) {
        return "minecraft".equals(domain) && this.structureName.equals(path);
    }

    public int getY() {
        return this.y;
    }

    public static String getStructureNameForSearch(String structureId) {
        return structureId.startsWith("minecraft:") ? structureId.toLowerCase(Locale.ROOT).substring(10) : structureId;
    }

    @Nullable
    private static SupportStructures getVanillaStructure(String domain, String path) {
        return domain.equals("minecraft") ? Stream.of(values()).filter(p -> p.structureName.equals(path)).findFirst().orElse(null) : null;
    }

    public static int getY(ResourceLocation structureRL) {
        return getY(structureRL.getNamespace(), structureRL.getPath());
    }

    public static int getY(String structureRLString) {
        Pair<String, String> rl = Helper.parseRLString(structureRLString);
        return getY(rl.getLeft(), rl.getRight());
    }

    public static int getY(String domain, String path) {
        SupportStructures struct = getVanillaStructure(domain, path);
        return struct != null ? struct.getY() : domain.equals("quark") ? 37 : 68;
    }

    @Nullable
    public static String getRandomVanillaStructure(Predicate<ResourceLocation> predic) {
        return getRandomStructure(p -> p.getNamespace().equals("minecraft") && predic.test(p));
    }

    @Nullable
    public static String getRandomStructure(Predicate<ResourceLocation> predic) {
        final List<ResourceLocation> list = ForgeRegistries.STRUCTURE_FEATURES.getKeys().stream().filter(predic).collect(Collectors.toList());
        return list.isEmpty() ? null : list.get(Helper.random.nextInt(list.size())).toString();
    }

    public static boolean hasStructureInWorld(@Nullable ServerWorld world, Structure<?> structure) {
        return world != null && world.getChunkProvider().getChunkGenerator().getBiomeProvider().hasStructure(structure);
    }

    @Nullable
    public static Structure<?> getStructure(String name) {
    	for (Structure<?> structure : ForgeRegistries.STRUCTURE_FEATURES) {
    		if (name.equalsIgnoreCase(structure.getRegistryName().toString())) {
    			return structure;
    		}
    	}
        return null;
    }

    public static List<RegistryKey<World>> getDimensionTypesForStructure(MinecraftServer server, Structure<?> structure) {
    	
        return Streams.stream(server.getWorlds()).filter(world -> hasStructureInWorld(world, structure)).map(World::func_234923_W_).collect(Collectors.toList());
    }

    private static final Map<String, String> STRUCTURE_NAMES = new HashMap<>();

    public static String getStructureName(String structureId) {
        return STRUCTURE_NAMES.computeIfAbsent(structureId, k -> getStructure(k).getStructureName());
    }
}

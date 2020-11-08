package ovh.corail.tombstone.registry;

import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.IForgeRegistry;
import ovh.corail.tombstone.api.capability.Perk;
import ovh.corail.tombstone.perk.PerkAlchemist;
import ovh.corail.tombstone.perk.PerkBoneCollector;
import ovh.corail.tombstone.perk.PerkDisenchanter;
import ovh.corail.tombstone.perk.PerkGhostlyShape;
import ovh.corail.tombstone.perk.PerkJailer;
import ovh.corail.tombstone.perk.PerkMementoMori;
import ovh.corail.tombstone.perk.PerkRuneInscriber;
import ovh.corail.tombstone.perk.PerkScribe;
import ovh.corail.tombstone.perk.PerkTombRaider;
import ovh.corail.tombstone.perk.PerkTreasureSeeker;
import ovh.corail.tombstone.perk.PerkVoodooPoppet;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModPerks extends Registrable {
    public static final Perk ghostly_shape = new PerkGhostlyShape();
    public static final Perk voodoo_poppet = new PerkVoodooPoppet();
    public static final Perk scribe = new PerkScribe();
    public static final Perk rune_inscriber = new PerkRuneInscriber();
    public static final Perk treasure_seeker = new PerkTreasureSeeker();
    public static final Perk jailer = new PerkJailer();
    public static final Perk bone_collector = new PerkBoneCollector();
    public static final Perk memento_mori = new PerkMementoMori();
    public static final Perk disenchanter = new PerkDisenchanter();
    public static final Perk alchemist = new PerkAlchemist();
    public static final Perk tomb_raider = new PerkTombRaider();

    @SubscribeEvent
    public static void registerPerk(final RegistryEvent.Register<Perk> event) {
        registerPerks(event.getRegistry(), ghostly_shape, voodoo_poppet, scribe, rune_inscriber, treasure_seeker, jailer, bone_collector, memento_mori, disenchanter, alchemist, tomb_raider);
    }

    private static void registerPerks(IForgeRegistry<Perk> registry, Perk... perks) {
        for (Perk perk : perks) {
            registerForgeEntry(registry, perk, perk.getString());
        }
    }
}

package ovh.corail.tombstone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.reflect.Reflection;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.loot.LootConditionType;
import net.minecraft.loot.conditions.LootConditionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import ovh.corail.tombstone.api.TombstoneAPIProps;
import ovh.corail.tombstone.api.capability.ITBCapability;
import ovh.corail.tombstone.api.magic.ISoulConsumer;
import ovh.corail.tombstone.api.magic.TBSoulConsumerProvider;
import ovh.corail.tombstone.capability.TBCapabilityDefault;
import ovh.corail.tombstone.capability.TBCapabilityStorage;
import ovh.corail.tombstone.compatibility.CompatibilityCosmeticArmor;
import ovh.corail.tombstone.compatibility.IntegrationTOP;
import ovh.corail.tombstone.compatibility.SupportMods;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.config.SharedConfigTombstone;
import ovh.corail.tombstone.config.TombstoneModConfig;
import ovh.corail.tombstone.gui.GuiConfig;
import ovh.corail.tombstone.helper.CallbackHandler;
import ovh.corail.tombstone.helper.CooldownHandler;
import ovh.corail.tombstone.helper.DeathHandler;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.loot.InOpenWaterCondition;
import ovh.corail.tombstone.network.PacketHandler;
import ovh.corail.tombstone.perk.PerkRegistry;
import ovh.corail.tombstone.proxy.ClientProxy;
import ovh.corail.tombstone.proxy.IProxy;
import ovh.corail.tombstone.proxy.ServerProxy;
import ovh.corail.tombstone.registry.ModBlocks;
import ovh.corail.tombstone.registry.ModTabs;
import ovh.corail.tombstone.registry.ModTriggers;

@Mod("tombstone")
public class ModTombstone {
    public static final String MOD_ID = "tombstone";
    public static final String MOD_NAME = "Corail Tombstone";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    public static final IProxy PROXY = DistExecutor.unsafeRunForDist(() -> ClientProxy::new, () -> ServerProxy::new);
    
    public static LootConditionType OPEN_WATER;

    public ModTombstone() {
    	
        TombstoneAPIProps.COOLDOWN_HANDLER = CooldownHandler.INSTANCE;
        Reflection.initialize(PerkRegistry.class, PacketHandler.class, ModTriggers.class, ModTabs.class);
        ModLoadingContext context = ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.CLIENT, ConfigTombstone.CLIENT_SPEC);
        context.registerConfig(ModConfig.Type.COMMON, ConfigTombstone.GENERAL_SPEC);
        registerSharedConfig(context);
        final IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.addListener(this::onServerStarting);
        forgeBus.addListener(this::onServerStoppingEvent);
        final IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::preInit);
        modBus.addListener(this::clientSetup);
        modBus.addListener(this::enqueueIMC);
        context.registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> GuiConfig::new);
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {
        if (SupportMods.TOP.isLoaded()) {
            InterModComms.sendTo("theoneprobe", "getTheOneProbe", IntegrationTOP::new);
        }
    }

    private void registerSharedConfig(ModLoadingContext context) {
        context.getActiveContainer().addConfig(new TombstoneModConfig(SharedConfigTombstone.CONFIG_SPEC, context.getActiveContainer()));
    }

    private void preInit(final FMLCommonSetupEvent event) {
        CapabilityManager.INSTANCE.register(ITBCapability.class, new TBCapabilityStorage(), TBCapabilityDefault::new);
        CapabilityManager.INSTANCE.register(ISoulConsumer.class, Helper.getNullStorage(), TBSoulConsumerProvider::getDefault);
        PROXY.preInit();
        MinecraftForge.EVENT_BUS.register(Helper.buildKnowledgeFunction());
        OPEN_WATER = LootConditionManager.register(MOD_ID + ":in_open_water", InOpenWaterCondition.SERIALIZER);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        ModBlocks.decorative_graves.values().forEach(block -> RenderTypeLookup.setRenderLayer(block, RenderType.getCutout()));
        ModBlocks.graves.values().forEach(block -> RenderTypeLookup.setRenderLayer(block, RenderType.getCutout()));
        Helper.initModelProperties();
    }

    private void onServerStarting(FMLServerStartingEvent event) {
        if (SupportMods.GRAVESTONE.isLoaded()) {
            throw new RuntimeException(MOD_NAME + " Exception : you loaded an incompatible grave mod : GraveStone");
        } else if (SupportMods.CORPSE.isLoaded()) {
            throw new RuntimeException(MOD_NAME + " Exception : you loaded an incompatible grave mod : Corpse");
        }
        if (SupportMods.COSMETIC_ARMOR.isLoaded()) {
            MinecraftForge.EVENT_BUS.register(CompatibilityCosmeticArmor.instance);
        }
        MinecraftForge.EVENT_BUS.register(new CallbackHandler());
    }

    private void onServerStoppingEvent(FMLServerStoppingEvent event) {
        CallbackHandler.clear();
        DeathHandler.INSTANCE.clear();
    }

    @SuppressWarnings("deprecation")
    public static final EnchantmentType TYPE_TOMBSTONE_ALL = EnchantmentType.create("type_tombstone_all", p -> p != null && (p.getItemEnchantability() > 0 || p.getMaxStackSize() == 1));
}

package ovh.corail.tombstone.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import ovh.corail.tombstone.block.BlockGraveMarble.MarbleType;
import ovh.corail.tombstone.block.GraveModel;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.network.PacketHandler;
import ovh.corail.tombstone.network.UpdateServerMessage;
import ovh.corail.tombstone.particle.ParticleCasting;
import ovh.corail.tombstone.particle.ParticleShadowStep;
import ovh.corail.tombstone.registry.ModBlocks;
import ovh.corail.tombstone.registry.ModParticleTypes;
import ovh.corail.tombstone.render.RenderWritableGrave;

import java.net.Proxy;
import java.util.function.Predicate;

@OnlyIn(Dist.CLIENT)
public class ClientProxy implements IProxy {
    private boolean isConfigDirty = false;

    @Override
    public void preInit() {
        ClientRegistry.bindTileEntityRenderer(ModBlocks.tile_decorative_grave, RenderWritableGrave::new);
        ClientRegistry.bindTileEntityRenderer(ModBlocks.tile_grave, RenderWritableGrave::new);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void produceGraveSmoke(World world, double x, double y, double z) {
        for (int i = 0; i < ConfigTombstone.client.fogDensity.get().ordinal(); i++) {
            Minecraft.getInstance().particles.addParticle(ModParticleTypes.GRAVE_SMOKE, x + world.rand.nextGaussian(), y, z + world.rand.nextGaussian(), 0d, 0d, 0d);
        }
    }

    @Override
    public void produceShadowStep(LivingEntity entity) {
        Minecraft mc = Minecraft.getInstance();
//        if (mc.player != null && mc.player.equals(entity) && mc.gameSettings.thirdPersonView == 0) {
        if (mc.player != null && mc.player.equals(entity) && mc.gameSettings.getPointOfView() == PointOfView.FIRST_PERSON) {
            return;
        }
        ClientWorld world = mc.world;
        if (world != null && !entity.isPassenger() && !entity.isSleeping()) {
            for (double i = 0d; i < 1d; i += 0.15d) {
                mc.particles.addEffect(new ParticleShadowStep(mc.world, entity, i));
            }
        }
    }

    @Override
    public void produceGraveSoul(World world, BlockPos pos) {
        world.addParticle(ModParticleTypes.GRAVE_SOUL, pos.getX(), pos.getY(), pos.getZ(), 0d, 0d, 0d);
    }

    @Override
    public void produceParticleCasting(LivingEntity caster, Predicate<LivingEntity> predic) {
        Minecraft mc = Minecraft.getInstance();
        ClientWorld world = mc.world;
        if (caster != null && world != null) {
            ParticleCasting particle;
            for (int i = 1; i <= 2; i++) {
                particle = new ParticleCasting(world, caster, predic, 0d, i * 0.5d);
                mc.particles.addEffect(particle);
                particle = new ParticleCasting(world, caster, predic, 0.5d, (i + 1) * 0.5d);
                mc.particles.addEffect(particle);
                particle = new ParticleCasting(world, caster, predic, 1d, i * 0.5d);
                mc.particles.addEffect(particle);
                particle = new ParticleCasting(world, caster, predic, 1.5d, (i + 1) * 0.5d);
                mc.particles.addEffect(particle);
                particle = new ParticleCasting(world, caster, predic, 2d, i * 0.5d);
                mc.particles.addEffect(particle);
            }
        }
    }

    @Override
    public void produceSmokeColumn(World world, double x, double y, double z) {
        world.addParticle(ModParticleTypes.SMOKE_COLUMN, x, y, z, 0d, 0d, 0d);
    }

    @Override
    public Proxy getNetProxy() {
        return Minecraft.getInstance().getProxy();
    }

    @Override
    public void markConfigDirty() {
        this.isConfigDirty = true;
    }

    private GraveModel lastGraveModel = null;
    private MarbleType lastMarbleType = null;
    private boolean lastEquipElytraInPriority, lastDisplayKnowledgeMessage, lastPriorizeToolOnHotbar, lastActivateGraveBySneaking;

    @SuppressWarnings("unused")
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && this.isConfigDirty) {
            this.isConfigDirty = false;
            if (Minecraft.getInstance().player == null) {
                this.lastGraveModel = ConfigTombstone.client.favoriteGrave.get();
                this.lastMarbleType = ConfigTombstone.client.favoriteGraveMarble.get();
                this.lastEquipElytraInPriority = ConfigTombstone.client.equipElytraInPriority.get();
                this.lastDisplayKnowledgeMessage = ConfigTombstone.client.displayKnowledgeMessage.get();
                this.lastPriorizeToolOnHotbar = ConfigTombstone.client.priorizeToolOnHotbar.get();
                this.lastActivateGraveBySneaking = ConfigTombstone.client.activateGraveBySneaking.get();
            } else {
                boolean changed = false;
                if (this.lastGraveModel != ConfigTombstone.client.favoriteGrave.get()) {
                    this.lastGraveModel = ConfigTombstone.client.favoriteGrave.get();
                    changed = true;
                }
                if (this.lastMarbleType != ConfigTombstone.client.favoriteGraveMarble.get()) {
                    this.lastMarbleType = ConfigTombstone.client.favoriteGraveMarble.get();
                    changed = true;
                }
                if (this.lastEquipElytraInPriority != ConfigTombstone.client.equipElytraInPriority.get()) {
                    this.lastEquipElytraInPriority = ConfigTombstone.client.equipElytraInPriority.get();
                    changed = true;
                }
                if (this.lastDisplayKnowledgeMessage != ConfigTombstone.client.displayKnowledgeMessage.get()) {
                    this.lastDisplayKnowledgeMessage = ConfigTombstone.client.displayKnowledgeMessage.get();
                    changed = true;
                }
                if (this.lastPriorizeToolOnHotbar != ConfigTombstone.client.priorizeToolOnHotbar.get()) {
                    this.lastPriorizeToolOnHotbar = ConfigTombstone.client.priorizeToolOnHotbar.get();
                    changed = true;
                }
                if (this.lastActivateGraveBySneaking != ConfigTombstone.client.activateGraveBySneaking.get()) {
                    this.lastActivateGraveBySneaking = ConfigTombstone.client.activateGraveBySneaking.get();
                    changed = true;
                }
                if (changed) {
                    Minecraft.getInstance().player.sendMessage(new StringTextComponent("Syncing Preferences on Server"), Util.DUMMY_UUID);
                    PacketHandler.sendToServer(new UpdateServerMessage(ConfigTombstone.client.favoriteGrave.get(), ConfigTombstone.client.favoriteGraveMarble.get(), ConfigTombstone.client.equipElytraInPriority.get(), ConfigTombstone.client.displayKnowledgeMessage.get(), ConfigTombstone.client.priorizeToolOnHotbar.get(), ConfigTombstone.client.activateGraveBySneaking.get(), false));
                }
            }
        }
    }
}

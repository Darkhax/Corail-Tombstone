package ovh.corail.tombstone.particle;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.helper.Helper;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

@OnlyIn(Dist.CLIENT)
public class ParticleShadowStep extends CustomParticle {
    private static final ResourceLocation COMMON_TEXTURE = new ResourceLocation(MOD_ID, "textures/particle/fake_fog.png");

    public ParticleShadowStep(ClientWorld world, LivingEntity entity, double angle) {
        super(world, entity.getPosX(), entity.getPosY() + 0.3d, entity.getPosZ());
        this.motionX = this.motionY = this.motionZ = 0d;
        setAlphaF(0.5f);
        this.particleScale = 0.3f;
        this.canCollide = false;
        this.particleGravity = 0f;
        this.prevParticleAngle = this.particleAngle = Helper.getRandom(0f, 2f * (float) Math.PI);
        double ratio = (entity.ticksExisted % 100) / 100d;
        double res = 2d * Math.PI * (angle + ratio);
        this.prevPosX = this.posX = entity.getPosX() + 0.5d * Math.cos(res);
        this.prevPosY = this.posY = entity.getPosY() + 0.3d;
        this.prevPosZ = this.posZ = entity.getPosZ() + 0.5d * Math.sin(res);
        float currentColor = Helper.getRandom(80f, 150f) / 255f; //* 0.0039215686f;
        setColor(currentColor, currentColor, currentColor);
    }

    @Override
    public void tick() {
        setExpired();
    }

    @Override
    protected int getBrightnessForRender(float partialTick) {
        int skylight = 15;
        int blocklight = 15;
        return skylight << 20 | blocklight << 4;
    }

    @Override
    ResourceLocation getTexture() {
        return COMMON_TEXTURE;
    }
}

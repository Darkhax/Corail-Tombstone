package ovh.corail.tombstone.particle;

import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.particle.data.ParticleDataTwoInt;

@OnlyIn(Dist.CLIENT)
public class ParticleBlinkingAura extends TransparentParticle {
    private final IAnimatedSprite spriteSet;
    private final float[] colorCodeMin;
    private final float[] colorCodeMax;

    private ParticleBlinkingAura(IAnimatedSprite spriteSet, ClientWorld world, double x, double y, double z, int colorMin, int colorMax) {
        super(world, x, y, z);
        this.motionX = this.motionY = this.motionZ = 0d;
        setAlphaF(0.15f);
        multiplyParticleScaleBy(Helper.getRandom(0.6f, 0.8f));
        setMaxAge(7);
        this.canCollide = false;
        this.colorCodeMin = Helper.getRGBColor3F(colorMin);
        this.colorCodeMax = Helper.getRGBColor3F(colorMax);
        setColor(this.colorCodeMin[0], this.colorCodeMin[1], this.colorCodeMin[2]);
        this.spriteSet = spriteSet;
        selectSpriteWithAge(this.spriteSet);
    }

    @Override
    public void tick() {
        super.tick();
        if (isAlive()) {
            setColor(Helper.getRandom(this.colorCodeMin[0], this.colorCodeMax[0]), Helper.getRandom(this.colorCodeMin[1], this.colorCodeMax[1]), Helper.getRandom(this.colorCodeMin[2], this.colorCodeMax[2]));
            selectSpriteWithAge(this.spriteSet);
        }
    }

    @Override
    protected int getBrightnessForRender(float partialTick) {
        int skylight = 15;
        int blocklight = 15;
        return skylight << 20 | blocklight << 4;
    }

    @Override
    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements IParticleFactory<ParticleDataTwoInt> {
        private IAnimatedSprite spriteSet;

        public Factory(IAnimatedSprite spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle makeParticle(ParticleDataTwoInt type, ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ) {
            return new ParticleBlinkingAura(this.spriteSet, world, x, y, z, type.oneInt, type.twoInt);
        }
    }
}

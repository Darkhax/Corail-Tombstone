package ovh.corail.tombstone.particle;

import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;

public class ParticleRotatingSmoke extends TransparentParticle {
    private final IAnimatedSprite spriteSet;
    private final float rotIncrement;

    private ParticleRotatingSmoke(IAnimatedSprite spriteSet, ClientWorld world, double x, double y, double z) {
        super(world, x, y + 0.3d, z);
        this.motionX = this.motionY = this.motionZ = 0d;
        setAlphaF(0.5f);
        multiplyParticleScaleBy(2f);
        setMaxAge(100);
        this.canCollide = false;
        this.prevParticleAngle = this.particleAngle = (float) (world.rand.nextFloat() * Math.PI * 2f);
        this.rotIncrement = (float) (Math.PI * 0.02f);
        setColor(0.7f, 0.7f, 0.7f);
        this.spriteSet = spriteSet;
        selectSpriteWithAge(this.spriteSet);
    }

    private void updatePosition() {
        this.prevParticleAngle = this.particleAngle;
        this.particleAngle += this.rotIncrement;
        float color = 0.6f + this.world.rand.nextFloat() * 0.2f;
        setColor(color, color, color);
    }

    @Override
    public void tick() {
        super.tick();
        if (isAlive()) {
            updatePosition();
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

    public static class Factory implements IParticleFactory<BasicParticleType> {
        private IAnimatedSprite spriteSet;

        public Factory(IAnimatedSprite spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle makeParticle(BasicParticleType type, ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ) {
            return new ParticleRotatingSmoke(this.spriteSet, world, x, y, z);
        }
    }
}

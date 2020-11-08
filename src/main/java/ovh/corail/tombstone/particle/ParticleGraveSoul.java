package ovh.corail.tombstone.particle;

import net.minecraft.client.particle.IAnimatedSprite;
import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.particle.data.ParticleDataTwoInt;
import ovh.corail.tombstone.registry.ModParticleTypes;

@OnlyIn(Dist.CLIENT)
public class ParticleGraveSoul extends SpriteTexturedParticle {
    private final IAnimatedSprite spriteSet;

    private final double radius, centerX, centerZ;

    private ParticleGraveSoul(IAnimatedSprite spriteSet, ClientWorld world, double x, double y, double z, double radius) {
        super(world, x, y + 0.85d, z);
        this.maxAge = 100;
        this.particleScale = 0.03f;
        this.centerX = x + 0.5d;
        this.centerZ = z + 0.5d;
        this.radius = radius;
        updatePosition();
        setAlphaF(0.7f);
        setColor(81f / 255f, 25f / 255f, 139f / 255f);
        this.canCollide = false;
        this.spriteSet = spriteSet;
        selectSpriteWithAge(this.spriteSet);
    }

    private void updatePosition() {
        double ratio = this.age / (double) this.maxAge;
        this.motionX = this.motionY = this.motionZ = 0d;
        this.prevPosX = this.posX = this.centerX + this.radius * Math.cos(2 * Math.PI * ratio);
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ = this.centerZ + this.radius * Math.sin(2 * Math.PI * ratio);
    }

    @Override
    public void tick() {
        super.tick();
        if (isAlive()) {
            selectSpriteWithAge(this.spriteSet);
            updatePosition();
            this.world.addParticle(new ParticleDataTwoInt(ModParticleTypes.BLINKING_SMOKE, 0x58198b, 0xbe199f), this.posX, this.posY + 0.02d, this.posZ, 0d, 0d, 0d);
        }
    }

    @Override
    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static class Factory implements IParticleFactory<BasicParticleType> {
        private IAnimatedSprite spriteSet;

        public Factory(IAnimatedSprite spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle makeParticle(BasicParticleType type, ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ) {
            return new ParticleGraveSoul(this.spriteSet, world, x, y, z, 0.3d);
        }
    }
}

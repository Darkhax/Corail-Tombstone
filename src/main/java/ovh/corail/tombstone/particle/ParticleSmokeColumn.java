package ovh.corail.tombstone.particle;

import net.minecraft.client.particle.IParticleFactory;
import net.minecraft.client.particle.MetaParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.BasicParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ovh.corail.tombstone.registry.ModParticleTypes;

@OnlyIn(Dist.CLIENT)
public class ParticleSmokeColumn extends MetaParticle {

    private ParticleSmokeColumn(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    public void tick() {
        double y = this.posY;
        for (int i = 0; i < 6; i++) {
            this.world.addParticle(ModParticleTypes.ROTATING_SMOKE, this.posX - 0.1d, y, this.posZ - 0.1d, 0d, 0d, 0d);
            this.world.addParticle(ModParticleTypes.ROTATING_SMOKE, this.posX - 0.1d, y, this.posZ + 0.1d, 0d, 0d, 0d);
            this.world.addParticle(ModParticleTypes.ROTATING_SMOKE, this.posX + 0.1d, y, this.posZ - 0.1d, 0d, 0d, 0d);
            this.world.addParticle(ModParticleTypes.ROTATING_SMOKE, this.posX + 0.1d, y, this.posZ + 0.1d, 0d, 0d, 0d);
            y += 0.3d;
        }
        setExpired();
    }

    public static class Factory implements IParticleFactory<BasicParticleType> {
        @Override
        public Particle makeParticle(BasicParticleType type, ClientWorld world, double x, double y, double z, double motionX, double motionY, double motionZ) {
            return new ParticleSmokeColumn(world, x, y, z);
        }
    }
}

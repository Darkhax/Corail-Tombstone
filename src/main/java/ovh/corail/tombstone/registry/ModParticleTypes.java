package ovh.corail.tombstone.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleType;
import ovh.corail.tombstone.particle.data.ParticleDataTwoInt;

import java.util.function.Function;

public class ModParticleTypes {
    public static final BasicParticleType GHOST = new BasicParticleType(false);
    public static final BasicParticleType GRAVE_SMOKE = new BasicParticleType(false);
    public static final BasicParticleType ROTATING_SMOKE = new BasicParticleType(false);
    public static final BasicParticleType SMOKE_COLUMN = new BasicParticleType(false);
    public static final ParticleType<ParticleDataTwoInt> BLINKING_SMOKE = new ParticleType<ParticleDataTwoInt>(false, ParticleDataTwoInt.DESERIALIZER) {
        @Override
        public Codec<ParticleDataTwoInt> func_230522_e_() {
            return funct.apply(this);
        }

        final Function<ParticleType<ParticleDataTwoInt>, Codec<ParticleDataTwoInt>> funct = f -> RecordCodecBuilder.create(p -> p.group(Codec.INT.fieldOf("one_int").forGetter(g -> g.oneInt), Codec.INT.fieldOf("two_int").forGetter(g -> g.twoInt)).apply(p, (t1,t2) -> new ParticleDataTwoInt(this, t1, t2)));
    };
    public static final BasicParticleType GRAVE_SOUL = new BasicParticleType(false);
}

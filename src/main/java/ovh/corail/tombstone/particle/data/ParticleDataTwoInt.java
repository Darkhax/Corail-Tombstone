package ovh.corail.tombstone.particle.data;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.network.PacketBuffer;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

public class ParticleDataTwoInt implements IParticleData {
    public static final IDeserializer<ParticleDataTwoInt> DESERIALIZER = new IDeserializer<ParticleDataTwoInt>() {
        @Override
        public ParticleDataTwoInt deserialize(ParticleType<ParticleDataTwoInt> particleType, StringReader reader) throws CommandSyntaxException {
            reader.expect(' ');
            int oneInt = 0xffffff, twoInt = 0xffffff;
            oneInt = reader.readInt();
            reader.expect(' ');
            twoInt = reader.readInt();
            return new ParticleDataTwoInt(particleType, oneInt, twoInt);
        }

        @Override
        public ParticleDataTwoInt read(ParticleType<ParticleDataTwoInt> particleType, PacketBuffer buf) {
            return new ParticleDataTwoInt(particleType, buf.readInt(), buf.readInt());
        }
    };
    private final ParticleType<ParticleDataTwoInt> particleType;
    public int oneInt, twoInt;

    public ParticleDataTwoInt(ParticleType<ParticleDataTwoInt> particleType, int oneInt, int twoInt) {
        this.particleType = particleType;
        this.oneInt = oneInt;
        this.twoInt = twoInt;
    }

    @Override
    public ParticleType<?> getType() {
        return this.particleType;
    }

    @Override
    public void write(PacketBuffer buf) {
        buf.writeInt(this.oneInt);
        buf.writeInt(this.twoInt);
    }

    @Override
    public String getParameters() {
        return String.format(Locale.ROOT, "%s %d %d", ForgeRegistries.PARTICLE_TYPES.getKey(getType()), this.oneInt, this.twoInt);
    }
}

package ovh.corail.tombstone.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.particle.IParticleRenderType;
import net.minecraft.client.particle.SpriteTexturedParticle;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;

public class TransparentParticle extends SpriteTexturedParticle {
    protected TransparentParticle(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Override
    public IParticleRenderType getRenderType() {
        return IParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public void renderParticle(IVertexBuilder p_225606_1_, ActiveRenderInfo p_225606_2_, float p_225606_3_) {
        RenderSystem.depthMask(false);
        super.renderParticle(p_225606_1_, p_225606_2_, p_225606_3_);
    }
}

package ovh.corail.tombstone.effect;

import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.helper.EffectHelper;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.TimeHelper;

public class GhostlyShapeEffect extends Effect {

    public GhostlyShapeEffect() {
        super(EffectType.BENEFICIAL, 2331802);
    }

    @Override
    public void performEffect(LivingEntity entity, int amplifier) {
        if (!EntityHelper.isValidPlayer(entity) || ConfigTombstone.general.nerfGhostlyShape.get() && (entity.collidedHorizontally || entity.collidedVertically) && entity.world.getEntitiesInAABBexcluding(entity, entity.getBoundingBox(), p -> p != null && !p.isOnSameTeam(entity)).size() > 0) {
            entity.removePotionEffect(this);
            return;
        }
        if (amplifier > 0) {
            FeatherFallEffect.performPotionLogic(entity);
            if (amplifier > 1) {
                entity.setAir(300);
                if (amplifier > 2 && TimeHelper.atInterval(entity.ticksExisted, 20)) {
                    EffectHelper.clearBadEffects(entity);
                    entity.heal(0.5f);
                }
            }
        }
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return true;
    }
}

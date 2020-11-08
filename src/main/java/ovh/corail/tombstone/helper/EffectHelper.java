package ovh.corail.tombstone.helper;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import ovh.corail.tombstone.config.ConfigTombstone;
import ovh.corail.tombstone.registry.ModEffects;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class EffectHelper {

    public static int getPotionDuration(@Nullable LivingEntity entity, @Nullable Effect potion) {
        EffectInstance effectInstance;
        return entity != null && potion != null && (effectInstance = entity.getActivePotionEffect(potion)) != null ? effectInstance.duration : 0;
    }

    public static boolean isPotionActive(@Nullable LivingEntity entity, @Nullable Effect potion) {
        return getPotionDuration(entity, potion) > 0;
    }

    public static boolean isPotionActive(@Nullable LivingEntity entity, @Nullable Effect potion, int amplifier) {
        EffectInstance effectInstance;
        return entity != null && potion != null && (effectInstance = entity.getActivePotionEffect(potion)) != null && effectInstance.amplifier >= amplifier && effectInstance.duration > 0;
    }

    public static boolean isUnstableIntangiblenessActive(@Nullable LivingEntity player) {
        return TimeHelper.inIntervalAfter(EffectHelper.getPotionDuration(player, ModEffects.unstable_intangibleness), 100, 79);
    }

    public static void capPotionDuration(@Nullable LivingEntity entity, @Nullable Effect potion, int maxDuration) {
        if (entity != null && potion != null) {
            EffectInstance effectInstance = entity.getActivePotionMap().get(potion);
            if (needCap(effectInstance, maxDuration)) {
                clearEffect(entity, potion);
                modifyEffectDuration(effectInstance, e -> e.duration > maxDuration ? maxDuration : e.duration);
                addEffect(entity, effectInstance);
            }
        }
    }

    private static boolean needCap(@Nullable EffectInstance effectInstance, int maxDuration) {
        return effectInstance != null && (effectInstance.duration > maxDuration || needCap(effectInstance.hiddenEffects, maxDuration));
    }

    public static void addEffect(@Nullable LivingEntity entity, @Nullable Effect potion, int duration) {
        addEffect(entity, potion, duration, 0);
    }

    public static void addEffect(@Nullable LivingEntity entity, @Nullable Effect potion, int duration, int amplifier, boolean... params) {
        if (entity != null && potion != null) {
            entity.addPotionEffect(new EffectInstance(potion, duration, amplifier, (params.length == 0 || params[0]), (params.length <= 1 || params[1])));
        }
    }

    public static void addEffect(@Nullable LivingEntity entity, @Nullable EffectInstance effectInstance) {
        if (entity != null && effectInstance != null && effectInstance.getPotion() != null) {
            entity.addPotionEffect(effectInstance);
        }
    }

    @SuppressWarnings("unused")
    public static boolean isPotionHidden(@Nullable LivingEntity entity, @Nullable Effect potion) {
        EffectInstance effectInstance;
        return entity != null && potion != null && (effectInstance = entity.getActivePotionEffect(potion)) != null && effectInstance.isAmbient() && !effectInstance.doesShowParticles();
    }

    @SuppressWarnings("unused")
    public static EffectInstance copyEffectWithHidden(EffectInstance effectInstance) {
        EffectInstance effectCopy = new EffectInstance(effectInstance);
        if (effectInstance.hiddenEffects != null) {
            effectCopy.hiddenEffects = copyEffectWithHidden(effectInstance.hiddenEffects);
        }
        return effectCopy;
    }

    public static EffectInstance modifyEffectDuration(EffectInstance effectInstance, Function<EffectInstance, Integer> function) {
        effectInstance.duration = function.apply(effectInstance);
        if (effectInstance.hiddenEffects != null) {
            modifyEffectDuration(effectInstance.hiddenEffects, function);
        }
        return effectInstance;
    }

    public static void clearBadEffects(@Nullable LivingEntity entity) {
        if (entity != null) {
            clearEffect(entity, effect -> effect.getEffectType() == EffectType.HARMFUL || (ConfigTombstone.decorative_grave.purificationAffectNeutralEffects.get() && effect.getEffectType() == EffectType.NEUTRAL));
            if (entity.isBurning()) {
                entity.extinguish();
            }
            if (entity.isGlowing()) {
                entity.setGlowing(false);
            }
        }
    }

    private static final Method methodOnFinishedPotionEffect = ObfuscationReflectionHelper.findMethod(LivingEntity.class, "func_70688_c", EffectInstance.class);

    public static boolean clearEffect(@Nullable LivingEntity entity, Effect effect) {
        if (entity != null && !entity.world.isRemote) {
            EffectInstance effectInstance = entity.removeActivePotionEffect(effect);
            if (effectInstance != null) {
                try {
                    methodOnFinishedPotionEffect.invoke(entity, effectInstance);
                    return true;
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private static void clearEffect(LivingEntity entity, Predicate<Effect> predic) {
        if (!entity.world.isRemote) {
            Iterator<Map.Entry<Effect, EffectInstance>> it = entity.getActivePotionMap().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Effect, EffectInstance> entry = it.next();
                if (predic.test(entry.getKey())) {
                    try {
                        methodOnFinishedPotionEffect.invoke(entity, entry.getValue());
                        it.remove();
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Nullable
    public static EffectInstance getRandomEffect(int duration, boolean isBad) {
        return getRandomEffect(duration, isBad, RANDOM_EFFECT_LEVEL);
    }

    public static Function<Random, Integer> RANDOM_EFFECT_LEVEL = random -> {
        int roll = random.nextInt(100) + 1;
        return roll < 10 ? 4 : (roll < 25 ? 3 : (roll < 45 ? 2 : (roll < 70 ? 1 : 0)));
    };

    @Nullable
    public static EffectInstance getRandomEffect(int duration, boolean isBad, Function<Random, Integer> functionEffectLevel) {
        List<Effect> potions = ForgeRegistries.POTIONS.getValues().stream().filter(isBad ? EffectHelper::isBadEffect : EffectHelper::isAllowedEffect).collect(Collectors.toList());
        if (potions.size() > 0) {
            Effect potion = potions.get(Helper.random.nextInt(potions.size()));
            if (potion != null) {
                return new EffectInstance(potion, duration, functionEffectLevel.apply(Helper.random));
            }
        }
        return null;
    }

    public static void addRandomEffect(PlayerEntity player, int duration, boolean withMessage) {
        addRandomEffect(player, duration, withMessage, false);
    }

    public static void addRandomEffect(PlayerEntity player, int duration, boolean withMessage, boolean isBad) {
        EffectInstance effect = getRandomEffect(duration, isBad);
        if (effect != null) {
            if (withMessage && !effect.getPotion().getName().isEmpty()) {
                LangKey.MESSAGE_SPELL_CAST_ON_YOU.sendMessage(player, StyleType.MESSAGE_SPELL, new TranslationTextComponent(effect.getPotion().getName()).setStyle(StyleType.MESSAGE_SPECIAL));
            }
            addEffect(player, effect);
        }
    }

    public static boolean isBadEffect(@Nullable Effect effect) {
        return effect != null && effect.getEffectType() == EffectType.HARMFUL && !Helper.containRL(ConfigTombstone.general.unhandledHarmfulEffects.get(), effect.getRegistryName());
    }

    public static boolean isAllowedEffect(@Nullable EffectInstance effectInstance) {
        return effectInstance != null && isAllowedEffect(effectInstance.getPotion());
    }

    public static boolean isAllowedEffect(@Nullable Effect effect) {
        return effect != null && effect.type == EffectType.BENEFICIAL && !Helper.containRL(ConfigTombstone.general.unhandledBeneficialEffects.get(), effect.getRegistryName());
    }
}

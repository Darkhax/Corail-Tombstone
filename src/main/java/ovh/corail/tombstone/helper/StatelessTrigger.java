package ovh.corail.tombstone.helper;

import com.google.gson.JsonObject;
import net.minecraft.advancements.ICriterionTrigger;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.advancements.criterion.CriterionInstance;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.loot.ConditionArrayParser;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

public class StatelessTrigger implements ICriterionTrigger<StatelessTrigger.Instance> {
    private final ResourceLocation rl;
    private final Map<PlayerAdvancements, Listeners> listeners = new HashMap<>();

    public StatelessTrigger(String name) {
        this.rl = new ResourceLocation(MOD_ID, name);
    }

    public StatelessTrigger(ResourceLocation rl) {
        this.rl = rl;
    }

    @Override
    public ResourceLocation getId() {
        return this.rl;
    }

    @Override
    public void addListener(PlayerAdvancements advancements, Listener<Instance> listener) {
        Listeners listeners = this.listeners.computeIfAbsent(advancements, Listeners::new);
        listeners.add(listener);
    }

    @Override
    public void removeListener(PlayerAdvancements advancements, Listener<Instance> listener) {
        Listeners listeners = this.listeners.get(advancements);
        if (listeners == null) {
            return;
        }
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            this.listeners.remove(advancements);
        }
    }

    @Override
    public void removeAllListeners(PlayerAdvancements advancements) {
        this.listeners.remove(advancements);
    }

    @Override
    public Instance deserialize(JsonObject object, ConditionArrayParser conditions) {
        return new Instance(this.rl);
    }

    public void trigger(ServerPlayerEntity player) {
        Listeners listener = this.listeners.get(player.getAdvancements());
        if (listener != null) {
            listener.trigger();
        }
    }

    public static class Instance extends CriterionInstance {
        Instance(ResourceLocation identifier) {
            super(identifier, EntityPredicate.AndPredicate.ANY_AND);
        }
    }

    static class Listeners {
        private final PlayerAdvancements advancements;
        private final Set<Listener<Instance>> listeners = new HashSet<>();

        public Listeners(PlayerAdvancements advancements) {
            this.advancements = advancements;
        }

        public boolean isEmpty() {
            return this.listeners.isEmpty();
        }

        public void add(Listener<Instance> listener) {
            this.listeners.add(listener);
        }

        public void remove(Listener<Instance> listener) {
            this.listeners.remove(listener);
        }

        public void trigger() {
            new ArrayList<>(this.listeners).forEach(listener -> listener.grantCriterion(this.advancements));
        }
    }
}

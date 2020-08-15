package ovh.corail.tombstone.helper;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.advancements.FunctionManager;
import net.minecraft.command.CommandSource;
import net.minecraft.command.FunctionObject;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IWorld;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ovh.corail.tombstone.ModTombstone.MOD_ID;

/*
	Made by Paul Fulham for Corail Tombstone
	https://gist.github.com/pau101/eba191f6a607db222f98edc5f76ab770/e5e9e8480ef7086b20700606913f81a52474b921
*/
@Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
@SuppressWarnings("unused")
public final class WorldFunctionInjector {
    private final ImmutableMap<ResourceLocation, CommandFunction> functions;

    private WorldFunctionInjector(ImmutableMap<ResourceLocation, CommandFunction> functions) {
        this.functions = functions;
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        IWorld world = event.getWorld();
        if (world instanceof ServerWorld) {
            FunctionManager manager = ((ServerWorld) world).getServer().getFunctionManager();
            //noinspection ConstantConditions
            if (manager != null) {
                WorldFunctionInjector.inject(manager, this.functions);
            }
        }
    }

    public static Builder builder() {
        return new WorldFunctionInjector.Builder();
    }

    private static void inject(FunctionManager manager, ImmutableMap<ResourceLocation, CommandFunction> functions) {
        // TODO disabled
        /*Map<ResourceLocation, FunctionObject> delegate = manager.getFunctions();
        ImmutableMap.Builder<ResourceLocation, CommandFunction> builder = ImmutableMap.builder();
        for (Map.Entry<ResourceLocation, CommandFunction> entry : functions.entrySet()) {
            if (!delegate.containsKey(entry.getKey())) {
                builder.put(entry.getKey(), entry.getValue());
            }
        }
        ImmutableMap<ResourceLocation, CommandFunction> uniqueFunctions = builder.build();
        if (!uniqueFunctions.isEmpty()) {
            manager.functions = new WorldFunctionInjector.InjectionMap(delegate, uniqueFunctions, Maps.newHashMap());
        }*/
    }

    private static final class InjectionMap extends ForwardingMap<ResourceLocation, FunctionObject> {
        private static final String PARAMETER_DELIMITER = "//";

        private static final Pattern PARAMETER = Pattern.compile(PARAMETER_DELIMITER + "([a-z_][a-z0-9_]*)/([a-z0-9._-]*)");

        private final Map<ResourceLocation, FunctionObject> delegate;

        private final ImmutableMap<ResourceLocation, CommandFunction> functions;

        private final Map<ResourceLocation, FunctionObject> objects;

        private InjectionMap(Map<ResourceLocation, FunctionObject> delegate, ImmutableMap<ResourceLocation, CommandFunction> functions, Map<ResourceLocation, FunctionObject> objects) {
            this.delegate = delegate;
            this.functions = functions;
            this.objects = objects;
        }

        @Override
        protected Map<ResourceLocation, FunctionObject> delegate() {
            return this.delegate;
        }

        @Override
        @Nullable
        public FunctionObject get(@Nullable Object key) {
            FunctionObject value = super.get(key);
            if (value == null && !super.containsKey(key) && key instanceof ResourceLocation) {
                return this.objects.computeIfAbsent((ResourceLocation) key, this::compute);
            }
            return value;
        }

        @Nullable
        private FunctionObject compute(ResourceLocation descriptor) {
            String path = descriptor.getPath();
            String namePart, parametersPart;
            int paramStart = path.indexOf(InjectionMap.PARAMETER_DELIMITER);
            if (paramStart >= 0) {
                namePart = path.substring(0, paramStart);
                parametersPart = path.substring(paramStart);
            } else {
                namePart = path;
                parametersPart = "";
            }
            CommandFunction function = this.functions.get(new ResourceLocation(descriptor.getNamespace(), namePart));
            if (function != null) {
                ImmutableMap.Builder<String, Object> parameters = ImmutableMap.builder();
                Matcher m = InjectionMap.PARAMETER.matcher(parametersPart).useTransparentBounds(true);
                for (; m.lookingAt(); m.region(m.end(), m.regionEnd())) {
                    parameters.put(m.group(1), this.parse(m.group(2)));
                }
                if (m.regionStart() == m.regionEnd()) {
                    ParameterMap map = new ParameterMap(parameters.build());
                    return new FunctionObject(new ResourceLocation(MOD_ID, "custom_function"), new FunctionObject.IEntry[] {
                            ((manager, sender, queue, maxChainLength) -> function.accept(sender, map))
                    });
                }
            }
            return null;
        }

        private Object parse(String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
            }
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ignored) {
            }
            if ("true".equals(value)) {
                return Boolean.TRUE;
            } else if ("false".equals(value)) {
                return Boolean.FALSE;
            }
            return value;
        }
    }

    public static final class Builder {
        private ImmutableMap.Builder<ResourceLocation, CommandFunction> functions;

        private Builder() {
            this(ImmutableMap.builder());
        }

        private Builder(ImmutableMap.Builder<ResourceLocation, CommandFunction> functions) {
            this.functions = functions;
        }

        public Builder put(ResourceLocation name, CommandFunction function) {
            this.functions.put(name, function);
            return this;
        }

        public WorldFunctionInjector build() {
            return new WorldFunctionInjector(this.functions.build());
        }
    }

    @FunctionalInterface
    public interface CommandFunction {
        void accept(CommandSource sender, ParameterMap parameters);
    }

    public static final class ParameterMap {
        private final ImmutableMap<String, Object> parameters;

        private ParameterMap(ImmutableMap<String, Object> parameters) {
            this.parameters = parameters;
        }

        public boolean hasBoolean(String name) {
            return this.get(name) instanceof Boolean;
        }

        public boolean getBoolean(String name, boolean defaultValue) {
            Object value = this.get(name);
            if (value instanceof Boolean) {
                return (Boolean) value;
            }
            return defaultValue;
        }

        public boolean hasInt(String name) {
            return this.get(name) instanceof Number;
        }

        public int getInt(String name, int defaultValue) {
            Object value = this.get(name);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return defaultValue;
        }

        public boolean hasDouble(String name) {
            return this.get(name) instanceof Number;
        }

        public double getDouble(String name, double defaultValue) {
            Object value = this.get(name);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return defaultValue;
        }

        public boolean hasString(String name) {
            return !Objects.toString(this.get(name), "").isEmpty();
        }

        public String getString(String name, String defaultValue) {
            String str = Objects.toString(this.get(name), "");
            if (str.isEmpty()) {
                return defaultValue;
            }
            return str;
        }

        private Object get(String name) {
            return this.parameters.get(name);
        }
    }
}

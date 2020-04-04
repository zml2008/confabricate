/*
 * Copyright 2020 zml
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ca.stellardrift.confabricate;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.types.DynamicOps;
import com.mojang.datafixers.types.Type;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.ValueType;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Implementation of DataFixerUpper's DynamicOps.
 *
 * When possible, the first node's {@link ConfigurationNode#copy()}  method will be used to create a new node to contain results.
 * Otherwise, the provided factory will be used. The default factory creates a {@link CommentedConfigurationNode} with
 * Confabricate's {@link Confabricate#getMinecraftTypeSerializers() own TypeSerializer collection}, but a custom factory may be provided.
 *
 */
public final class ConfigurateOps implements DynamicOps<ConfigurationNode> {
    private static final ConfigurateOps INSTANCE = new ConfigurateOps(() ->
            SimpleCommentedConfigurationNode.root(ConfigurationOptions.defaults()
                    .setSerializers(Confabricate.getMinecraftTypeSerializers())));

    private final Supplier<? extends ConfigurationNode> factory;

    /**
     * Get the shared instance of this class, which creates new nodes using the default factory
     *
     * @return The shared instance
     */
    public static DynamicOps<ConfigurationNode> getInstance() {
        return INSTANCE;
    }

    /**
     * Create a new instance of the ops, with a custom node factory
     *
     * @param factory The factory function
     * @return A new ops instance
     */
    public static DynamicOps<ConfigurationNode> getWithNodeFactory(Supplier<? extends ConfigurationNode> factory) {
        return new ConfigurateOps(factory);
    }

    /**
     * Wrap a ConfigurationNode in a {@link Dynamic} instance. The returned Dynamic will use the same type
     * serializer collection as the original node for its operations.
     *
     * @param node The node to wrap
     * @return a wrapped node
     */
    public static Dynamic<ConfigurationNode> wrap(ConfigurationNode node) {
        if (node.getOptions().getSerializers().equals(Confabricate.getMinecraftTypeSerializers())) {
            return new Dynamic<>(getInstance(), node);
        } else {
            final ConfigurationOptions opts = node.getOptions();
            return new Dynamic<>(getWithNodeFactory(() -> SimpleCommentedConfigurationNode.root(opts)), node);
        }
    }

    protected ConfigurateOps(Supplier<? extends ConfigurationNode> factory) {
        this.factory = factory;
    }

    @Override
    public ConfigurationNode empty() {
        return factory.get();
    }

    @Override
    public Type<?> getType(ConfigurationNode input) {
        if (input == null) {
            throw new NullPointerException("input is null");
        }

        switch (input.getValueType()) {
            case SCALAR:
                Object value = input.getValue();
                if (value instanceof String) {
                    return DSL.string();
                } else if (value instanceof Boolean) {
                    return DSL.bool();
                } else if (value instanceof Short) {
                    return DSL.shortType();
                } else if (value instanceof Integer) {
                    return DSL.intType();
                } else if (value instanceof Long) {
                    return DSL.longType();
                } else if (value instanceof Float) {
                    return DSL.floatType();
                } else if (value instanceof Double) {
                    return DSL.doubleType();
                } else if (value instanceof Byte) {
                    return DSL.byteType();
                } else {
                    throw new IllegalArgumentException("Scalar value '" + input + "' has an unknown type: " + value.getClass().getName());
                }
            case MAP:
                return DSL.compoundList(DSL.remainderType(), DSL.remainderType());
            case LIST:
                return DSL.list(DSL.remainderType());
            case NULL:
                return DSL.nilType();
            default:
                throw new IllegalArgumentException("Value type '" + input + "' has an unknown type: " + input.getValue().getClass().getName());
        }
    }

    @Override
    public Optional<Number> getNumberValue(ConfigurationNode input) {
        if (input.getValueType() == ValueType.SCALAR) {
            if (input.getValue() instanceof Number) {
                return Optional.of((Number) input.getValue());
            } else if (input.getValue() instanceof Boolean) {
                return Optional.of(input.getBoolean() ? 1 : 0);
            }
        }

        return Optional.empty();
    }

    @Override
    public ConfigurationNode createNumeric(Number i) {
        return empty().setValue(i);
    }

    @Override
    public ConfigurationNode createBoolean(final boolean value) {
        return empty().setValue(value);
    }

    @Override
    public Optional<String> getStringValue(ConfigurationNode input) {
        return Optional.ofNullable(input.getString());
    }

    @Override
    public ConfigurationNode createString(String value) {
        return empty().setValue(value);
    }

    @Override
    public ConfigurationNode mergeInto(ConfigurationNode input, ConfigurationNode value) {
        if (input.hasListChildren()) {
            ConfigurationNode ret = input.copy();
            ret.getAppendedNode().setValue(value);
            return ret;
        }
        return input;
    }

    @Override
    public ConfigurationNode mergeInto(ConfigurationNode input, ConfigurationNode key, ConfigurationNode value) {
        return input.copy().getNode(key.getValue()).setValue(value);
    }

    /**
     * Merge into a newly created node
     *
     * @param first The primary node
     * @param second The second node, with values that will override those in the first node
     * @return A newly created node
     */
    @Override
    public ConfigurationNode merge(ConfigurationNode first, ConfigurationNode second) {
        return first.copy().mergeValuesFrom(second);

    }

    @Override
    public Optional<Map<ConfigurationNode, ConfigurationNode>> getMapValues(ConfigurationNode input) {
        if(input.hasMapChildren()) {
            ImmutableMap.Builder<ConfigurationNode, ConfigurationNode> builder = ImmutableMap.builder();
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : input.getChildrenMap().entrySet()) {
                builder.put(empty().setValue(entry.getKey()), entry.getValue().copy());
            }
            return Optional.of(builder.build());
        }

        return Optional.empty();
    }

    @Override
    public ConfigurationNode createMap(Map<ConfigurationNode, ConfigurationNode> map) {
        final ConfigurationNode ret = empty();

        for(Map.Entry<ConfigurationNode, ConfigurationNode> entry : map.entrySet()) {
            ret.getNode(entry.getKey().getValue()).setValue(entry.getValue());
        }

        return ret;
    }

    @Override
    public Optional<Stream<ConfigurationNode>> getStream(ConfigurationNode input) {
        if(input.hasListChildren()) {
            Stream<ConfigurationNode> stream = input.getChildrenList().stream().map(it -> it);
            return Optional.of(stream);
        }

        return Optional.empty();
    }

    @Override
    public ConfigurationNode createList(Stream<ConfigurationNode> input) {
        ConfigurationNode ret = empty();
        input.forEach(it -> {
            ret.getAppendedNode().setValue(it);
        });
        return ret;
    }

    @Override
    public ConfigurationNode remove(ConfigurationNode input, String key) {
        if(input.hasMapChildren()) {
            ConfigurationNode ret = input.copy();
            ret.getNode(key).setValue(null);
            return ret;
        }

        return input;
    }

    @Override
    public Optional<ConfigurationNode> get(final ConfigurationNode input, final String key) {
        ConfigurationNode ret = input.getNode(key);
        return ret.isVirtual() ? Optional.empty() : Optional.of(ret);
    }

    @Override
    public Optional<ConfigurationNode> getGeneric(final ConfigurationNode input, final ConfigurationNode key) {
        ConfigurationNode ret = input.getNode(key.getValue());
        return ret.isVirtual() ? Optional.empty() : Optional.of(ret);
    }

    @Override
    public ConfigurationNode set(final ConfigurationNode input, final String key, final ConfigurationNode value) {
        ConfigurationNode ret = input.copy();
        ret.getNode(key).setValue(value);
        return ret;
    }

    @Override
    public ConfigurationNode update(final ConfigurationNode input, final String key, final Function<ConfigurationNode, ConfigurationNode> function) {
        if (input.getNode(key).isVirtual()) {
            return input;
        }

        ConfigurationNode ret = input.copy();

        ConfigurationNode child = ret.getNode(key);
        child.setValue(function.apply(child));
        return ret;
    }

    @Override
    public ConfigurationNode updateGeneric(final ConfigurationNode input, final ConfigurationNode wrappedKey, final Function<ConfigurationNode, ConfigurationNode> function) {
        final Object key = wrappedKey.getValue();
        if (input.getNode(key).isVirtual()) {
            return input;
        }

        ConfigurationNode ret = input.copy();

        ConfigurationNode child = ret.getNode(key);
        child.setValue(function.apply(child));
        return ret;
    }

    @Override
    public String toString() {
        return "Configurate";
    }
}

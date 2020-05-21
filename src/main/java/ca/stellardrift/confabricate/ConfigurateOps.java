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

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Implementation of DataFixerUpper's DynamicOps.
 *
 * <p>When possible, the first node's {@link ConfigurationNode#copy()} method
 * will be used to create a new node to contain results. Otherwise, the provided
 * factory will be used. The default factory creates a
 * {@link CommentedConfigurationNode} with Confabricate's
 * {@link Confabricate#getMinecraftTypeSerializers() own TypeSerializer collection},
 * but a custom factory may be provided.
 *
 */
public final class ConfigurateOps implements DynamicOps<ConfigurationNode> {

    private static final ConfigurateOps INSTANCE = new ConfigurateOps(() ->
            CommentedConfigurationNode.root(ConfigurationOptions.defaults()
                    .withSerializers(Confabricate.getMinecraftTypeSerializers())));

    private final Supplier<? extends ConfigurationNode> factory;

    /**
     * Get the shared instance of this class, which creates new nodes using
     * the default factory.
     *
     * @return The shared instance
     */
    public static DynamicOps<ConfigurationNode> getInstance() {
        return INSTANCE;
    }

    /**
     * Create a new instance of the ops, with a custom node factory.
     *
     * @param factory The factory function
     * @return A new ops instance
     */
    public static DynamicOps<ConfigurationNode> getWithNodeFactory(final Supplier<? extends ConfigurationNode> factory) {
        return new ConfigurateOps(factory);
    }

    /**
     * Wrap a ConfigurationNode in a {@link Dynamic} instance. The returned
     * Dynamic will use the same type serializer collection as the original node
     * for its operations.
     *
     * @param node The node to wrap
     * @return a wrapped node
     */
    public static Dynamic<ConfigurationNode> wrap(final ConfigurationNode node) {
        if (node.getOptions().getSerializers().equals(Confabricate.getMinecraftTypeSerializers())) {
            return new Dynamic<>(getInstance(), node);
        } else {
            final ConfigurationOptions opts = node.getOptions();
            return new Dynamic<>(getWithNodeFactory(() -> CommentedConfigurationNode.root(opts)), node);
        }
    }

    protected ConfigurateOps(final Supplier<? extends ConfigurationNode> factory) {
        this.factory = factory;
    }

    @Override
    public ConfigurationNode empty() {
        return this.factory.get();
    }

    @Override
    public <U> U convertTo(final DynamicOps<U> outOps, final ConfigurationNode input) {
        if (input == null) {
            throw new NullPointerException("input is null");
        }

        if (input.isMap()) {
            return convertMap(outOps, input);

        } else if (input.isList()) {
            return convertList(outOps, input);
        } else {
            final Object value = input.getValue();
            if (value == null) {
                return outOps.empty();
            } else if (value instanceof String) {
                return outOps.createString((String) value);
            } else if (value instanceof Boolean) {
                return outOps.createBoolean((Boolean) value);
            } else if (value instanceof Short) {
                return outOps.createShort((Short) value);
            } else if (value instanceof Integer) {
                return outOps.createInt((Integer) value);
            } else if (value instanceof Long) {
                return outOps.createLong((Long) value);
            } else if (value instanceof Float) {
                return outOps.createFloat((Float) value);
            } else if (value instanceof Double) {
                return outOps.createDouble((Double) value);
            } else if (value instanceof Byte) {
                return outOps.createByte((Byte) value);
            } else {
                throw new IllegalArgumentException("Scalar value '" + input + "' has an unknown type: " + value.getClass().getName());
            }
        }
    }

    @Override
    public DataResult<Number> getNumberValue(final ConfigurationNode input) {
        if (!(input.isMap() || input.isList())) {
            if (input.getValue() instanceof Number) {
                return DataResult.success((Number) input.getValue());
            } else if (input.getValue() instanceof Boolean) {
                return DataResult.success(input.getBoolean() ? 1 : 0);
            }
        }

        return DataResult.error("Not a number: " + input);
    }

    @Override
    public ConfigurationNode createNumeric(final Number i) {
        return empty().setValue(i);
    }

    @Override
    public ConfigurationNode createBoolean(final boolean value) {
        return empty().setValue(value);
    }

    @Override
    public DataResult<String> getStringValue(final ConfigurationNode input) {
        if (input.getString() != null) {
            return DataResult.success(input.getString());
        }

        return DataResult.error("Not a string: " + input);
    }

    @Override
    public ConfigurationNode createString(final String value) {
        return empty().setValue(value);
    }

    @Override
    public DataResult<ConfigurationNode> mergeToList(final ConfigurationNode input, final ConfigurationNode value) {
        if (input.isList()) {
            final ConfigurationNode ret = input.copy();
            ret.appendListNode().setValue(value);
            return DataResult.success(ret);
        }

        return DataResult.error("mergeToList called on a node which is not a list: " + input, input);
    }

    @Override
    public DataResult<ConfigurationNode> mergeToMap(final ConfigurationNode input, final ConfigurationNode key, final ConfigurationNode value) {
        if (input.isMap()) {
            return DataResult.success(input.copy().getNode(key.getValue()).setValue(value));
        }

        return DataResult.error("mergeToMap called on a node which is not a map: " + input, input);
    }

    @Override
    public DataResult<Stream<Pair<ConfigurationNode, ConfigurationNode>>> getMapValues(final ConfigurationNode input) {
        if (input.isMap()) {
            return DataResult.success(input.getChildrenMap().entrySet().stream()
                    .map(entry -> Pair.of(empty().setValue(entry.getKey()), entry.getValue().copy())));
        }

        return DataResult.error("Not a map: " + input);
    }

    @Override
    public ConfigurationNode createMap(final Stream<Pair<ConfigurationNode, ConfigurationNode>> map) {
        final ConfigurationNode ret = empty();

        map.forEach(p -> ret.getNode(p.getFirst().getValue()).setValue(p.getSecond().getValue()));

        return ret;
    }

    @Override
    public ConfigurationNode createMap(final Map<ConfigurationNode, ConfigurationNode> map) {
        final ConfigurationNode ret = empty();

        for (Map.Entry<ConfigurationNode, ConfigurationNode> entry : map.entrySet()) {
            ret.getNode(entry.getKey().getValue()).setValue(entry.getValue());
        }

        return ret;
    }

    @Override
    public DataResult<Stream<ConfigurationNode>> getStream(final ConfigurationNode input) {
        if (input.isList()) {
            final Stream<ConfigurationNode> stream = input.getChildrenList().stream().map(it -> it);
            return DataResult.success(stream);
        }

        return DataResult.error("Not a list: " + input);
    }

    @Override
    public ConfigurationNode createList(final Stream<ConfigurationNode> input) {
        final ConfigurationNode ret = empty();
        input.forEach(it -> {
            ret.appendListNode().setValue(it);
        });
        return ret;
    }

    @Override
    public ConfigurationNode remove(final ConfigurationNode input, final String key) {
        if (input.isMap()) {
            final ConfigurationNode ret = input.copy();
            ret.getNode(key).setValue(null);
            return ret;
        }

        return input;
    }

    @Override
    public DataResult<ConfigurationNode> get(final ConfigurationNode input, final String key) {
        final ConfigurationNode ret = input.getNode(key);
        return ret.isVirtual() ? DataResult.error("No element " + key + " in the map " + input) : DataResult.success(ret);
    }

    @Override
    public DataResult<ConfigurationNode> getGeneric(final ConfigurationNode input, final ConfigurationNode key) {
        final ConfigurationNode ret = input.getNode(key.getValue());
        return ret.isVirtual() ? DataResult.error("No element " + key + " in the map " + input) : DataResult.success(ret);
    }

    @Override
    public ConfigurationNode set(final ConfigurationNode input, final String key, final ConfigurationNode value) {
        final ConfigurationNode ret = input.copy();
        ret.getNode(key).setValue(value);
        return ret;
    }

    @Override
    public ConfigurationNode update(final ConfigurationNode input, final String key, final Function<ConfigurationNode, ConfigurationNode> function) {
        if (input.getNode(key).isVirtual()) {
            return input;
        }

        final ConfigurationNode ret = input.copy();
        final ConfigurationNode child = ret.getNode(key);
        child.setValue(function.apply(child));
        return ret;
    }

    @Override
    public ConfigurationNode updateGeneric(final ConfigurationNode input, final ConfigurationNode wrappedKey,
            final Function<ConfigurationNode, ConfigurationNode> function) {
        final Object key = wrappedKey.getValue();
        if (input.getNode(key).isVirtual()) {
            return input;
        }

        final ConfigurationNode ret = input.copy();

        final ConfigurationNode child = ret.getNode(key);
        child.setValue(function.apply(child));
        return ret;
    }

    @Override
    public String toString() {
        return "Configurate";
    }

}

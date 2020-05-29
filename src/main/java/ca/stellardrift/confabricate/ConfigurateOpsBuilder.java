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

import static java.util.Objects.requireNonNull;

import com.mojang.serialization.Dynamic;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;

import java.util.function.Supplier;

/**
 * A builder for {@link ConfigurateOps} instances.
 */
public final class ConfigurateOpsBuilder {

    private Supplier<ConfigurationNode> nodeSupplier = ConfigurateOps::createDefaultNode;
    private boolean compressed = false;

    ConfigurateOpsBuilder() {}

    /**
     * Set the node factory for the returned ops.
     *
     * <p>The default node factory wil create {@link CommentedConfigurationNode}
     * instances using Confabricate's minecraft serializers.
     *
     * @param supplier source for new nodes created to store values in
     *     the {@code create*} methods
     * @return this
     */
    public ConfigurateOpsBuilder factory(final Supplier<ConfigurationNode> supplier) {
        this.nodeSupplier = requireNonNull(supplier, "nodeSupplier");
        return this;
    }

    /**
     * Set a node factory that will use the provided collection.
     *
     * <p>This will replace any set {@link #factory(Supplier)}.
     *
     * @param collection type serializers to use for nodes.
     * @return this
     */
    public ConfigurateOpsBuilder factoryFromSerializers(final TypeSerializerCollection collection) {
        requireNonNull(collection, "collection");
        return factory(() -> CommentedConfigurationNode.root(ConfigurationOptions.defaults().withSerializers(collection)));
    }

    public ConfigurateOpsBuilder factoryFromNode(final ConfigurationNode node) {
        final ConfigurationOptions options = requireNonNull(node, "node").getOptions();
        return factory(() -> CommentedConfigurationNode.root(options));
    }

    /**
     * Set whether {@link com.mojang.serialization.Keyable} values should be compressed.
     *
     * @see ConfigurateOps#compressMaps() for more about what compression is
     * @param compressed whether to compress values
     * @return this
     */
    public ConfigurateOpsBuilder compressed(final boolean compressed) {
        this.compressed = compressed;
        return this;
    }

    /**
     * Create a new ops instance.
     *
     * <p>All options have defaults provided and all setters validate their
     * input, so by the time this method is reached the builder will be in a
     * valid state.
     *
     * @return The new instance
     */
    public ConfigurateOps build() {
        return new ConfigurateOps(this.nodeSupplier, this.compressed);
    }

    /**
     * Build a new ops instance, returned as part of a {@linkplain Dynamic}.
     *
     * <p>Returned ops instances will not take type serializers or other options
     * from the provided node. For that, use {@link #factoryFromNode(ConfigurationNode)}.
     *
     * @param node wrapped node
     * @return new dynamic
     */
    public Dynamic<ConfigurationNode> buildWrapping(final ConfigurationNode node) {
        return new Dynamic<>(build(), node);
    }

}

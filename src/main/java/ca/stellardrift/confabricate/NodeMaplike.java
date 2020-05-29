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
import com.mojang.serialization.MapLike;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.stream.Stream;

/**
 * View of a configuration node in its map representation.
 */
final class NodeMaplike implements MapLike<ConfigurationNode> {

    private final ConfigurateOps ops;
    private final ConfigurationOptions options;
    private final Map<Object, ? extends ConfigurationNode> node;

    NodeMaplike(final ConfigurateOps ops, final ConfigurationOptions options, final Map<Object, ? extends ConfigurationNode> node) {
        this.ops = ops;
        this.options = options;
        this.node = node;
    }

    @Override
    public @Nullable ConfigurationNode get(final ConfigurationNode key) {
        final @Nullable ConfigurationNode ret = this.node.get(ConfigurateOps.keyFrom(key));
        return ret == null ? null : this.ops.guardOutputRead(ret);
    }

    @Override
    public @Nullable ConfigurationNode get(final String key) {
        final @Nullable ConfigurationNode ret = this.node.get(key);
        return ret == null ? null : this.ops.guardOutputRead(ret);
    }

    @Override
    public Stream<Pair<ConfigurationNode, ConfigurationNode>> entries() {
        return this.node.entrySet().stream()
                .map(ent -> Pair.of(ConfigurationNode.root(this.options).setValue(ent.getKey()), this.ops.guardOutputRead(ent.getValue())));
    }

}

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

package ca.stellardrift.confabricate.typeserializers;

import static java.util.Objects.requireNonNull;

import ca.stellardrift.confabricate.ConfigurateOps;
import com.google.common.reflect.TypeToken;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * TypeSerializer implementation wrapping around codecs.
 */
final class CodecSerializer<V> implements TypeSerializer<V> {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Codec<V> codec;

    CodecSerializer(final Codec<V> codec) {
        this.codec = requireNonNull(codec, "codec");
    }

    @Override
    public @Nullable V deserialize(@NonNull final TypeToken<?> type, @NonNull final ConfigurationNode value) throws ObjectMappingException {
        final DataResult<Pair<V, ConfigurationNode>> result = this.codec.decode(ConfigurateOps.fromNode(value), value);
        final DataResult.PartialResult<Pair<V, ConfigurationNode>> error = result.error().orElse(null);
        if (error != null) {
            LOGGER.trace("Unable to decode value using {} due to {}", this.codec, error);
            throw new ObjectMappingException(error.message());
        }
        return result.result().orElseThrow(() -> new ObjectMappingException("Neither a result or error was present")).getFirst();
    }

    @Override public void serialize(@NonNull final TypeToken<?> type, @Nullable final V obj, @NonNull final ConfigurationNode value)
            throws ObjectMappingException {
        final DataResult<ConfigurationNode> result = this.codec.encode(obj, ConfigurateOps.fromNode(value), value);
        final DataResult.PartialResult<ConfigurationNode> error = result.error().orElse(null);
        if (error != null) {
            LOGGER.trace("Unable to encode value using {} due to {}", this.codec, error);
            throw new ObjectMappingException(error.message());
        }

        value.setValue(result.result().orElseThrow(() -> new ObjectMappingException("Neither a result or error was present")));
    }

}

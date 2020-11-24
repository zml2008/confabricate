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

import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

final class RegistrySerializer<T> implements TypeSerializer<T> {

    private final Registry<T> registry;

    RegistrySerializer(final Registry<T> registry) {
        this.registry = registry;
    }

    @Override
    public @Nullable T deserialize(final @NonNull Type type, final @NonNull ConfigurationNode value) throws SerializationException {
        final Identifier ident = IdentifierSerializer.fromNode(value);
        if (ident == null) {
            return null;
        }

        return this.registry.get(ident);
    }

    @Override
    public void serialize(final @NonNull Type type, final @Nullable T obj,
            final @NonNull ConfigurationNode value) throws SerializationException {
        if (obj == null) {
            value.raw(null);
        }

        final Identifier ident = this.registry.getId(obj);
        if (ident == null) {
            throw new SerializationException("Unknown registry element " + obj);
        }
        IdentifierSerializer.toNode(ident, value);
    }

}

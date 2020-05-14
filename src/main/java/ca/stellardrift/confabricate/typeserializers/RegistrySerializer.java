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

import com.google.common.reflect.TypeToken;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RegistrySerializer<T> implements TypeSerializer<T> {
    private final Registry<T> registry;

    public RegistrySerializer(Registry<T> registry) {
        this.registry = registry;
    }


    @Nullable
    @Override
    public T deserialize(@NonNull TypeToken<?> type, @NonNull ConfigurationNode value) throws ObjectMappingException {
        Identifier ident = IdentifierSerializer.fromNode(value);
        if (ident == null) {
            return null;
        }

        return registry.get(ident);
    }

    @Override
    public void serialize(@NonNull TypeToken<?> type, @Nullable T obj, @NonNull ConfigurationNode value) throws ObjectMappingException {
        if (obj == null) {
            value.setValue(null);
        }

        Identifier ident = registry.getId(obj);
        if (ident == null) {
            throw new ObjectMappingException("Unknown registry element " + obj);
        }
        IdentifierSerializer.toNode(ident, value);
    }
}

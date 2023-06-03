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

import java.lang.reflect.Type;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

final class RegistrySerializer<T> extends RegistryBasedSerializer<T, T> {

    RegistrySerializer(final RegistryAccess access, final ResourceKey<? extends Registry<T>> registry) {
        super(access, registry);
    }

    @Override
    public @Nullable T deserialize(final @NonNull Type type, final @NonNull ConfigurationNode value) throws SerializationException {
        final ResourceLocation loc = ResourceLocationSerializer.fromNode(value);
        if (loc == null) {
            return null;
        }

        return this.registry().get(loc);
    }

    @Override
    public void serialize(final @NonNull Type type, final @Nullable T obj,
            final @NonNull ConfigurationNode value) throws SerializationException {
        if (obj == null) {
            value.raw(null);
        }

        final ResourceLocation loc = this.registry().getKey(obj);
        if (loc == null) {
            throw new SerializationException("Unknown registry element " + obj);
        }
        ResourceLocationSerializer.toNode(loc, value);
    }

}

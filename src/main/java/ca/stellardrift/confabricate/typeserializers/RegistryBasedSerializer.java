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

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

/**
 * An abstract supertype for serializers working with registries.
 *
 * @param <E> registry entry type
 * @param <V> handled config value type
 */
abstract class RegistryBasedSerializer<E, V> implements TypeSerializer<V> {

    private final RegistryAccess access;
    protected final ResourceKey<? extends Registry<E>> registry;

    RegistryBasedSerializer(final RegistryAccess access, final ResourceKey<? extends Registry<E>> registry) {
        this.access = access;
        this.registry = registry;
    }

    protected Registry<E> uncheckedRegistry() {
        return this.access.registryOrThrow(this.registry);
    }

    protected Registry<E> registry() throws SerializationException {
        return this.access.registry(this.registry)
            .orElseThrow(() -> new SerializationException("No registry " + this.registry + " present in the current context!"));
    }

}

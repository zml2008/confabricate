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

import ca.stellardrift.confabricate.Confabricate;
import ca.stellardrift.confabricate.ConfigurateOps;
import com.google.common.reflect.TypeToken;
import com.mojang.serialization.Codec;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;

/**
 * Access serializers for Minecraft types.
 *
 * <p>The {@link #collection()} provides an easily accessible collection of
 * built-in type serializers, while other factory methods allow creating custom
 * type serializers that interact with game serialization mechanisms.
 */
public final class MinecraftSerializers {

    private static TypeSerializerCollection MINECRAFT_COLLECTION;

    private MinecraftSerializers() {}

    public static <V> TypeSerializer<V> forCodec(final Codec<V> codec) {
        return new CodecSerializer<>(codec);
    }

    public static <V, S extends V> Codec<S> forSerializer(final TypeToken<S> type) {
        return forSerializer(type, MINECRAFT_COLLECTION);
    }

    public static <V> Codec<V> forSerializer(final TypeToken<V> type, final TypeSerializerCollection collection) {
        final TypeSerializer<V> serial = collection.get(type);
        return new TypeSerializerCodec<>(type, serial, ConfigurateOps.getForSerializers(collection));
    }

    /**
     * The default collection of game serializers.
     *
     * <p>This collection includes:
     *
     * @return minecraft serializers
     */
    public static TypeSerializerCollection collection() {
        if (MINECRAFT_COLLECTION == null) {
            MINECRAFT_COLLECTION = Confabricate.getMinecraftTypeSerializers();
            // populate(TypeSerializerCollection.defaults().newChild());
        }
        return MINECRAFT_COLLECTION;
    }

    /**
     * Register Minecraft {@link TypeSerializer}s with the provided collection.
     *
     * @param collection to populate
     * @return provided collection
     */
    public static TypeSerializerCollection populate(final TypeSerializerCollection collection) {
        throw new UnsupportedOperationException("Not yet implemented");
        //return collection;
    }

}

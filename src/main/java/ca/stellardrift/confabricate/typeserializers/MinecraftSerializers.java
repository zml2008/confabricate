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

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.geantyref.TypeToken;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.dimension.DimensionType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.extra.dfu.v4.ConfigurateOps;
import org.spongepowered.configurate.extra.dfu.v4.DfuSerializers;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

/**
 * Access serializers for Minecraft types.
 *
 * <p>The {@link #collection()} provides an easily accessible collection of
 * built-in type serializers, while other factory methods allow creating custom
 * type serializers that interact with game serialization mechanisms.
 *
 * @since 1.2.0
 */
public final class MinecraftSerializers {

    // impl note: initialization order is critical here to ensure we can test
    // most parts of Confabricate without having to fully initialize Minecraft
    // and use any of our Mixins

    /**
     * Registries that should not be added to a serializer collection.
     */
    private static final Set<Registry<?>> SPECIAL_REGISTRIES = Set.of(Registry.CUSTOM_STAT); // Type of identifier

    private static @LazyInit Set<Map.Entry<Type, TypeSerializer<?>>> KNOWN_REGISTRIES;

    private static final TypeToken<Registry<?>> TYPE_REGISTRY_GENERIC = new TypeToken<Registry<?>>() {};
    private static final Logger LOGGER = LogManager.getLogger();

    private static @LazyInit TypeSerializerCollection MINECRAFT_COLLECTION;
    private static @LazyInit ConfigurateOps DEFAULT_OPS;

    private MinecraftSerializers() {}

    static DynamicOps<ConfigurationNode> opsFor(final ConfigurationNode node) {
        if (node.options().serializers().equals(MINECRAFT_COLLECTION)) {
            final @Nullable ConfigurateOps ops = DEFAULT_OPS;
            if (ops == null) {
                return DEFAULT_OPS = ConfigurateOps.builder()
                                .factoryFromSerializers(collection())
                                .readWriteProtection(ConfigurateOps.Protection.NONE)
                                .build();
            }
            return DEFAULT_OPS;
        } else {
            return ConfigurateOps.builder()
                    .factoryFromNode(node)
                    .readWriteProtection(ConfigurateOps.Protection.NONE)
                    .build();
        }
    }

    /**
     * Create a new serializer wrapping the provided {@link Codec}.
     *
     * @param codec codec to use for the serialization operation
     * @param <V> value type
     * @return a new serializer
     * @see DfuSerializers#serializer(Codec) for more information
     * @since 2.0.0
     */
    public static <V> TypeSerializer<V> serializer(final Codec<V> codec) {
        return DfuSerializers.serializer(codec);
    }

    /**
     * Create a new codec that uses the default type serializer collection
     * to serialize an object of the provided type.
     *
     * @param type token representing a value type
     * @param <S> value type
     * @return a codec for the type, or null if an appropriate
     *      {@link TypeSerializer} could not be found.
     * @see DfuSerializers#codec(TypeToken) for more information
     * @since 2.0.0
     */
    public static <S> @Nullable Codec<S> codec(final TypeToken<S> type) {
        return codec(type, collection());
    }

    /**
     * Create a new codec based on a Configurate {@link TypeSerializer}.
     *
     * @param type type to serialize
     * @param collection source for values
     * @param <V> value type
     * @return a codec, or null if an appropriate {@link TypeSerializer}
     *      could not be found for the TypeToken.
     * @see DfuSerializers#codec(TypeToken, TypeSerializerCollection)
     * @since 2.0.0
     */
    public static <V> @Nullable Codec<V> codec(final TypeToken<V> type, final TypeSerializerCollection collection) {
        return DfuSerializers.codec(type, collection);
    }

    /**
     * Create a {@link TypeSerializer} than can interpret values in the
     * provided registry.
     *
     * @param registry the registry
     * @param <T> the type registered by the registry
     * @return a serializer for the registry
     * @since 1.2.0
     */
    public static <T> TypeSerializer<T> forRegistry(final Registry<T> registry) {
        return new RegistrySerializer<>(registry);
    }

    /**
     * The default collection of game serializers.
     *
     * <p>While the collection is mutable, modifying it is discouraged in favor
     * of working with a new child, created with
     * {@link TypeSerializerCollection#childBuilder()} ()}. Collections of serializers
     * will become immutable in Configurate 4.0
     *
     * @return minecraft serializers
     * @see #populate(TypeSerializerCollection.Builder) for information about
     *      which serializers this collection will include
     * @since 1.2.0
     */
    public static TypeSerializerCollection collection() {
        TypeSerializerCollection collection = MINECRAFT_COLLECTION;
        if (collection == null) {
            collection = MINECRAFT_COLLECTION = populate(TypeSerializerCollection.defaults().childBuilder()).build();
        }
        return collection;
    }

    /**
     * Check if a collection is our populated collection without attempting to
     * initialize serializers.
     *
     * <p>This helps to integrate with Confabricate in test environments.
     *
     * @param collection collection to test
     * @return if tested collection is the confabricate default collection
     * @since 1.2.0
     */
    public static boolean isCommonCollection(final TypeSerializerCollection collection) {
        return requireNonNull(collection, "collection").equals(MINECRAFT_COLLECTION);
    }

    private static boolean shouldRegister(final Registry<?> registry) {
        // Don't register root registry -- its key can't be looked up :(
        return !SPECIAL_REGISTRIES.contains(registry);
    }

    /**
     * Register Minecraft {@link TypeSerializer}s with the provided collection.
     *
     * <p>This will add serializers for: <ul>
     *     <li>{@link ResourceLocation resource locations}</li>
     *     <li>{@link net.minecraft.network.chat.Component} (as a string)</li>
     *     <li>Any elements in vanilla {@link Registry registries}</li>
     *     <li>{@link HolderSet} of a combination of identifiers and
     *          tags for any taggable registry</li>
     *     <li>{@link ItemStack}</li>
     *     <li>{@link CompoundTag} instances</li>
     * </ul>
     *
     * @param collection to populate
     * @return provided collection
     * @since 1.2.0
     */
    public static TypeSerializerCollection.Builder populate(final TypeSerializerCollection.Builder collection) {
        collection.registerExact(ResourceLocation.class, ResourceLocationSerializer.INSTANCE)
                .register(Component.class, ComponentSerializer.INSTANCE);

        for (final Map.Entry<Type, TypeSerializer<?>> registry : knownRegistries()) {
            registerRegistry(collection, registry.getKey(), registry.getValue());
        }

        collection.register(ItemStack.class, serializer(ItemStack.CODEC));
        collection.register(CompoundTag.class, serializer(CompoundTag.CODEC));

        // All registries here should be in SPECIAL_REGISTRIES

        collection.registerAll(GsonConfigurationLoader.gsonSerializers());

        return collection;
    }

    // Type serializers that use the server resource manager
    private static TypeSerializerCollection.Builder populateServer(final MinecraftServer server, final TypeSerializerCollection.Builder collection) {
        registerRegistry(collection, DimensionType.class, forRegistry(server.registryAccess().registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY)));
        return collection;
    }

    // Type serializers that source registry + tag information from the client
    @Environment(EnvType.CLIENT)
    private static TypeSerializerCollection.Builder populateClient(final MinecraftClient client, final TypeSerializerCollection.Builder collection) {
        return collection;
    }

    /**
     * Lazily initialize our set of vanilla registries.
     *
     * <p>This is moderately expensive due to having to reflectively analyze
     * the fields in the Registry class, so we cache the created serializers
     * after the first initialization.
     *
     * @return collection of built-in registries
     */
    private static Set<Map.Entry<Type, TypeSerializer<?>>> knownRegistries() {
        Set<Map.Entry<Type, TypeSerializer<?>>> registries = KNOWN_REGISTRIES;
        if (registries == null) {
            final ImmutableSet.Builder<Map.Entry<Type, TypeSerializer<?>>> accumulator = ImmutableSet.builder();
            for (final Field registryField : Registry.class.getFields()) {
                // only look at public static fields (excludes the ROOT registry)
                if ((registryField.getModifiers() & (Modifier.STATIC | Modifier.PUBLIC)) != (Modifier.STATIC | Modifier.PUBLIC)) {
                    continue;
                }

                final Type fieldType = registryField.getGenericType();
                if (!GenericTypeReflector.isSuperType(TYPE_REGISTRY_GENERIC.getType(), fieldType)) { // if not a registry (keys)
                    continue;
                }

                final Type elementType = ((ParameterizedType) fieldType).getActualTypeArguments()[0];
                try {
                    final Registry<?> registry = (Registry<?>) registryField.get(null);
                    if (shouldRegister(registry)) {
                        accumulator.add(new AbstractMap.SimpleImmutableEntry<>(elementType, forRegistry(registry)));
                        LOGGER.debug("Created serializer for Minecraft registry {} with element type {}", registry, elementType);
                    }
                } catch (final IllegalAccessException e) {
                    LOGGER.error("Unable to create serializer for registry of type {} due to access error", elementType, e);
                }
            }
            registries = KNOWN_REGISTRIES = accumulator.build();
        }
        return registries;
    }

    // Limit scope of warning suppression for reflective registry initialization
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerRegistry(final TypeSerializerCollection.Builder collection, final Type type, final TypeSerializer<?> registry) {
        collection.registerExact(TypeToken.get(type), (TypeSerializer) registry);
    }

    /**
     * Add a serializer for the registry and its {@link HolderSet}.
     *
     * @param collection the collection to serialize
     * @param token generic element type of the registry
     * @param registry registry containing values
     * @param <T> element type
     */
    @SuppressWarnings("unchecked")
    private static <T> void populateTaggedRegistry(final TypeSerializerCollection.Builder collection, final TypeToken<T> token,
            final Registry<T> registry) {
        final Type tagType = TypeFactory.parameterizedClass(HolderSet.class, token.getType());

        // collection.registerExact((TypeToken<RegistryEntryList<T>>) TypeToken.get(tagType), new TagSerializer<>(registry, tagRegistry));
        // collection.registerExact(token, new RegistrySerializer<>(registry));
    }

}

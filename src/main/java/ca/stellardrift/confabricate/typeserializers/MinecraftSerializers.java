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
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;
import io.leangen.geantyref.TypeToken;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
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
import java.lang.reflect.WildcardType;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

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

    private static final Logger LOGGER = LogUtils.getLogger();

    // impl note: initialization order is critical here to ensure we can test
    // most parts of Confabricate without having to fully initialize Minecraft
    // and use any of our Mixins

    /**
     * Registries that should not be added to a serializer collection.
     */
    private static final Set<ResourceKey<? extends Registry<?>>> SPECIAL_REGISTRIES = Set.of(Registry.CUSTOM_STAT_REGISTRY); // Type of RL
    private static final TypeToken<ResourceKey<? extends Registry<?>>> TYPE_RESOURCE_KEY_GENERIC
        = new TypeToken<ResourceKey<? extends Registry<?>>>() {};

    private static @LazyInit Set<Map.Entry<Type, ResourceKey<? extends Registry<?>>>> KNOWN_REGISTRIES;
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
     * Populate a type serializer collection with all applicable serializers
     * for values in a registry.
     *
     * <p>This includes a serializer for the literal values, a serializer
     * for {@link Holder Holders} of values, and a serializer for
     * {@link HolderSet HolderSets} (tags) of values.</p>
     *
     * @param builder the builder to populate
     * @param entryType the type contained in the registry
     * @param access registry access to read the registry contents from
     * @param registry the registry to query
     * @param <T> the type registered by the registry
     * @since 3.0.0
     */
    @SuppressWarnings("unchecked")
    public static <T> void populateRegistry(
        final TypeSerializerCollection.Builder builder,
        final TypeToken<T> entryType,
        final RegistryAccess access,
        final ResourceKey<? extends Registry<T>> registry
    ) {
        // serializer
        // holder serializer
        builder.registerExact(entryType, new RegistrySerializer<>(access, registry));
        builder.registerExact(
            (TypeToken<Holder<T>>) TypeToken.get(TypeFactory.parameterizedClass(Holder.class, entryType.getType())),
            new HolderSerializer<>(access, registry)
        );
        builder.registerExact(
            (TypeToken<HolderSet<T>>) TypeToken.get(TypeFactory.parameterizedClass(HolderSet.class, entryType.getType())),
            new HolderSetSerializer<>(access, registry)
        );
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

    private static boolean shouldRegister(final ResourceKey<? extends Registry<?>> registry) {
        // Don't register root registry -- its key can't be looked up :(
        return !SPECIAL_REGISTRIES.contains(registry);
    }

    /**
     * Register Minecraft {@link TypeSerializer}s with the provided collection.
     *
     * <p>This will add serializers for: <ul>
     *     <li>{@link ResourceLocation resource locations}</li>
     *     <li>{@link net.minecraft.network.chat.Component} (as a string)</li>
     *     <li>Any individual elements in vanilla {@link Registry registries},
     *         as raw values or {@link Holder}</li>
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

        for (final Map.Entry<Type, ResourceKey<? extends Registry<?>>> registry : knownRegistries()) {
            registerRegistry(
                collection,
                registry.getKey(),
                RegistryAccess.BUILTIN,
                registry.getValue()
            );
        }

        collection.register(ItemStack.class, serializer(ItemStack.CODEC));
        collection.register(CompoundTag.class, serializer(CompoundTag.CODEC));

        // All registries here should be in SPECIAL_REGISTRIES

        collection.registerAll(GsonConfigurationLoader.gsonSerializers());

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
    @SuppressWarnings("unchecked")
    private static Set<Map.Entry<Type, ResourceKey<? extends Registry<?>>>> knownRegistries() {
        Set<Map.Entry<Type, ResourceKey<? extends Registry<?>>>> registries = KNOWN_REGISTRIES;
        if (registries == null) {
            final ImmutableSet.Builder<Map.Entry<Type, ResourceKey<? extends Registry<?>>>> accumulator = ImmutableSet.builder();
            for (final Field registryField : Registry.class.getFields()) {
                // only look at public static fields (excludes the ROOT registry)
                if ((registryField.getModifiers() & (Modifier.STATIC | Modifier.PUBLIC)) != (Modifier.STATIC | Modifier.PUBLIC)) {
                    continue;
                }

                final Type fieldType = registryField.getGenericType();
                if (!GenericTypeReflector.isSuperType(TYPE_RESOURCE_KEY_GENERIC.getType(), fieldType)) { // if not a registry
                    continue;
                }

                final ResourceKey<? extends Registry<?>> registry;
                try {
                    registry = (ResourceKey<? extends Registry<?>>) registryField.get(null);
                } catch (final IllegalAccessException e) {
                    LOGGER.error("Unable to create serializer for registry of type {} due to access error", fieldType, e);
                    continue;
                }

                try {
                    if (shouldRegister(registry)) {
                        final Type registryKeyType = ((ParameterizedType) fieldType).getActualTypeArguments()[0];
                        final Type registryType =
                            registryKeyType instanceof WildcardType ? ((WildcardType) registryKeyType).getUpperBounds()[0]
                            : registryKeyType;
                        final Type elementType = ((ParameterizedType) registryType).getActualTypeArguments()[0];
                        accumulator.add(Map.entry(elementType, registry));
                        LOGGER.debug("Created serializer for Minecraft registry {} with element type {}", registry, elementType);
                    }
                } catch (final Exception ex) {
                    LOGGER.error("Error attempting to discover registry entry type for {} from field type {}", registry, fieldType, ex);
                }
            }
            registries = KNOWN_REGISTRIES = accumulator.build();
        }
        return registries;
    }

    // Limit scope of warning suppression for reflective registry initialization
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerRegistry(
        final TypeSerializerCollection.Builder collection,
        final Type type,
        final Supplier<? extends RegistryAccess> access,
        final ResourceKey<? extends Registry<?>> registry
    ) {
        populateRegistry(
            collection,
            TypeToken.get(type),
            access.get(), // todo: make things lazy?
            (ResourceKey) registry
        );
    }

}

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
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.EntityTypeTags;
import net.minecraft.tag.FluidTags;
import net.minecraft.tag.ItemTags;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagGroup;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.DimensionType;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Access serializers for Minecraft types.
 *
 * <p>The {@link #collection()} provides an easily accessible collection of
 * built-in type serializers, while other factory methods allow creating custom
 * type serializers that interact with game serialization mechanisms.
 */
public final class MinecraftSerializers {

    // impl note: initialization order is critical here to ensure we can test
    // most parts of Confabricate without having to fully initialize Minecraft
    // and use any of our Mixins

    /**
     * Registries that should not be added to a serializer collection.
     */
    private static final ImmutableSet<Registry<?>> SPECIAL_REGISTRIES =
            ImmutableSet.of(Registry.CUSTOM_STAT, // Type of identifier
                    Registry.FLUID,
                    Registry.BLOCK,
                    Registry.ITEM,
                    Registry.ENTITY_TYPE
            );

    private static @LazyInit Set<Map.Entry<TypeToken<?>, TypeSerializer<?>>> KNOWN_REGISTRIES;

    private static final TypeToken<Registry<?>> TYPE_REGISTRY_GENERIC = new TypeToken<Registry<?>>() {};
    private static final Type TYPE_REGISTRY_ELEMENT = Registry.class.getTypeParameters()[0];
    private static final Logger LOGGER = LogManager.getLogger();

    private static @LazyInit TypeSerializerCollection MINECRAFT_COLLECTION;

    private MinecraftSerializers() {}

    public static <V> TypeSerializer<V> forCodec(final Codec<V> codec) {
        return new CodecSerializer<>(codec);
    }

    public static <V, S extends V> @Nullable Codec<S> forSerializer(final TypeToken<S> type) {
        return forSerializer(type, collection());
    }

    /**
     * Create a new codec based on a Configurate {@link TypeSerializer}.
     *
     * @param type type to serialize
     * @param collection source for values
     * @param <V> value type
     * @return a codec, or null if an appropriate {@link TypeSerializer}
     *      could not be found for the TypeToken.
     */
    public static <V> @Nullable Codec<V> forSerializer(final TypeToken<V> type, final TypeSerializerCollection collection) {
        final TypeSerializer<V> serial = collection.get(type);
        if (serial == null) {
            return null;
        }
        return new TypeSerializerCodec<>(type, serial, ConfigurateOps.getForSerializers(collection)).withLifecycle(Lifecycle.stable());
    }

    /**
     * Create a {@link TypeSerializer} than can interpret values in the
     * provided registry.
     *
     * @param registry The registry
     * @param <T> The type registered by the registry
     * @return a serializer for the registry
     */
    public static <T> TypeSerializer<T> forRegistry(final Registry<T> registry) {
        return new RegistrySerializer<>(registry);
    }

    /**
     * The default collection of game serializers.
     *
     * <p>While the collection is mutable, modifying it is discouraged in favor
     * of working with a new child, created with
     * {@link TypeSerializerCollection#newChild()}. Collections of serializers
     * will become immutable in Configurate 4.0
     *
     *
     * @see #populate(TypeSerializerCollection) for information about which
     *      serializers this collection will include
     * @return minecraft serializers
     */
    public static TypeSerializerCollection collection() {
        TypeSerializerCollection collection = MINECRAFT_COLLECTION;
        if (collection == null) {
            collection = MINECRAFT_COLLECTION = populate(TypeSerializerCollection.defaults().newChild());
        }
        return collection;
    }

    /**
     * Check if a collection is our populated collection without attempting to
     * initialize serializers.
     *
     * <p>This helps to integrate with Confabricate in test environments.
     *
     * @param collection Collection to test
     * @return if tested collection is the confabricate default collection
     */
    public static boolean isCommonCollection(final TypeSerializerCollection collection) {
        return requireNonNull(collection, "collection").equals(MINECRAFT_COLLECTION);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean shouldRegister(final Registry<?> registry) {
        // Don't register root registry -- its key can't be looked up :(
        return !SPECIAL_REGISTRIES.contains(registry);
    }

    /**
     * Register Minecraft {@link TypeSerializer}s with the provided collection.
     *
     * <p>This will add serializers for: <ul>
     *     <li>{@link net.minecraft.util.Identifier Identifiers}</li>
     *     <li>{@link net.minecraft.text.Text} (as a string)</li>
     *     <li>Any elements in vanilla {@link Registry registries}</li>
     *     <li>{@link TaggableCollection} of a combination of identifiers and
     *          tags for blocks, items, fluids, and entity types</li>
     *     <li>{@link ItemStack}</li>
     *     <li>{@link CompoundTag} instances</li>
     * </ul>
     *
     * @param collection to populate
     * @return provided collection
     */
    public static TypeSerializerCollection populate(final TypeSerializerCollection collection) {
        collection.register(IdentifierSerializer.TOKEN, IdentifierSerializer.INSTANCE)
                .register(TextSerializer.TYPE, TextSerializer.INSTANCE);

        for (Map.Entry<TypeToken<?>, TypeSerializer<?>> registry : getKnownRegistries()) {
            registerRegistry(collection, registry.getKey(), registry.getValue());
        }

        collection.register(TypeToken.of(ItemStack.class), forCodec(ItemStack.CODEC));
        collection.register(TypeToken.of(CompoundTag.class), forCodec(CompoundTag.field_25128));

        // All registries here should be in SPECIAL_REGISTRIES
        populateTaggedRegistry(collection, TypeToken.of(Fluid.class), Registry.FLUID, FluidTags::getTagGroup);
        populateTaggedRegistry(collection, TypeToken.of(Block.class), Registry.BLOCK, BlockTags::getTagGroup);
        populateTaggedRegistry(collection, new TypeToken<EntityType<?>>() {}, Registry.ENTITY_TYPE, EntityTypeTags::getTagGroup);
        populateTaggedRegistry(collection, TypeToken.of(Item.class), Registry.ITEM, ItemTags::getTagGroup);

        return collection;
    }

    // Type serializers that use the server resource manager
    public static TypeSerializerCollection populateServer(final MinecraftServer server, final TypeSerializerCollection collection) {
        registerRegistry(collection, TypeToken.of(DimensionType.class), forRegistry(server.getRegistryManager().getDimensionTypes()));
        return collection;
    }

    // Type serializers that source registry + tag information from the client
    @Environment(EnvType.CLIENT)
    public static TypeSerializerCollection populateClient(final MinecraftClient client, final TypeSerializerCollection collection) {
        return collection;
    }

    /**
     * Lazily initialize our set of vanilla registries.
     *
     * <p>This is moderately expensive due to having to reflectively analyze
     * the fields in the Registry class, so we cache the created serializers
     * after the first initialization.
     *
     * @return Collection of built-in registries
     */
    private static Set<Map.Entry<TypeToken<?>, TypeSerializer<?>>> getKnownRegistries() {
        Set<Map.Entry<TypeToken<?>, TypeSerializer<?>>> registries = KNOWN_REGISTRIES;
        if (registries == null) {
            final ImmutableSet.Builder<Map.Entry<TypeToken<?>, TypeSerializer<?>>> accumulator = ImmutableSet.builder();
            for (Field registryField : Registry.class.getFields()) {
                // only look at public static fields (excludes the ROOT registry)
                if ((registryField.getModifiers() & (Modifier.STATIC | Modifier.PUBLIC)) != (Modifier.STATIC | Modifier.PUBLIC)) {
                    continue;
                }

                final TypeToken<?> fieldType = TypeToken.of(registryField.getGenericType());
                if (!fieldType.isSubtypeOf(TYPE_REGISTRY_GENERIC)) { // if not a registry (keys)
                    continue;
                }

                final TypeToken<?> elementType = fieldType.resolveType(TYPE_REGISTRY_ELEMENT);
                try {
                    final Registry<?> registry = (Registry<?>) registryField.get(null);
                    if (shouldRegister(registry)) {
                        accumulator.add(new AbstractMap.SimpleImmutableEntry<>(elementType, forRegistry(registry)));
                        LOGGER.debug("Created serializer for Minecraft registry {} with element type {}", registry, elementType);
                    }
                } catch (final IllegalAccessException e) {
                    LOGGER.error("Unable to create serializer for registry of type " + elementType + " due to access error", e);
                }
            }
            registries = KNOWN_REGISTRIES = accumulator.build();
        }
        return registries;
    }

    // Limit scope of warning suppression for reflective registry initialization
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerRegistry(final TypeSerializerCollection collection, final TypeToken<?> type, final TypeSerializer<?> registry) {
        collection.register(type, (TypeSerializer) registry);
    }

    /**
     * Add a serializer for the registry and its {@link TaggableCollection}.
     *
     * @param collection The collection to serialize
     * @param token Generic element type of the registry
     * @param registry Registry containing values
     * @param tagRegistry Tag container for values in the registry
     * @param <T> element type
     */
    private static <T> void populateTaggedRegistry(final TypeSerializerCollection collection, final TypeToken<T> token, final Registry<T> registry,
            final Supplier<TagGroup<T>> tagRegistry) {
        final TypeParameter<T> tParam = new TypeParameter<T>() {};
        final TypeToken<TaggableCollection<T>> taggableType = new TypeToken<TaggableCollection<T>>() {}.where(tParam, token);
        final TypeToken<Tag<T>> tagType = new TypeToken<Tag<T>>() {}.where(tParam, token);


        collection.register(token, new RegistrySerializer<>(registry));
        // deprecated
        collection.register(taggableType, new TaggableCollectionSerializer<>(registry, tagRegistry));
    }

}

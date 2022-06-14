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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.concurrent.LazyInit;
import com.mojang.datafixers.util.Either;
import net.minecraft.ResourceLocationException;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A tag implementation that keeps its original form for reserialization.
 *
 * @param <V> element type
 */
final class ConfabricateHolderSet<V> extends HolderSet.ListBacked<V> {

    private final List<TagEntry<V>> serializedForm;
    private final Supplier<Registry<V>> elementResolver;
    @LazyInit private volatile List<Holder<V>> values;

    /**
     * Create a new lazily initialized tag.
     *
     * @param serializedForm serialized form of the tag
     * @param elementResolver element-based resolver
     */
    ConfabricateHolderSet(final List<TagEntry<V>> serializedForm, final Supplier<Registry<V>> elementResolver) {
        this.serializedForm = List.copyOf(serializedForm);
        this.elementResolver = elementResolver;
    }

    public List<TagEntry<V>> serializedForm() {
        return this.serializedForm;
    }

    private List<Holder<V>> resolve() {
        final ImmutableList.Builder<Holder<V>> builder = ImmutableList.builder();

        final Registry<V> registry = this.elementResolver.get();
        for (final TagEntry<V> entry : this.serializedForm) {
            entry.collect(registry, builder::add);
        }

        return builder.build();
    }

    @Override
    public List<Holder<V>> contents() {
        List<Holder<V>> values = this.values;
        if (values == null) {
            this.values = values = this.resolve();
        }
        return values;
    }

    @Override
    public Either<TagKey<V>, List<Holder<V>>> unwrap() {
        return Either.right(this.contents());
    }

    @Override
    public boolean contains(final Holder<V> entry) {
        return this.contents().contains(entry);
    }

    sealed interface TagEntry<V> {

        String TAG_PREFIX = "#";
        String ID = "id";
        String REQUIRED = "required";

        static <E> TagEntry<E> fromNode(
            final ResourceKey<? extends Registry<E>> registry,
            final ConfigurationNode value
        ) throws SerializationException {
            final String id;
            final boolean required;
            if (value.isMap()) { // reference to optional tag
                id = value.node(ID).getString();
                required = value.node(REQUIRED).getBoolean();
            } else {
                id = value.getString();
                required = true;
            }

            if (id == null) {
                throw new SerializationException("a tag id field is required to deserialize");
            }

            try {
                if (id.startsWith(TAG_PREFIX)) {
                    final ResourceLocation loc = new ResourceLocation(id.substring(1));
                    return new Tag<>(TagKey.create(registry, loc), required);
                    // return required ? new Tag.TagEntry(ident) : new Tag.OptionalTagEntry(ident);
                } else {
                    final ResourceLocation loc = new ResourceLocation(id);
                    return new Single<>(ResourceKey.create(registry, loc), required);
                }
            } catch (final ResourceLocationException ex) {
                throw new SerializationException("Invalid resource location " + id);
            }
        }

        boolean required();

        void collect(Registry<V> registry, Consumer<Holder<V>> collector);

        void toNode(ConfigurationNode target) throws SerializationException;

        record Single<V>(ResourceKey<V> item, boolean required) implements TagEntry<V> {

            @Override
            public void collect(final Registry<V> registry, final Consumer<Holder<V>> collector) {
                collector.accept(registry.getOrCreateHolder(this.item));
            }

            @Override
            public void toNode(final ConfigurationNode target) throws SerializationException {
                if (this.required) {
                    ResourceLocationSerializer.toNode(this.item.location(), target);
                } else {
                    ResourceLocationSerializer.toNode(this.item.location(), target.node(ID));
                    target.node(REQUIRED).set(false);
                }
            }

        }

        record Tag<V>(TagKey<V> tagKey, boolean required) implements TagEntry<V> {

            @Override
            public void collect(final Registry<V> registry, final Consumer<Holder<V>> collector) {
                for (final Holder<V> entry : registry.getOrCreateTag(this.tagKey)) {
                    collector.accept(entry);
                }
            }

            @Override
            public void toNode(final ConfigurationNode target) throws SerializationException {
                if (this.required) {
                    target.set(TAG_PREFIX + this.tagKey.location());
                } else {
                    target.node(ID).set(TAG_PREFIX + this.tagKey.location());
                    target.node(REQUIRED).set(false);
                }
            }

        }

    }

}

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
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.Tag;
import net.minecraft.tags.TagKey;

import java.util.List;
import java.util.function.Supplier;

/**
 * A tag implementation that keeps its original form for reserialization.
 *
 * @param <V> element type
 */
final class ConfabricateTag<V> extends HolderSet.ListBacked<V> {

    private final List<Tag.Entry> serializedForm;
    private final Supplier<Registry<V>> elementResolver;
    @LazyInit private volatile List<Holder<V>> values;

    /**
     * Create a new lazily initialized tag.
     *
     * @param serializedForm serialized form of the tag
     * @param elementResolver element-based resolver
     * @param tagResolver tag-based resolver
     */
    ConfabricateTag(final List<Tag.Entry> serializedForm, final Supplier<Registry<V>> elementResolver, final Supplier<Tag<V>> tagResolver) {
        this.serializedForm = ImmutableList.copyOf(serializedForm);
        this.elementResolver = elementResolver;
    }

    public List<Tag.Entry> serializedForm() {
        return this.serializedForm;
    }

    private List<Holder<V>> resolve() {
        final ImmutableList.Builder<Holder<V>> builder = ImmutableList.builder();

        /*for (final Tag.Entry entry : this.serializedForm) {
            if (!entry.resolve(tag -> this.tagResolver.get().getTag(tag),
                obj -> this.elementResolver.get().get(obj),
                builder::add)) {
                throw new IllegalArgumentException("Unknown tag entry " + entry);
            }
        }*/

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
        return false; // TODO
    }

    sealed interface TagEntry<V> {
        HolderSet<V> resolve(Registry<V> registry);

        record Single<V>(ResourceKey<V> item) implements TagEntry<V> {

            @Override
            public HolderSet<V> resolve(final Registry<V> registry) {
                return HolderSet.direct(registry.getOrCreateHolder(this.item));
            }
        }

        record Tag<V>(TagKey<V> tagKey) implements TagEntry<V> {
            @Override
            public HolderSet<V> resolve(final Registry<V> registry) {
                return registry.getOrCreateTag(this.tagKey);
            }
        }

    }

}

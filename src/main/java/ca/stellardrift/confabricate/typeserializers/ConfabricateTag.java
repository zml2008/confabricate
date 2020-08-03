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
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagGroup;
import net.minecraft.util.registry.Registry;

import java.util.List;
import java.util.function.Supplier;

/**
 * A tag implementation that keeps its original form for reserialization.
 *
 * @param <V> element type
 */
class ConfabricateTag<V> implements Tag<V> {

    private final List<Tag.Entry> serializedForm;
    private final Supplier<Registry<V>> elementResolver;
    private final Supplier<TagGroup<V>> tagResolver;
    private @LazyInit volatile List<V> values;

    /**
     * Create a new lazily initialized tag.
     *
     * @param serializedForm Serialized form of the tag
     * @param elementResolver Element-based resolver
     * @param tagResolver tag-based resolver
     */
    ConfabricateTag(final List<Entry> serializedForm, final Supplier<Registry<V>> elementResolver, final Supplier<TagGroup<V>> tagResolver) {
        this.serializedForm = ImmutableList.copyOf(serializedForm);
        this.elementResolver = elementResolver;
        this.tagResolver = tagResolver;
    }

    @Override
    public boolean contains(final V entry) {
        return values().contains(entry);
    }

    public List<Tag.Entry> serializedForm() {
        return this.serializedForm;
    }

    private List<V> resolve() {
        final ImmutableList.Builder<V> builder = ImmutableList.builder();

        for (Tag.Entry entry : this.serializedForm) {
            if (!entry.resolve(tag -> this.tagResolver.get().getTag(tag),
                obj -> this.elementResolver.get().get(obj),
                builder::add)) {
                throw new IllegalArgumentException("Unknown tag entry " + entry);
            }
        }

        return builder.build();
    }

    @Override
    public List<V> values() {
        List<V> values = this.values;
        if (values == null) {
            this.values = values = resolve();
        }
        return values;
    }

}

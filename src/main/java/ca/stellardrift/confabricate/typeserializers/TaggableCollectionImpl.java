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
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.Iterator;
import java.util.Set;

class TaggableCollectionImpl<T> implements TaggableCollection<T> {

    private final Registry<T> registry;
    private final TagGroup<T> tags;

    private final Set<T> elements;
    private final Set<Tag<T>> tagElements;

    TaggableCollectionImpl(final Registry<T> registry, final TagGroup<T> tags, final Set<T> elements, final Set<Tag<T>> tagElements) {
        this.registry = requireNonNull(registry, "registry");
        this.tags = requireNonNull(tags, "tags");
        this.elements = ImmutableSet.copyOf(elements);
        this.tagElements = ImmutableSet.copyOf(tagElements);
    }

    @Override
    public Registry<T> getContainingRegistry() {
        return this.registry;
    }

    @Override
    public TagGroup<T> getTagContainer() {
        return this.tags;
    }

    @Override
    public Set<T> getSpecificElements() {
        return this.elements;
    }

    @Override
    public Set<Tag<T>> getTaggedElements() {
        return this.tagElements;
    }

    @Override
    public TaggableCollection<T> addingSingle(final Identifier ident) {
        final T element = requireNonNull(this.registry.get(ident), "no such member of registry!");
        if (!this.elements.contains(element)) {
            return newCollection(ImmutableSet.<T>builder().addAll(this.elements).add(element).build(), null);
        }
        return this;
    }

    @Override
    public TaggableCollection<T> addingTag(final Identifier tag) {
        final Tag<T> element = requireNonNull(this.tags.getTag(tag), "no such member of registry!");
        if (!this.tagElements.contains(element)) {
            return newCollection(null, ImmutableSet.<Tag<T>>builder().addAll(this.tagElements).add(element).build());
        }
        return this;
    }

    @Override
    public TaggableCollection<T> removingSingle(final Identifier ident) {
        final ImmutableSet.Builder<T> newBuild = ImmutableSet.builder();
        boolean changed = false;
        for (T element : this.elements) {
            if (!ident.equals(this.registry.getId(element))) {
                newBuild.add(element);
                changed = true;
            }
        }
        return changed ? newCollection(newBuild.build(), null) : this;
    }

    @Override
    public TaggableCollection<T> removingTag(final Identifier tag) {
        final ImmutableSet.Builder<Tag<T>> newBuild = ImmutableSet.builder();
        boolean changed = false;
        for (Tag<T> element : this.tagElements) {
            if (!tag.equals(this.tags.getTagId(element))) {
                newBuild.add(element);
                changed = true;
            }
        }
        return changed ? newCollection(null, newBuild.build()) : this;
    }

    private TaggableCollectionImpl<T> newCollection(final Set<T> elements, final Set<Tag<T>> tagElements) {
        return new TaggableCollectionImpl<>(this.registry, this.tags,
                elements == null ? this.elements : elements,
                tagElements == null ? this.tagElements : tagElements);
    }

    @Override
    public Iterator<T> iterator() {
        return new TaggableCollectionIterator();
    }

    class TaggableCollectionIterator implements Iterator<T> {

        private Iterator<T> current;
        private Iterator<Tag<T>> tags;

        TaggableCollectionIterator() {
            this.current = TaggableCollectionImpl.this.elements.iterator();
            this.tags = TaggableCollectionImpl.this.tagElements.iterator();
        }

        @Override
        public boolean hasNext() {
            return this.current.hasNext() || this.tags.hasNext();
        }

        @Override
        public T next() {
            while (!this.current.hasNext()) {
                this.current = this.tags.next().values().iterator();
            }
            return this.current.next();
        }

    }

}

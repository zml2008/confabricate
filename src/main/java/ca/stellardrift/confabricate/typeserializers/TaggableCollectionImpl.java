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

import com.google.common.collect.ImmutableSet;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagContainer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.Iterator;
import java.util.Set;

import static java.util.Objects.requireNonNull;

class TaggableCollectionImpl<T> implements TaggableCollection<T> {
    private final Registry<T> registry;
    private final TagContainer<T> tags;

    private final Set<T> elements;
    private final Set<Tag<T>> tagElements;

    TaggableCollectionImpl(Registry<T> registry, TagContainer<T> tags, Set<T> elements, Set<Tag<T>> tagElements) {
        this.registry = requireNonNull(registry, "registry");
        this.tags = requireNonNull(tags, "tags");
        this.elements = ImmutableSet.copyOf(elements);
        this.tagElements = ImmutableSet.copyOf(tagElements);
    }

    @Override
    public Registry<T> getContainingRegistry() {
        return registry;
    }

    @Override
    public TagContainer<T> getTagContainer() {
        return tags;
    }

    @Override
    public Set<T> getSpecificElements() {
        return elements;
    }

    @Override
    public Set<Tag<T>> getTaggedElements() {
        return tagElements;
    }

    @Override
    public TaggableCollection<T> addingSingle(Identifier ident) {
        T element = requireNonNull(registry.get(ident), "no such member of registry!");
        if (!elements.contains(element)) {
            return newCollection(ImmutableSet.<T>builder().addAll(elements).add(element).build(), null);
        }
        return this;
    }

    @Override
    public TaggableCollection<T> addingTag(Identifier tag) {
        Tag<T> element = requireNonNull(tags.get(tag), "no such member of registry!");
        if (!tagElements.contains(element)) {
            return newCollection(null, ImmutableSet.<Tag<T>>builder().addAll(tagElements).add(element).build());
        }
        return this;
    }

    @Override
    public TaggableCollection<T> removingSingle(Identifier ident) {
        ImmutableSet.Builder<T> newBuild = ImmutableSet.builder();
        boolean changed = false;
        for (T element : elements) {
            if (!ident.equals(registry.getId(element))) {
                newBuild.add(element);
                changed = true;
            }
        }
        return changed ? newCollection(newBuild.build(), null) : this;
    }

    @Override
    public TaggableCollection<T> removingTag(Identifier tag) {
        ImmutableSet.Builder<Tag<T>> newBuild = ImmutableSet.builder();
        boolean changed = false;
        for (Tag<T> element : tagElements) {
            if (!tag.equals(element.getId())) {
                newBuild.add(element);
                changed = true;
            }
        }
        return changed ? newCollection(null, newBuild.build()) : this;
    }

    private TaggableCollectionImpl<T> newCollection(Set<T> elements, Set<Tag<T>> tagElements) {
        return new TaggableCollectionImpl<>(registry, tags,
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
            current = elements.iterator();
            tags = tagElements.iterator();
        }

        @Override
        public boolean hasNext() {
            return current.hasNext() || tags.hasNext();
        }

        @Override
        public T next() {
            while (!current.hasNext()) {
                current = tags.next().values().iterator();
            }
            return current.next();
        }
    }
}

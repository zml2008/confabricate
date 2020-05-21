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

import static ca.stellardrift.confabricate.typeserializers.IdentifierSerializer.createIdentifier;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagContainer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TaggableCollectionSerializer<T> implements TypeSerializer<TaggableCollection<T>> {

    private static final String TAG_PREFIX = "#";
    private final Registry<T> registry;
    private final TagContainer<T> tagRegistry;

    public TaggableCollectionSerializer(final Registry<T> registry, final TagContainer<T> tagRegistry) {
        this.registry = registry;
        this.tagRegistry = tagRegistry;
    }

    @Nullable
    @Override
    public TaggableCollection<T> deserialize(final @NonNull TypeToken<?> type, final @NonNull ConfigurationNode value) throws ObjectMappingException {
        if (value.isMap()) {
            throw new ObjectMappingException("Tags cannot be provided in map format");
        }

        final ImmutableSet.Builder<T> elements = ImmutableSet.builder();
        final ImmutableSet.Builder<Tag<T>> tagElements = ImmutableSet.builder();

        if (value.isList()) {
            for (ConfigurationNode node : value.getChildrenList()) {
                handleSingle(node, elements, tagElements);
            }
        } else {
            handleSingle(value, elements, tagElements);
        }
        return new TaggableCollectionImpl<>(this.registry, this.tagRegistry, elements.build(), tagElements.build());
    }

    private void handleSingle(final ConfigurationNode node, final ImmutableSet.Builder<T> elements,
            final ImmutableSet.Builder<Tag<T>> tagElements) throws ObjectMappingException {
        boolean isTag = false;
        String ident = String.valueOf(node.getValue());
        if (ident.startsWith(TAG_PREFIX)) {
            isTag = true;
            ident = ident.substring(1);
        }
        final Identifier id = createIdentifier(ident);

        if (isTag) {
            final Tag<T> tag = this.tagRegistry.get(id);
            if (tag == null) {
                throw new ObjectMappingException("Unknown tag #" + id);
            }
            tagElements.add(tag);

        } else {
            final T element = this.registry.get(id);
            if (element == null) {
                throw new ObjectMappingException("Unknown member of registry " + id);
            }
            elements.add(element);
        }
    }

    @Override
    public void serialize(final @NonNull TypeToken<?> type, final @Nullable TaggableCollection<T> obj,
            final @NonNull ConfigurationNode value) throws ObjectMappingException {
        value.setValue(ImmutableList.of());
        if (obj != null) {
            for (T element : obj.getSpecificElements()) {
                final Identifier id = this.registry.getId(element);
                if (id == null) {
                    throw new ObjectMappingException("Unknown element " + element);
                }
                IdentifierSerializer.toNode(id, value.appendListNode());
            }

            for (Tag<T> tag : obj.getTaggedElements()) {
                value.appendListNode().setValue(TAG_PREFIX + this.tagRegistry.getId(tag));
            }
        }
    }

}

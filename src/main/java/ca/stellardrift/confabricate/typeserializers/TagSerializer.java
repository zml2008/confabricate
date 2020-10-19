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
import com.google.common.collect.ImmutableSet;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.function.Supplier;

final class TagSerializer<V> implements TypeSerializer<Tag<V>> {

    private final Registry<V> registry;
    private final Supplier<TagGroup<V>> tags;

    TagSerializer(final Registry<V> registry, final Supplier<TagGroup<V>> tags) {
        this.registry = registry;
        this.tags = tags;
    }

    private static final String TAG_PREFIX = "#";

    private static final String ID = "id";
    private static final String REQUIRED = "required";

    @Override
    public Tag<V> deserialize(@NonNull final Type type, @NonNull final ConfigurationNode value) throws SerializationException {
        if (value.isList()) { // anonymous tag
            final ImmutableList.Builder<Tag.Entry> entries = ImmutableList.builder();
            for (final ConfigurationNode child : value.childrenList()) {
                entries.add(entryFromNode(child));
            }
            return new ConfabricateTag<>(entries.build(), () -> this.registry, this.tags);
        } else if (!value.isMap()) { // definitely a reference
            final String id = value.getString();
            return TagRegistry.create(IdentifierSerializer.createIdentifier(id), this.tags);
        } else {
            final String id = value.node(ID).getString();
            final boolean required = value.node(REQUIRED).getBoolean();
            if (id == null) {
                throw new SerializationException("An ID is required");
            } else {
                if (required && id.startsWith(TAG_PREFIX)) {
                    return TagRegistry.create(IdentifierSerializer.createIdentifier(id.substring(1)), this.tags);
                } else {
                    return new ConfabricateTag<>(ImmutableList.of(entryFromNode(value)), () -> this.registry, this.tags);
                }
            }
        }
    }

    @Override
    public void serialize(@NonNull final Type type, @Nullable final Tag<V> obj, @NonNull final ConfigurationNode value)
            throws SerializationException {
        if (obj == null) {
            value.set(null);
            return;
        }

        if (obj instanceof Tag.Identified<?>) { // named tag
            value.set(TAG_PREFIX + ((Tag.Identified<V>) obj).getId().toString());
        } else if (obj instanceof ConfabricateTag<?>) {
            final ConfabricateTag<V> tag = (ConfabricateTag<V>) obj;
            if (value.childrenList().size() == tag.serializedForm().size()) { // update existing list
                for (int i = 0; i < tag.serializedForm().size(); ++i) {
                    final ConfigurationNode child = value.node(i);
                    try {
                        entryToNode(tag.serializedForm().get(i), child);
                    } catch (final SerializationException ex) {
                        ex.initPath(child::path);
                        throw ex;
                    }
                }
            } else {
                value.raw(null);
                for (Tag.Entry entry : tag.serializedForm()) {
                    entryToNode(entry, value.appendListNode());
                }
            }
        } else {
            value.raw(null);
            for (V element : obj.values()) {
                IdentifierSerializer.toNode(this.registry.getId(element), value.appendListNode());
            }
        }
    }

    private Tag.Entry entryFromNode(final ConfigurationNode value) throws SerializationException {
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

        if (id.startsWith(TAG_PREFIX)) {
            final Identifier ident = new Identifier(id.substring(1));
            return required ? new Tag.TagEntry(ident) : new Tag.OptionalTagEntry(ident);
        } else {
            final Identifier ident = new Identifier(id);
            return required ? new Tag.ObjectEntry(ident) : new Tag.OptionalObjectEntry(ident);
        }
    }

    private void entryToNode(final Tag.Entry entry, final ConfigurationNode target) throws SerializationException {
        // TODO: Properly propagate exceptions
        if (entry instanceof Tag.ObjectEntry) {
            entry.resolve(id -> null, id -> {
                target.raw(id.toString());
                return null;
            }, val -> {});
        } else if (entry instanceof Tag.OptionalObjectEntry) {
            entry.resolve(id -> null, id -> {
                target.node(ID).raw(id.toString());
                target.node(REQUIRED).raw(false);
                return null;
            }, val -> {});
        } else if (entry instanceof Tag.TagEntry) {
            entry.resolve(id -> {
                target.raw(TAG_PREFIX + id.toString());
                return null;
            }, id -> null, val -> {});
        } else if (entry instanceof Tag.OptionalTagEntry) {
            entry.resolve(id -> {
                target.node(ID).raw(TAG_PREFIX + id.toString());
                target.node(REQUIRED).raw(false);
                return null;
            }, id -> null, val -> {});
        }
        throw new SerializationException("Unknown tag entry type " + entry);
    }

    @Override
    public Tag<V> emptyValue(final Type specificType, final ConfigurationOptions options) {
        return Tag.of(ImmutableSet.of());
    }

}

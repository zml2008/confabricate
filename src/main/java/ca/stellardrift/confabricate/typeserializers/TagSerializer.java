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

import com.google.common.reflect.TypeToken;
import net.fabricmc.fabric.api.tag.TagRegistry;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Supplier;

final class TagSerializer<V> implements TypeSerializer<Tag<V>> {

    private final Registry<V> registry;
    private final Supplier<TagGroup<V>> tags;

    TagSerializer(final Registry<V> registry, final Supplier<TagGroup<V>> tags) {
        this.registry = registry;
        this.tags = tags;
    }

    private static final String ID = "id";
    private static final String REQUIRED = "required";

    @Override
    public @Nullable Tag<V> deserialize(@NonNull final TypeToken<?> type, @NonNull final ConfigurationNode value) throws ObjectMappingException {
        if (value.isList()) { // anonymous tag
            final Tag.Builder builder = new Tag.Builder();
            for (final ConfigurationNode child : value.getChildrenList()) {
                builder.add(entryFromNode(child), String.valueOf(value.getKey()));
            }
            return builder.build(id -> tags.get().getTag(id), this.registry::get)
                    .orElseThrow(() -> new ObjectMappingException("Unable to deserialize tag!"));
        } else if (!value.isMap()) {  // definitely a reference
            final String id = value.getString();
            return TagRegistry.create(IdentifierSerializer.createIdentifier(id), this.tags);
        } else {
            final String id = value.getNode(ID).getString();
            final boolean required = value.getNode(REQUIRED).getBoolean();
        }
    }

    @Override
    public void serialize(@NonNull final TypeToken<?> type, @Nullable final Tag<V> obj, @NonNull final ConfigurationNode value)
            throws ObjectMappingException {
        if (obj == null) {
            value.setValue(null);
            return;
        }

        if (obj instanceof Tag.Identified<?>) { // named tag
            value.setValue("#" + ((Tag.Identified<V>) obj).getId().toString());
        } else if (obj instanceof ConfabricateTag<?>) {
            final ConfabricateTag<V> tag = (ConfabricateTag<V>) obj;
            if (value.getChildrenList().size() == tag.serializedForm().size()) { // update existing list

            } else {
                value.setValue(null);
                for (Tag.Entry entry : tag.serializedForm()) {

                }
            }
        } else {
            value.setValue(null);
            for (V element : obj.values()) {

            }
        }
    }

    private Tag.Entry entryFromNode(final ConfigurationNode value) throws ObjectMappingException {
        final String id;
        final boolean required;
        if (value.isMap()) { // reference to optional tag
            id = value.getNode(ID).getString();
            required = value.getNode(REQUIRED).getBoolean();
        } else {
            id = value.getString();
            required = true;
        }

        if (id == null) {
            throw new ObjectMappingException("a tag id field is required to deserialize");
        }

        if (id.startsWith("#")) {
            final Identifier ident = new Identifier(id.substring(1));
            return required ? new Tag.TagEntry(ident) : new Tag.OptionalTagEntry(ident);
        } else {
            final Identifier ident = new Identifier(id);
            return required ? new Tag.ObjectEntry(ident) : new Tag.OptionalObjectEntry(ident);
        }
    }

    private void entryToNode(final Tag.Entry entry, final ConfigurationNode target) throws ObjectMappingException {
        // TODO: Abuse resolve() method to access value?
        if (entry instanceof Tag.ObjectEntry) {
        } else if (entry instanceof Tag.OptionalObjectEntry) {

        } else if (entry instanceof Tag.TagEntry) {

        } else if (entry instanceof Tag.OptionalTagEntry) {

        }
        throw new ObjectMappingException("Unknown tag entry type " + entry);
    }
}

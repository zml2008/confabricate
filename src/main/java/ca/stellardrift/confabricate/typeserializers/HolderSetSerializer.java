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
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.List;

final class HolderSetSerializer<V> extends RegistryBasedSerializer<V, HolderSet<V>> {

    HolderSetSerializer(final RegistryAccess access, final ResourceKey<? extends Registry<V>> registry) {
        super(access, registry);
    }

    private static final String TAG_PREFIX = "#";

    private static final String ID = "id";
    private static final String REQUIRED = "required";

    @Override
    public HolderSet<V> deserialize(final @NonNull Type type, final @NonNull ConfigurationNode value) throws SerializationException {
        if (value.isList()) { // anonymous tag
            final ImmutableList.Builder<ConfabricateHolderSet.TagEntry<V>> entries = ImmutableList.builder();
            for (final ConfigurationNode child : value.childrenList()) {
                entries.add(ConfabricateHolderSet.TagEntry.fromNode(this.registry, child));
            }
            return new ConfabricateHolderSet<>(entries.build(), this::uncheckedRegistry);
        } else if (!value.isMap()) { // definitely a reference
            final String id = value.getString();
            return this.registry().getOrCreateTag(TagKey.create(this.registry, ResourceLocationSerializer.createIdentifier(id)));
        } else {
            final String id = value.node(ID).getString();
            final boolean required = value.node(REQUIRED).getBoolean();
            if (id == null) {
                throw new SerializationException("An ID is required");
            } else {
                if (required && id.startsWith(TAG_PREFIX)) {
                    return this.registry().getOrCreateTag(TagKey.create(this.registry, ResourceLocationSerializer.createIdentifier(id.substring(1))));
                } else {
                    return new ConfabricateHolderSet<>(
                        List.of(ConfabricateHolderSet.TagEntry.fromNode(this.registry, value)),
                        this::uncheckedRegistry
                    );
                }
            }
        }
    }

    @Override
    public void serialize(@NonNull final Type type, @Nullable final HolderSet<V> obj, @NonNull final ConfigurationNode value)
            throws SerializationException {
        if (obj == null) {
            value.set(null);
            return;
        }

        if (obj instanceof HolderSet.Named<V>) { // named tag
            value.set(TAG_PREFIX + ((HolderSet.Named<V>) obj).key().location().toString());
        } else if (obj instanceof final ConfabricateHolderSet<V> tag) {
            if (value.childrenList().size() == tag.serializedForm().size()) { // update existing list
                for (int i = 0; i < tag.serializedForm().size(); ++i) {
                    final ConfigurationNode child = value.node(i);
                    try {
                        tag.serializedForm().get(i).toNode(child);
                    } catch (final SerializationException ex) {
                        ex.initPath(child::path);
                        throw ex;
                    }
                }
            } else {
                value.raw(null);
                for (final ConfabricateHolderSet.TagEntry<V> entry : tag.serializedForm()) {
                    entry.toNode(value.appendListNode());
                }
            }
        } else {
            value.raw(null);
            for (final Holder<V> element : obj.unwrap().right().orElseThrow()) {
                ResourceLocationSerializer.toNode(
                    element.unwrapKey().orElseThrow().location(),
                    value.appendListNode()
                );
            }
        }
    }

    @Override
    public HolderSet<V> emptyValue(final Type specificType, final ConfigurationOptions options) {
        return HolderSet.direct(List.of());
    }

}

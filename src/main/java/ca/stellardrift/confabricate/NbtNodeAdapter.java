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
package ca.stellardrift.confabricate;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationNodeFactory;
import org.spongepowered.configurate.ConfigurationOptions;

/**
 * A configuration adapter that will convert Minecraft NBT data into a
 * Configurate {@link ConfigurationNode}.
 *
 * @since 1.0.0
 */
public final class NbtNodeAdapter {

    private static final ConfigurationNodeFactory<BasicConfigurationNode> FACTORY = new ConfigurationNodeFactory<>() {

        @Override
        public BasicConfigurationNode createNode(final ConfigurationOptions options) {
            return BasicConfigurationNode.root(options
                    .nativeTypes(Set.of(Map.class, List.class, Byte.class,
                            Short.class, Integer.class, Long.class, Float.class, Double.class,
                            long[].class, byte[].class, int[].class, String.class)));
        }

    };

    private NbtNodeAdapter() {}

    /**
     * Given a tag, convert it to a node.
     *
     * <p>Depending on the configuration of the provided node, the conversion
     * may lose some data when roundtripped back. For example, array tags may
     * be converted to lists if the node provided does not support arrays.
     *
     * @param tag the tag to convert
     * @param node the node to populate
     * @throws IOException if invalid tags are provided
     * @since 1.0.0
     */
    public static void tagToNode(final Tag tag, final ConfigurationNode node) throws IOException {
        if (tag instanceof final CompoundTag compoundTag) {
            for (final String key : compoundTag.getAllKeys()) {
                tagToNode(compoundTag.get(key), node.node(key));
            }
        } else if (tag instanceof final ListTag list) {
            for (final Tag value : list) {
                tagToNode(value, node.appendListNode());
            }
        } else if (tag instanceof StringTag) {
            node.raw(tag.getAsString());
        } else if (tag instanceof final ByteTag b) {
            node.raw(b.getAsByte());
        } else if (tag instanceof final ShortTag s) {
            node.raw(s.getAsShort());
        } else if (tag instanceof final IntTag i) {
            node.raw(i.getAsInt());
        } else if (tag instanceof final LongTag l) {
            node.raw(l.getAsLong());
        } else if (tag instanceof final FloatTag f) {
            node.raw(f.getAsFloat());
        } else if (tag instanceof final DoubleTag d) {
            node.raw(d.getAsDouble());
        } else if (tag instanceof final ByteArrayTag arr) {
            if (node.options().acceptsType(byte[].class)) {
                node.raw(arr.getAsByteArray());
            } else {
                node.raw(null);
                for (final byte b : arr.getAsByteArray()) {
                    node.appendListNode().raw(b);
                }
            }
        } else if (tag instanceof final IntArrayTag arr) {
            if (node.options().acceptsType(int[].class)) {
                node.raw(arr.getAsIntArray());
            } else {
                node.raw(null);
                for (final int i : arr.getAsIntArray()) {
                    node.appendListNode().raw(i);
                }
            }

        } else if (tag instanceof final LongArrayTag arr) {
            if (node.options().acceptsType(long[].class)) {
                node.raw(arr.getAsLongArray());
            } else {
                node.raw(null);
                for (final long l : arr.getAsLongArray()) {
                    node.appendListNode().raw(l);
                }
            }
        } else if (tag instanceof EndTag) {
            // no-op
        } else {
            throw new IOException("Unknown tag type: " + tag.getClass());
        }
    }

    /**
     * Convert a node to tag. Because NBT is strongly typed and does not permit
     * lists with mixed types, some configuration nodes will not be convertible
     * to Tags.
     *
     * @param node the configuration node
     * @return the converted tag object
     * @throws IOException if an IO error occurs while converting the tag
     * @since 1.0.0
     */
    public static Tag nodeToTag(final ConfigurationNode node) throws IOException {
        if (node.isMap()) {
            final CompoundTag tag = new CompoundTag();
            for (final Map.Entry<Object, ? extends ConfigurationNode> ent : node.childrenMap().entrySet()) {
                tag.put(ent.getKey().toString(), nodeToTag(ent.getValue()));
            }
            return tag;
        } else if (node.isList()) {
            final ListTag list = new ListTag();
            for (final ConfigurationNode child : node.childrenList()) {
                list.add(nodeToTag(child));
            }
            return list;
        } else {
            final Object obj = node.raw();
            if (obj instanceof final byte[] arr) {
                return new ByteArrayTag(arr);
            } else if (obj instanceof final int[] arr) {
                return new IntArrayTag(arr);
            } else if (obj instanceof final long[] arr) {
                return new LongArrayTag(arr);
            } else if (obj instanceof final Byte b) {
                return ByteTag.valueOf(b);
            } else if (obj instanceof final Short s) {
                return ShortTag.valueOf(s);
            } else if (obj instanceof final Integer i) {
                return IntTag.valueOf(i);
            } else if (obj instanceof final Long l) {
                return LongTag.valueOf(l);
            } else if (obj instanceof final Float f) {
                return FloatTag.valueOf(f);
            } else if (obj instanceof final Double d) {
                return DoubleTag.valueOf(d);
            } else if (obj instanceof final String s) {
                return StringTag.valueOf(s);
            } else {
                throw new IOException("Unsupported object type " + (obj == null ? null : obj.getClass()));
            }
        }
    }

    /**
     * Create an empty node with options appropriate for handling NBT data.
     *
     * @return the new node
     * @since 1.0.0
     */
    public static ConfigurationNode createEmptyNode() {
        return FACTORY.createNode(Confabricate.confabricateOptions());
    }

    /**
     * Create an empty node with options appropriate for handling NBT data.
     *
     * @param options options to work with
     * @return the new node
     * @since 1.0.0
     */
    public static ConfigurationNode createEmptyNode(final @NonNull ConfigurationOptions options) {
        return FACTORY.createNode(options);
    }

    /**
     * Get a factory for nodes prepared to handle NBT data.
     *
     * @return the factory
     * @since 3.0.0
     */
    public static ConfigurationNodeFactory<BasicConfigurationNode> nodeFactory() {
        return FACTORY;
    }

}

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

import com.google.common.collect.ImmutableSet;
import net.minecraft.nbt.NbtByte;
import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtDouble;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtEnd;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.nbt.NbtLongArray;
import net.minecraft.nbt.NbtShort;
import net.minecraft.nbt.NbtString;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.BasicConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A configuration adapter that will convert Minecraft NBT data into a
 * Configurate {@link ConfigurationNode}.
 *
 * @since 1.0.0
 */
public final class NbtNodeAdapter {

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
    public static void tagToNode(final NbtElement tag, final ConfigurationNode node) throws IOException {
        if (tag instanceof NbtCompound) {
            final NbtCompound compoundTag = (NbtCompound) tag;
            for (final String key : compoundTag.getKeys()) {
                tagToNode(compoundTag.get(key), node.node(key));
            }
        } else if (tag instanceof NbtList) {
            for (final NbtElement value : (NbtList) tag) {
                tagToNode(value, node.appendListNode());
            }
        } else if (tag instanceof NbtString) {
            node.raw(tag.asString());
        } else if (tag instanceof NbtByte) {
            node.raw(((NbtByte) tag).byteValue());
        } else if (tag instanceof NbtShort) {
            node.raw(((NbtShort) tag).shortValue());
        } else if (tag instanceof NbtInt) {
            node.raw(((NbtInt) tag).intValue());
        } else if (tag instanceof NbtLong) {
            node.raw(((NbtLong) tag).longValue());
        } else if (tag instanceof NbtFloat) {
            node.raw(((NbtFloat) tag).floatValue());
        } else if (tag instanceof NbtDouble) {
            node.raw(((NbtDouble) tag).doubleValue());
        } else if (tag instanceof NbtByteArray) {
            if (node.options().acceptsType(byte[].class)) {
                node.raw(((NbtByteArray) tag).getByteArray());
            } else {
                node.raw(null);
                for (final byte b : ((NbtByteArray) tag).getByteArray()) {
                    node.appendListNode().raw(b);
                }
            }
        } else if (tag instanceof NbtIntArray) {
            if (node.options().acceptsType(int[].class)) {
                node.raw(((NbtIntArray) tag).getIntArray());
            } else {
                node.raw(null);
                for (final int i : ((NbtIntArray) tag).getIntArray()) {
                    node.appendListNode().raw(i);
                }
            }

        } else if (tag instanceof NbtLongArray) {
            if (node.options().acceptsType(long[].class)) {
                node.raw(((NbtLongArray) tag).getLongArray());
            } else {
                node.raw(null);
                for (final long l : ((NbtLongArray) tag).getLongArray()) {
                    node.appendListNode().raw(l);
                }
            }
        } else if (tag instanceof NbtEnd) {
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
    public static NbtElement nodeToTag(final ConfigurationNode node) throws IOException {
        if (node.isMap()) {
            final NbtCompound tag = new NbtCompound();
            for (final Map.Entry<Object, ? extends ConfigurationNode> ent : node.childrenMap().entrySet()) {
                tag.put(ent.getKey().toString(), nodeToTag(ent.getValue()));
            }
            return tag;
        } else if (node.isList()) {
            final NbtList list = new NbtList();
            for (final ConfigurationNode child : node.childrenList()) {
                list.add(nodeToTag(child));
            }
            return list;
        } else {
            final Object obj = node.raw();
            if (obj instanceof byte[]) {
                return new NbtByteArray((byte[]) obj);
            } else if (obj instanceof int[]) {
                return new NbtIntArray((int[]) obj);
            } else if (obj instanceof long[]) {
                return new NbtLongArray((long[]) obj);
            } else if (obj instanceof Byte) {
                return NbtByte.of((Byte) obj);
            } else if (obj instanceof Short) {
                return NbtShort.of((Short) obj);
            } else if (obj instanceof Integer) {
                return NbtInt.of((Integer) obj);
            } else if (obj instanceof Long) {
                return NbtLong.of((Long) obj);
            } else if (obj instanceof Float) {
                return NbtFloat.of((Float) obj);
            } else if (obj instanceof Double) {
                return NbtDouble.of((Double) obj);
            } else if (obj instanceof String) {
                return NbtString.of((String) obj);
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
        return createEmptyNode(Confabricate.confabricateOptions());
    }

    /**
     * Create an empty node with options appropriate for handling NBT data.
     *
     * @param options options to work with
     * @return the new node
     * @since 1.0.0
     */
    public static ConfigurationNode createEmptyNode(final @NonNull ConfigurationOptions options) {
        return BasicConfigurationNode.root(options
                .nativeTypes(ImmutableSet.of(Map.class, List.class, Byte.class,
                        Short.class, Integer.class, Long.class, Float.class, Double.class,
                        long[].class, byte[].class, int[].class, String.class)));
    }

}

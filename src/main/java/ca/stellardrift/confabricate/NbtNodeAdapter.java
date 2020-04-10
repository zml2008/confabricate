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
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.SimpleConfigurationNode;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A configuration adapter that will convert Minecraft NBT data into a Configurate {@link ConfigurationNode}
 */
public class NbtNodeAdapter {

    /**
     * Given a tag, convert it to a node.
     * Depending on the configuration of the provided node, the conversion may lose some data when roundtripped back.
     * For example, array tags may be converted to lists if the node provided does not support arrays.
     *
     * @param tag The tag to convert
     * @param node The node to pupulate
     * @throws IOException If invalid tags are provided
     */
    public static void tagToNode(Tag tag, ConfigurationNode node) throws IOException {
        if (tag instanceof CompoundTag) {
            CompoundTag compoundTag = (CompoundTag) tag;
            for (String key : compoundTag.getKeys()) {
                tagToNode(compoundTag.get(key), node.getNode(key));
            }
        } else if (tag instanceof ListTag) {
            for (Tag value : (ListTag) tag) {
                tagToNode(value, node.getAppendedNode());
            }
        } else if (tag instanceof StringTag) {
            node.setValue(tag.asString());
        } else if (tag instanceof ByteTag) {
            node.setValue(((ByteTag) tag).getByte());
        } else if (tag instanceof ShortTag) {
            node.setValue(((ShortTag) tag).getShort());
        } else if (tag instanceof IntTag) {
            node.setValue(((IntTag) tag).getInt());
        } else if (tag instanceof LongTag) {
            node.setValue(((LongTag) tag).getLong());
        } else if (tag instanceof FloatTag) {
            node.setValue(((FloatTag) tag).getFloat());
        } else if (tag instanceof DoubleTag) {
            node.setValue(((DoubleTag) tag).getDouble());
        } else if (tag instanceof ByteArrayTag) {
            if (node.getOptions().acceptsType(byte[].class)) {
                node.setValue(((ByteArrayTag) tag).getByteArray());
            } else {
                node.setValue(null);
                for (byte b : ((ByteArrayTag) tag).getByteArray()) {
                    node.getAppendedNode().setValue(b);
                }
            }
        } else if (tag instanceof IntArrayTag) {
            if (node.getOptions().acceptsType(int[].class)) {
                node.setValue(((IntArrayTag) tag).getIntArray());
            } else {
                node.setValue(null);
                for (int i : ((IntArrayTag) tag).getIntArray()) {
                    node.getAppendedNode().setValue(i);
                }
            }

        } else if (tag instanceof LongArrayTag) {
            if (node.getOptions().acceptsType(long[].class)) {
                node.setValue(((LongArrayTag) tag).getLongArray());
            } else {
                node.setValue(null);
                for (long l : ((LongArrayTag) tag).getLongArray()) {
                    node.getAppendedNode().setValue(l);
                }
            }
        } else if (tag instanceof EndTag) {
            // no-op
        } else {
            throw new IOException("Unknown tag type: " + tag.getClass());
        }
    }

    /**
     * Convert a node to tag. Because NBT is strongly typed and does not permit lists with mixed types,
     * some configuration nodes will not be convertible to Tags.
     *
     * @param node The configuration node
     * @return The converted tag object
     * @throws IOException if an IO error occurs while converting the tag
     */
    public static Tag nodeToTag(ConfigurationNode node) throws IOException {
        if (node.hasMapChildren()) {
            CompoundTag tag = new CompoundTag();
            for (Map.Entry<Object, ? extends ConfigurationNode> ent : node.getChildrenMap().entrySet()) {
                tag.put(ent.getKey().toString(), nodeToTag(ent.getValue()));
            }
            return tag;
        } else if (node.hasListChildren()) {
            ListTag list = new ListTag();
            for (ConfigurationNode child : node.getChildrenList()) {
               list.add(nodeToTag(child));
            }
            return list;
        } else {
            Object obj = node.getValue();
            if (obj instanceof byte[]) {
                return new ByteArrayTag((byte[]) obj);
            } else if (obj instanceof int[]) {
                return new IntArrayTag(((int[]) obj));
            } else if (obj instanceof long[]) {
                return new LongArrayTag((long[]) obj);
            } else if (obj instanceof Byte) {
                return ByteTag.of((Byte) obj);
            } else if (obj instanceof Short) {
                return ShortTag.of((Short) obj);
            } else if (obj instanceof Integer) {
                return IntTag.of((Integer) obj);
            } else if (obj instanceof Long) {
                return LongTag.of((Long) obj);
            }  else if (obj instanceof Float) {
                return FloatTag.of((Float) obj);
            } else if (obj instanceof Double) {
                return DoubleTag.of((Double) obj);
            } else if (obj instanceof String) {
                return StringTag.of((String) obj);
            } else {
                throw new IOException("Unsupported object type " + (obj == null ? null : obj.getClass()));
            }
        }
    }

    public static ConfigurationNode createEmptyNode() {
        return createEmptyNode(ConfigurationOptions.defaults().setSerializers(Confabricate.getMinecraftTypeSerializers()));
    }

    public static ConfigurationNode createEmptyNode(@NonNull ConfigurationOptions options) {
        return SimpleConfigurationNode.root(options
                .setAcceptedTypes(ImmutableSet.of(Map.class, List.class, Byte.class,
                        Short.class, Integer.class, Long.class, Float.class, Double.class,
                        long[].class, byte[].class, int[].class, String.class)));
    }
}

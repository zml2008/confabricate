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
package ca.stellardrift.confabricate.nbt;

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
import net.minecraft.nbt.PositionTracker;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagReaders;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.commented.SimpleCommentedConfigurationNode;
import ninja.leaping.configurate.loader.AbstractConfigurationLoader;
import ninja.leaping.configurate.loader.CommentHandler;
import ninja.leaping.configurate.loader.HeaderMode;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.io.output.WriterOutputStream;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A configuration loader that will convert Minecraft NBT data into a Configurate {@link ConfigurationNode}
 */
public class NbtConfigurationLoader extends AbstractConfigurationLoader<CommentedConfigurationNode> {
    private static final byte END_TAG_TYPE = 0;
    private static final String COMMENT_KEY = "__comment__";
    private final boolean compressed;

    public static Builder builder() {
        return new Builder();
    }

    protected NbtConfigurationLoader(@NonNull Builder builder) {
        super(builder, new CommentHandler[]{});
        this.compressed = builder.isCompressed();
    }

    @Override
    protected void loadInternal(CommentedConfigurationNode node, BufferedReader reader) throws IOException {
        DataInputStream dis;
        if (compressed) {
            dis = new DataInputStream(new GZIPInputStream(new ReaderInputStream(reader, StandardCharsets.UTF_8))); // eww, fix in configurate v4
        } else {
            dis = new DataInputStream(new ReaderInputStream(reader, StandardCharsets.UTF_8)); // eww, fix in configurate v4
        }

        byte b = dis.readByte();
        if (b != END_TAG_TYPE) {
            Tag tag = TagReaders.of(b).read(dis, 0, PositionTracker.DEFAULT);
            tagToNode(tag, node);
        }
    }

    private static void tagToNode(Tag tag, ConfigurationNode node) throws IOException {
        if (tag instanceof CompoundTag) {
            CompoundTag compoundTag = (CompoundTag) tag;
            for (String key : compoundTag.getKeys()) {
                if (node instanceof CommentedConfigurationNode && key.equals(COMMENT_KEY)) {
                    ((CommentedConfigurationNode) node).setComment(compoundTag.getString(key));
                } else {
                    tagToNode(compoundTag.get(key), node.getNode(key));
                }
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

    private static Tag nodeToTag(ConfigurationNode node) throws IOException {
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

    @Override
    protected void saveInternal(ConfigurationNode node, Writer writer) throws IOException {
        WriterOutputStream os = new WriterOutputStream(writer, StandardCharsets.UTF_8);
        DataOutputStream dos;
        if (compressed) {
            dos = new DataOutputStream(new GZIPOutputStream(os));
        } else {
            dos = new DataOutputStream(os);
        }

        Tag tag = nodeToTag(node);

        dos.writeByte(tag.getType());
        if (tag.getType() != END_TAG_TYPE) {
            dos.writeUTF("");
            tag.write(dos);
        }
    }

    @NonNull
    @Override
    public CommentedConfigurationNode createEmptyNode(@NonNull ConfigurationOptions options) {
        return SimpleCommentedConfigurationNode.root(options
                .setAcceptedTypes(ImmutableSet.of(Map.class, List.class, Byte.class,
                        Short.class, Integer.class, Long.class, Float.class, Double.class,
                        long[].class, byte[].class, int[].class, String.class)));
    }

    public static class Builder extends AbstractConfigurationLoader.Builder<Builder> {
        private boolean compressed = true;

        protected Builder() {

        }

        public Builder setCompressed(boolean compressed) {
            this.compressed = compressed;
            return this;
        }

        public boolean isCompressed() {
            return this.compressed;
        }

        @Override
        public @NonNull NbtConfigurationLoader build() {
            this.setHeaderMode(HeaderMode.NONE);
            return new NbtConfigurationLoader(this);
        }
    }
}

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertLinesMatch;

import ca.stellardrift.confabricate.typeserializers.MinecraftSerializers;
import com.google.common.io.Resources;
import com.google.common.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.minecraft.util.math.BlockPos;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MinecraftSerializersTest {

    private static final TypeToken<BlockPos> BLOCK_POS_TYPE = TypeToken.of(BlockPos.class);

    @Test
    public void testCodecSerializer() throws ObjectMappingException {
        final TypeSerializerCollection serializers = TypeSerializerCollection.defaults().newChild();
        serializers.register(BLOCK_POS_TYPE, MinecraftSerializers.forCodec(BlockPos.field_25064));

        final ConfigurationNode testElement = ConfigurationNode.root(ConfigurationOptions.defaults().withSerializers(serializers), n -> {
            n.appendListNode().setValue(4);
            n.appendListNode().setValue(5);
            n.appendListNode().setValue(8);
        });

        final BlockPos pos = testElement.getValue(BLOCK_POS_TYPE);

        assertEquals(new BlockPos(4, 5, 8), pos);
    }

    @ConfigSerializable
    @SuppressWarnings("UnusedVariable")
    static class TestSerializable {
        @Setting
        private String testValue = "hello world";

        @Setting
        private BlockPos position = new BlockPos(1, 8, 4);
    }

    @Test
    public void testSerializerCodec() throws IOException {
        final TypeSerializerCollection serializers = TypeSerializerCollection.defaults().newChild();
        serializers.register(BLOCK_POS_TYPE, MinecraftSerializers.forCodec(BlockPos.field_25064));

        final Codec<TestSerializable> codec = MinecraftSerializers.forSerializer(TypeToken.of(TestSerializable.class), serializers);

        final DataResult<JsonElement> out = codec.encode(new TestSerializable(), JsonOps.INSTANCE, JsonOps.INSTANCE.empty());
        out.error().ifPresent(err -> {
            throw new RuntimeException(err.message());
        });

        final StringWriter buffer = new StringWriter();
        try (JsonWriter writer = new JsonWriter(buffer)) {
            writer.setIndent("    ");
            Streams.write(out.result().orElseThrow(() -> new RuntimeException("No result present!")), writer);
        }

        assertLinesMatch(Resources.readLines(getClass().getResource("test-serialize-codec.json"), StandardCharsets.UTF_8),
                Arrays.asList(buffer.toString().split("\n")));

    }

}

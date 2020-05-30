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
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.ScalarSerializer;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Predicate;

public final class TextSerializer implements TypeSerializer<Text> {

    public static final TypeToken<Text> TYPE = TypeToken.of(Text.class);
    public static final TypeSerializer<Text> INSTANCE = new TextSerializer();

    private TextSerializer() {}

    @Nullable
    @Override
    public Text deserialize(@NonNull final TypeToken<?> type, @NonNull final ConfigurationNode value) throws ObjectMappingException {
        if (value.isMap() || value.isList()) {
            final JsonElement element = CodecSerializer.opsFor(value).convertTo(JsonOps.INSTANCE, value);
            return Text.Serializer.fromJson(element);
        } else {
            final String text = value.getString();
            if (text == null) {
                return null;
            }
            if (text.startsWith("{")) { // Legacy format as JSON
                return Text.Serializer.fromLenientJson(text);
            } else {
                return new LiteralText(text);
            }
        }
    }

    @Override
    public void serialize(@NonNull final TypeToken<?> type, @Nullable final Text obj, @NonNull final ConfigurationNode value)
            throws ObjectMappingException {
        if (obj == null) {
            value.setValue(null);
            return;
        }

        if (obj instanceof LiteralText) {
            final LiteralText literal = (LiteralText) obj;
            if (literal.getSiblings().isEmpty() && literal.getStyle().equals(Style.EMPTY)) {
                value.setValue(literal.getRawString());
                return;
            }
        }

        value.setValue(JsonOps.INSTANCE.convertTo(CodecSerializer.opsFor(value), Text.Serializer.toJsonTree(obj)));
    }

}

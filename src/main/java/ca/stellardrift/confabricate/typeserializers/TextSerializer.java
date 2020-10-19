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

import static ca.stellardrift.confabricate.typeserializers.MinecraftSerializers.opsFor;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

final class TextSerializer implements TypeSerializer<Text> {

    static final TypeSerializer<Text> INSTANCE = new TextSerializer();

    private TextSerializer() {}

    @Override
    public Text deserialize(final @NonNull Type type, final @NonNull ConfigurationNode value) throws SerializationException {
        if (value.isMap() || value.isList()) {
            final JsonElement element = opsFor(value).convertTo(JsonOps.INSTANCE, value);
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
    public void serialize(final @NonNull Type type, final @Nullable Text obj, final @NonNull ConfigurationNode value) throws SerializationException {
        if (obj == null) {
            value.raw(null);
            return;
        }

        if (obj instanceof LiteralText) {
            final LiteralText literal = (LiteralText) obj;
            if (literal.getSiblings().isEmpty() && literal.getStyle().equals(Style.EMPTY)) {
                value.raw(literal.getRawString());
                return;
            }
        }

        value.from(JsonOps.INSTANCE.convertTo(opsFor(value), Text.Serializer.toJsonTree(obj)));
    }

    @Override
    public Text emptyValue(final Type specificType, final ConfigurationOptions options) {
        return new LiteralText("");
    }

}

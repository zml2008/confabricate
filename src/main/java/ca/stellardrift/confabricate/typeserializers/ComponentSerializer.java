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

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import java.lang.reflect.Type;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import static ca.stellardrift.confabricate.typeserializers.MinecraftSerializers.opsFor;

final class ComponentSerializer implements TypeSerializer<Component> {

    static final TypeSerializer<Component> INSTANCE = new ComponentSerializer();

    private ComponentSerializer() {}

    @Override
    public Component deserialize(final @NonNull Type type, final @NonNull ConfigurationNode value) throws SerializationException {
        if (value.isMap() || value.isList()) {
            final JsonElement element = opsFor(value).convertTo(JsonOps.INSTANCE, value);
            return Component.Serializer.fromJson(element);
        } else {
            final String text = value.getString();
            if (text == null) {
                return null;
            }
            if (text.startsWith("{")) { // Legacy format as JSON
                return Component.Serializer.fromJson(text);
            } else {
                return new TextComponent(text);
            }
        }
    }

    @Override
    public void serialize(
        final @NonNull Type type,
        final @Nullable Component obj,
        final @NonNull ConfigurationNode value
    ) throws SerializationException {
        if (obj == null) {
            value.raw(null);
            return;
        }

        if (obj instanceof final TextComponent text) {
            if (text.getSiblings().isEmpty() && text.getStyle().equals(Style.EMPTY)) {
                value.raw(text.getContents());
                return;
            }
        }

        value.from(JsonOps.INSTANCE.convertTo(opsFor(value), Component.Serializer.toJsonTree(obj)));
    }

    @Override
    public Component emptyValue(final Type specificType, final ConfigurationOptions options) {
        return new TextComponent("");
    }

}

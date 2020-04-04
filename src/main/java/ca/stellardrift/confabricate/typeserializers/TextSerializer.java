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
import net.minecraft.text.Text;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TextSerializer implements TypeSerializer<Text> {
    public static final TypeToken<Text> TOKEN = TypeToken.of(Text.class);
    public static final TypeSerializer<Text> INSTANCE = new TextSerializer();

    private TextSerializer() {
    }

    @Nullable
    @Override
    public Text deserialize(@NonNull TypeToken<?> type, @NonNull ConfigurationNode value) throws ObjectMappingException {
        final String text = value.getString();
        if (text == null) {
            return null;
        }
        return Text.Serializer.fromLenientJson(text);
    }

    @Override
    public void serialize(@NonNull TypeToken<?> type, @Nullable Text obj, @NonNull ConfigurationNode value) throws ObjectMappingException {
        if (obj == null) {
            value.setValue(null);
        } else {
            value.setValue(Text.Serializer.toJson(obj));
        }
    }
}

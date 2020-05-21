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
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.ScalarSerializer;

import java.util.function.Predicate;

public final class TextSerializer extends ScalarSerializer<Text> {

    public static final ScalarSerializer<Text> INSTANCE = new TextSerializer();

    private TextSerializer() {
        super(Text.class);
    }

    @Override
    public Text deserialize(final TypeToken<?> type, final Object obj) throws ObjectMappingException {
        if (obj instanceof CharSequence) {
            return Text.Serializer.fromLenientJson(((CharSequence) obj).toString());
        }
        throw new ObjectMappingException("Provided value was not a CharSequence");
    }

    @Override
    public Object serialize(final Text item, final Predicate<Class<?>> typeSupported) {
        return Text.Serializer.toJson(item); // String always supported
    }

}

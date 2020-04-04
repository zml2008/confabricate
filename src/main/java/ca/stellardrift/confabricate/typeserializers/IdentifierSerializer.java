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
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Serializes an {@link Identifier} to a configuration object.
 *
 * When identifiers are output, they are given in the canonical string format.
 *
 * When identifiers are read, they are accepted in the formats of:
 * <ul>
 *     <li>A mapping, where the identifier's namespace is under the key <pre>namespace</pre>
 *     or <pre>key</pre>, and the path is under <pre>path</pre> or <pre>value</pre>.</li>
 *     <li>
 *         A list of either one or two elements. If the list is two elements,
 *         the first is the namespace and the second is a path. If the list is one element,
 *         that element is parsed as a string (see below).
 *     </li>
 *     <li>A string, in standard <pre>[namespace:]&ltpath></pre> format, where the default namespace is <pre>minecraft</pre></li>
 * </ul>
 */
public class IdentifierSerializer implements TypeSerializer<Identifier> {
    //private static final String NAMESPACE_MINECRAFT = "minecraft";
    public static final IdentifierSerializer INSTANCE = new IdentifierSerializer();
    public static final TypeToken<Identifier> TOKEN = TypeToken.of(Identifier.class);

    @Nullable
    @Override
    public Identifier deserialize(@NonNull TypeToken<?> type, @NonNull ConfigurationNode value) throws ObjectMappingException {
        return fromNode(value);
    }

    @Override
    public void serialize(@NonNull TypeToken<?> type, @Nullable Identifier obj, @NonNull ConfigurationNode value) throws ObjectMappingException {
        toNode(obj, value);
    }

    static Identifier fromNode(ConfigurationNode node) throws ObjectMappingException {
        if (node.isVirtual()) {
            return null;
        }
        if (node.hasMapChildren()) {
            String key = firstPresentChild(node, "namespace", "key").getString();
            String value = firstPresentChild(node, "path", "value").getString();
            if (value == null) {
                throw listAcceptedFormats();
            }
            if (key == null) {
                if (value.contains(":")) {
                    return createIdentifier(value);
                } else {
                    throw listAcceptedFormats();
                }
            }
            return createIdentifier(key, value);
        } else if (node.hasListChildren()) {
            List<? extends ConfigurationNode> children = node.getChildrenList();
            switch (children.size()) {
                case 2:
                    final String key = children.get(0).getString();
                    final String value = children.get(1).getString();
                    if (key != null && value != null) {
                        return createIdentifier(key, value);
                    }
                case 1:
                    final String combined = children.get(0).getString();
                    if (combined != null) {
                        return createIdentifier(combined);
                    }
                default:
                    throw listAcceptedFormats();

            }
        } else {
            String val = node.getString();
            if (val == null) {
                throw listAcceptedFormats();
            }
            return new Identifier(val);
        }
    }

    private static ConfigurationNode firstPresentChild(ConfigurationNode parent, String... values) {
        Map<Object, ? extends ConfigurationNode> children =parent.getChildrenMap();
        for (String value : values) {
            if (children.containsKey(value)) {
                return children.get(value);
            }
        }
        return parent.getNode(values[0]);
    }

    private static Identifier createIdentifier(String key, String value) throws ObjectMappingException {
        try {
            return new Identifier(key, value);
        } catch (InvalidIdentifierException ex) {
            throw new ObjectMappingException(ex);
        }
    }

    private static Identifier createIdentifier(String data) throws ObjectMappingException {
        try {
            return new Identifier(data);
        } catch (InvalidIdentifierException ex) {
            throw new ObjectMappingException(ex.getMessage());
        }
    }

    private static ObjectMappingException listAcceptedFormats() {
        return new ObjectMappingException("The provided format is not an acceptable Identifier format. See IdentifierSerializer javadocs for details");
    }

    static void toNode(Identifier ident, ConfigurationNode node) {
        if (ident == null) {
            node.setValue(null);
        } else {
            node.setValue(ident.toString());
        }
    }
}

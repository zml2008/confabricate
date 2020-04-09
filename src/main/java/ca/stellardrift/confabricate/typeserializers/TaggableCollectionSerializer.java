package ca.stellardrift.confabricate.typeserializers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.TypeToken;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagContainer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import static ca.stellardrift.confabricate.typeserializers.IdentifierSerializer.createIdentifier;

public class TaggableCollectionSerializer<T> implements TypeSerializer<TaggableCollection<T>> {
    private static final String TAG_PREFIX = "#";
    private final Registry<T> registry;
    private final TagContainer<T> tagRegistry;

    public TaggableCollectionSerializer(Registry<T> registry, TagContainer<T> tagRegistry) {
        this.registry = registry;
        this.tagRegistry = tagRegistry;
    }

    @Nullable
    @Override
    public TaggableCollection<T> deserialize(@NonNull TypeToken<?> type, @NonNull ConfigurationNode value) throws ObjectMappingException {
        if (value.hasMapChildren()) {
            throw new ObjectMappingException("Tags cannot be provided in map format");
        }

        ImmutableSet.Builder<T> elements = ImmutableSet.builder();
        ImmutableSet.Builder<Tag<T>> tagElements = ImmutableSet.builder();

        if (value.hasListChildren()) {
            for (ConfigurationNode node : value.getChildrenList()) {
                handleSingle(node, elements, tagElements);
            }
        } else {
            handleSingle(value, elements, tagElements);
        }
        return new TaggableCollectionImpl<>(registry, tagRegistry, elements.build(), tagElements.build());
    }

    private void handleSingle(ConfigurationNode node, ImmutableSet.Builder<T> elements, ImmutableSet.Builder<Tag<T>> tagElements) throws ObjectMappingException {
        boolean isTag = false;
        String ident = String.valueOf(node.getValue());
        if (ident.startsWith(TAG_PREFIX)) {
            isTag = true;
            ident = ident.substring(1);
        }
        Identifier id = createIdentifier(ident);

        if (isTag) {
            Tag<T> tag = tagRegistry.get(id);
            if (tag == null) {
                throw new ObjectMappingException("Unknown tag #" + id);
            }
            tagElements.add(tag);

        } else {
            T element = registry.get(id);
            if (element == null) {
                throw new ObjectMappingException("Unknown member of registry " + id);
            }
            elements.add(element);
        }
    }



    @Override
    public void serialize(@NonNull TypeToken<?> type, @Nullable TaggableCollection<T> obj, @NonNull ConfigurationNode value) throws ObjectMappingException {
        value.setValue(ImmutableList.of());
        if (obj != null) {
            for (T element : obj.getSpecificElements()) {
                Identifier id = registry.getId(element);
                if (id == null) {
                    throw new ObjectMappingException("Unknown element " + element);
                }
                IdentifierSerializer.toNode(id, value.getAppendedNode());
            }

            for (Tag<T> tag : obj.getTaggedElements()) {
                value.getAppendedNode().setValue(TAG_PREFIX + tag.getId().toString());
            }
        }

    }
}

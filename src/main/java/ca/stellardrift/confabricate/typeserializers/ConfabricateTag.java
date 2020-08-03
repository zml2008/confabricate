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

import com.google.common.collect.ImmutableList;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagGroup;
import net.minecraft.util.registry.Registry;

import java.util.List;
import java.util.function.Supplier;

/**
 * A tag implementation that keeps its original form for reserialization.
 *
 * @param <V> element type
 */
public class ConfabricateTag<V> implements Tag<V> {

    private final List<Tag.Entry> serializedForm;

    public ConfabricateTag(final List<Entry> serializedForm, final Supplier<Registry<V>> elementResolver, final Supplier<TagGroup<V>> tagResolver) {
        this.serializedForm = ImmutableList.copyOf(serializedForm);
    }

    @Override
    public boolean contains(final V entry) {
        return false;
    }

    public List<Tag.Entry> serializedForm() {
        return this.serializedForm;
    }

    @Override
    public List<V> values() {
        return null;
    }
}

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

import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;

import java.util.Collection;

public interface TaggableCollection<T> extends Iterable<T> {
    Collection<T> getSpecificElements();
    Collection<Tag<T>> getTaggedElements();

    TaggableCollection<T> addingSingle(Identifier ident);
    TaggableCollection<T> addingTag(Identifier tag);

    TaggableCollection<T> removingSingle(Identifier ident);
    TaggableCollection<T> removingTag(Identifier tag);
}

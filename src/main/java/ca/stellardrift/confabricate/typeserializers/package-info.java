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
/**
 * Minecraft-specific type serializers.
 *
 * <p>{@link ca.stellardrift.confabricate.typeserializers.MinecraftSerializers}
 * holds a common collection of standard serializers, plus factory methods to
 * serialize custom {@link net.minecraft.core.Registry Regstries}
 * and {@link com.mojang.serialization.Codec Codecs}.
 *
 * <p>To be able to handle a mixed collection of registry elements and {@link net.minecraft.tags.Tag tags},
 * Confabricate can create anonymous {@link net.minecraft.tags.Tag Tags}
 * which can be used in object-mapped classes to preserve information on how
 * elements were described in the configuration file.
 */
package ca.stellardrift.confabricate.typeserializers;

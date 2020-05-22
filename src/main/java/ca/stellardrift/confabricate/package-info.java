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
 * Configurate bindings for Fabric.
 *
 * <p>Most direct access to Configurate should occur through
 * {@link ca.stellardrift.confabricate.Confabricate}.
 *
 * <p>To convert between NBT tags and {@link ninja.leaping.configurate.ConfigurationNode nodes},
 * the {@link ca.stellardrift.confabricate.NbtNodeAdapter} can interpret the types.
 *
 * <p>{@link ca.stellardrift.confabricate.ConfigurateOps} provides basic
 * integration between DataFixerUpper's type system and Configurate nodes. Other
 * integration for {@link com.mojang.serialization.Codec Codecs} is in the
 * {@code typeserializers} package.
 */
package ca.stellardrift.confabricate;

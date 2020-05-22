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

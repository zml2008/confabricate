/**
 * Minecraft-specific type serializers.
 *
 * <p>{@link ca.stellardrift.confabricate.typeserializers.MinecraftSerializers}
 * holds a common collection of standard serializers, plus factory methods to
 * serialize custom {@link net.minecraft.util.registry.Registry Regstries}
 * and {@link com.mojang.serialization.Codec Codecs}.
 *
 * <p>To be able to handle a mixed collection of registry elements and {@link net.minecraft.tag.Tag tags},
 * Confabricate has a {@link ca.stellardrift.confabricate.typeserializers.TaggableCollection}
 * which can be used in object-mapped classes to preserve information on how
 * elements were described in the configuration file.
 */
package ca.stellardrift.confabricate.typeserializers;

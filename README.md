# Confabricate

A mod that provides Configurate's core, gson and hocon serializers, and useful utilities for a Fabric environment.

## Fabric-specific features

### TypeSerializers for:

- `Identifier`s
- Any item stored in a `Registry`
- `Text` (as json)

### NBTNodeAdapter

Translates back and forth between Configurate nodes and Minecraft's own NBT `Tags`

### DataFixerUpper integration

Thanks to @i509VCB for providing an initial implementation of DynamicOps

- DynamicOps implementation for ConfigurationNodes
- Basic integration between ConfigurationTransformations and DataFixers

### Configuration per-mod

- Utility methods in `Confabricate` to get a HOCON-format configuration for a mod

## How to use

Confabricate versions are in the format `<confabricate version>+<configurate version>` for easy identification

It is recommeded to use this project in jar-in-jar packaging

Gradle:

```kotlin

dependencies {
    include("ca.stellardrift:confabricate:[...]")
    modImplementation("ca.stellardrift:confabricate:[...]")
}
```

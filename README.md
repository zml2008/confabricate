# Confabricate

[![javadoc](https://javadoc.io/badge2/ca.stellardrift/confabricate/javadoc.svg)](https://javadoc.io/doc/ca.stellardrift/confabricate) | ![build status](https://img.shields.io/github/workflow/status/zml2008/confabricate/Publish) | [![CurseForge page](http://cf.way2muchnoise.eu/versions/confabricate.svg)](https://www.curseforge.com/minecraft/mc-mods/confabricate)

A mod that provides Configurate's core, gson and hocon serializers, and useful utilities for a Fabric environment.

## Fabric-specific features

### TypeSerializers for:

- `Identifier`s
- Any item stored in a `Registry`
- `Text` (as json)
- `ItemStack`s
- `Tags`, both datapack-defined and defined in the config, currently for blocks, items, entity types, and fluids

### NBTNodeAdapter

Translates back and forth between Configurate nodes and Minecraft's own NBT `Tags`

### DataFixerUpper integration

Thanks to @i509VCB for providing an initial implementation of DynamicOps

- DynamicOps implementation for ConfigurationNodes in `ConfigurateOps`
- Basic integration between ConfigurationTransformations and DataFixers
- Support for accessing `Codec`s as `TypeSerializer`s (and vice versa!)

### Configuration per-mod

- Utility methods in `Confabricate` to get a HOCON-format configuration for a mod
- Common watch service and methods to get an automatically reloading HOCON-format configuration for a mod.

## How to use

Confabricate versions are in the format `<confabricate version>+<configurate version>` for easy identification

**Version compatibility**

Minecraft     | Confabricate
------------- | ------------
1.15          | 1.1+3.7
1.16 + 1.16.1 | 1.2+3.7
1.16.2        | 1.3+3.7.1


It is recommended to use this project in jar-in-jar packaging

Releases of Confabricate are on jCenter, as are releases of Configurate. Snapshots of Confabricate and Configurate are published on the PermissionsEx repository and Sonatype OSS respectively.

```kotlin

repositories {
    jcenter()
    // Snapshots only
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots/") {
        name = "sonatype"
    }
    maven(url = "https://repo.glaremasters.me/repository/permissionsex") {
        name = "pex"
    }
}

dependencies {
    modImplementation(include("ca.stellardrift:confabricate:1.3+3.7.1")!!)
}
```


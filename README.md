# Confabricate

[![javadoc](https://javadoc.io/badge2/ca.stellardrift/confabricate/javadoc.svg)](https://javadoc.io/doc/ca.stellardrift/confabricate) | ![build status](https://img.shields.io/github/workflow/status/zml2008/confabricate/Publish) | [![CurseForge page](http://cf.way2muchnoise.eu/versions/confabricate.svg)](https://www.curseforge.com/minecraft/mc-mods/confabricate)

A mod that provides Configurate's core, gson and hocon serializers, and useful utilities for a Fabric environment.

## Fabric-specific features

### TypeSerializers for:

- `ResourceLocation`s
- Any item stored in a `Registry`
- `Component` (as json)
- `ItemStack`s
- tags (as `HolderSet`), both datapack-defined and defined in the config, currently for blocks, items, entity types, and fluids

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

**Version compatibility**

Minecraft      | Confabricate   | Configurate
-------------- | -------------- | ------------
1.15           | 1.1+3.7        | 3.7
1.16 + 1.16.1  | 1.2+3.7        | 3.7
1.16.2         | 1.3+3.7.1      | 3.7.1
1.16.2-1.16.5  | 2.1.0          | 4.1.1
1.17           | 2.2.0-SNAPSHOT | 4.1.1
1.18.2         | 3.0.0-SNAPSHOT | 4.1.2

It is recommended to use this project in jar-in-jar packaging.

Releases of Confabricate are on Maven Central, as are releases of Configurate. Snapshots of Confabricate and Configurate are published on Sonatype 
OSS.

Additionally, both releases and snapshots of Confabricate are published on the `stellardrift` repository.

```kotlin

repositories {
    mavenCentral()
    // Snapshots only
    maven(url = "https://repo.stellardrift.ca/repository/snapshots/") {
        name = "stellardriftSnapshots"
        mavenContent { snapshotsOnly() }
    }
}

dependencies {
    modImplementation(include("ca.stellardrift:confabricate:3.0.0-SNAPSHOT")!!)
}
```


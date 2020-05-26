# Confabricate

A mod that provides Configurate's core, gson and hocon serializers, and useful utilities for a Fabric environment.

## Fabric-specific features

### TypeSerializers for:

- `Identifier`s
- Any item stored in a `Registry`
- `Text` (as json)
- Mixed lists of items and tags (represented by `TaggableCollection`s), currently for blocks, items, entity types, and fluids

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

**Version compatibility**

Minecraft | Confabricate
------------- | ------------------
1.15         | 1.0+3.6.1
1.16 (snapshots) | 1.2-SNAPSHOT+3.7

It is recommended to use this project in jar-in-jar packaging

Releases of Confabricate are on jCenter, and Configurate is on the Sponge repository, so we need to declare both at the moment. Snapshots are published on the PermissionsEx repository.

```kotlin

repositories {
    jcenter()
    maven(url = "https://repo.spongepowered.org/maven") {
        name = "sponge"
    }
    maven(url= "https://repo.glaremasters.me/repository/permissionsex") {
        name = "pex"
    }
}

dependencies {
    include("ca.stellardrift:confabricate:1.0+3.6.1")
    modImplementation("ca.stellardrift:confabricate:1.0+3.6.1")
}
```


# Confabricate

A mod that provides Configurate, plus useful utilities, for a Fabric environment.

## Fabric-specific features

### TypeSerializers for:

- `Identifier`s
- Any item stored in a `Registry`
- `Text` (as json)

### Configuration per-mod

- Utility methods in `Confabricate` to get a HOCON-format configuration


## How to use

Jar-in-jar

Versioning is intended to include the version of Configurate being built against

Gradle:

```kotlin

dependencies {
    include("ca.stellardrift:confabricate:[...]")
    modImplementation("ca.stellardrift:confabricate:[...]")
}
```

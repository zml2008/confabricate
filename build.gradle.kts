import ca.stellardrift.build.apiInclude
import ca.stellardrift.build.configurate
import ca.stellardrift.build.sponge

plugins {
    id("fabric-loom") version "0.2.6-SNAPSHOT"
    id("ca.stellardrift.opinionated") version "1.0.1"
}

val versionBase = "1.1-SNAPSHOT"
val versionMinecraft: String by project
val versionMappings: String by project
val versionLoader: String by project
val versionFabricApi: String by project
val versionConfigurate: String by project

group = "ca.stellardrift"
version = "$versionBase+$versionConfigurate"
description = ext["longDescription"] as String

minecraft {
    refmapName = "${rootProject.name.toLowerCase()}-refmap.json"
}

license {
    header = rootProject.file("LICENSE_HEADER")
}

repositories {
    jcenter()
    sponge()
}

tasks.withType(Jar::class).configureEach {
    manifest {
        attributes("Specification-Name" to "Configurate",
                "Specification-Version" to versionConfigurate,
                "Implementation-Name" to project.name,
                "Implementation-Version" to versionBase)
    }
}

tasks.processResources {
    expand("project" to project)
}

dependencies {
    minecraft("com.mojang:minecraft:$versionMinecraft")
    mappings("net.fabricmc:yarn:$versionMinecraft+build.$versionMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$versionLoader")

    apiInclude(configurate("core", versionConfigurate)) {
        exclude("com.google.guava")
    }
    apiInclude(configurate("hocon", versionConfigurate)) {
        exclude("com.google.guava")
    }

    include("com.typesafe:config:1.4.0")
    apiInclude(configurate("gson", versionConfigurate)) { isTransitive = false }
    // For test commands
    // listOf("commands-v0", "api-base").forEach {
    //     implementationInclude("net.fabricmc.fabric-api:fabric-$it:$versionFabricApi")
    // }
}

opinionated {
    github("zml2008", "confabricate")
    apache2()
    publication.apply {
        artifact(tasks.jar.get()) {
            classifier = "dev"
        }
        artifact(tasks.remapJar.get())

        artifact(tasks.getByName("sourcesJar")) {
            builtBy(tasks.remapSourcesJar.get())
        }
        artifact(tasks.getByName("javadocJar"))

        pom {
            developers {
                developer {
                    name.set("zml")
                    email.set("zml at stellardrift dot ca")
                }
            }
        }

    }
}

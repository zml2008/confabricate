
import ca.stellardrift.build.common.configurate

plugins {
    id("net.ltgt.errorprone") version "1.3.0"
    id("ca.stellardrift.opinionated.fabric") version "4.1"
    id("net.kyori.indra.publishing.bintray") version "1.2.1"
}

val versionBase = "2.0-SNAPSHOT"
val versionMinecraft: String by project
val versionMappings: String by project
val versionLoader: String by project
val versionFabricApi: String by project
val versionConfigurate: String by project
val versionErrorprone: String by project

group = "ca.stellardrift"
version = "$versionBase+${versionConfigurate.replace("-SNAPSHOT", "")}"
description = ext["longDescription"] as String

repositories {
    mavenCentral()
    jcenter()
}

tasks.withType(Jar::class).configureEach {
    manifest {
        attributes("Specification-Title" to "Configurate",
                "Specification-Version" to versionConfigurate,
                "Implementation-Title" to project.name,
                "Implementation-Version" to versionBase)
    }
}

tasks.withType(Javadoc::class).configureEach {
    val options = this.options
    if (options is StandardJavadocDocletOptions) {
        options.links(
                "https://configurate.aoeu.xyz/$versionConfigurate/apidocs/"
        )
    }
}

dependencies {
    compileOnly("com.google.errorprone:error_prone_annotations:$versionErrorprone")
    errorprone("com.google.errorprone:error_prone_core:$versionErrorprone")
    compileOnlyApi("org.checkerframework:checker-qual:3.8.0")

    minecraft("com.mojang:minecraft:$versionMinecraft")
    mappings("net.fabricmc:yarn:$versionMinecraft+build.$versionMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$versionLoader")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$versionFabricApi")

    // We add the bom, but because Loom can't handle reading versions from there, we'll have to
    modApi(enforcedPlatform(configurate("bom", versionConfigurate)))
    include(modApi(configurate("core", versionConfigurate))!!)
    include(modApi(configurate("hocon", versionConfigurate))!!)
    include(modApi(configurate("gson", versionConfigurate)) { isTransitive = false })
    include(modApi(configurate("extra-dfu4", versionConfigurate)) {
        exclude("com.mojang") // Use the game's DFU version
    })

    include("com.typesafe:config:1.4.1")
    include("io.leangen.geantyref:geantyref:1.3.11")

    checkstyle("ca.stellardrift:stylecheck:0.1")
}

indra {
    github("zml2008", "confabricate")
    apache2License()

    javaVersions {
        testWith(8, 11, 15)
    }

    configurePublications {
        pom {
            developers {
                developer {
                    name.set("zml")
                    email.set("zml at stellardrift dot ca")
                }
            }
        }
    }

    publishAllTo("pex", "https://repo.glaremasters.me/repository/permissionsex")
}

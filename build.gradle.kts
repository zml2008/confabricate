
import ca.stellardrift.build.common.configurate
import ca.stellardrift.build.common.stellardriftReleases
import ca.stellardrift.build.common.stellardriftSnapshots

plugins {
    id("net.ltgt.errorprone") version "2.0.1"
    id("ca.stellardrift.opinionated.fabric") version "5.0.0"
    id("net.kyori.indra.publishing.sonatype") version "2.0.3"
}

val versionMinecraft: String by project
val versionMappings: String by project
val versionLoader: String by project
val versionFabricApi: String by project
val versionConfigurate: String by project
val versionErrorprone: String by project

group = "ca.stellardrift"
version = "2.1.0-SNAPSHOT"
description = ext["longDescription"] as String

repositories {
    stellardriftReleases()
    stellardriftSnapshots()
}

tasks.withType(Jar::class).configureEach {
    manifest {
        attributes("Specification-Title" to "Configurate",
                "Specification-Version" to versionConfigurate,
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version)
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

tasks.withType(ProcessResources::class) {
    inputs.property("versionConfigurate", versionConfigurate)
    expand("project" to project, "versionConfigurate" to versionConfigurate)
}

dependencies {
    compileOnly("com.google.errorprone:error_prone_annotations:$versionErrorprone")
    errorprone("com.google.errorprone:error_prone_core:$versionErrorprone")
    compileOnlyApi("org.checkerframework:checker-qual:3.13.0")

    minecraft("com.mojang:minecraft:$versionMinecraft")
    mappings("net.fabricmc:yarn:$versionMinecraft+build.$versionMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$versionLoader")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$versionFabricApi")

    // Don't use the bom because it's broken
    include(modApi(configurate("core", versionConfigurate))!!)
    include(modApi(configurate("hocon", versionConfigurate))!!)
    include(modApi(configurate("gson", versionConfigurate)) {
        exclude("com.google.code.gson") // Use Minecraft's gson
    })
    include(modApi(configurate("extra-dfu4", versionConfigurate)) {
        exclude("com.mojang") // Use the game's DFU version
    })

    include("com.typesafe:config:1.4.1")
    include("io.leangen.geantyref:geantyref:1.3.11")

    checkstyle("ca.stellardrift:stylecheck:0.1")
}

indra {
    github("zml2008", "confabricate") {
        ci(true)
    }
    apache2License()

    javaVersions {
        testWith(8, 11, 16)
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
    publishReleasesTo("stellardrift", "https://repo.stellardrift.ca/repository/releases/")
    publishSnapshotsTo("stellardrift", "https://repo.stellardrift.ca/repository/snapshots/")
}

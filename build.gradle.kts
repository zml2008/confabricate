
import ca.stellardrift.build.common.configurate

plugins {
    id("net.ltgt.errorprone") version "1.3.0"
    id("ca.stellardrift.opinionated.fabric") version "4.1"
    id("net.kyori.indra.publishing.bintray") version "1.2.1"
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
    maven("https://repo.stellardrift.ca/repository/stable/") {
        name = "stellardriftReleases"
        mavenContent { releasesOnly() }
    }

    maven("https://repo.stellardrift.ca/repository/snapshots/") {
        name = "stellardriftSnapshots"
        mavenContent { snapshotsOnly() }
    }
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

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("versionConfigurate", versionConfigurate)
}

dependencies {
    compileOnly("com.google.errorprone:error_prone_annotations:$versionErrorprone")
    errorprone("com.google.errorprone:error_prone_core:$versionErrorprone")
    compileOnlyApi("org.checkerframework:checker-qual:3.8.0")

    minecraft("com.mojang:minecraft:$versionMinecraft")
    mappings("net.fabricmc:yarn:$versionMinecraft+build.$versionMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$versionLoader")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$versionFabricApi")

    // We can't add the bom because loom doesn't put it into our pom correctly
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
    publishReleasesTo("stellardrift", "https://repo.stellardrift.ca/repository/releases/")
    publishSnapshotsTo("stellardrift", "https://repo.stellardrift.ca/repository/snapshots/")
}

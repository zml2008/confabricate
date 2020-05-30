import ca.stellardrift.build.apiInclude
import ca.stellardrift.build.configurate
import ca.stellardrift.build.sponge

plugins {
    id("fabric-loom") version "0.2.7-SNAPSHOT"
    id("net.ltgt.errorprone") version "1.1.1"
    id("ca.stellardrift.opinionated") version "2.0.1"
    id("ca.stellardrift.opinionated.publish") version "2.0.1"
    checkstyle
}

val versionBase = "1.2-SNAPSHOT"
val versionMinecraft: String by project
val versionMappings: String by project
val versionLoader: String by project
val versionFabricApi: String by project
val versionConfigurate: String by project
val versionErrorprone: String by project

group = "ca.stellardrift"
version = "$versionBase+${versionConfigurate.replace("-SNAPSHOT", "")}"
description = ext["longDescription"] as String

/*minecraft {
    refmapName = "${rootProject.name.toLowerCase()}-refmap.json"
}*/

sourceSets.register("testmod") {
    compileClasspath += sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().runtimeClasspath
}

dependencies {
    "testmodCompile"(sourceSets.main.get().output)
}

license {
    header = rootProject.file("LICENSE_HEADER")
}

repositories {
    jcenter()
    maven(url="https://repo.glaremasters.me/repository/permissionsex") {
        name = "pex"
    }
    sponge()
}

tasks.withType(Jar::class).configureEach {
    manifest {
        attributes("Specification-Title" to "Configurate",
                "Specification-Version" to versionConfigurate,
                "Implementation-Title" to project.name,
                "Implementation-Version" to versionBase)
    }
}

tasks.processResources {
    expand("project" to project)
}

tasks.named("processTestmodResources", ProcessResources::class).configure {
    expand("project" to project)
}

checkstyle {
    toolVersion = "8.32"
    configDirectory.set(project.projectDir.resolve("etc/checkstyle"))
    configProperties = mapOf(
            "severity" to "error"
    )
}

tasks.withType(Javadoc::class).configureEach {
    val options = this.options
    if (options is StandardJavadocDocletOptions) {
        options.links(
                "https://configurate.aoeu.xyz/$versionConfigurate/apidocs/",
                "https://maven.fabricmc.net/docs/yarn-$versionMinecraft+build.$versionMappings/",
                "https://docs.oracle.com/javase/8/docs/api/"
        )
    }
}

dependencies {
    api("com.google.errorprone:error_prone_annotations:$versionErrorprone")
    errorprone("com.google.errorprone:error_prone_core:$versionErrorprone")
    minecraft("com.mojang:minecraft:$versionMinecraft")
    mappings("net.fabricmc:yarn:$versionMinecraft+build.$versionMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$versionLoader")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$versionFabricApi")

    api(enforcedPlatform(configurate("bom", versionConfigurate)))
    apiInclude(configurate("core", versionConfigurate)) {
        exclude("com.google.guava")
    }
    apiInclude(configurate("hocon", versionConfigurate)) {
        exclude("com.google.guava")
    }

    include("com.typesafe:config:1.4.0")
    apiInclude(configurate("gson", versionConfigurate)) { isTransitive = false }
    //modRuntime("net.fabricmc.fabric-api:fabricapi:$versionFabricApi")
}

opinionated {
    github("zml2008", "confabricate")
    apache2()
    useJUnit5()

    publication?.apply {
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

publishing {
    repositories {
        if (project.hasProperty("pexUsername") && project.hasProperty("pexPassword")) {
            maven {
                name = "pex"
                url = uri("https://repo.glaremasters.me/repository/permissionsex")
                credentials {
                    username = project.property("pexUsername").toString()
                    password = project.property("pexPassword").toString()
                }
            }
        }
    }
}

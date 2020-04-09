plugins {
    id("fabric-loom") version "0.2.6-SNAPSHOT"
    id("com.github.hierynomus.license") version "0.15.0"
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
}

val versionMinecraft = ext["version.minecraft"] as String
val versionMappings = ext["version.mappings"] as String
val versionLoader = ext["version.loader"] as String
val versionFabricApi = ext["version.fabricApi"] as String
val versionConfigurate = ext["version.configurate"] as String

group = "ca.stellardrift"
version = "1.0-SNAPSHOT+$versionConfigurate"

minecraft {
    refmapName = "${rootProject.name.toLowerCase()}-refmap.json"
}

license {
    header = rootProject.file("LICENSE_HEADER")
    mapping("java", "SLASHSTAR_STYLE")
    include("**/*.java")
}

repositories {
    jcenter()
    maven(url = "https://repo.spongepowered.org/maven") {
        name = "sponge"
    }
}


java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.processResources {
    expand("project" to project)
}

fun DependencyHandlerScope.apiInclude(spec: String, func: ExternalModuleDependency.() -> Unit = {}) {
    modApi(spec, func)
    include(spec, func)
}

fun DependencyHandlerScope.implementationInclude(spec: String, func: ExternalModuleDependency.() -> Unit = {}) {
    modApi(spec, func)
    include(spec, func)
}

dependencies {
    minecraft("com.mojang:minecraft:$versionMinecraft")
    mappings("net.fabricmc:yarn:$versionMinecraft+build.14:v2")
    modImplementation("net.fabricmc:fabric-loader:0.7.9+build.190")

    apiInclude("org.spongepowered:configurate-core:$versionConfigurate") {
        exclude("com.google.guava")
    }
    apiInclude("org.spongepowered:configurate-hocon:$versionConfigurate") {
        exclude("com.google.guava")
    }
    include("com.typesafe:config:1.4.0")
    apiInclude("org.spongepowered:configurate-gson:$versionConfigurate") { isTransitive = false }
    // For test commands
    // listOf("commands-v0", "api-base").forEach {
    //     implementationInclude("net.fabricmc.fabric-api:fabric-$it:$versionFabricApi")
    // }
}



plugins {
    id("fabric-loom") version "0.2.6-SNAPSHOT"
    id("com.github.hierynomus.license") version "0.15.0"
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
}

val versionConfigurate = "3.6.1"
val versionMinecraft = "1.15.2"

group = "ca.stellardrift"
version = "$versionConfigurate-SNAPSHOT"

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
}


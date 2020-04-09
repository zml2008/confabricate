plugins {
    id("fabric-loom") version "0.2.6-SNAPSHOT"
    id("com.github.hierynomus.license") version "0.15.0"
    `java-library`

    // Publication
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.5"
    id("org.ajoberstar.grgit") version "4.0.2"
}

val versionBase = "1.0-SNAPSHOT"
val versionMinecraft = ext["version.minecraft"] as String
val versionMappings = ext["version.mappings"] as String
val versionLoader = ext["version.loader"] as String
val versionFabricApi = ext["version.fabricApi"] as String
val versionConfigurate = ext["version.configurate"] as String

group = "ca.stellardrift"
version = "$versionBase+$versionConfigurate"

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
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Jar> {
    manifest {
        attributes("Specification-Name" to "Configurate",
        "Specification-Version" to versionConfigurate,
        "Implementation-Name" to project.name,
        "Implementation-Version" to versionBase)
    }
}

val javadocJar by tasks.getting
val sourcesJar by tasks.getting

tasks.build {
    dependsOn(javadocJar)
    dependsOn(sourcesJar)
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

val publicationId = "maven"
publishing {
    publications {
        register(publicationId, MavenPublication::class) {
            artifact(tasks.jar.get()) {
                classifier = "dev"
            }
            artifact(tasks.remapJar.get())

            artifact(sourcesJar) {
                builtBy(tasks.remapSourcesJar.get())
            }
            artifact(javadocJar)
            //from(components["java"]) // doesn't work on fabric

            pom {
                description.set(project.ext["longDescription"] as String)

                developers {
                    developer {
                        name.set("zml")
                        email.set("zml at stellardrift dot ca")
                    }
                }

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                scm {
                    url.set("https://github.com/zml2008/confabricate")
                    connection.set("scm:git:https://github.com/zml2008/confabricate.git")
                    developerConnection.set("scm:git:ssh://git@github.com/zml2008/confabricate.git")
                }

                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/zml2008/confabricate/issues")
                }
            }
        }

    }
}
bintray {
    user = findProperty("bintrayUser") as String? ?: System.getenv("BINTRAY_USER")
    key = findProperty("bintrayKey") as String? ?: System.getenv("BINTRAY_KEY")
    pkg.apply {
        repo = "stellardrift-repo"
        name = project.name
        setLicenses("Apache-2.0")
        vcsUrl = "https://github.com/zml2008/confabricate.git"
        version.apply {
            val tagRef = grgit.repository.jgit.tagList().call().firstOrNull()
            val tag = grgit.resolve.toTag(tagRef?.name)
            name = project.version as String
            vcsTag = tag?.name
            desc = tag?.fullMessage
            released = tag?.commit?.dateTime?.toString()
        }
    }
    setPublications(publicationId)
}
val requireClean by tasks.registering {
    doLast {
        if (!grgit.status().isClean) {
            throw TaskExecutionException(this, Exception("Source root must be clean! Make sure your changes are committed."))
        }
    }
}

tasks.bintrayUpload {
    dependsOn(requireClean)
    onlyIf {
        !(version as String).contains("SNAPSHOT")
    }
}


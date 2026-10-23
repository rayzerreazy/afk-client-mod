plugins {
    alias(libs.plugins.fabric.loom)
}

base {
    archivesName.set("afk-client")
}

version = libs.versions.mod.version.get()
group = "com.afk"

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    minecraft(libs.minecraft)
    mappings(variantOf(libs.yarn) { classifier("v2") })
    modImplementation(libs.fabric.loader)

    // Fabric API (events used for ticking and the HUD)
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.141.6+1.21.11")
}

tasks {
    processResources {
        val version = project.version
        inputs.property("version", version)

        filesMatching("fabric.mod.json") {
            expand("version" to version)
        }
    }

    withType<JavaCompile> {
        options.release.set(21)
        options.encoding = "UTF-8"
    }
}

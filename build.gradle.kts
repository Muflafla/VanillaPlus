plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.3.1"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    implementation("org.bstats:bstats-bukkit:3.2.1")
}

// Force Gradle to override Paper API's outdated build chain components
configurations.all {
    resolutionStrategy {
        force("org.apache.commons:commons-lang3:3.18.0")
        force("org.codehaus.plexus:plexus-utils:4.0.3")
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.21.4")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
    //Turn off the default unshaded jar compilation
    jar {
        enabled = false
    }
}


tasks.shadowJar {
    archiveClassifier.set("")
    configurations = project.configurations.runtimeClasspath.map { setOf(it) }

    dependencies {
        // Only merge bStats into the final jar, no other dependencies
        exclude { it.moduleGroup != "org.bstats" }
    }

    // Relocate bStats into the plugin's package to avoid conflicts with other
    // plugins using bStats
    relocate("org.bstats", project.group.toString())
}

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.24"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "net.spacetivity.blocko"
version = "1.0-SNAPSHOT"

repositories {
    maven {
        isAllowInsecureProtocol = true
        url = uri("http://37.114.42.133:8081/repository/maven-public/")
        credentials {
            username = property("nexusUsername") as String
            password = property("nexusPassword") as String
        }
    }
    mavenCentral()
}

dependencies {
    compileOnly(libs.gson)
    compileOnly(libs.api.inventory)
    compileOnly(libs.bundles.database)
    compileOnly(libs.bundles.paper)
}

tasks.shadowJar {
    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }
}

kotlin {
    jvmToolchain(libs.versions.java.get().toInt())
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = libs.versions.java.get()
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveFileName.set("${project.name}-${project.version}.jar")
        mergeServiceFiles()

        exclude("org/**")
        exclude("kotlin/**")
        exclude("kotlinx/**")

        exclude("META-INF/**kotlin**")
        exclude("META-INF/**/*kotlin*")

        exclude("**/META-INF/**/*kotlin*")

        dependencies {
            exclude(dependency("org.jetbrains.exposed:exposed-core:${libs.versions.exposed}"))
            exclude(dependency("org.jetbrains.exposed:exposed-dao:${libs.versions.exposed}"))
            exclude(dependency("org.jetbrains.exposed:exposed-jdbc:${libs.versions.exposed}"))
        }
    }
}
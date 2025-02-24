import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.1.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "net.spacetivity.blocko"
version = "1.0-SNAPSHOT"

repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        url = uri("https://maven.pkg.github.com/Spacetivity/SpaceInventories") // Your GitHub repository URL
        credentials {
            username = project.findProperty("username")?.toString() ?: "defaultUsername"
            password = project.findProperty("token")?.toString() ?: "defaultToken"
        }
    }
    mavenCentral()
}

dependencies {
    compileOnly(libs.gson)
    compileOnly(libs.api.inventory)
    compileOnly(libs.guava)
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
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(libs.versions.java.get()))
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
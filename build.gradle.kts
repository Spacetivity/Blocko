import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.23"
}

group = "net.spacetivity.blocko"
version = "1.0-SNAPSHOT"

repositories {
    maven {
        url = uri("https://nexus.spacetivity.net/repository/maven-public/")
        credentials {
            username = property("nexusUsername") as String
            password = property("nexusPassword") as String
        }
    }
    mavenCentral()
}

val exposedVersion: String by project

dependencies {
    compileOnly("com.google.code.gson:gson:2.10.1")
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("net.spacetivity.inventory:inventory-api:1.0-SNAPSHOT")

    compileOnly(group = "org.mariadb.jdbc", name = "mariadb-java-client", version = "3.0.7")

    api("org.jetbrains.exposed:exposed-core:$exposedVersion")
    api("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    api("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    compileOnly(group = "org.slf4j", name = "slf4j-api", version = "1.7.25")
    compileOnly(group = "org.slf4j", name = "slf4j-simple", version = "1.7.25")
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
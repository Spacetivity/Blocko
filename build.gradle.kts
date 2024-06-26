import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.9.0"
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

    implementation(group = "org.mariadb.jdbc", name = "mariadb-java-client", version = "3.0.7")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    implementation(group = "org.slf4j", name = "slf4j-api", version = "1.7.25")
    implementation(group = "org.slf4j", name = "slf4j-simple", version = "1.7.25")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "17"
}

val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "17"
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({ configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) } })
}

task("sourcesJar", type = Jar::class) {
    from(sourceSets.main.get().allSource)
    archiveClassifier.set("sources")
}
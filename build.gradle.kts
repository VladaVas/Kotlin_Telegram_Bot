plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("org.example.additional.TelegramKt")
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveClassifier.set("all")
        manifest {
            attributes["Main-Class"] = "org.example.additional.TelegramKt"
        }
    }
}

tasks.register<Copy>("deployBot") {
    dependsOn("shadowJar")
    from("build/libs/KotlinTelegramBot-1.0-SNAPSHOT-all.jar")
    into("/root")
    rename { "bot.jar" }
}
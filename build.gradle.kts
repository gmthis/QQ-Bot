plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "tea.ulong"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
}

var miraiVersion = "2.16.0-0c5f30a-SNAPSHOT"

dependencies {
    testImplementation(kotlin("test"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
    implementation("com.github.oshi:oshi-core:6.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    api("net.mamoe:mirai-core-api:2.16.0")
    implementation("top.mrxiaom:overflow-core:$miraiVersion")
    implementation(kotlin("reflect"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

application{
    mainClass.set("tea.ulong.MainKt")
}

tasks{
    shadowJar{
        archiveBaseName.set("release")
        archiveVersion.set("0.0.1")
    }
}
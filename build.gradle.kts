plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    application
}

val packName = "release"
val packVersion = "0.0.1"

tasks.register("buildProject") {
    group = "build"
    description = "Copies shadow JARs from subprojects to the appropriate directory."

    dependsOn(subprojects.map { subproject ->
        subproject.tasks.getByName("jar")
    })

    doLast{
        val appDir = file("build/libs/app")
        val pluginDir = file("build/libs/app/plugin")

        appDir.mkdirs()
        pluginDir.mkdirs()

        subprojects.forEach { subproject ->
            val destinationDir = when (subproject.name) {
                "bot-api" -> null
                "bot-core" -> appDir
                else -> pluginDir
            }

            destinationDir?.let {
                subproject.layout.buildDirectory.dir("libs").get().asFile.listFiles()?.forEach { file ->
                    file.copyTo(it.resolve(file.name), true)
                }
            }
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "org.gradle.application")

    repositories {
        mavenCentral()
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
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
        jar{
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            if (this@subprojects.name in listOf("bot-api", "bot-core")) {
                from(configurations.runtimeClasspath.get().map {
                    if (it.isDirectory) it else zipTree(it)
                })
                from(sourceSets.main.get().output)
            }
            manifest{
                archiveBaseName.set(this@subprojects.name)
                archiveAppendix.set(packName)
                archiveVersion.set(packVersion)
            }
        }
    }
}
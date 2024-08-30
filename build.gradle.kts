plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0"
    application
}

val packName = "release"
val packVersion = "0.0.1"

repositories {
    mavenCentral()
}

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
            when (subproject.name) {
                "bot-api" -> {}
                "bot-core" -> {
                    subproject.layout.buildDirectory.dir("libs").get().asFile.listFiles()?.forEach { file ->
                        file.copyTo(appDir.resolve(file.name), true)
                    }
                }
                else -> {
                    if (subprojects.find { it.name == "bot-core" }!!.configurations.implementation.get().dependencies.find { it.name == subproject.name } == null &&
                        subprojects.find { it.name == "bot-core" }!!.configurations.api.get().dependencies.find { it.name == subproject.name } == null){
                        subproject.layout.buildDirectory.dir("libs").get().asFile.listFiles()?.forEach { file ->
                            file.copyTo(pluginDir.resolve(file.name), true)
                            file.copyTo(File("plugin").resolve(file.name), true)
                        }
                    }
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

    dependencies{
        if ("plugin" in (this@subprojects).name){
            compileOnly(project(":bot-api"))
        }
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
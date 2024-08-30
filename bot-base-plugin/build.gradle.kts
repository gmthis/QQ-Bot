group = "tea.ulong"
version = "1.0-SNAPSHOT"

dependencies {
    testImplementation(kotlin("test"))

    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
    compileOnly("com.github.oshi:oshi-core:6.6.2")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    compileOnly(project(":bot-core-plugin-helper"))
}
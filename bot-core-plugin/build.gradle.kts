group = "tea.ulong"
version = "1.0-SNAPSHOT"

dependencies {
    testImplementation(kotlin("test"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
    implementation("com.github.oshi:oshi-core:6.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    implementation(project(":bot-api"))
}
group = "tea.ulong"
version = "1.0-SNAPSHOT"

var miraiVersion = "2.16.0-0c5f30a-SNAPSHOT"

dependencies {
    testImplementation(kotlin("test"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    api("net.mamoe:mirai-core-api:2.16.0")
    api("top.mrxiaom:overflow-core:$miraiVersion")
    implementation(kotlin("reflect"))
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "QQ-Bot"
include("bot-api")
include("bot-core")
include("bot-base-plugin")
include("bot-core-plugin-helper")
include("bot-core-plugin-user")

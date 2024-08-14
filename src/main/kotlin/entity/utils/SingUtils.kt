package tea.ulong.entity.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import tea.ulong.entity.json.LocalDateTimeSerializer

val json = Json {
    prettyPrint = true
    serializersModule = SerializersModule {
        contextual(LocalDateTimeSerializer)
    }
    encodeDefaults = true
}

val emptyJsonObject = buildJsonObject {  }
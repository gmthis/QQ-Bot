package tea.ulong.entity.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import tea.ulong.json.LocalDateTimeSerializer

/**
 * 请在项目中使用该对象来进行json的序列化与反序列化
 */
val json = Json {
    prettyPrint = true
    serializersModule = SerializersModule {
        contextual(LocalDateTimeSerializer)
    }
    encodeDefaults = true
}

/**
 * 空json object, 用于占位
 */
val emptyJsonObject = buildJsonObject {  }

internal val innerDynamicContainers = mutableMapOf<String, Any?>().also {
    it["DynamicContainers"] = it
    it["DynamicContainers".lowercase()] = it
}
val DynamicContainers
    get() = innerDynamicContainers
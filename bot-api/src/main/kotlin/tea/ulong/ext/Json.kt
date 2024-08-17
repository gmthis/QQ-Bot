package tea.ulong.ext

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * 将string转换为json primitive
 */
fun String?.toJsonPrimitive() = JsonPrimitive(this)

/**
 * 将list转换为json array
 */
fun <E: JsonElement> List<E>.toJsonArray() = JsonArray(this)

/**
 * 将map转换为json object
 */
fun <E: JsonElement> Map<String, E>.toJsonObject() = JsonObject(this)
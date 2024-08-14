package tea.ulong.ext

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import tea.ulong.entity.event.processor.annotation.Template
import tea.ulong.loader.entity.TemplateLoader
import java.time.LocalDateTime
import kotlin.reflect.KProperty
import kotlin.reflect.full.*

fun Long.format(digits: Int) = "%.${digits}f".format(this.toDouble())
fun Double.format(digits: Int) = "%.${digits}f".format(this)

fun LocalDateTime.zero() = this
    .withHour(0)
    .withMinute(0)
    .withSecond(0)
    .withNano(0)

fun String.fillTemplate(content: Map<String, String>): String {
    var result = this
    for ((k,v) in content){
        result = result.replace("{{${k}}}", v)
    }
    return result
}

fun String?.toJsonPrimitive() = JsonPrimitive(this)

fun <E: JsonElement> List<E>.toJsonArray() = JsonArray(this)
fun <E: JsonElement> Map<String, E>.toJsonObject() = JsonObject(this)

val templateMap = mutableMapOf<String, List<Pair<String, KProperty<*>>>>()

inline fun <reified E> E.getTemplate(templateName: String): String? {
    val clazz = E::class

    var result: Any? = TemplateLoader.loader(clazz, templateName)
    if (result != null) return result as String

    val properties = templateMap[clazz.qualifiedName ?: "innerClass"] ?: run {
        val companionClass = clazz.companionObject ?: return null

        val cache = (companionClass.memberProperties + companionClass.memberExtensionProperties).filter {
            it.findAnnotation<Template>() != null
        }.map {
            Pair(it.findAnnotation<Template>()!!.name.run { this.ifEmpty { it.name } }, it)
        }
        templateMap[clazz.qualifiedName ?: "innerClass"] = cache
        cache
    }

    result = properties.filter { it.first == templateName }.map { it.second.getter.call(clazz.companionObjectInstance) }.apply {
        for (item in this){
            if (item is String){
                TemplateLoader.save(clazz, templateName, item)
            }
        }
    }.apply { TemplateLoader.reload() }.random()

    if (result is String) {
        return result
    }
    return null
}
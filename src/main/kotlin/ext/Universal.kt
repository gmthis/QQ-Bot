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

/**
 * 将long格式化为[digits]位数的字符串
 */
fun Long.format(digits: Int) = "%.${digits}f".format(this.toDouble())

/**
 * 将double格式化为[digits]位数的字符串
 */
fun Double.format(digits: Int) = "%.${digits}f".format(this)

/**
 * 获取当日的零点零分零秒零纳秒
 */
fun LocalDateTime.zero() = this
    .withHour(0)
    .withMinute(0)
    .withSecond(0)
    .withNano(0)

/**
 * 为当前字符串填充模板内容,字符串中的占位符为"{{`key`}}",例子:
 *
 * "现在是北京时间: {{date}}"
 *
 * 在[content]中传入:{date: "上午八点整"}
 *
 * 得结果: "现在是北京时间: 上午八点整"
 *
 * @param content 用于填充到模板的属性
 * @return 填充结果
 */
fun String.fillTemplate(content: Map<String, String>): String {
    var result = this
    for ((k,v) in content){
        result = result.replace("{{${k}}}", v)
    }
    return result
}

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

/**
 * getTemplate函数使用的缓存容器,如非必要,请勿使用.
 */
val templateMap = mutableMapOf<String, List<Pair<String, KProperty<*>>>>()

/**
 * 通过[templateName]获取当前类下的模板,如果该模板名对应多个模板,则会随机返回
 *
 * @param templateName 需要的模板名
 *
 * @return 模板,当模板不存在时可能为空
 */
inline fun <reified E> E.getTemplate(templateName: String): String? {
    val clazz = E::class

    // 加载模板,如果缓存中存在模板直接返回
    var result: Any? = TemplateLoader.loader(clazz, templateName)
    if (result != null) return result as String

    // 获取该类下的带有Template的静态属性
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

    // 获取所有符合条件的静态属性,保存后返回,如果有多个则随机返回一个.
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
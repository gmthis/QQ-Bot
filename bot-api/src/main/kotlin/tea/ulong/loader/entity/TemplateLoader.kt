package tea.ulong.loader.entity

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import tea.ulong.entity.event.processor.annotation.TemplatePackName
import tea.ulong.entity.utils.emptyJsonObject
import tea.ulong.entity.utils.json
import tea.ulong.ext.toJsonArray
import tea.ulong.ext.toJsonObject
import tea.ulong.ext.toJsonPrimitive
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

object TemplateLoader {

    private val file = File(".template")
    private var templateContainer: JsonObject

    init {
        if (!file.exists()){
            file.createNewFile()
            templateContainer = emptyJsonObject
        }else{
            val fileContent = file.readText()
            templateContainer = if (fileContent.isEmpty()) {
                emptyJsonObject
            }else{
                json.decodeFromString<JsonObject>(fileContent)
            }

        }
    }

    /**
     * 重新从磁盘中加载模板
     */
    fun reload() {
        templateContainer = json.decodeFromString(file.readText())
    }

    /**
     * 获取一个类下的模板内容,如果[templateName]对应多个内容,则会随即返回
     *
     * 内部实际使用了缓存,并不会立即反应模板文件的内容变化
     * @param clazz 目标类
     * @param templateName 模板名
     * @return 模板内容
     */
    fun loader(clazz: KClass<*>, templateName:String): String? {
        val pack = templateContainer[clazz.simpleName ?: ""]
        if (pack == null) return null
        val result = pack.jsonObject[templateName] ?: return null
        if (result is JsonArray) return result.random().jsonPrimitive.content
        return result.jsonPrimitive.content
    }

    /**
     * 将模板保存到模板文件中,保存时如果[templateName]对应的内容已经存在,则会将原模板转换为数组并追加新模板
     *
     * 另见[TemplatePackName]
     * @param clazz 该函数会从[clazz]中反射获取默认模板内容
     * @param templateName 模板名,会作为[clazz]中的一个属性添加到json中
     * @param content 模板内容
     */
    fun save(clazz:KClass<*>, templateName: String, content: String){
        val packName = clazz.findAnnotation<TemplatePackName>()?.name ?: clazz.simpleName ?: "innerClass"
        val pack = templateContainer[packName]?.jsonObject
        val newPack = if (pack != null){
            val target = pack[templateName]
            val newContent: JsonElement = if (target != null) {
                // 如果模板已经存在,那么判断模板是不是数组,是则追加不是则转换为数组后追加
                val cache = if (target is JsonArray){
                    val targetList = target.toMutableList()
                    // 去重
                    if (targetList.any{ it.jsonPrimitive.content == content }){
                        target
                    }else{
                        targetList.apply {
                            add(content.toJsonPrimitive())
                        }.toJsonArray()
                    }
                } else {
                    buildJsonArray {
                        add(target)
                        add(content.toJsonPrimitive())
                    }
                }
                cache
            }else{
                // 模板不存在,直接添加
                content.toJsonPrimitive()
            }
            pack.toMutableMap().apply {
                put(templateName, newContent)
            }.toJsonObject()
        }else{
            // clazz还没有被保存过,直接新建json保存
            buildJsonObject {
                put(templateName, content)
            }
        }

        templateContainer = templateContainer.toMutableMap().apply {
            put(packName, newPack)
        }.toJsonObject()

        file.writeText(json.encodeToString(templateContainer))
    }

}
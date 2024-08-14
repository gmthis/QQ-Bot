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

    fun reload() {
        templateContainer = json.decodeFromString(file.readText())
    }

    fun loader(clazz: KClass<*>, templateName:String): String? {
        val pack = templateContainer[clazz.simpleName ?: ""]
        if (pack == null) return null
        val result = pack.jsonObject[templateName] ?: return null
        if (result is JsonArray) return result.random().jsonPrimitive.content
        return result.jsonPrimitive.content
    }

    fun save(clazz:KClass<*>, templateName: String, content: String){
        val packName = clazz.findAnnotation<TemplatePackName>()?.name ?: clazz.simpleName ?: "innerClass"
        val pack = templateContainer[packName]?.jsonObject
        val newPack = if (pack != null){
            val target = pack[templateName]
            val newContent: JsonElement = if (target != null) {
                val cache = if (target is JsonArray){
                    val targetList = target.toMutableList()
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
                content.toJsonPrimitive()
            }
            pack.toMutableMap().apply {
                put(templateName, newContent)
            }.toJsonObject()
        }else{
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
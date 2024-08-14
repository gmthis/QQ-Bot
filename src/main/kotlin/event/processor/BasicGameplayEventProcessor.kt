package tea.ulong.event.processor

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive
import net.mamoe.mirai.event.events.MessageEvent
import tea.ulong.entity.event.processor.annotation.*
import tea.ulong.entity.utils.json
import tea.ulong.ext.fillTemplate
import tea.ulong.ext.getTemplate
import tea.ulong.ext.toJsonPrimitive
import tea.ulong.ext.zero
import tea.ulong.loader.entity.UserLoader
import java.time.LocalDateTime

@TemplatePackName("基本玩法")
class BasicGameplayEventProcessor {

    companion object{
        private val RandomContainers = listOf(
            "大吉", "中吉", "小吉",
            "平",
            "小凶", "中凶", "大凶"
        )
        private const val TEMPLATE_KEY = "今日人品"

        @Template(TEMPLATE_KEY)
        const val RETURN_TEMPLATE = "今日人品测试例1:{{rp}}"

        @Template(TEMPLATE_KEY)
        const val RETURN_TEMPLATE2 = "今日人品测试例2:{{rp}}"
    }

    @Trigger("jrrp", "今日人品")
    @Authentication(Authority.User)
    fun luck(
        messageEvent: MessageEvent
    ): String {
        val qq = messageEvent.sender.id
        val user = UserLoader.loader(qq)

        val jrrpPrev = user.extendedProperties["jrrp_next"]
        val date: LocalDateTime?
        if (jrrpPrev == null){
            date = LocalDateTime.now().zero()
                .plusDays(-1)
        } else {
            if (jrrpPrev is JsonPrimitive){
                if (jrrpPrev.isString){
                    date = LocalDateTime.parse(jrrpPrev.content)
                }else{
                    date = LocalDateTime.now().zero().plusDays(-1)
                    user.extendedProperties["jrrp_next"] = json.encodeToJsonElement(date)
                }
            }else{
                date = LocalDateTime.now().zero().plusDays(-1)
                user.extendedProperties["jrrp_next"] = json.encodeToJsonElement(date)
            }
        }

        val current = LocalDateTime.now()
        val result = if (current <= date){
            val jrrpContent = user.extendedProperties["jrrp_content"]
            val value = jrrpContent?.jsonPrimitive?.content ?: RandomContainers.random()
            val template = getTemplate(TEMPLATE_KEY) ?: ""
            template.fillTemplate(mapOf("rp" to value))
        } else {
            val value = RandomContainers.random()
            user.extendedProperties["jrrp_content"] = value.toJsonPrimitive()
            user.extendedProperties["jrrp_next"] = json.encodeToJsonElement(current.plusDays(1).zero())
            user.save()
            val template = getTemplate(TEMPLATE_KEY) ?: ""
            template.fillTemplate(mapOf("rp" to value))
        }

        return result
    }
}
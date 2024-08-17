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
import java.time.format.DateTimeFormatter
import kotlin.random.Random

@TemplatePackName("抽签游戏")
class LotteryGame {

    companion object{
        private val RandomContainers = listOf(
            "大吉", "吉", "吉", "小吉", "小吉", "小吉",
            "平", "平", "平",
            "略凶","略凶","略凶", "凶", "凶", "大凶"
        )

        @Template("大吉")
        const val DAJI_TEMPLATE1 = "时间: {{date}}\n结果: {{rp}}\n福星高照, 吉祥如意, 喜事连连!"
        @Template("大吉")
        const val DAJI_TEMPLATE2 = "时间: {{date}}\n结果: {{rp}}\n鸿运当头, 财源广进, 蒸蒸日上!"
        @Template("大吉")
        const val DAJI_TEMPLATE3 = "时间: {{date}}\n结果: {{rp}}\n家宅平安, 福寿绵长, 万事顺心!"

        @Template("吉")
        const val JI_TEMPLATE1 = "时间: {{date}}\n结果: {{rp}}\n风调雨顺, 百事顺遂, 心想事成!"
        @Template("吉")
        const val JI_TEMPLATE2 = "时间: {{date}}\n结果: {{rp}}\n福运绵延, 好运常伴, 平安喜乐!"
        @Template("吉")
        const val JI_TEMPLATE3 = "时间: {{date}}\n结果: {{rp}}\n事事如意, 家庭和睦, 幸福安康!"

        @Template("小吉")
        const val XIAOJI_TEMPLATE1 = "时间: {{date}}\n结果: {{rp}}\n平安顺遂, 福气常在, 细水长流!"
        @Template("小吉")
        const val XIAOJI_TEMPLATE2 = "时间: {{date}}\n结果: {{rp}}\n日渐进步, 喜事将至, 心安事成!"
        @Template("小吉")
        const val XIAOJI_TEMPLATE3 = "时间: {{date}}\n结果: {{rp}}\n小有收获, 吉祥相伴, 一切顺心!"

        @Template("平")
        const val PING_TEMPLATE1 = "时间: {{date}}\n结果: {{rp}}\n平平常常, 心境宁静, 一切安稳."
        @Template("平")
        const val PING_TEMPLATE2 = "时间: {{date}}\n结果: {{rp}}\n生活平淡, 无忧无虑, 日复一日."
        @Template("平")
        const val PING_TEMPLATE3 = "时间: {{date}}\n结果: {{rp}}\n平安无虞, 稳步向前, 不慌不忙."

        @Template("略凶")
        const val LVEXIONG_TEMPLATE1 = "时间: {{date}}\n结果: {{rp}}\n小心陷阱, 注意风险, 谨慎行事."
        @Template("略凶")
        const val LVEXIONG_TEMPLATE2 = "时间: {{date}}\n结果: {{rp}}\n遇到阻碍, 风险增加, 保持警觉."
        @Template("略凶")
        const val LVEXIONG_TEMPLATE3 = "时间: {{date}}\n结果: {{rp}}\n运势低迷, 障碍重重, 防范为主."

        @Template("凶")
        const val XIONG_TEMPLATE1 = "时间: {{date}}\n结果: {{rp}}\n困难重重, 倍加小心, 遇事谨慎."
        @Template("凶")
        const val XIONG_TEMPLATE2 = "时间: {{date}}\n结果: {{rp}}\n风险极大, 状况不佳, 防范为主."
        @Template("凶")
        const val XIONG_TEMPLATE3 = "时间: {{date}}\n结果: {{rp}}\n厄运当前, 问题频发, 保持警惕."

        @Template("大凶")
        const val DAXIONG_TEMPLATE1 = "时间: {{date}}\n结果: {{rp}}\n大难临头, 祸端难解, 不可忽视."
        @Template("大凶")
        const val DAXIONG_TEMPLATE2 = "时间: {{date}}\n结果: {{rp}}\n险象环生, 形势严峻, 切忌冒进."
        @Template("大凶")
        const val DAXIONG_TEMPLATE3 = "时间: {{date}}\n结果: {{rp}}\n祸从天降, 危机四伏, 生死未卜."

        @Template("欧皇")
        const val OUHUANG_TEMPLATE = "时间: {{date}}\n结果: {{rp}}\n0.0001‰(千万分之一)的概率也不过如此,下次还抽."
        @Template("大胸")
        const val JURU_TEMPLATE = "时间: {{date}}\n结果: {{rp}}\n巨乳拯救世界!"
        @Template("平胸")
        const val PINGXION_TEMPLATE = "时间: {{date}}\n结果: {{rp}}\n飞机来了,AA在哪?"
        @Template("小吉吉")
        const val XIAOJIJI_TEMPLATE = "时间: {{date}}\n结果: {{rp}}\n不要气馁."
        @Template("大吉吉")
        const val DAJIJI_TEMPLATE = "时间: {{date}}\n结果: {{rp}}\n不要骄傲."
        @Template("鸡")
        const val JINITAIMEI_TEMPLATE = "时间: {{date}}\n结果: {{rp}}\n只因你实在是太美."
        @Template("稀有")
        const val HANJIAN_TEMPLATE = "时间: {{date}}\n结果: {{rp}}\n我是罕见."
        @Template("不稀有")
        const val MASHEIHANJIAN_TEMPLATE = "时间: {{date}}\n结果: {{rp}}\n谁罕见?啊?骂谁汉奸呢?....呵..呼...呵,骂谁罕见!!!!!!"
        @Template("man")
        const val MAN_TEMPLATE = "时间: {{date}}\n结果: {{rp}}\nwhat can i say"
        @Template("好似")
        const val HAOSI_TEMPLATE = "时间: {{date}}\n结果: {{rp}}\n灌注塔菲喵~灌注塔菲喵~"
        @Template("雪豹")
        const val XUEBAO_TEMPLATE = "时间: {{date}}\n结果: {{rp}}\n闭嘴!!!!!!!!!!!!"
        @Template("雪豹")
        const val XUEBAO_TEMPLATE2 = "时间: {{date}}\n结果: {{rp}}\n知识学爆!!!"
        @Template("理塘")
        const val LITANG_TEMPLATE2 = "时间: {{date}}\n结果: {{rp}}\n到达世界最高城!"
    }

    @Trigger("jrrp", "今日人品")
    @Authentication(Authority.User)
    fun luck(
        messageEvent: MessageEvent
    ): String {
        val qq = messageEvent.sender.id
        val user = UserLoader.loader(qq)

        val jrrpNext = user.extendedProperties["下次允许时间戳"]
        val date: LocalDateTime?
        if (jrrpNext == null){
            date = LocalDateTime.now().zero()
                .plusDays(-1)
        } else {
            if (jrrpNext is JsonPrimitive){
                if (jrrpNext.isString){
                    date = LocalDateTime.parse(jrrpNext.content)
                }else{
                    date = LocalDateTime.now().zero().plusDays(-1)
                    user.extendedProperties["下次允许时间戳"] = json.encodeToJsonElement(date)
                }
            }else{
                date = LocalDateTime.now().zero().plusDays(-1)
                user.extendedProperties["下次允许时间戳"] = json.encodeToJsonElement(date)
            }
        }

        val current = LocalDateTime.now()
        val result = if (current <= date){
            val jrrpContent = user.extendedProperties["抽签内容"]
            val value = jrrpContent?.jsonPrimitive?.content
            if (value == null){
                val cache = getRandom()
                val template = getTemplate(cache) ?: ""
                template.fillTemplate(mapOf("rp" to cache, "date" to current.format(DateTimeFormatter.ISO_DATE)))
            }else value

        } else {
            val value = getRandom()
            val template = getTemplate(value) ?: ""
            val result = template.fillTemplate(mapOf("rp" to value, "date" to current.format(DateTimeFormatter.ISO_DATE)))
            user.extendedProperties["抽签内容"] = result.toJsonPrimitive()
            user.extendedProperties["下次允许时间戳"] = json.encodeToJsonElement(current.plusDays(1).zero())
            user.save()
            result
        }

        return result
    }

    private fun getRandom() =
        when (Random.nextInt(10000000)){
            // 0.00001%
            in 0..0 -> "欧皇"
            // 0.05%
            in 1..5000 -> "大胸"
            // 0.05%
            in 5001..10000 -> "平胸"
            // 0.05%
            in 10001..15000 -> "小吉吉"
            // 0.05%
            in 15001..20000 -> "大吉吉"
            //0.05%
            in 20001 .. 25000 -> "鸡"
            //0.05%
            in 25001 .. 30000 -> "稀有"
            //0.05%
            in 3001 .. 35000 -> "不稀有"
            //0.05%
            in 35001 .. 40000 -> "man"
            //0.05%
            in 40001 .. 45000 -> "好似"
            //0.05%
            in 45001 .. 50000 -> "雪豹"
            //0.05%
            in 50001 .. 55000 -> "理塘"
            else -> RandomContainers.random()
        }

}
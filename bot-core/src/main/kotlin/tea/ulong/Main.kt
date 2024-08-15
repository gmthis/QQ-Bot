package tea.ulong

import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.content
import tea.ulong.event.processor.Prefix
import tea.ulong.event.processor.ProcessorFun
import tea.ulong.entity.utils.DynamicContainers
import tea.ulong.loader.entity.ConfigLoader
import tea.ulong.loader.event.processor.InternalProcessorLoader
import top.mrxiaom.overflow.BotBuilder

lateinit var bot: Bot

fun main() = runBlocking {
    bot = BotBuilder.positive(ConfigLoader.config.onebotWebSocketAddress)
        .token(ConfigLoader.config.onebotToken)
        .connect() ?: run {
            println("bot is null")
            return@runBlocking
        }

    DynamicContainers["bot"] = bot

    val processor = InternalProcessorLoader.getInternalProcessorList()

    val processorFunMap = mutableMapOf<Prefix, MutableMap<String, MutableList<ProcessorFun>>>()
    for (item in processor){
        for ((k,v) in item.triggerMap){
            val map = processorFunMap[k]
            if (map == null) {
                processorFunMap[k] = v
            }else{
                map.putAll(v)
            }
        }
    }

    bot.eventChannel.subscribeAlways<MessageEvent> { event ->
        val key = if (event.message.content.contains("@${bot.id}")) {
            val cache = event.message.content.replace("@${bot.id}", "").trim()
            "@${bot.id} ${cache.split(" ", "　")[0]}"
        }else {
            event.message.content.split(" ", "　")[0]
        }

        val prefixKey = processorFunMap.keys.find { it.check(key) } ?: return@subscribeAlways
        val funcMap = processorFunMap[prefixKey] ?: return@subscribeAlways

        val trigger = prefixKey.getTrigger(key) ?: return@subscribeAlways
        val func = funcMap[trigger] ?: return@subscribeAlways

        func[0].run(key, event)
    }

    Unit
}
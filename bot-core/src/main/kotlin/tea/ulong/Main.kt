package tea.ulong

import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.*
import tea.ulong.entity.MessageEntity
import tea.ulong.entity.utils.DynamicContainers
import tea.ulong.loader.entity.ConfigLoader
import tea.ulong.loader.event.processor.ProcessorLoader
import top.mrxiaom.overflow.BotBuilder

lateinit var bot: Bot

fun main() = runBlocking {
    bot = BotBuilder.positive(ConfigLoader.config.onebotWebSocketAddress)
        .token(ConfigLoader.config.onebotToken)
        .connect() ?: run {
            println("bot is null")
            return@runBlocking
        }

    DynamicContainers["bot".lowercase()] = bot

    val (processorFunMap, emptyPrefixProcessorFunMap) = ProcessorLoader.loadChainMap("tea.ulong.core.plugin", "plugin")

    bot.eventChannel.subscribeAlways<MessageEvent> { event ->
        val messageEntity = MessageEntity()
        for (singleMessage in event.message) {
            when (singleMessage) {
                is PlainText -> {
                    messageEntity.messageChain.add(singleMessage)
                    messageEntity.plainTextAndImageMessageChain.add(singleMessage)
                }
                is Image -> {
                    messageEntity.messageChain.add(singleMessage)
                    messageEntity.plainTextAndImageMessageChain.add(singleMessage)
                }
                is At -> {
                    messageEntity.isAt = true
                    messageEntity.atMessage = singleMessage
                }
                is AtAll -> {
                    messageEntity.isAtAll = true
                    messageEntity.atAllMessage = singleMessage
                }
                is MessageSource -> {
                    messageEntity.isHaveMessageSource = true
                    messageEntity.messageSource = singleMessage
                }
                is QuoteReply -> {
                    messageEntity.isQuteReply = true
                    messageEntity.quteReply = singleMessage
                }
                else -> {
                    messageEntity.messageChain.add(singleMessage)
                }
            }
        }
        val pKey = processorFunMap.keys.find { it.check(messageEntity) }

        val splitCache = messageEntity.plainTextAndImageMessageContent.split(" ", "　")
        messageEntity.triggerList.add(pKey?.getTrigger(splitCache[0]) ?: splitCache[0])
        messageEntity.paramList.addAll(splitCache.subList(1, splitCache.size))
        val funcTarget = processorFunMap[pKey]?.get(messageEntity.triggerList[0]) ?: emptyPrefixProcessorFunMap[messageEntity.triggerList[0]] ?: return@subscribeAlways

        if (funcTarget[0].next.isEmpty()){
            funcTarget[0].run(messageEntity, event)
        }else{
            var runFun = funcTarget[0]
            val funcChain = mutableListOf(runFun)
            val triggerOrContent = mutableListOf<String>().also { it.addAll(messageEntity.paramList) }
            for (item in triggerOrContent){
                val next = runFun.next[item]
                if (next != null){
                    messageEntity.triggerList.add(item)
                    messageEntity.paramList.removeFirst()
                    runFun = next
                    funcChain.add(next)
                    if (runFun.next.isEmpty()){
                        break
                    }
                }else{
                    break
                }
            }
            runFun.run(messageEntity, event, funcChain)
        }
    }

    Unit
}
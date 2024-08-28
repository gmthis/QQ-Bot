package tea.ulong

import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.*
import tea.ulong.entity.MessageEntity
import tea.ulong.entity.event.processor.Prefix
import tea.ulong.entity.event.processor.ProcessorFun
import tea.ulong.entity.utils.DynamicContainers
import tea.ulong.loader.entity.ConfigLoader
import tea.ulong.loader.event.processor.ProcessorLoader
import top.mrxiaom.overflow.BotBuilder
import java.util.*

lateinit var bot: Bot

fun main() = runBlocking {
    bot = BotBuilder.positive(ConfigLoader.config.onebotWebSocketAddress)
        .token(ConfigLoader.config.onebotToken)
        .connect() ?: run {
            println("bot is null")
            return@runBlocking
        }

    DynamicContainers["bot"] = bot

    val processors = ProcessorLoader.loadInternalProcessor("tea.ulong.event.processor") + ProcessorLoader.loadExternalProcessor("plugin")

    val processorFunMap = mutableMapOf<Prefix, MutableMap<String, MutableList<ProcessorFun>>>()
    val emptyPrefixProcessorFunMap = mutableMapOf<String, MutableList<ProcessorFun>>()
    val haveFrontProcessorFun = LinkedList<ProcessorFun>()
    for (processor in processors){
        for (func in processor.triggerFun){
            // 如果该函数没有前驱函数
            if (func.trigger.front.isEmpty()){
                // 将函数链的头存入映射表
                for (prefix in func.prefixs){
                    if (prefix.symbol == ""){
                        for (trigger in func.trigger.triggers){
                            emptyPrefixProcessorFunMap.getOrPut(trigger){ mutableListOf() }.add(func)
                        }
                        continue
                    }
                    val map = processorFunMap.getOrPut(prefix) { mutableMapOf() }
                    for (trigger in func.trigger.triggers){
                        map.getOrPut(trigger){ mutableListOf() }.add(func)
                    }
                }
                continue
            }
            haveFrontProcessorFun.offer(func)
        }
    }

    var func: ProcessorFun?
    while (haveFrontProcessorFun.poll().apply { func = this } != null){
        val assertFunc = func!!
        if (assertFunc.trigger.front.trim() == "*"){
            processorFunMap.values.forEach {
                for (kv in it){
                    for (v in kv.value){
                        for (key in assertFunc.trigger.triggers){
                            v.next[key] = assertFunc
                        }
                        for (trigger in v.trigger.triggers){
                            assertFunc.prev[trigger] = v
                        }
                    }
                }
            }
            continue
        }

        val queue = LinkedList<Pair<String, ProcessorFun>>()
        for (processorFun in processorFunMap.values){
            for (kv in processorFun){
                for (v in kv.value){
                    queue.offer(v.funFullName to v)
                }
            }
        }
        if (queue.isEmpty()) break
        var target = queue.pop()
        var isYes = false
        do {
            val flag = assertFunc.trigger.front == target.first
            if (flag){
                for (key in assertFunc.trigger.triggers){
                    target.second.next[key] = assertFunc
                }
                for (trigger in target.second.trigger.triggers){
                    assertFunc.prev[trigger] = target.second
                }
                isYes = true
            }
            for (next in target.second.next) {
                queue.offer(next.value.funFullName to next.value)
            }
        }while(!queue.isEmpty().apply { if (!this) target = queue.pop() })
        run {
            if (!isYes){
                val subTarget = haveFrontProcessorFun.find { it.funFullName == assertFunc.trigger.front } ?: return@run
                for (key in assertFunc.trigger.triggers){
                    subTarget.next[key] = assertFunc
                }
                for (trigger in subTarget.trigger.triggers){
                    assertFunc.prev[trigger] = subTarget
                }
            }
        }
    }

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
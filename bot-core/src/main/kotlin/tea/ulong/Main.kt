package tea.ulong

import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.content
import tea.ulong.entity.event.UniversalLevel
import tea.ulong.entity.event.processor.Prefix
import tea.ulong.entity.event.processor.ProcessorFun
import tea.ulong.entity.utils.DynamicContainers
import tea.ulong.loader.entity.ConfigLoader
import tea.ulong.loader.event.processor.InternalProcessorLoader
import top.mrxiaom.overflow.BotBuilder
import java.util.LinkedList
import java.util.Queue
import java.util.Stack
import java.util.concurrent.LinkedBlockingQueue

lateinit var bot: Bot

fun main() = runBlocking {
    bot = BotBuilder.positive(ConfigLoader.config.onebotWebSocketAddress)
        .token(ConfigLoader.config.onebotToken)
        .connect() ?: run {
            println("bot is null")
            return@runBlocking
        }

    DynamicContainers["bot"] = bot

    val processors = InternalProcessorLoader.getInternalProcessorList()

    val processorFunMap = mutableMapOf<Prefix, MutableMap<String, MutableList<ProcessorFun>>>()
    val haveFrontProcessorFun = LinkedList<ProcessorFun>()
    for (processor in processors){
        for (func in processor.triggerFun){
            // 如果该函数没有前驱函数
            if (func.trigger.front.isEmpty()){
                // 将函数链的头存入映射表
                for (prefix in func.prefixs){
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
        val key = if (event.message.content.contains("@${bot.id}")) {
            val cache = event.message.content.replace("@${bot.id}", "").trim()
            "@${bot.id} ${cache.split(" ", "　")[0]}"
        }else {
            event.message.content.split(" ", "　")[0]
        }

        val prefixKey = processorFunMap.keys.find { it.check(key) } ?: return@subscribeAlways
        val funcMap = processorFunMap[prefixKey] ?: return@subscribeAlways

        val trigger = prefixKey.getTrigger(key) ?: return@subscribeAlways
        val funcTarget = funcMap[trigger] ?: return@subscribeAlways

        if (funcTarget[0].next.isEmpty()){
            funcTarget[0].run(key, event)
        }else{
            val triggerOrContent = event.message.content.removePrefix(key).trim().split(" ", "　")
            var runFun = funcTarget[0]
            var triggerKey = key
            val funcChain = mutableListOf(runFun)
            for (item in triggerOrContent){
                val next = runFun.next[item]
                if (next != null){
                    triggerKey += "$triggerKey $item"
                    runFun = next
                    funcChain.add(next)
                    if (runFun.next.isEmpty()){
                        break
                    }
                }else{
                    break
                }
            }
            runFun.run(triggerKey, event, funcChain)
        }
    }

    Unit
}
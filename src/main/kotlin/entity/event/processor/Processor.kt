package tea.ulong.entity.event.processor

import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import tea.ulong.DynamicContainers
import tea.ulong.entity.event.processor.annotation.ApplyMethod
import tea.ulong.entity.event.processor.annotation.ApplyTo
import tea.ulong.entity.event.processor.annotation.Trigger
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.full.*

class Processor(val clazz: KClass<*>) {

    companion object {
        var UniversalPrefix = listOf(
            Prefix.fetch("."),
            Prefix.fetch("。"),
            Prefix.fetch("#@bot.id@#", prefix = "@")
        )

        var UniversalApplyMethod = listOf(
            ApplyMethod.PrivateChat, ApplyMethod.GroupChat
        )
    }

    val triggerFun: List<ProcessorFun>
    val triggerMap: MutableMap<Prefix, MutableMap<String, MutableList<ProcessorFun>>> = mutableMapOf()

    private val single: Any? by lazy { structure() }
    val executor: Any
        get() {
            return structure()
        }

    init {
        val funcList = clazz.functions.filter {
            it.findAnnotation<Trigger>() != null
        }
        triggerFun = funcList.map { ProcessorFun(it, this) }

        for (func in triggerFun) {
            for (item in func.trigger.triggers){
                for (prefix in func.prefixs){

                    var list: MutableMap<String, MutableList<ProcessorFun>>? = triggerMap[prefix]
                    if (list == null) {
                        list = mutableMapOf()
                        triggerMap[prefix] = list
                    }

                    var funList: MutableList<ProcessorFun>? = list[item]
                    if (funList == null) {
                        funList = mutableListOf()
                        list[item] = funList
                    }
                    funList.add(func)
                }
            }
        }
    }

    fun run(func: ProcessorFun, event: MessageEvent): Any? {
        var runFlag = false
        when(event){
            is GroupMessageEvent -> {
                if (func.applyTo.contains(ApplyMethod.GroupChat)){
                    runFlag = true
                }
            }
            is FriendMessageEvent -> {
                if (func.applyTo.contains(ApplyMethod.PrivateChat)){
                    runFlag = true
                }
            }
            else -> runFlag = false
        }
        var result: Any? = null
        if (runFlag){
            val params = func.function.parameters
            val paramMap = mutableMapOf<KParameter, Any>()
            for (param in params){
                if (param.kind == KParameter.Kind.INSTANCE){
                    paramMap[param] = executor
                    continue
                }
                if (event::class.createType().isSubtypeOf(param.type)){
                    paramMap[param] = event
                }
            }
            result = func.function.callBy(paramMap)
        }

        return result
    }

    fun structure(): Any {
        val parameterlessConstructor = clazz.constructors.find { it.parameters.isEmpty() }
        if (parameterlessConstructor != null){
            return parameterlessConstructor.call()
        }
        return Any()
    }

}

class ProcessorFun(val function: KFunction<*>, val processor: Processor) {
    val trigger: Trigger = function.findAnnotation<Trigger>()!!
    var prefixs: List<Prefix>
    var applyTo: List<ApplyMethod> =
        function.findAnnotation<ApplyTo>()?.method?.toList() ?:
        Processor.UniversalApplyMethod

    init {
        val annotation = function.findAnnotations<tea.ulong.entity.event.processor.annotation.Prefix>()
        if (annotation.isEmpty()){
            prefixs = Processor.UniversalPrefix
        }else {
            val list = mutableListOf<Prefix>()
            for (item in annotation){
                list.add(Prefix.fetch(item.symbol, item.prefix, item.postfix))
            }
            prefixs = list
        }
    }

    suspend fun run(key:String, event: Event) {
        var result: Any? = null
        if (event is MessageEvent){
            if (prefixs.any{ it.check(key) }) {
                 result = processor.run(this, event)
            }
            if (result is String){
                event.subject.sendMessage(event.message.quote() + result)
            }
        }
    }
}

class Prefix (val symbol: String, val prefix: String = "", val postfix:String = "") {

    companion object{
        private val prefixMap = mutableMapOf<String, Prefix>()

        fun fetch(
            symbol: String,
            prefix: String = "",
            postfix: String = ""
        ): Prefix {
            val key = prefix + symbol + postfix
            var cache = prefixMap[key]
            if (cache != null) return cache
            cache = Prefix(symbol, prefix, postfix)
            prefixMap[key] = cache
            return cache
        }
    }

    val isDynamicProperty = symbol.startsWith("#@") && symbol.endsWith("@#")
    var isVal = true
    var dynamicResultCache: String? = null

    fun check(statement: String): Boolean {
        return if (isDynamicProperty)
            statement.startsWith("$prefix${dynamicallyAcquired() ?: return false}$postfix")
        else statement.startsWith("$prefix$symbol$postfix")
    }

    fun getTrigger(statement: String): String? {
        return (if (isDynamicProperty)
            statement.replace("$prefix${dynamicallyAcquired() ?: return null}$postfix", "")
        else statement.replace("$prefix$symbol$postfix", "")).trim()
    }

    /**
     * 从动态容器中获取前缀
     */
    fun dynamicallyAcquired(): String? {
        if (isVal && dynamicResultCache != null) return dynamicResultCache
        val symbolContent = symbol.substring(2, symbol.length - 2)
        var symbolSplit = symbolContent.split(".")

        val dynamicName = symbolSplit[0]
        val dynamicTarget = DynamicContainers[dynamicName] ?: return null

        var target: Any = dynamicTarget
        symbolSplit = symbolSplit.subList(1, symbolSplit.size)
        if (symbolSplit.isEmpty()) return target.toString()


        for (item in symbolSplit){
            val result = target::class.memberProperties.find { it.name == item } ?:
                target::class.memberExtensionProperties.find { it.name == item }
            if (result == null) break

            if (result is KMutableProperty<*>) {
                isVal = false
            }
            target = result.getter.call(target) ?: break
        }

        if (isVal) dynamicResultCache = target.toString()
        return target.toString()
    }
}
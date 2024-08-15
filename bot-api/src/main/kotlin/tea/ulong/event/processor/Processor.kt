package tea.ulong.event.processor

import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import tea.ulong.entity.event.processor.annotation.*
import tea.ulong.entity.utils.DynamicContainers
import tea.ulong.ext.User
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KParameter
import kotlin.reflect.full.*

/**
 * Processor的代理类
 */
class Processor(val clazz: KClass<*>) {

    companion object {
        /**
         * 默认前缀,未来会通过config配置,但不会移动api
         */
        var DefaultPrefix = listOf(
            Prefix.fetch("."),
            Prefix.fetch("。"),
            Prefix.fetch("#@bot.id@#", prefix = "@")
        )

        /**
         * 默认事件类型
         */
        var DefaultRespondEvent = listOf(
            RespondEventModel.FriendMessageEvent, RespondEventModel.GroupMessageEvent
        )
    }

    /**
     * 该类中的所有可触发函数的代理
     */
    val triggerFun: List<ProcessorFun>

    /**
     * 前缀.触发词与可触发函数的映射关系,未来可能调整为entity而不是使用map一把梭
     */
    val triggerMap: MutableMap<Prefix, MutableMap<String, MutableList<ProcessorFun>>> = mutableMapOf()

    /**
     * 单例,如果某个函数使用单例模式,那么会使用该属性,目前未实现
     */
    private val single: Any? by lazy { clazz.constructors.find { it.parameters.isEmpty() }?.call() }

    /**
     * 执行函数的执行实例,根据不同的生命周期返回不同的对象,该api未来可能发生变动可能会被删除或者修改为函数
     */
    val executor: Any
        get() {
            val parameterlessConstructor = clazz.constructors.find { it.parameters.isEmpty() }
            if (parameterlessConstructor != null){
                return parameterlessConstructor.call()
            }
            return Any()
        }

    init {
        // 获取所有标记为可触发的函数
        val funcList = clazz.functions.filter {
            it.findAnnotation<Trigger>() != null
        }
        // 代理这些函数
        triggerFun = funcList.map { ProcessorFun(it, this) }

        // 构建映射表
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

    /**
     * 执行某个可触发函数
     *
     * @param func 目标函数
     * @param event 事件
     * @return 执行结果
     */
    fun run(func: ProcessorFun, event: Event): Any? {
        var runFlag = false
        // 判断事件是否符合条件
        when(event){
            is GroupMessageEvent -> {
                if (func.respondEvent.contains(RespondEventModel.GroupMessageEvent)){
                    runFlag = true
                }
            }
            is FriendMessageEvent -> {
                if (func.respondEvent.contains(RespondEventModel.FriendMessageEvent)){
                    runFlag = true
                }
            }
            else -> runFlag = false
        }
        var result: Any? = null
        // 仅符合条件时才会运行
        if (runFlag){
            val params = func.function.parameters
            val paramMap = mutableMapOf<KParameter, Any>()
            // 映射参数,目前支持获取event,未来会支持更多
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

}

/**
 * 可触发函数代理类
 */
class ProcessorFun(val function: KFunction<*>, val processor: Processor) {
    val trigger: Trigger = function.findAnnotation<Trigger>()!!
    var prefixs: List<Prefix>
    var respondEvent: List<RespondEventModel> =
        function.findAnnotation<RespondEvent>()?.model?.toList() ?: Processor.DefaultRespondEvent
    var authentication = function.findAnnotation<Authentication>() ?: Authentication()

    init {
        val annotation = function.findAnnotations<tea.ulong.entity.event.processor.annotation.Prefix>()
        if (annotation.isEmpty()){
            prefixs = Processor.DefaultPrefix
        }else {
            val list = mutableListOf<Prefix>()
            for (item in annotation){
                list.add(Prefix.fetch(item.symbol, item.prefix, item.postfix))
            }
            prefixs = list
        }
    }

    /**
     * 执行该函数
     * @param key 触发词,该参数未来可能被删除
     * @param event 事件
     */
    suspend fun run(key:String, event: Event) {
        if (event is MessageEvent){
            val user = event.sender.User
            //权限检查
            if (authentication.check(user.authority)){
                val result = processor.run(this, event)
                if (result is String){
                    event.subject.sendMessage(event.message.quote() + result)
                }
            }

        }
    }
}

/**
 * 前缀类,参数与[Prefix][tea.ulong.entity.event.processor.annotation.Prefix]意义相同
 */
class Prefix private constructor(val symbol: String, val prefix: String = "", val postfix:String = "") {

    companion object{
        /**
         * 前缀缓存
         */
        private val prefixMap = mutableMapOf<String, Prefix>()

        /**
         * 获取一个前缀,内部使用缓存
         */
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

    /**
     * 检查一个trigger的前缀是否符合条件
     */
    fun check(statement: String): Boolean {
        return if (isDynamicProperty)
            statement.startsWith("$prefix${dynamicallyAcquired() ?: return false}$postfix")
        else statement.startsWith("$prefix$symbol$postfix")
    }

    /**
     * 获取一个trigger的内容,去除其前缀
     */
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

        // 循环向下查找
        for (item in symbolSplit){
            val result = target::class.memberProperties.find { it.name == item } ?:
                target::class.memberExtensionProperties.find { it.name == item }
            if (result == null) break

            if (result is KMutableProperty<*>) {
                // 整个环节中只要有一个是可变的,则整个流程不可信,每次都需要动态获取
                isVal = false
            }
            target = result.getter.call(target) ?: break
        }

        if (isVal) dynamicResultCache = target.toString()
        return target.toString()
    }
}
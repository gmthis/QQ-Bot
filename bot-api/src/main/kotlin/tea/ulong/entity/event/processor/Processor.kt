package tea.ulong.entity.event.processor

import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.OtherClient
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import tea.ulong.entity.MessageEntity
import tea.ulong.entity.event.processor.annotation.*
import tea.ulong.entity.event.processor.annotation.LifecycleModel.*
import tea.ulong.entity.exception.ProcessorBindException
import tea.ulong.entity.exception.ProcessorNotHaveConstructorException
import tea.ulong.entity.utils.DynamicContainers
import tea.ulong.ext.User
import tea.ulong.ext.fullName
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
            Prefix.fetch(SpecialPrefix.AtBot.symbol)
        )

        /**
         * 默认事件类型
         */
        var DefaultRespondEvent = listOf(
            RespondEventModel.FriendMessage, RespondEventModel.GroupMessage
        )
    }

    /**
     * 该类中的所有可触发函数的代理
     */
    val triggerFun: List<ProcessorFun>

    /**
     * 被代理Processor的构造函数
     */
    val constructor = clazz.primaryConstructor ?:
        clazz.constructors.find { it.findAnnotation<UseThis>() != null } ?:
        clazz.constructors.find { it.parameters.isEmpty() } ?: ::createInstance

    val lifecycleAnnotation = clazz.findAnnotation<Lifecycle>()

    /**
     * 单例,如果某个函数使用单例模式,那么会使用该属性
     */
    private val single: Any? by lazy { getInstance() }

    /**
     * 成员绑定映射表
     */
    private val memberBindMap by lazy { mutableMapOf<String, Any>() }

    /**
     * 群绑定映射表
     */
    private val groupBindMap by lazy { mutableMapOf<String, Any>() }

    /**
     * 群内成员绑定映射表
     */
    private val inGroupMemberBindMap by lazy { mutableMapOf<String, Any>() }

    init {
        // 获取所有标记为可触发的函数
        val funcList = clazz.functions.filter {
            it.findAnnotation<Trigger>() != null
        }
        // 代理这些函数
        triggerFun = funcList.map { ProcessorFun(it, this) }
    }

    /**
     * 执行某个可触发函数
     *
     * @param func 目标函数
     * @param event 事件
     * @return 执行结果
     */
    fun run(
        func: ProcessorFun,
        messageEntity: MessageEntity,
        event: Event,
        funcChain: List<ProcessorFun>? = null,
        paramMap: Map<String, Any>
    ): Any? {
        var result: Any? = null
        // 判断事件是否符合条件
        if (func.respondEvent.any { it.clazz == event::class }){
            val parameters = func.function.parameters
            val params = mutableMapOf<KParameter, Any?>()
            // 映射参数,目前支持获取event,未来会支持更多
            for (param in parameters){
                if (param.kind == KParameter.Kind.INSTANCE){
                    params[param] = executor(event, lifecycleAnnotation?.cycle ?: func.lifecycle)
                    continue
                }
                if (param.type.isSubtypeOf(ProcessorFunI::class.createType())){
                    params[param] =
                        ProcessorFunProxy((funcChain?.get(funcChain.indexOf(func).minus(1)) ?: func), funcChain)
                }
                if (param.name == "key"){
                    params[param] = messageEntity.triggerList.joinToString(" ")
                }
                if (event::class.createType().isSubtypeOf(param.type)){
                    params[param] = event
                }
                val annotation = param.findAnnotation<Param>() ?: continue
                if (annotation.name != SpecialParam.NON){
                    params[param] = paramMap[annotation.name] ?: paramMap[annotation.index.toString()]
                }else if (annotation.index != -1){
                    params[param] = paramMap[annotation.index.toString()]
                }
            }
            result = func.function.callBy(params)
        }
        return result
    }

    /**
     * 获取执行函数的执行实例,根据不同的生命周期返回不同的对象,该api未来可能发生变动
     */
    fun executor(event: Event, lifecycle: LifecycleModel): Any {

        if (lifecycle == Scope) return getInstance()
        if (lifecycle == single) return getInstance()

        val getInstanceByLifecycle = when (lifecycle) {
            BindMember -> { e: MessageEvent ->
                memberBindMap.getOrPut(e.subject.id.toString()){ getInstance() }
            }
            GroupSing -> { e: MessageEvent ->
                when (e) {
                    is UserMessageEvent -> memberBindMap.getOrPut(e.subject.id.toString()){ getInstance() }
                    is OtherClient -> {
                        memberBindMap.getOrPut(e.subject.id.toString()){ getInstance() }
                    }
                    else -> groupBindMap.getOrPut(e.subject.id.toString()){ getInstance() }
                }
            }
            else -> { e: MessageEvent ->
                inGroupMemberBindMap.getOrPut(e.subject.id.toString() + e.sender.id.toString()){ getInstance() }
            }
        }

        return when(event){
            is BotPassiveEvent -> {
                when(event){
                    is MessageEvent -> {
                        getInstanceByLifecycle(event)
                    }
                    else ->
                        throw ProcessorBindException("${clazz.fullName}的生命周期绑定异常,绑定为${lifecycle}," +
                                "但传入的事件为${event::class.fullName}类型")
                }
            }
            else ->
                throw ProcessorBindException("${clazz.fullName}的生命周期绑定异常,绑定为${lifecycle}," +
                        "但传入的事件为${event::class.fullName}类型")

        }
    }

    /**
     * 获取被代理类的实例,目前没有实现依赖注入.
     */
    fun getInstance(): Any {
        return constructor.call()
    }

    private fun createInstance(){
        try {
            clazz.createInstance()
        }catch (e: IllegalArgumentException){
            throw ProcessorNotHaveConstructorException("${clazz.fullName} 无法通过createInstance()函数构造实例")
        }
    }
}

interface ProcessorFunI{
    val function: KFunction<*>
    val processor: Processor
    val prev: MutableMap<String, ProcessorFun>
    val funFullName:String
    val trigger: Trigger
    var prefixs: List<Prefix>
    var respondEvent: List<RespondEventModel>
    val authentication: Authentication
    val lifecycle: LifecycleModel
    val next: MutableMap<String, ProcessorFun>

    /**
     * 执行该函数
     * @param key 触发词,该参数未来可能被删除
     * @param event 事件
     */
    suspend fun run(messageEntity: MessageEntity, event: Event, funcChain: List<ProcessorFun>? = null)
}

/**
 * 可触发函数代理类
 */
class ProcessorFun(override val function: KFunction<*>, override val processor: Processor): ProcessorFunI {
    override val prev: MutableMap<String, ProcessorFun> = mutableMapOf()
    override val funFullName = "${processor.clazz.simpleName}.${function.name}"
    override val trigger: Trigger = function.findAnnotation<Trigger>()!!
    override var prefixs: List<Prefix>
    override var respondEvent: List<RespondEventModel> =
         function.findAnnotation<RespondEvent>()?.model?.toList() ?: Processor.DefaultRespondEvent
    override val authentication = function.findAnnotation<Authentication>() ?: Authentication()
    override val lifecycle = function.findAnnotation<Lifecycle>()?.cycle ?: Scope
    override val next: MutableMap<String, ProcessorFun> = mutableMapOf()

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
    override suspend fun run(messageEntity: MessageEntity, event: Event, funcChain: List<ProcessorFun>?) {
        if (event is MessageEvent){
            val user = event.sender.User
            //权限检查
            if (authentication.check(user.authority)){
                val paramMap = mutableMapOf<String, Any>()
                var index = 0
                var position = 1
                while (index < messageEntity.paramList.size) {
                    val token = messageEntity.paramList[index]
                    if (token.startsWith("-") && token.length > 1) {
                        val key = token.substring(1)
                        if (index + 1 < messageEntity.paramList.size && !messageEntity.paramList[index + 1].startsWith("-")) {
                            val value = messageEntity.paramList[index + 1]
                            paramMap[key] = value
                            paramMap[position.toString()] = value
                            index += 2
                        } else {
                            index += 1
                        }
                    } else {
                        paramMap[position.toString()] = token
                        index += 1
                    }
                    position += 1
                }
                val result = processor.run(this, messageEntity, event, funcChain, paramMap)
                if (result is String){
                    event.subject.sendMessage(event.message.quote() + result)
                }
            }
        }
    }
}

class ProcessorFunProxy(val processorFun: ProcessorFun, val funcChain: List<ProcessorFun>?): ProcessorFunI by processorFun {
    override suspend fun run(messageEntity: MessageEntity, event: Event, funcChain: List<ProcessorFun>?) {
        processorFun.run(messageEntity, event, this.funcChain)
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

    fun check(statement: MessageEntity): Boolean{
        return if (symbol == SpecialPrefix.AtBot.symbol && statement.isAt){
            statement.atMessage!!.target == (DynamicContainers["bot"] as Bot).id
        } else {
            check(statement.plainTextAndImageMessageContent)
        }
    }

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
    fun getTrigger(statement: String): String {
        return (if (isDynamicProperty)
            statement.replace("$prefix${dynamicallyAcquired() ?: "null"}$postfix", "")
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
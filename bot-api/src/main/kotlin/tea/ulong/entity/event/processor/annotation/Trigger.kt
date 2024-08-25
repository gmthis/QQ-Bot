package tea.ulong.entity.event.processor.annotation

import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import tea.ulong.entity.event.UniversalLevel
import tea.ulong.entity.event.processor.annotation.AuthenticationModel.*
import tea.ulong.entity.utils.DynamicContainers
import tea.ulong.ext.containsAll
import kotlin.reflect.KClass

/**
 * 在Processor中的函数中注明该函数通过哪些关键词触发
 *
 * 另见:[Prefix]
 * @param triggers 关键词列表
 * @param level 标记该函数的优先级,暂时没有实现这个功能
 * @param isUsedUniversalPrefix 是否使用通用前缀,如果不使用且希望使用自定义前缀则应通过[Prefix]指定需要的前缀
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Trigger(
    vararg val triggers: String,
    val front: String = "",
    val level: UniversalLevel = UniversalLevel.Ordinary,
    /**是否使用通用前缀,true为使用,false为不使用**/
    val isUsedUniversalPrefix: Boolean = true
)

/**
 * 如果不使用通用前缀,但仍然需要前缀,则应使用该注解定义需要的前缀类型.
 * 
 * 可以使用特殊定义来引用[DynamicContainers]中定义的动态属性,如下:
 * 
 * Prefix("#@bot.id@#", prefix = "@")
 * 
 * 该注解会被"@{机器人qq号}"的纯文本内容前缀触发
 * @param symbol 前缀内容
 * @param prefix 虽然很奇怪,但是这是前缀的前缀,主要用于动态引用的实现
 * @param postfix 这是前缀的后缀
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Prefix(
    val symbol: String,
    val prefix: String,
    val postfix: String
)

/**
 * 特殊前缀,用来标记特殊的触发方式,比如说@事件,甚至这个事件可以不在前缀出现.
 */
enum class SpecialPrefix(val symbol: String){
    AtBot("29phand;fn2987h5baklba09801h")
}

/**
 * 标记当前Processor fun的所需要的权限等级,暂未实现.
 *
 * 另见:
 *
 * [Authority]
 *
 * [AuthenticationModel]
 * @param authority 目标权限等级
 * @param model 检查模式
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class Authentication(
    vararg val authority: Authority = [Authority.User],
    val model: AuthenticationModel = Exceed
)

/**
 * 检查target是否符合权限要求
 */
fun Authentication.check(target: Authority) = when(model){
    Exceed -> target.level >= authority.maxBy { it.level }.level
    Include -> authority.contains(target)
    Congruent -> if (authority.size > 1) false else if (authority.size == 1) authority[0] == target else false
    Exclude -> !authority.contains(target)
}

/**
 * 检查target是否符合权限要求
 */
fun Authentication.check(target: List<Authority>) = when(model){
    Exceed -> target.maxBy { it.level } .level>= authority.maxBy { it.level }.level
    Include -> authority.containsAll(target)
    Congruent -> authority.size == target.size && target.containsAll(authority.toList())
    Exclude -> target.none{ it in authority }
}

/**
 * 权限等级,含义与其名相同
 */
enum class Authority(val level: Int) {
    Master(100), GlobalAdmin(95),  Admin(90), GroupAdmin(80),
    User(70), Restricted(60), InBlacklist(50), Shield(0)
}

/**
 * 权限检查模式
 */
enum class AuthenticationModel {
    /**>=大于等于模式**/Exceed,  /**in包含模式**/Include, /**=全等模式**/Congruent,
    /**notin排除模式**/Exclude
}

/**
 * 标记Process的生命周期
 *
 * 另见:[LifecycleModel]
 * @param cycle 生命周期标记
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Lifecycle(
    val cycle: LifecycleModel = LifecycleModel.Scope
)

/**
 * 生命周期定义
 */
enum class LifecycleModel {
    /**单例模式**/Sing,
    /**成员绑定模式,所有被绑定的成员会获取到固定的单例,当不存在绑定的成员时销毁,注意这是跨群的**/BindMember,
    /**每个群会获取到对应的单体实例,这个实例在程序重启前不会变化**/GroupSing,
    /**群内成员绑定模式,与成员绑定模式相同,但不跨群**/GroupBindMember,
    /**每次请求均使用新实例**/Scope
}

/**
 * 标记Process fun支持的事件类型,默认为私聊和群聊
 *
 * 另见: [RespondEventModel]
 * @param model: 支持的事件列表
 */
annotation class RespondEvent(
    vararg val model: RespondEventModel = [RespondEventModel.FriendMessage, RespondEventModel.GroupMessage]
)

/**
 * 这里只会列出支持的事件类型,如果未列出则代表暂不支持
 */
enum class RespondEventModel(val clazz: KClass<out Event>){
    FriendMessage(FriendMessageEvent::class), GroupMessage(GroupMessageEvent::class)
}

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Param(
    val name: String = SpecialParam.NON,
    val index: Int = -1
)

class SpecialParam{
    companion object{
        const val NON = "-2n134yv59-2,-1.cjoluwhd"
    }
}
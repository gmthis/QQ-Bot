package tea.ulong.entity.event.processor.annotation

import tea.ulong.entity.event.UniversalLevel

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Trigger(
    vararg val triggers: String,
    val level: UniversalLevel = UniversalLevel.Ordinary,
    /**是否使用通用前缀,true为使用,false为不使用**/
    val isUsedUniversalPrefix: Boolean = true
)

/**
 * 如果不使用通用前缀,但仍然需要前缀,则应使用该注解定义需要的前缀类型.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Prefix(
    val symbol: String,
    val prefix: String,
    val postfix: String
)

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class Authentication(
    vararg val authority: Authority = [Authority.User],
    val model: AuthenticationMethod = AuthenticationMethod.Exceed
)

enum class Authority(val level: Int) {
    Master(100), GlobalAdmin(95),  Admin(90), GroupAdmin(80),
    User(70), Restricted(60), InBlacklist(50), Shield(0)
}

enum class AuthenticationMethod {
    /**>=大于等于模式**/Exceed,  /**in包含模式**/Include, /**=全等模式**/Congruent,
    /**notin排除模式**/Exclude
}

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Execution(
    val method: ExecutionMethod = ExecutionMethod.Scope
)

enum class ExecutionMethod {
    Queue,
    SingQueues,
    Scope
}

annotation class ApplyTo(
    vararg val method: ApplyMethod = [ApplyMethod.PrivateChat, ApplyMethod.GroupChat]
)

enum class  ApplyMethod{
    PrivateChat, GroupChat
}
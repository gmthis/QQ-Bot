package tea.ulong.entity.event.processor.annotation

import tea.ulong.entity.utils.DynamicContainers

/**
 * 指定使用的构造函数,每个类只可以指定一个,如果不指定则会使用主构造函数.
 * 如果没有主构造函数会使用空构造函数.
 *
 * Processor应该至少提供一个构造函数.
 */
@Target(AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.RUNTIME)
annotation class UseThis

/**
 * 标记类为Processor
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Processor

/**
 * 为类和函数指定一个Symbol来避免重名的出现,在任何情况下都推荐使用Symbol来
 * 定义一个别名,如果多个类名相同的class被同时加载,那么在[DynamicContainers]
 * 中会出现冲突,后来者会覆盖掉前者导致无法在依赖注入时注入正确的参数(因为注入的是代理
 * [Processor][tea.ulong.entity.event.processor.Processor]和
 * [ProcessorFun][tea.ulong.entity.event.processor.ProcessorFunI],所以这甚至可能不会导致运行时错误).
 *
 * 因此使用该注解定义一个别名是十分重要的.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Symbol(
    val value: String
)

/**
 * 为Processor标记初始化函数,该函数之可以用于companion object中的函数.
 * 目的是为了进行通用的初始化而非对对象构建时的初始化,如果对对象构建时的初始化有需求
 * 应使用[UseThis]指定带有参数的构造器实现.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Init
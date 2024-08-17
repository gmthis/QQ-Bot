package tea.ulong.entity.event.processor.annotation

/**
 * 指定使用的构造函数,每个类只可以指定一个,如果不指定则会使用主构造函数.
 * 如果没有主构造函数会使用空构造函数.
 *
 * Processor应该至少提供一个构造函数.
 */
@Target(AnnotationTarget.CONSTRUCTOR)
annotation class UseThis()
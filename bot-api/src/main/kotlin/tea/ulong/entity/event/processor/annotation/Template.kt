package tea.ulong.entity.event.processor.annotation

/**
 * 在静态属性上标记模板,只应使用在静态属性上,[name]为保存到文件时的名字
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class Template(val name: String)

/**
 * 标记类在保存在template文件时的名字
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class TemplatePackName(val name: String)
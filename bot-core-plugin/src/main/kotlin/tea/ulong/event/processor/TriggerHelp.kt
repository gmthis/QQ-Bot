package tea.ulong.event.processor

import tea.ulong.entity.event.processor.ProcessorFunI
import tea.ulong.entity.event.processor.annotation.Authentication
import tea.ulong.entity.event.processor.annotation.Authority
import tea.ulong.entity.event.processor.annotation.Trigger
import kotlin.reflect.full.findAnnotation

class TriggerHelp {

    @Trigger("help", "帮助", "bz", front = "*")
    @Authentication(Authority.User)
    fun help(prev: ProcessorFunI): String {
        val help = prev.function.findAnnotation<Help>() ?: return "调用对象不支持help功能."
        return help.message
    }


}

annotation class Help(
    val message: String
)
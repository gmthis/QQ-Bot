package tea.ulong.core.plugin

import tea.ulong.entity.event.processor.ProcessorFunI
import tea.ulong.entity.event.processor.annotation.*
import tea.ulong.entity.event.processor.anntation.Help
import kotlin.reflect.full.findAnnotation

@Processor
class TriggerHelp {

    companion object {

        @Init
        fun init(processorFunList: List<ProcessorFunI>) {
            for (processorFunI in processorFunList) {
                println(processorFunI.funFullName)
            }
        }

    }

    @Trigger("help", "帮助", "bz", front = "*")
    @Authentication(Authority.User)
    fun help(@PrevProcessorFun prev: ProcessorFunI): String {
        val help = prev.function.findAnnotation<Help>() ?: return "调用对象不支持help功能."
        return help.message
    }

}


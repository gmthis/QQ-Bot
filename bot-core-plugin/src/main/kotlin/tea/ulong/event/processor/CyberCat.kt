package tea.ulong.event.processor

import tea.ulong.entity.event.processor.annotation.*
import tea.ulong.ext.fillTemplate
import tea.ulong.ext.format
import tea.ulong.ext.getTemplate
import java.time.Duration
import java.time.LocalDateTime

@Lifecycle(LifecycleModel.GroupSing)
@TemplatePackName("赛博猫猫")
class CyberCat{

    val cat = Cat()

    companion object{
        @Template("状态")
        const val STATUS_TEMPLATE = "姓名: {{name}}\n开心度: {{happiness}}\n饥饿度: {{hunger}}"
        @Template("改名")
        const val RENAME_TEMPLATE = "猫猫已改名为: {{new}}"
        @Template("喂食")
        const val FEED_TEMPLATE = "{{name}}吃的很饱."
        @Template("喂食")
        const val FEED_TEMPLATE2 = "{{name}}吃饱了."
        @Template("玩")
        const val PLAY_TEMPLATE = "你陪{{name}}玩了一会儿,现在{{name}}非常开心"
        @Template("玩")
        const val PLAY_TEMPLATE2 = "你用激光笔逗得{{name}}在屋子里乱跑~"
    }

    @Trigger("猫猫", "mm", "猫咪", "cat")
    fun cat(): String{
        return "目前有以下功能:\n" +
                "改名(rename, r, R)\n" +
                "状态(s, S)\n" +
                "喂(f, F, w, W)\n" +
                "玩(p, P)"
    }

    @Trigger("改名", "rename", "r", "R", front = "CyberCat.cat")
    fun rename(@Param(index = 1) newName: String): String {
        cat.name = newName
        return getTemplate("改名")!!.fillTemplate(mapOf("new" to newName))
    }

    @Trigger("状态", "s", "S", front = "CyberCat.cat")
    fun catStatus(): String {
        val currentTime = LocalDateTime.now()

        // 计算时间差，以分钟为单位
        val minutesSinceLastHappinessChange = Duration.between(cat.lastHappinessChange, currentTime).toMinutes()
        val minutesSinceLastHungerChange = Duration.between(cat.lastHungerChange, currentTime).toMinutes()

        // 计算幸福度下降值
        val happinessDecrease = minutesSinceLastHappinessChange * 0.015
        // 计算饥饿度下降值
        val hungerDecrease = minutesSinceLastHungerChange * 0.005

        // 输出为百分比
        val happinessPercentage = (cat.happiness - happinessDecrease) * 100
        val hungerPercentage = (cat.hunger - hungerDecrease) * 100

        return getTemplate("状态")!!.fillTemplate(mapOf(
            "name" to cat.name,
            "happiness" to happinessPercentage.format(2),
            "hunger" to hungerPercentage.format(2)
        ))
    }

    @Trigger("喂", "f", "F", "w", "W", front = "CyberCat.cat")
    fun feeding(): String {
        cat.lastHungerChange = LocalDateTime.now()
        return getTemplate("喂食")!!.fillTemplate(mapOf(
            "name" to cat.name
        ))
    }

    @Trigger("玩", "p", "P", front = "CyberCat.cat")
    fun play(): String {
        cat.lastHappinessChange = LocalDateTime.now()
        return getTemplate("玩")!!.fillTemplate(mapOf(
            "name" to cat.name
        ))
    }
}

class Cat{
    var name = "猫猫"
    var happiness = 1.0
    var lastHappinessChange = LocalDateTime.now()
    var hunger = 1.0
    var lastHungerChange = LocalDateTime.now()
}
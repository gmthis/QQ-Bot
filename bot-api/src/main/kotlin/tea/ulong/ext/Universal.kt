package tea.ulong.ext

import net.mamoe.mirai.contact.Contact
import tea.ulong.entity.User
import tea.ulong.loader.entity.UserLoader
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

/**
 * 将long格式化为[digits]位数的字符串
 */
fun Long.format(digits: Int) = "%.${digits}f".format(this.toDouble())

/**
 * 将double格式化为[digits]位数的字符串
 */
fun Double.format(digits: Int) = "%.${digits}f".format(this)

/**
 * 获取当日的零点零分零秒零纳秒
 */
fun LocalDateTime.zero(): LocalDateTime = this
    .withHour(0)
    .withMinute(0)
    .withSecond(0)
    .withNano(0)


val Contact.User: User
    get() = UserLoader.loader(this.id)

fun Array<*>.containsAll(target: List<*>) = target.all { it in this }

val KClass<*>.fullName: String
    get() = this.qualifiedName ?: this.jvmName
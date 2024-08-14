package tea.ulong.loader.entity

import kotlinx.serialization.encodeToString
import tea.ulong.entity.Config
import tea.ulong.entity.User
import tea.ulong.entity.utils.json
import java.io.File
import java.time.LocalDateTime

object UserLoader {

    private val path = File(Config.USER_PATH)

    init {
        if (!path.exists()){
            path.mkdirs()
        }
    }

    fun loader(qq: Long) : User {
        val qqFile = File(path,  "$qq.json")
        if (!qqFile.exists()){
            qqFile.createNewFile()
            val user = User(qq, LocalDateTime.now())
            qqFile.writeText(json.encodeToString(user), charset("UTF-8"))
            return user
        }
        val user: User = json.decodeFromString(qqFile.readText())
        return user
    }

}
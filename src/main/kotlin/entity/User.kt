package tea.ulong.entity

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import tea.ulong.entity.utils.json
import java.io.File
import java.time.LocalDateTime

@Serializable
data class User(
    var id: Long,
    @Contextual
    var lastInteraction: LocalDateTime,
    val extendedProperties : MutableMap<String, JsonElement> = mutableMapOf()
) {
    fun save() {
        val file = File(Config.USER_PATH, "$id.json")
        file.writeText(json.encodeToString(this))
    }
}

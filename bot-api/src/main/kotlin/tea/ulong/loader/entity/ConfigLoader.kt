package tea.ulong.loader.entity

import kotlinx.serialization.encodeToString
import tea.ulong.entity.Config
import tea.ulong.entity.utils.json
import java.io.File

/**
 * 配置加载器
 */
object ConfigLoader {
    private val file = File(Config.CONFIG_FILE)

    init {
        if (!file.exists()){
            file.createNewFile()
            file.writeText(json.encodeToString(Config()))
        }
    }

    private var _config: Config? = null

    /**
     * 配置
     */
    val config: Config
        get() {
            if (_config == null){
                _config = loader()
            }
            return _config!!
        }

    /**
     * 从磁盘中加载配置内容
     *
     * @return 配置内容
     */
    fun loader(): Config {
        return json.decodeFromString(file.readText())
    }

}
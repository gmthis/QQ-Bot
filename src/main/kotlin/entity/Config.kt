package tea.ulong.entity

import kotlinx.serialization.Serializable

@Serializable
class Config {

    companion object{
        const val USER_PATH = ".User"
        const val CONFIG_FILE = ".config"
    }

    var master = 0L
    var onebotWebSocketAddress = "websocket服务地址"
    var onebotToken = "连接onebot服务的token"
}
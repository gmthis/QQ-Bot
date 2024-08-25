package tea.ulong.entity

import net.mamoe.mirai.message.data.*

class MessageEntity {
    var isAt = false
    var atMessage: At? = null
    var isAtAll = false
    var atAllMessage: AtAll? = null

    var isHaveMessageSource = false
    var messageSource: MessageSource? = null

    var isQuteReply = false
    var quteReply: Message? = null

    /**
     * 不包含at消息的消息链
     */
    val messageChain = mutableListOf<Message>()

    /**
     * 不包含at消息的消息内容
     */
    val content: String
        get() = messageChain.toMessageChain().content

    /**
     * 只包含文字与图片的消息链,可能包含标签
     */
    val plainTextAndImageMessageChain = mutableListOf<Message>()

    /**
     * 只包含纯文本与图片的消息内容
     */
    val plainTextAndImageMessageContent
        get() = plainTextAndImageMessageChain.toMessageChain().content

    val triggerList = mutableListOf<String>()
    val paramList = mutableListOf<String>()
}
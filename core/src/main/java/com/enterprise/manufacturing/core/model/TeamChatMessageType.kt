package com.enterprise.manufacturing.core.model

/** Тип сообщения в общих и чертежных чатах (расширяется при появлении вложений). */
enum class TeamChatMessageType {
    TEXT,
    VOICE,
    FILE,
}

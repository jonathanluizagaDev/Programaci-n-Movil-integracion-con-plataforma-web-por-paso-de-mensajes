package org.ucb.appp1.model

enum class MessageSender {
    ME,
    OTHER,
}

data class ChatMessage(
    val id: String,
    val text: String,
    val sender: MessageSender,
    val timeLabel: String,
)

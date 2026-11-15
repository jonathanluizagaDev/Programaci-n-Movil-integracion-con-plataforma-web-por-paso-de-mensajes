package org.ucb.appp1.data

import org.ucb.appp1.domain.MessageRepository
import org.ucb.appp1.model.ChatMessage
import org.ucb.appp1.model.MessageSender

class FakeMessageRepository : MessageRepository {
    private var nextMessageNumber = 3

    override fun getInitialMessages(): List<ChatMessage> = listOf(
        ChatMessage(
            id = "demo-1",
            text = "Hola, este es el canal de mensajes de demostración.",
            sender = MessageSender.OTHER,
            timeLabel = "10:02",
        ),
        ChatMessage(
            id = "demo-2",
            text = "La app todavía no está conectada al servidor web.",
            sender = MessageSender.OTHER,
            timeLabel = "10:03",
        ),
    )

    override fun sendMessage(text: String): ChatMessage = ChatMessage(
        id = "demo-${nextMessageNumber++}",
        text = text,
        sender = MessageSender.ME,
        timeLabel = "Ahora",
    )
}

package org.ucb.appp1.domain

import org.ucb.appp1.model.ChatMessage

interface MessageRepository {
    fun getInitialMessages(): List<ChatMessage>

    fun sendMessage(text: String): ChatMessage
}

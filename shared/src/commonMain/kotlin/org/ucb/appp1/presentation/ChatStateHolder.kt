package org.ucb.appp1.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.ucb.appp1.domain.MessageRepository
import org.ucb.appp1.model.ChatMessage

class ChatStateHolder(
    private val messageRepository: MessageRepository,
) {
    val messages = mutableStateListOf<ChatMessage>().apply {
        addAll(messageRepository.getInitialMessages())
    }

    var inputText by mutableStateOf("")
        private set

    fun onInputChanged(value: String) {
        inputText = value
    }

    fun sendMessage() {
        val text = inputText.trim()
        if (text.isEmpty()) return

        messages.add(messageRepository.sendMessage(text))
        inputText = ""
    }
}

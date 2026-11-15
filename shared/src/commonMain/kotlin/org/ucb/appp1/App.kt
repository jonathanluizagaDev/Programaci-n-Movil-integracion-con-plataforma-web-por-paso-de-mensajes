package org.ucb.appp1

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.ucb.appp1.data.FakeMessageRepository
import org.ucb.appp1.presentation.ChatScreen
import org.ucb.appp1.presentation.ChatStateHolder

@Composable
fun App() {
    MaterialTheme {
        val chatState = remember { ChatStateHolder(FakeMessageRepository()) }
        ChatScreen(state = chatState)
    }
}

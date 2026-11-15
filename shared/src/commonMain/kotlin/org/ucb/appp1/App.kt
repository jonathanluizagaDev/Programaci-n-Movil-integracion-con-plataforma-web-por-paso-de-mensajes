package org.ucb.appp1

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.ucb.appp1.data.FakeEventRepository
import org.ucb.appp1.presentation.EventConsoleScreen
import org.ucb.appp1.presentation.EventStateHolder

@Composable
fun App() {
    MaterialTheme {
        val eventState = remember { EventStateHolder(FakeEventRepository()) }
        EventConsoleScreen(state = eventState)
    }
}

package org.ucb.appp1.presentation

import androidx.compose.runtime.mutableStateListOf
import org.ucb.appp1.domain.EventRepository
import org.ucb.appp1.model.EventLogEntry
import org.ucb.appp1.model.EventOrigin

class EventStateHolder(
    private val eventRepository: EventRepository,
) {
    val logs = mutableStateListOf<EventLogEntry>()

    fun startFromApp() {
        logs.addAll(eventRepository.simulateRoundTrip(EventOrigin.APP))
    }

    fun simulateStartFromWeb() {
        logs.addAll(eventRepository.simulateRoundTrip(EventOrigin.WEB))
    }

    fun clearLogs() {
        logs.clear()
    }
}

package org.ucb.appp1.domain

import org.ucb.appp1.model.EventLogEntry
import org.ucb.appp1.model.EventOrigin

interface EventRepository {
    fun simulateRoundTrip(origin: EventOrigin): List<EventLogEntry>
}

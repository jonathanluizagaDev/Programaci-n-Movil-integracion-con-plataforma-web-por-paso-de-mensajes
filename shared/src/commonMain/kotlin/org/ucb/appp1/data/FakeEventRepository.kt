package org.ucb.appp1.data

import org.ucb.appp1.domain.EventRepository
import org.ucb.appp1.model.EventLogEntry
import org.ucb.appp1.model.EventLogStage
import org.ucb.appp1.model.EventOrigin

class FakeEventRepository : EventRepository {
    private var nextEventNumber = 1

    override fun simulateRoundTrip(origin: EventOrigin): List<EventLogEntry> {
        require(origin == EventOrigin.APP || origin == EventOrigin.WEB) {
            "El origen debe ser la app o la plataforma web"
        }
        val number = nextEventNumber++
        val requestId = "req-${number.toString().padStart(3, '0')}"
        val eventId = "evt-${number.toString().padStart(3, '0')}"
        val destination = if (origin == EventOrigin.APP) EventOrigin.WEB else EventOrigin.APP
        val originName = origin.label
        val destinationName = destination.label

        return listOf(
            EventLogEntry(
                id = "$eventId-request",
                requestId = requestId,
                eventId = eventId,
                source = origin,
                stage = EventLogStage.REQUEST,
                description = "$originName envía POST HTTPS /api/events: activity.start",
            ),
            EventLogEntry(
                id = "$eventId-response",
                requestId = requestId,
                eventId = eventId,
                source = EventOrigin.SERVER,
                stage = EventLogStage.RESPONSE,
                description = "HTTP 202 Accepted. La orden fue registrada.",
            ),
            EventLogEntry(
                id = "$eventId-delivery",
                requestId = requestId,
                eventId = eventId,
                source = destination,
                stage = EventLogStage.DELIVERY,
                description = "$destinationName recibe el evento activity.started.",
            ),
            EventLogEntry(
                id = "$eventId-ack",
                requestId = requestId,
                eventId = eventId,
                source = destination,
                stage = EventLogStage.ACKNOWLEDGEMENT,
                description = "POST HTTPS /api/events/$eventId/ack: processed",
            ),
            EventLogEntry(
                id = "$eventId-completion",
                requestId = requestId,
                eventId = eventId,
                source = origin,
                stage = EventLogStage.COMPLETION,
                description = "El servidor confirma el procesamiento y cierra el flujo.",
            ),
        )
    }
}

package org.ucb.appp1.model

enum class EventOrigin(val label: String) {
    APP("App móvil"),
    WEB("Plataforma web"),
    SERVER("Servidor"),
}

enum class EventLogStage(val label: String) {
    REQUEST("Solicitud HTTPS"),
    RESPONSE("Respuesta HTTP"),
    DELIVERY("Evento recibido"),
    ACKNOWLEDGEMENT("Confirmación HTTPS"),
    COMPLETION("Resultado final"),
}

data class EventLogEntry(
    val id: String,
    val requestId: String,
    val eventId: String,
    val source: EventOrigin,
    val stage: EventLogStage,
    val description: String,
)

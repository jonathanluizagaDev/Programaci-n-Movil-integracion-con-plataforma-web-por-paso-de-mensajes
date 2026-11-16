# Avance del prototipo móvil: consola de eventos

> Documento anterior al servidor de demostración. El estado actual y sus instrucciones están en [DEMO_HTTPS_TEMA.md](./DEMO_HTTPS_TEMA.md).

**Fecha:** 2026-11-15

**Rama:** `feature/https-eventos-prototipo`

**Estado:** simulación local del flujo; aún sin API ni servidor

## Qué se implementó

Se reemplazó la pantalla de chat de demostración por una consola que ayuda a visualizar la idea acordada con el docente: una acción se origina en la app o en la web, recibe una respuesta HTTP, llega como evento al otro lado y termina con una confirmación. La pantalla muestra los pasos y los identificadores `requestId` y `eventId`.

La consola tiene dos acciones:

- **Iniciar evento desde la app**: simula que la app es el origen y la web el destino.
- **Simular evento iniciado desde la web**: simula el recorrido inverso.

Cada acción agrega cinco registros: solicitud HTTPS, respuesta HTTP `202 Accepted`, entrega del evento, confirmación HTTPS y resultado final. Son datos de ejemplo generados en memoria. No se abre una conexión ni se envían datos por la red.

## Archivos modificados

### `shared/src/commonMain/kotlin/org/ucb/appp1/App.kt`

Se reemplazó la creación del estado y pantalla de chat por:

```kotlin
val eventState = remember { EventStateHolder(FakeEventRepository()) }
EventConsoleScreen(state = eventState)
```

`remember` conserva el estado durante la composición. La pantalla recibe la interfaz `EventRepository`, conectada por ahora a `FakeEventRepository`.

### Documentación

- `docs/INTEGRACION_WEB_MENSAJES.md`: registra el objetivo, el estado actual, el flujo HTTPS y el plan de integración.
- `docs/ANALISIS_HTTPS_EVENTOS.md`: detalla la arquitectura, el contrato provisional y las fases técnicas.
- `docs/GUIA_EXPLICACION_DOCENTE.md`: contiene una explicación oral y el recorrido que se puede demostrar.

## Archivos nuevos

### `shared/src/commonMain/kotlin/org/ucb/appp1/model/EventLogEntry.kt`

Define los datos de cada registro:

```kotlin
enum class EventOrigin { APP, WEB, SERVER }
enum class EventLogStage { REQUEST, RESPONSE, DELIVERY, ACKNOWLEDGEMENT, COMPLETION }

data class EventLogEntry(
    val id: String,
    val requestId: String,
    val eventId: String,
    val source: EventOrigin,
    val stage: EventLogStage,
    val description: String,
)
```

Los enums identifican quién participa y en qué etapa está el flujo. Los IDs permiten relacionar visualmente los cinco registros.

### `shared/src/commonMain/kotlin/org/ucb/appp1/domain/EventRepository.kt`

Declara el límite entre la interfaz y el mecanismo que produce los registros:

```kotlin
interface EventRepository {
    fun simulateRoundTrip(origin: EventOrigin): List<EventLogEntry>
}
```

La pantalla no necesita saber si los datos vienen de una simulación o, en el futuro, de un servicio remoto.

### `shared/src/commonMain/kotlin/org/ucb/appp1/data/FakeEventRepository.kt`

Implementa la simulación. Genera IDs secuenciales `req-001` y `evt-001`, decide el destino opuesto al origen y devuelve los cinco registros. En concreto, las descripciones muestran `POST HTTPS /api/events`, `HTTP 202 Accepted`, entrega de `activity.started`, `POST HTTPS /api/events/{eventId}/ack` y cierre.

Aunque las descripciones enseñan el formato esperado, el repositorio solo construye objetos Kotlin localmente. Las rutas son ilustrativas y no se han verificado con un backend.

### `shared/src/commonMain/kotlin/org/ucb/appp1/presentation/EventStateHolder.kt`

Mantiene una lista observable en Compose y contiene las acciones de los botones:

```kotlin
val logs = mutableStateListOf<EventLogEntry>()
fun startFromApp() = logs.addAll(eventRepository.simulateRoundTrip(EventOrigin.APP))
fun simulateStartFromWeb() = logs.addAll(eventRepository.simulateRoundTrip(EventOrigin.WEB))
fun clearLogs() = logs.clear()
```

Al cambiar la lista, Compose vuelve a dibujar el registro. Limpiar solo borra el estado local.

### `shared/src/commonMain/kotlin/org/ucb/appp1/presentation/EventConsoleScreen.kt`

Presenta los botones, el aviso **“Simulación local · API HTTPS aún no conectada”**, la lista de tarjetas, la etapa y el origen de cada paso, y los IDs relacionados. La lista baja automáticamente al último registro y el botón **Limpiar** vacía los registros.

## Archivos retirados del prototipo anterior

Se eliminaron los cinco archivos que formaban el chat local y que ya no corresponden al flujo acordado:

- `presentation/ChatScreen.kt`
- `presentation/ChatStateHolder.kt`
- `model/ChatMessage.kt`
- `domain/MessageRepository.kt`
- `data/FakeMessageRepository.kt`

La nueva consola sustituye el ejemplo de conversación por un ejemplo de eventos con solicitud, respuesta y confirmación. No se borró el historial de Git de los cambios anteriores.

## Cómo mostrarlo

1. Ejecutar la configuración de la app en Android Studio.
2. En la pantalla, pulsar **Iniciar evento desde la app**.
3. Observar las cinco tarjetas y que comparten `requestId`/`eventId`.
4. Pulsar **Iniciar evento desde la app** otra vez y notar que se generan IDs nuevos.
5. Limpiar y pulsar **Simular evento iniciado desde la web** para ver el sentido inverso.

Esto demuestra la estructura de la interacción y el registro visual. No demuestra conectividad con la web.

## Qué falta para completar la integración

1. Obtener el código o la documentación del backend y la plataforma web.
2. Elegir la acción real de demostración y acordar URL, método, autenticación, JSON y errores.
3. Acordar cómo el servidor entrega eventos al cliente (por ejemplo, SSE o polling HTTP; no WebSocket).
4. Implementar el cliente HTTPS y el receptor; reemplazar `FakeEventRepository` por uno remoto.
5. Implementar en el servidor y la web la publicación y el acuse de recibo.
6. Probar ambos sentidos y verificar logs reales con los IDs correlacionados.

El “50 %” solo puede usarse como una estimación del **prototipo cliente visual y de arquitectura**. No es 50 % de una integración end-to-end: todavía no se ha conectado ningún sistema real. El porcentaje del proyecto académico depende de la rúbrica del docente.

# Guía para explicar y demostrar el avance

> Documento anterior al servidor de demostración. El estado actual y sus instrucciones están en [DEMO_HTTPS_TEMA.md](./DEMO_HTTPS_TEMA.md).

## Explicación breve

> “Reemplacé el chat de mensajes de ejemplo por una consola que representa el flujo de eventos que definimos: la app o la web inicia una acción, el servidor responde, el evento llega al otro cliente y ese cliente confirma el procesamiento. Cada paso muestra el mismo `requestId` y `eventId` para poder seguirlo en los logs. Por ahora el flujo es simulado localmente; todavía no existe conexión HTTPS con un backend ni con la plataforma web. La siguiente etapa es acordar el contrato y sustituir el repositorio falso por una implementación de red.”

## Demostración en Android Studio

1. Ejecuta la app en el emulador.
2. Pulsa **Iniciar evento desde la app** y explica que simula la app como origen y la plataforma web como destino.
3. Muestra las etapas: **Solicitud HTTPS**, **Respuesta HTTP**, **Evento recibido**, **Confirmación HTTPS** y **Resultado final**.
4. Señala que los registros comparten `requestId` y `eventId`, que permiten correlacionar la orden en los logs.
5. Limpia el registro y pulsa **Simular evento iniciado desde la web** para mostrar el sentido inverso.
6. Aclara que los nombres HTTP y las rutas aparecen como ejemplo: la app no hizo una llamada de red.

## Qué hace cada archivo

| Archivo | Responsabilidad | Cambio realizado |
|---|---|---|
| `shared/src/commonMain/kotlin/org/ucb/appp1/App.kt` | Entrada Compose compartida | Construye `EventStateHolder` con `FakeEventRepository` y muestra `EventConsoleScreen`. |
| `shared/src/commonMain/kotlin/org/ucb/appp1/presentation/EventConsoleScreen.kt` | Interfaz | Añade acciones para ambos orígenes, aviso de simulación, lista de etapas, IDs y control para limpiar. |
| `shared/src/commonMain/kotlin/org/ucb/appp1/presentation/EventStateHolder.kt` | Estado y acciones | Conserva logs observables; agrega un recorrido desde app o web simulada, y permite limpiar. |
| `shared/src/commonMain/kotlin/org/ucb/appp1/model/EventLogEntry.kt` | Modelo | Define origen, etapa, IDs de correlación y descripción de cada log. |
| `shared/src/commonMain/kotlin/org/ucb/appp1/domain/EventRepository.kt` | Contrato interno | Separa la presentación de la fuente de los registros. |
| `shared/src/commonMain/kotlin/org/ucb/appp1/data/FakeEventRepository.kt` | Datos de demostración | Genera cinco pasos en memoria, con IDs secuenciales, para uno u otro origen. |

Se retiró el prototipo anterior de chat: `ChatScreen.kt`, `ChatStateHolder.kt`, `ChatMessage.kt`, `MessageRepository.kt` y `FakeMessageRepository.kt`. Las razones y el alcance del cambio están documentados en [AVANCE_PROTO_MENSAJERIA_50.md](./AVANCE_PROTO_MENSAJERIA_50.md).

## Código clave para explicar

### Separación entre pantalla y datos

En `App.kt`:

```kotlin
val eventState = remember { EventStateHolder(FakeEventRepository()) }
EventConsoleScreen(state = eventState)
```

`App` conecta la pantalla con el estado. Se inyecta `FakeEventRepository` para que el ejemplo pueda cambiar a un repositorio HTTP sin poner la lógica de red en la interfaz.

En `EventRepository.kt`:

```kotlin
interface EventRepository {
    fun simulateRoundTrip(origin: EventOrigin): List<EventLogEntry>
}
```

Esta interfaz establece qué necesita la presentación. La implementación actual simula; en la integración, una implementación remota realizará las solicitudes acordadas con el backend.

### Identificadores del flujo

En `FakeEventRepository.kt` se crean identificadores diferentes por recorrido:

```kotlin
val requestId = "req-${number.toString().padStart(3, '0')}"
val eventId = "evt-${number.toString().padStart(3, '0')}"
```

En un sistema real el servidor debería devolver o aceptar IDs con reglas acordadas. Aquí solo permiten reconocer visualmente qué registros pertenecen a la misma operación.

### Actualización de la interfaz

En `EventStateHolder.kt`:

```kotlin
val logs = mutableStateListOf<EventLogEntry>()

fun startFromApp() {
    logs.addAll(eventRepository.simulateRoundTrip(EventOrigin.APP))
}
```

La lista observable de Compose se actualiza al agregar los registros, y la pantalla se redibuja con el nuevo contenido.

## Qué significa (y qué no significa) el progreso

El avance implementa una pantalla funcional para iniciar ambas simulaciones, un modelo de logs correlacionados y una capa separada para reemplazar la fuente de datos. Esto permite demostrar el recorrido esperado de la app.

No incluye servidor, API, cifrado/transporte real, autenticación, cliente HTTPS, SSE/polling ni cambios en la plataforma web. Por tanto, “50 %” solo describe un hito aproximado del prototipo móvil, si el docente acepta ese alcance; no significa que la integración total esté completada a la mitad. El porcentaje formal depende de la rúbrica del curso.

## Próximo paso técnico

Conseguir acceso a la plataforma web y al backend; acordar una acción de demostración y su contrato (endpoint HTTPS, autenticación, solicitud, respuesta, evento, confirmación y errores); decidir cómo recibe eventos cada cliente mediante una opción HTTP como SSE o polling; y entonces sustituir `FakeEventRepository` por la implementación de red y probar ambos sentidos con logs reales.

El análisis técnico y el mapa de archivos están en [ANALISIS_HTTPS_EVENTOS.md](./ANALISIS_HTTPS_EVENTOS.md). El resumen completo del cambio está en [AVANCE_PROTO_MENSAJERIA_50.md](./AVANCE_PROTO_MENSAJERIA_50.md).

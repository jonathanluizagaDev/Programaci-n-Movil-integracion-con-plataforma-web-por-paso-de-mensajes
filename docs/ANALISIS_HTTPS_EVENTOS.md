# Análisis de implementación: HTTPS, respuestas y eventos

**Fecha:** 2026-11-15  
**Estado:** diseño preliminar; requiere conocer el backend y la plataforma web oficiales  
**Repositorio inspeccionado:** repositorio oficial, clonado localmente en la rama `feature/https-eventos-prototipo`; su contenido inicial estaba vacío y se importó el proyecto móvil desde la descarga ZIP previa.

## 1. Requisito entendido

La integración debe permitir iniciar un evento desde cualquiera de los dos lados:

- desde la app Android/iOS hacia la plataforma web;
- desde la plataforma web hacia la app Android/iOS.

El cliente que inicia envía una solicitud HTTPS y recibe una respuesta HTTP. El servidor registra la solicitud, propaga un evento al otro cliente y recibe su confirmación. Los logs deben permitir seguir el recorrido completo y relacionar sus pasos mediante identificadores.

**No se usará WebSocket.** La entrega de eventos debe usar un mecanismo basado en HTTP. SSE es una opción para enviar eventos desde el servidor hacia un cliente; las órdenes y confirmaciones se envían en solicitudes HTTPS independientes. SSE es unidireccional según [MDN](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events). Si no es adecuado para el backend o para el cliente Kotlin Multiplatform, se puede evaluar polling HTTP.

Los ejemplos del docente (activar modo noche, iniciar una actividad de realidad virtual, medir distancia) sirven para ilustrar el patrón. **Todavía no se decidió cuál será la acción concreta del proyecto.**

## 2. Arquitectura propuesta

```text
App móvil ── HTTPS command/response ──┐
App móvil ◀── stream HTTP de eventos ──┤
                                      ├── Backend/API ── registro/almacenamiento
Web ─────── HTTPS command/response ───┤
Web ◀────── stream HTTP de eventos ────┘
```

El backend es el intermediario y la fuente de verdad. No se recomienda que la app llame directamente al navegador, ni que se modifique el navegador completo. La acción solo debe afectar a la plataforma y a los clientes autorizados.

## 3. Recorrido de cada operación

1. Un usuario inicia una acción en la app o en la web.
2. El cliente envía `POST` por HTTPS con un identificador de solicitud, el tipo de evento y los datos necesarios.
3. El backend autentica y autoriza al usuario, valida el contenido, registra la orden y responde con un estado inicial, por ejemplo `accepted` o `rejected`.
4. Si la acepta, el backend publica un evento para el otro cliente (o para los destinatarios autorizados).
5. El receptor procesa el evento y envía por HTTPS una confirmación `processed` o `failed`.
6. El backend registra la confirmación y puede enviar un evento de resultado al cliente que inició la acción.
7. La app y la web muestran el estado final y el log asociado al mismo `requestId`/`eventId`.

La respuesta HTTP inicial significa que el backend recibió/aceptó la orden; no necesariamente que el otro cliente ya la aplicó. La confirmación de procesamiento es un paso posterior.

## 4. Contrato de mensajes propuesto (provisional)

Los nombres y campos siguientes son ejemplos para acordar con el backend; no están implementados ni deben considerarse API existente.

### Solicitud HTTPS

```http
POST /api/events HTTP/1.1
Content-Type: application/json
Authorization: Bearer <access-token>

{
  "requestId": "req-123",
  "type": "activity.start",
  "origin": "mobile",
  "target": "web-session",
  "payload": { "activity": "demo" }
}
```

### Respuesta inmediata al iniciador

```json
{
  "requestId": "req-123",
  "eventId": "evt-456",
  "status": "accepted"
}
```

### Evento que recibe el cliente destino

```json
{
  "eventId": "evt-456",
  "requestId": "req-123",
  "type": "activity.start",
  "origin": "mobile",
  "payload": { "activity": "demo" }
}
```

### Confirmación del receptor

```http
POST /api/events/evt-456/ack HTTP/1.1
Content-Type: application/json

{
  "status": "processed",
  "clientId": "web-session-9"
}
```

Para el ejemplo de distancia, primero se debe decidir si la app mide con sensores, recibe medidas de un dispositivo VR, o usa una cifra de demostración. También hay que definir si manda cada medición, lotes periódicos o un resumen al terminar.

## 5. Registro y observabilidad

Para que la demostración muestre claramente el request/response y los eventos, el log del servidor debería registrar como mínimo:

| Etapa | Ejemplo de registro |
|---|---|
| Solicitud recibida | `request_received`, `requestId`, origen, usuario/cliente |
| Validación | `validated` o `rejected`, motivo si se rechaza |
| Respuesta al iniciador | `response_sent`, estado HTTP y estado de negocio |
| Publicación | `event_published`, `eventId`, destinatarios |
| Entrega/procesamiento | `event_processed` o `event_failed`, cliente receptor |
| Cierre | `completed` o `failed`, duración y correlación |

No registrar tokens, contraseñas u otros secretos. En UI, mostrar una vista de actividad con solicitud, respuesta del servidor, evento recibido y confirmación, usando los IDs para correlacionarlos.

## 6. Eventos HTTP: SSE o polling

### SSE como primera opción a evaluar

- Mantiene un stream HTTP por el que el servidor envía eventos a los clientes suscritos.
- El navegador puede procesar eventos nombrados, por ejemplo `activity.started`.
- El canal es del servidor hacia el cliente; para iniciar acciones o confirmar recepción se usan `POST` HTTPS aparte.
- Hay que verificar cómo se autenticará y mantendrá el stream en la app Android/iOS, y qué hace el backend ante reconexiones.

### Polling como alternativa sencilla

- El cliente consulta periódicamente si hay eventos pendientes.
- Usa HTTPS estándar y puede ser más simple para una primera demo.
- Añade demora y solicitudes repetidas; se debe definir intervalo, cursor de último evento y cómo evitar duplicados.

La selección depende de las capacidades del backend existente y del requisito de rapidez. No fijar SSE ni polling hasta revisar el servidor.

## 7. Archivos del cliente que probablemente cambiarán

Rutas basadas en el proyecto móvil importado; verificar backend y web en el resto del repositorio o en repositorios relacionados.

| Archivo actual/nuevo | Trabajo previsto |
|---|---|
| `shared/src/commonMain/kotlin/org/ucb/appp1/App.kt` | Mantener como raíz compartida y conectar la nueva pantalla de acciones/eventos. |
| `presentation/EventConsoleScreen.kt` | Consola actual: iniciar desde app o simular origen web y mostrar cada paso del flujo. |
| `presentation/EventStateHolder.kt` | Estado observable y acciones de la consola actual; adaptar para estados reales al conectar red. |
| `model/EventLogEntry.kt` | Modelo actual para registros simulados; ampliar con DTOs según el contrato real. |
| `domain/EventRepository.kt` | Interfaz actual que separa la UI del origen del flujo de eventos. |
| `data/FakeEventRepository.kt` | Simula solicitud, respuesta, entrega y confirmación; sustituir por implementación remota. |
| Próximo `data/remote/EventsApi.kt` | Enviar acciones por HTTPS y leer la respuesta inmediata del backend. |
| Próximo `data/remote/EventStreamClient.kt` | Suscribirse a SSE o implementar polling si eso define el backend. |
| `shared/build.gradle.kts` y `gradle/libs.versions.toml` | Añadir cliente HTTP/JSON y, si se necesita, librería de streaming compatible con Android e iOS. |
| `androidApp/src/main/AndroidManifest.xml` | Añadir `INTERNET` al conectar la app a la red. En iOS, HTTPS debería evitar excepciones de transporte inseguro. |

`MainActivity.kt`, `ContentView.swift` y `MainViewController.kt` probablemente se mantienen como entradas de plataforma; la lógica de eventos permanece en `shared` siempre que las librerías elegidas lo permitan.

## 8. Trabajo fuera del repositorio móvil

El repositorio importado contiene el cliente móvil; no se confirmó que incluya el backend o la web. Para realizar el flujo completo hace falta acceso al código o documentación de:

- backend/API HTTPS;
- interfaz web que se suscribirá a los eventos;
- almacenamiento/log del servidor, si ya existe;
- autenticación y autorización;
- entorno de desarrollo accesible para app y navegador.

Si el backend o la web están en otro repositorio, sus cambios se implementan allí; no se resuelven únicamente editando `shared`.

## 9. Fases y criterios para demostrarlo

### Fase 1: acordar y documentar contrato

Elegir una acción de demo, origen/destino, endpoint, autenticación, payload, respuesta inmediata, confirmación, nombres de evento y política de reconexión.

**Criterio de salida:** app, web y backend usan el mismo contrato escrito.

### Fase 2: implementar backend y logs

Recibir acciones de ambos clientes; validar; emitir response; registrar request/event/ack; publicar al destino; evitar duplicados por `requestId`.

**Criterio de salida:** requests válidos producen un evento correlacionado; inválidos devuelven error claro y quedan registrados sin filtrar secretos.

### Fase 3: conectar la web

La web puede iniciar la acción, recibir el response, escuchar eventos, procesarlos y confirmar el resultado.

**Criterio de salida:** el log y la UI web muestran el resultado con `requestId`/`eventId`.

### Fase 4: conectar la app Kotlin

Implementar cliente HTTPS en `shared`, recepción de eventos según transporte seleccionado, confirmación, estado de UI y manejo de errores/reintentos.

**Criterio de salida:** la app inicia y recibe eventos sin depender de `FakeEventRepository`.

### Fase 5: prueba de ida y vuelta

- Iniciar un evento desde la app y confirmar que la web lo procesa.
- Iniciar un evento desde la web y confirmar que la app lo procesa.
- Verificar respuestas inmediatas y confirmaciones posteriores.
- Verificar el recorrido en logs correlacionados.
- Probar evento inválido, usuario no autorizado, cliente desconectado y reconexión.

**Criterio final:** ambos sentidos funcionan sobre HTTPS y se puede demostrar la orden, respuesta, entrega y confirmación con evidencia.

### Estimación preliminar

| Escenario | Esfuerzo aproximado | Supuesto |
|---|---:|---|
| Demo local app + web + backend mínimo | 2–4 jornadas | Se elige una acción sencilla y se usa infraestructura de desarrollo. |
| Integración de ida y vuelta con backend existente | 4–8 jornadas | Hay endpoint/credenciales, formato acordable y soporte de eventos o polling disponible. |
| Construir o modificar backend y web además de la app | 8–15+ jornadas | Incluye autorización, almacenamiento/logs, entrega de eventos, confirmaciones y manejo de fallos. |

Son rangos de planificación, no una fecha comprometida. Cambian si se añaden datos de sensores/VR, despliegue, notificaciones, persistencia avanzada o requisitos de seguridad adicionales.

## 10. Qué está hecho y qué no

- **Prototipo anterior:** pantalla Compose de chat local, retirada al reemplazarla por la consola de eventos.
- **Hecho ahora:** consola Compose que simula eventos originados desde app o web y registra request/response/entrega/ack en memoria. El proyecto está en `feature/https-eventos-prototipo`.
- **No hecho:** llamadas HTTPS, eventos remotos, SSE/polling, backend, autenticación, logs de servidor y comunicación real con la web.
- **Importante:** la consola de eventos actual tampoco es integración end-to-end; no hace solicitudes HTTPS ni recibe eventos remotos. Sirve para presentar el recorrido esperado mientras se obtiene el contrato real.
- **Historial:** el proyecto original se importó desde una descarga ZIP sin historial Git; el repositorio oficial recibió esta copia como base de trabajo.

## 11. Información necesaria para empezar la integración real

1. Ruta local o acceso al repositorio oficial móvil, web y backend.
2. Endpoint HTTPS de desarrollo y cómo se autentica.
3. Confirmación de si el backend soporta SSE; si no, si el docente acepta polling.
4. Acción/evento que se demostrará y qué respuesta debe producir.
5. Usuarios/clientes que recibirán el evento y cómo se identifican.
6. Si hay mediciones, origen del dato, frecuencia y unidad.
7. Qué campos se deben mostrar en el log.

## Referencias

- [MDN: Server-Sent Events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events)
- [Ktor Client: crear una aplicación cliente](https://ktor.io/docs/client-create-new-application.html)
- [Ktor Client: plataformas compatibles](https://ktor.io/docs/client-supported-platforms.html)

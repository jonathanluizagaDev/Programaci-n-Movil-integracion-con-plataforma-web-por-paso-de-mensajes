# Integración de la app móvil con una plataforma web mediante mensajes

**Estado:** investigación y prototipo cliente local; HTTPS con respuestas y eventos, sin WebSocket  
**Última actualización:** 2026-11-15

## Repositorio de trabajo

El repositorio oficial es [Programaci-n-Movil-integracion-con-plataforma-web-por-paso-de-mensajes](https://github.com/jonathanluizagaDev/Programaci-n-Movil-integracion-con-plataforma-web-por-paso-de-mensajes). La copia de trabajo está en la carpeta local del mismo nombre y la rama actual es `feature/https-eventos-prototipo`.

El remoto estaba vacío al conectarse. Para poblarlo se importó el proyecto Kotlin Multiplatform y los documentos desde la copia `ucbp1_project`, descargada previamente como ZIP. La importación no conserva historial Git del ZIP original.

La app ahora tiene una consola local de eventos simulados que muestra ambos sentidos del flujo y sus logs. El detalle de archivos, líneas y comportamiento está en [AVANCE_PROTO_MENSAJERIA_50.md](./AVANCE_PROTO_MENSAJERIA_50.md). Todavía no es una integración real con la plataforma.

El análisis actualizado del flujo HTTPS/eventos, archivos, fases y criterios de demostración está en [ANALISIS_HTTPS_EVENTOS.md](./ANALISIS_HTTPS_EVENTOS.md).

## Objetivo

Integrar una app Kotlin con una plataforma web mediante HTTPS y eventos que circulen entre ambos lados por el servidor, dejando evidencia del recorrido en logs. Un evento debe poder iniciarse desde la app o desde la web. El origen recibe una respuesta a su solicitud y el otro lado recibe el evento; la confirmación puede volver por HTTPS. Los ejemplos del docente (actividad de realidad virtual, distancia o modo noche) ilustran el patrón, pero no definen todavía la acción concreta.

## Estado actual del proyecto

- El proyecto usa Kotlin Multiplatform y Compose Multiplatform.
- Los destinos configurados son Android e iOS.
- `shared` contiene el código Kotlin compartido.
- La app tiene una consola de eventos con datos simulados, pero no tiene cliente de red ni contrato acordado con el backend.
- No se ha confirmado que exista un backend o API para la plataforma web.

## Diferencia entre el prototipo actual y el objetivo final

La consola de eventos implementada demuestra localmente un recorrido en ambos sentidos. La función final debe demostrar un evento real: un cliente inicia una acción, el servidor responde y la registra, el otro cliente procesa el evento y confirma; los logs muestran los identificadores relacionados. Los ejemplos del docente (iniciar una actividad, medir movimiento o cambiar una preferencia) ilustran el flujo; todavía no se ha elegido la acción concreta.

Si el ejemplo elegido es una preferencia visual, el cambio aplicaría a las páginas/sesiones de nuestra plataforma web conectadas y autorizadas. Una app o página web no puede cambiar la interfaz del navegador completo ni modificar sitios web ajenos. Si se elige medir movimiento o realidad virtual, esos datos requerirían además el sensor o dispositivo correspondiente; esa función todavía no está definida.

## Arquitectura propuesta

La app móvil y la plataforma web pueden iniciar órdenes mediante HTTPS. El servidor valida y registra cada solicitud, responde al iniciador y publica el evento hacia el otro lado. El receptor procesa el evento y puede confirmar el resultado al servidor, que lo registra y lo comunica al iniciador. Si se requiere historial, el servidor almacena el estado y los eventos.

```text
App Android/iOS ── HTTPS request/response ──┐
App Android/iOS ◀── HTTPS event stream ─────┤
                                            ├── Servidor/API ── log/almacenamiento
Web/navegador ── HTTPS request/response ────┤
Web/navegador ◀── HTTPS event stream ────────┘
```

La app no debería depender de la interfaz visual de la página web. La integración se realiza mediante una interfaz de servidor documentada y compartida por ambas aplicaciones.

## Flujo elegido: HTTPS, respuestas y eventos

**Decisión actual:** no se usará WebSocket. Las órdenes y confirmaciones viajarán como solicitudes HTTPS con respuesta HTTP. Para notificar eventos a los clientes se elegirá un mecanismo basado en HTTP (SSE u otra alternativa que confirme el backend).

1. El usuario inicia una acción en la app o en la web.
2. El cliente origen envía por HTTPS una solicitud con `requestId`, tipo de evento y datos.
3. El servidor valida y registra la solicitud, y devuelve al origen una respuesta HTTP (`accepted` o un error) con identificadores correlacionables.
4. El servidor entrega el evento al otro cliente por su canal de eventos HTTPS.
5. El cliente receptor procesa el evento y envía una confirmación/resultado por HTTPS.
6. El servidor registra esa respuesta y notifica el resultado al iniciador, para que ambos lados puedan mostrar el estado del recorrido.

Una opción para entregar eventos a los navegadores es **Server-Sent Events (SSE)**: mantiene una conexión HTTP por la que el servidor envía eventos al navegador. SSE es unidireccional; por eso la orden de la app y la confirmación del navegador viajan en solicitudes HTTPS separadas. MDN documenta esta limitación y el formato `text/event-stream` ([guía de Server-Sent Events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events)). La tecnología final del canal de eventos debe confirmarse con el backend.

### Ejemplo ilustrativo de orden, respuesta y evento

La app o la web puede iniciar una solicitud como:

```http
POST /api/events HTTP/1.1
Content-Type: application/json
Authorization: Bearer <token>

{"type":"activity.start","requestId":"req-123","payload":{"activity":"demo"}}
```

El servidor responde inmediatamente al cliente que inició la solicitud:

```json
{
  "requestId": "req-123",
  "status": "accepted",
  "eventId": "evt-456"
}
```

El otro cliente recibe el evento (por ejemplo, si la app lo inició, lo recibe la web; si la web lo inició, lo recibe la app):

```text
event: activity.started
id: evt-456
data: {"requestId":"req-123","status":"started"}
```

El receptor confirma el procesamiento mediante otra solicitud HTTPS con el `eventId` y el resultado. El servidor puede emitir luego un evento de resultado al iniciador. Rutas y campos son ejemplos, no un contrato implementado. Si la actividad incluye medición (por ejemplo, distancia), hay que acordar cómo enviar esas mediciones y si se transmiten periódicamente o al finalizar.

## Plan de trabajo propuesto

1. Revisar la plataforma web y localizar el servidor/API que recibe las preferencias.
2. Elegir la acción/evento de demostración y acordar endpoint HTTPS, autenticación, datos de la orden, respuesta y formato/nombre del evento.
3. Confirmar cómo reciben eventos los navegadores (SSE, polling u otra opción HTTP).
4. Implementar en el backend autorización, registro, almacenamiento y publicación de la orden.
5. Implementar en la app el cliente HTTPS y reemplazar `FakeEventRepository`.
6. Implementar en la web la suscripción al evento, aplicar el tema y enviar confirmación.
7. Probar la cadena completa y revisar los logs de orden, evento, entrega y confirmación.

## Estimación inicial y definición de 50 %

La estimación es preliminar y representa esfuerzo de desarrollo, no tiempo calendario. Cambiará cuando conozcamos la plataforma, el backend y el protocolo.

### Hito aproximado para el prototipo en este repositorio

Consideraremos que el prototipo llega aproximadamente al 50 % cuando:

- exista una capa de eventos en `shared`, separada de la interfaz;
- existan modelos de logs correlacionados y un flujo ilustrativo;
- la app muestre eventos iniciados desde ambos orígenes simulados;
- estén documentados los puntos que habrá que sustituir o configurar para conectar el backend real.

Este hito valida estructura y flujo de interfaz, pero no cuenta como integración funcional con la plataforma web. El adaptador simulado no entrega eventos a la web ni valida el protocolo real. El alcance y sus límites están detallados en [AVANCE_PROTO_MENSAJERIA_50.md](./AVANCE_PROTO_MENSAJERIA_50.md); el porcentaje formal depende de la rúbrica del docente.

### Esfuerzo aproximado

| Escenario | Estimación inicial | Supuestos |
|---|---:|---|
| Prototipo cliente hasta el hito anterior | 1–2 jornadas de desarrollo | Se usa un adaptador simulado y no se crea backend. |
| Integración real bidireccional si el backend ya existe | 4–8 jornadas de desarrollo | API HTTPS accesible, contrato estable y transporte de eventos disponible. |
| Si además hay que construir/modificar backend y web | 8–15+ jornadas de desarrollo | Depende de autorización, persistencia/logs, entrega de eventos, confirmaciones y despliegue. |

Las cifras son rangos de planificación para un prototipo inicial. No incluyen esperas por accesos, cambios de requisitos, revisión de infraestructura ni publicación en tiendas.

### Desglose orientativo

1. Descubrir plataforma, backend y requisitos: 2–4 horas.
2. Acordar contrato de órdenes, respuestas, eventos y autenticación: 2–4 horas.
3. Diseñar e implementar cambios de backend/logs: 1–3 jornadas, según el estado actual.
4. Conectar la web como iniciador y receptor de eventos: 1–2 jornadas.
5. Conectar la app Android/iOS al request-response y stream/eventos: 1–2 jornadas.
6. Verificar el flujo de ida y vuelta y documentarlo: 4–8 horas.

El orden de magnitud para el prototipo es de 10–20 horas de trabajo. La integración real queda condicionada a conocer el backend.

## Decisiones pendientes

- ¿Qué plataforma web se integrará?
- ¿Ya existe backend/API? ¿Quién lo administra y dónde está su documentación?
- **Decidido por el docente:** HTTPS con petición/respuesta y eventos iniciables desde la app o desde la web.
- ¿Los eventos para navegadores se entregarán con SSE, polling u otra tecnología HTTP?
- ¿Qué evento de demostración se implementará (orden simple, preferencia, medición de actividad, etc.)?
- ¿Se requiere historial de eventos, entrega a clientes que se conecten después y notificaciones push?
- ¿Cuáles son las URLs de la API y del canal de eventos, el esquema de datos y el mecanismo de autenticación?

## Mapa preliminar de archivos

Este mapa se basa en el proyecto móvil importado al repositorio oficial. Las rutas de backend y web deben verificarse cuando se revise si esos componentes existen en ramas o repositorios separados.

### Archivos existentes que probablemente se modificarían

| Archivo | Trabajo probable |
|---|---|
| `shared/src/commonMain/kotlin/org/ucb/appp1/App.kt` | Conectar la raíz compartida a la consola de eventos. |
| `shared/build.gradle.kts` | Agregar dependencias de cliente HTTPS y serialización; para SSE, elegir una implementación compatible con Android e iOS. |
| `gradle/libs.versions.toml` | Centralizar versiones y alias de las librerías añadidas. |
| `androidApp/src/main/AndroidManifest.xml` | Añadir permiso `INTERNET` para llamadas de red. En producción se usará HTTPS. |
| `README.md` | Explicar configuración local, dirección del backend y pasos para ejecutar la app integrada, sin incluir secretos. |

### Archivos existentes que en principio seguirían siendo entradas de plataforma

| Archivo | Uso esperado |
|---|---|
| `androidApp/src/main/kotlin/org/ucb/appp1/MainActivity.kt` | Mantener como punto de entrada Android que muestra `App()`. Solo cambiar si hace falta inyección/configuración específica de Android. |
| `iosApp/iosApp/ContentView.swift` | Mantener el puente SwiftUI hacia Compose. En general, no debería contener la lógica de mensajes. |
| `shared/src/iosMain/kotlin/org/ucb/appp1/MainViewController.kt` | Mantener como punto de entrada Compose para iOS, salvo que una integración nativa lo requiera. |
| `shared/src/commonMain/kotlin/org/ucb/appp1/Platform.kt` y sus implementaciones `androidMain`/`iosMain` | No deberían cargar la lógica del protocolo; solo se tocarían si aparece una necesidad realmente dependiente de la plataforma. |

### Archivos nuevos recomendados

Las responsabilidades se pueden separar en paquetes bajo `shared/src/commonMain/kotlin/org/ucb/appp1/`:

- `model/EventLogEntry.kt`: origen, etapa y datos correlacionados de cada log.
- `model/MessageEnvelope.kt`: formato JSON de entrada/salida acordado con el servidor.
- `data/remote/PreferencesApi.kt`: envío HTTPS de órdenes y lectura de respuestas.
- `data/remote/EventStreamClient.kt`: suscripción a eventos si el backend confirma SSE u otro stream HTTP.
- `data/repository/HttpsEventRepository.kt`: implementación que adapta el protocolo al dominio compartido.
- `domain/EventRepository.kt`: interfaz que desacopla la UI del transporte; actualmente se usa un adaptador simulado.
- `presentation/EventStateHolder.kt`: estado y acciones de la consola.
- `presentation/EventConsoleScreen.kt`: pantalla Compose con controles y registro de eventos.
- `commonMain`/`androidMain`/`iosMain` de configuración de `HttpClient`, si los motores seleccionados exigen configuración por plataforma.

Para la integración real habrá que añadir `data/remote/EventsApi.kt`, un receptor SSE o polling y modelos del contrato que defina el backend. La implementación depende de ese contrato.

### Posible trabajo fuera de este repositorio

Si todavía no existe backend, habrá que crear un servicio separado o un módulo servidor con autenticación, almacenamiento, API y distribución de mensajes. La carpeta `shared` es código de cliente compartido entre Android e iOS; no sustituye al servidor que debe conectar la app con la plataforma web.

## Referencias técnicas

- [MDN: uso de Server-Sent Events](https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events)
- [Ktor: desarrollo full-stack con Kotlin Multiplatform](https://ktor.io/docs/full-stack-development-with-kotlin-multiplatform.html)
- [Ktor: motores de cliente](https://ktor.io/docs/client-engines.html)


# Avance del prototipo de mensajería

**Fecha:** 2026-11-13  
**Alcance:** prototipo de la app móvil, importado al repositorio oficial en `feature/https-eventos-prototipo`  
**Estado:** UI y flujo local implementados con datos simulados; no conectado a la plataforma web.

## Qué se implementó

La plantilla de bienvenida fue reemplazada por una pantalla de conversación compartida entre Android e iOS. La pantalla muestra mensajes de ejemplo, permite escribir y enviar un mensaje, agrega el mensaje a la conversación y desplaza la lista al más reciente.

Los datos vienen de `FakeMessageRepository`. Por eso los mensajes solo viven en memoria durante esta sesión y no llegan a ningún servidor. El rótulo de la pantalla deja esta limitación visible.

Este trabajo cubre aproximadamente el hito de **50 % del prototipo cliente** acordado. No equivale a 50 % de integración completa: aún no hay API HTTPS, eventos remotos, autenticación, persistencia ni conexión con la web.

## Archivos modificados

### `shared/src/commonMain/kotlin/org/ucb/appp1/App.kt` — líneas 1–16

Se reemplazó por completo la pantalla de bienvenida del template.

- Líneas 3–8 importan Compose y los componentes del chat.
- Líneas 10–16 crean el tema, instancian el estado con `FakeMessageRepository` mediante `remember` y muestran `ChatScreen`.

**Motivo y efecto:** `App()` es el punto Compose común que ya se utiliza en Android e iOS. Al conectarlo a la pantalla nueva, ambas apps muestran el mismo prototipo. `remember` conserva el estado mientras esta composición permanezca activa.

### `shared/src/commonMain/kotlin/org/ucb/appp1/presentation/ChatScreen.kt` — líneas 1–119 (archivo nuevo)

- Líneas 30–38 crean el estado de scroll y llevan la lista al último mensaje cuando cambia su cantidad.
- Líneas 40–66 construyen el encabezado y la lista desplazable de mensajes, respetando el área segura de pantalla.
- Líneas 45–51 indican que es una demostración sin servidor conectado.
- Líneas 68–86 muestran el campo de texto y el botón Enviar, deshabilitado si el texto está vacío.
- Líneas 90–119 dibujan cada burbuja, alineando y coloreando distinto los mensajes propios y los recibidos.

**Motivo y efecto:** permite demostrar el flujo visible de conversación y deja separada la UI de la lógica de mensajes.

### `shared/src/commonMain/kotlin/org/ucb/appp1/presentation/ChatStateHolder.kt` — líneas 1–31 (archivo nuevo)

- Líneas 10–15 cargan los mensajes iniciales en una lista observable de Compose.
- Líneas 17–22 mantienen y actualizan el texto del campo.
- Líneas 24–30 recortan espacios, ignoran mensajes vacíos, agregan el mensaje devuelto por el repositorio y limpian el campo.

**Motivo y efecto:** concentra el estado y las acciones de pantalla para no poner esa lógica dentro del composable. Es un estado simple de demostración, no un `ViewModel` ligado al ciclo de vida.

### `shared/src/commonMain/kotlin/org/ucb/appp1/model/ChatMessage.kt` — líneas 1–13 (archivo nuevo)

- Líneas 3–6 definen si el emisor es el usuario actual (`ME`) o la otra parte (`OTHER`).
- Líneas 8–13 definen el identificador, texto, emisor y etiqueta de hora.

**Motivo y efecto:** ofrece una estructura común que la pantalla puede presentar y que luego se podrá adaptar al contrato JSON real.

### `shared/src/commonMain/kotlin/org/ucb/appp1/domain/MessageRepository.kt` — líneas 1–9 (archivo nuevo)

- Líneas 5–9 declaran operaciones para obtener mensajes iniciales y enviar un mensaje.

**Motivo y efecto:** desacopla la pantalla del origen de datos. Más adelante se puede cambiar el repositorio simulado por uno conectado a la API sin rehacer la interfaz.

### `shared/src/commonMain/kotlin/org/ucb/appp1/data/FakeMessageRepository.kt` — líneas 1–31 (archivo nuevo)

- Líneas 7–23 proveen dos mensajes iniciales claramente identificados como demostración.
- Líneas 25–31 crean una respuesta local con emisor `ME`, identificador incremental y etiqueta “Ahora”.

**Motivo y efecto:** permite recorrer la pantalla y demostrar el envío sin depender de una plataforma web todavía desconocida. No simula la entrega a otro usuario o servidor.

## Qué archivos no cambiaron

- `androidApp/src/main/kotlin/org/ucb/appp1/MainActivity.kt`: sigue abriendo `App()`.
- `iosApp/iosApp/ContentView.swift` y `shared/src/iosMain/kotlin/org/ucb/appp1/MainViewController.kt`: siguen sirviendo como entrada y puente hacia Compose.
- `shared/build.gradle.kts`, `gradle/libs.versions.toml` y `androidApp/src/main/AndroidManifest.xml`: no se añadieron dependencias de red ni permisos porque esta etapa usa datos locales.

## Trabajo pendiente para la integración real

1. Obtener el checkout y revisar el contenido del repositorio oficial.
2. Identificar el backend HTTPS, sus endpoints, autenticación y formato de órdenes/respuestas/eventos.
3. Definir errores, confirmación de entrega, reconexión e historial.
4. Añadir el cliente HTTPS y la tecnología de recepción de eventos que defina el backend, con soporte para Android e iOS.
5. Implementar el repositorio real y reemplazar `FakeMessageRepository` en la configuración de la app.
6. Gestionar las claves/tokens de forma segura y configurar permisos/red por plataforma según corresponda.

## Nueva dirección indicada por el docente

El transporte requerido es HTTPS con petición/respuesta y eventos iniciables tanto desde la app como desde la web. **No se usará WebSocket.** El cliente que inicia envía la solicitud y recibe una respuesta; el otro lado recibe el evento, lo procesa y puede devolver una confirmación, que queda registrada y se comunica al iniciador. El docente dio ejemplos como modo noche o una actividad de realidad virtual con medición de distancia; son ejemplos del patrón y no requisitos confirmados. El chat actual es una demostración local y no representa todavía la función final. La siguiente UI deberá enfocarse en la acción/evento que se elija y en mostrar la solicitud, la respuesta y los logs.

Para eventos unidireccionales servidor→cliente se está considerando SSE sobre HTTPS; falta confirmarlo con el backend. Si se elige medición de movimiento/realidad virtual, también habrá que definir el dispositivo/sensor y el formato de esos datos.

## Limitaciones de esta entrega

- El repositorio oficial se creó vacío; el proyecto móvil se importó desde la copia descargada `ucbp1_project`, que no conservaba historial Git.
- El trabajo queda organizado en la rama `feature/https-eventos-prototipo`.
- No se ejecutaron pruebas automatizadas ni una compilación adicional en esta entrega.

## Comprobación manual en emulador

El usuario ejecutó la app en el emulador Android API 36 y confirmó visualmente que:

- se muestran los dos mensajes iniciales;
- al escribir y enviar `Hola desde el emulador`, aparece una burbuja a la derecha con la etiqueta `Ahora`;
- el campo de texto queda vacío después del envío.

Esta comprobación valida el flujo local de demostración, no la comunicación con un servidor.

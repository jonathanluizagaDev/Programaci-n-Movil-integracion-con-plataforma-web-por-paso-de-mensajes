# Guía para explicar el prototipo de mensajería

Esta guía describe el prototipo móvil importado desde la copia local anterior al repositorio oficial. El prototipo fue ejecutado en el emulador Android y se comprobó que muestra los mensajes iniciales y agrega el mensaje escrito a la conversación. El docente indicó después que la integración final debe usar HTTPS con respuestas y eventos; esa función aún no está implementada.

> **Alcance:** los mensajes son locales y simulados. Esta versión todavía no se conecta a la plataforma web ni a un servidor.

## Resumen para presentar

Se reemplazó la pantalla inicial de ejemplo por una pantalla de conversación hecha con Compose Multiplatform. Se separó la interfaz, el estado, el modelo de datos y el origen de los mensajes. Actualmente se usa un repositorio simulado para demostrar el flujo de envío sin depender de un backend. Esta organización sirve como base, pero la interfaz final deberá adaptarse para enviar órdenes/preferencias por HTTPS y mostrar respuestas y eventos del servidor.

## 1. Archivo modificado: `shared/src/commonMain/kotlin/org/ucb/appp1/App.kt`

### Código

```kotlin
package org.ucb.appp1

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.ucb.appp1.data.FakeMessageRepository
import org.ucb.appp1.presentation.ChatScreen
import org.ucb.appp1.presentation.ChatStateHolder

@Composable
fun App() {
    MaterialTheme {
        val chatState = remember { ChatStateHolder(FakeMessageRepository()) }
        ChatScreen(state = chatState)
    }
}
```

### Explicación

- Los imports conectan la función principal con Compose, la pantalla, el estado y el repositorio de demostración.
- `@Composable` indica que `App` construye interfaz de usuario con Compose.
- `MaterialTheme` aplica los colores y estilos Material a la pantalla.
- `remember` conserva el objeto `ChatStateHolder` mientras la pantalla siga en composición; así el texto y los mensajes no se reinician en cada actualización visual.
- `FakeMessageRepository()` entrega los datos de ejemplo. `ChatScreen(state = chatState)` muestra la interfaz y recibe el estado.

**Por qué se modificó:** `App()` era la entrada común que mostraba la pantalla de bienvenida del template. Ahora conecta Android e iOS con el prototipo del chat.

## 2. Archivo añadido: `shared/src/commonMain/kotlin/org/ucb/appp1/presentation/ChatScreen.kt`

### Código

```kotlin
package org.ucb.appp1.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.ucb.appp1.model.ChatMessage
import org.ucb.appp1.model.MessageSender

@Composable
fun ChatScreen(state: ChatStateHolder) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Mensajes", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Modo demostración · servidor web aún no conectado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(state.messages, key = ChatMessage::id) { message ->
                MessageBubble(message)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.inputText,
                onValueChange = state::onInputChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe un mensaje") },
                maxLines = 3,
            )
            Button(
                onClick = state::sendMessage,
                enabled = state.inputText.isNotBlank(),
            ) {
                Text("Enviar")
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isMine = message.sender == MessageSender.ME
    val bubbleColor = if (isMine) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(message.text, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = message.timeLabel,
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

### Explicación

- Los imports habilitan los elementos visuales de Compose: columnas, filas, lista desplazable, campo de texto, botón, formas, colores y márgenes.
- `ChatScreen(state)` recibe el estado desde fuera; no crea directamente el repositorio.
- `rememberLazyListState()` conserva la posición de desplazamiento de la lista.
- `LaunchedEffect(state.messages.size)` reacciona cuando cambia la cantidad de mensajes y desplaza la conversación hasta el último.
- El `Column` principal ocupa la pantalla completa. `safeContentPadding()` evita que el contenido quede bajo las barras del sistema, y `background(...)` aplica el color de fondo del tema.
- El primer `Column` muestra el título “Mensajes” y avisa que todavía se trata de una demostración local.
- `LazyColumn` presenta los mensajes en una lista que puede desplazarse. La clave `ChatMessage::id` permite identificar cada mensaje de forma estable.
- El `Row` inferior contiene el campo para escribir y el botón de envío. `onValueChange` avisa al estado cuando cambia el texto. El botón queda desactivado si solo hay espacios o el campo está vacío.
- `MessageBubble` dibuja una burbuja por mensaje. Si el emisor es `ME`, la burbuja se alinea a la derecha y usa el color principal; los mensajes `OTHER` se alinean a la izquierda y usan el color secundario.
- Cada burbuja presenta el texto y una etiqueta de hora.

**Por qué se añadió:** contiene la pantalla que demuestra la conversación y mantiene la interfaz separada del procesamiento de los mensajes.

## 3. Archivo añadido: `shared/src/commonMain/kotlin/org/ucb/appp1/presentation/ChatStateHolder.kt`

### Código

```kotlin
package org.ucb.appp1.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.ucb.appp1.domain.MessageRepository
import org.ucb.appp1.model.ChatMessage

class ChatStateHolder(
    private val messageRepository: MessageRepository,
) {
    val messages = mutableStateListOf<ChatMessage>().apply {
        addAll(messageRepository.getInitialMessages())
    }

    var inputText by mutableStateOf("")
        private set

    fun onInputChanged(value: String) {
        inputText = value
    }

    fun sendMessage() {
        val text = inputText.trim()
        if (text.isEmpty()) return

        messages.add(messageRepository.sendMessage(text))
        inputText = ""
    }
}
```

### Explicación

- `MessageRepository` se recibe como dependencia, por eso el estado no depende directamente de la clase simulada.
- `messages` es una lista observable de Compose. Se inicializa con los mensajes que devuelve el repositorio; cuando se agrega uno, Compose vuelve a dibujar la lista.
- `inputText` mantiene el contenido del campo. `private set` permite leerlo desde la pantalla, pero solo modificarlo mediante las funciones de esta clase.
- `onInputChanged` actualiza el valor del campo cuando el usuario escribe.
- `sendMessage` elimina espacios al inicio y al final. Si el texto queda vacío, no envía nada. En otro caso solicita al repositorio un mensaje, lo agrega a la lista y limpia el campo.

**Por qué se añadió:** concentra los datos y las acciones de la pantalla en un lugar separado de los composables. Es un estado simple retenido con `remember`, no un `ViewModel` del ciclo de vida.

## 4. Archivo añadido: `shared/src/commonMain/kotlin/org/ucb/appp1/model/ChatMessage.kt`

### Código

```kotlin
package org.ucb.appp1.model

enum class MessageSender {
    ME,
    OTHER,
}

data class ChatMessage(
    val id: String,
    val text: String,
    val sender: MessageSender,
    val timeLabel: String,
)
```

### Explicación

- `MessageSender` distingue un mensaje propio (`ME`) de uno recibido (`OTHER`). La pantalla utiliza esa diferencia para alinear y colorear las burbujas.
- `ChatMessage` agrupa los datos que necesita mostrar la interfaz: identificador único, texto, emisor y etiqueta de hora.
- `data class` genera funciones útiles para manejar objetos de datos, como igualdad estructural y copia.

**Por qué se añadió:** tanto la UI como el repositorio necesitan un tipo común y consistente para representar mensajes. Más adelante se puede adaptar a los campos del JSON que defina el servidor.

## 5. Archivo añadido: `shared/src/commonMain/kotlin/org/ucb/appp1/domain/MessageRepository.kt`

### Código

```kotlin
package org.ucb.appp1.domain

import org.ucb.appp1.model.ChatMessage

interface MessageRepository {
    fun getInitialMessages(): List<ChatMessage>

    fun sendMessage(text: String): ChatMessage
}
```

### Explicación

- `getInitialMessages()` define cómo obtener la conversación inicial.
- `sendMessage(text)` define cómo convertir el texto que escribe el usuario en un mensaje.
- Esta interfaz describe las operaciones, pero no decide si los datos vienen de memoria o de una implementación remota.

**Por qué se añadió:** permite reemplazar el repositorio simulado por una implementación de red sin cambiar la pantalla ni su estado.

## 6. Archivo añadido: `shared/src/commonMain/kotlin/org/ucb/appp1/data/FakeMessageRepository.kt`

### Código

```kotlin
package org.ucb.appp1.data

import org.ucb.appp1.domain.MessageRepository
import org.ucb.appp1.model.ChatMessage
import org.ucb.appp1.model.MessageSender

class FakeMessageRepository : MessageRepository {
    private var nextMessageNumber = 3

    override fun getInitialMessages(): List<ChatMessage> = listOf(
        ChatMessage(
            id = "demo-1",
            text = "Hola, este es el canal de mensajes de demostración.",
            sender = MessageSender.OTHER,
            timeLabel = "10:02",
        ),
        ChatMessage(
            id = "demo-2",
            text = "La app todavía no está conectada al servidor web.",
            sender = MessageSender.OTHER,
            timeLabel = "10:03",
        ),
    )

    override fun sendMessage(text: String): ChatMessage = ChatMessage(
        id = "demo-${nextMessageNumber++}",
        text = text,
        sender = MessageSender.ME,
        timeLabel = "Ahora",
    )
}
```

### Explicación

- `FakeMessageRepository` implementa el contrato `MessageRepository`, pero utiliza datos escritos directamente en el código.
- `getInitialMessages()` devuelve dos mensajes de ejemplo marcados como recibidos (`OTHER`).
- `nextMessageNumber` incrementa el identificador de cada nuevo mensaje enviado para que la lista tenga claves distintas.
- `sendMessage(text)` crea un objeto local con el texto ingresado, emisor `ME` y hora “Ahora”. No lo transmite a ningún servidor.

**Por qué se añadió:** permite presentar y probar el flujo visual mientras se averigua cómo funciona el backend de la plataforma web. Es una implementación temporal que luego se sustituirá por una conectada al servidor.

## Flujo completo del prototipo

1. `App` crea el repositorio simulado y el estado de pantalla.
2. `ChatStateHolder` solicita los mensajes iniciales al repositorio.
3. `ChatScreen` representa esos mensajes en una lista.
4. Al escribir, la pantalla pasa el texto al estado mediante `onInputChanged`.
5. Al pulsar Enviar, el estado valida el texto y solicita al repositorio crear un mensaje.
6. El mensaje local se agrega a la lista, la pantalla se actualiza y el campo queda vacío.

## Qué falta para conectarse a la plataforma web

El prototipo no incluye todavía conexión HTTPS, autenticación, almacenamiento ni recepción de eventos remotos. Para avanzar se necesita conocer el backend oficial, elegir la acción/evento de demostración, acordar endpoints, autenticación y formato de órdenes/respuestas/eventos, y reemplazar `FakeMessageRepository` por una implementación conectada. Si se elige modo noche, se aplicaría a la plataforma web; si se elige medir movimiento, habría que definir el sensor/dispositivo.

## Forma breve de explicarlo al docente

> “Reemplacé la pantalla inicial por un prototipo de chat en Compose Multiplatform y separé la interfaz, el estado, el modelo y el repositorio. El envío actual es simulado. La integración final demostrará eventos que pueden iniciarse tanto desde la app como desde la web, usando HTTPS y sin WebSocket: el origen envía una solicitud, recibe respuesta del servidor, el otro lado procesa el evento y devuelve una confirmación. Los logs permitirán seguir todo el recorrido. Los ejemplos de modo noche o realidad virtual ilustran el patrón; todavía debemos elegir la acción concreta y acordar el protocolo con el backend.”


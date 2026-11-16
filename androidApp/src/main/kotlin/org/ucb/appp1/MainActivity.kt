package org.ucb.appp1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private data class RemoteEvent(val eventId: String, val requestId: String, val origin: String, val theme: String, val status: String)

class MainActivity : ComponentActivity() {
    private var dark by mutableStateOf(false)
    private var connection by mutableStateOf("Conectando al servidor…")
    private var events by mutableStateOf<List<RemoteEvent>>(emptyList())
    private var executor = Executors.newSingleThreadScheduledExecutor()
    private val baseUrl = "https://10.0.2.2:8443"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Eventos HTTPS", style = MaterialTheme.typography.headlineSmall)
                        Text(connection)
                        Text("Modo actual: ${if (dark) "oscuro" else "claro"}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { sendTheme("light") }) { Text("Modo claro") }
                            Button(onClick = { sendTheme("dark") }) { Text("Modo oscuro") }
                        }
                        Text("Registro de actividad", style = MaterialTheme.typography.titleMedium)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(events, key = { it.eventId }) { event ->
                                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("${event.origin} → ${event.theme}: ${event.status}")
                                        Text("requestId: ${event.requestId}", style = MaterialTheme.typography.bodySmall)
                                        Text("eventId: ${event.eventId}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        executor = Executors.newSingleThreadScheduledExecutor()
        executor.scheduleWithFixedDelay({ refresh() }, 0, 2, TimeUnit.SECONDS)
    }

    override fun onStop() {
        executor.shutdownNow()
        super.onStop()
    }

    private fun sendTheme(theme: String) {
        executor.execute {
            try {
                request("/api/events", JSONObject().put("origin", "android").put("theme", theme))
                refresh()
            } catch (e: Exception) {
                runOnUiThread { connection = "Error al enviar: ${e.message}" }
            }
        }
    }

    private fun refresh() {
        try {
            val result = request("/api/state")
            val parsed = mutableListOf<RemoteEvent>()
            val entries = result.getJSONArray("events")
            for (i in 0 until entries.length()) {
                val e = entries.getJSONObject(i)
                val event = RemoteEvent(e.getString("eventId"), e.getString("requestId"), e.getString("origin"), e.getString("theme"), e.getString("status"))
                // La confirmación se envía después de aplicar el estado en la interfaz.
                parsed.add(event)
            }
            runOnUiThread {
                dark = result.getString("theme") == "dark"
                events = parsed.asReversed()
                connection = "Conectado por HTTPS"
                if (!executor.isShutdown) executor.execute {
                    try {
                        parsed.filter { it.origin == "web" && it.status == "accepted" }.forEach {
                            request("/api/ack", JSONObject().put("eventId", it.eventId).put("client", "android"))
                        }
                    } catch (_: Exception) { /* Se reintenta en la siguiente consulta. */ }
                }
            }
        } catch (e: Exception) {
            runOnUiThread { connection = "Sin conexión: ${e.message}" }
        }
    }

    private fun request(path: String, body: JSONObject? = null): JSONObject {
        val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 4000
            connection.readTimeout = 4000
            if (body != null) {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            if (code !in 200..299) throw IllegalStateException("HTTP $code: $text")
            return JSONObject(text)
        } finally { connection.disconnect() }
    }
}
